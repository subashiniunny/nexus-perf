package com.sonatype.nexus.perftest.controller;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.management.remote.JMXServiceURL;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class AgentPool
{
  private static final Logger log = LoggerFactory.getLogger(AgentPool.class);

  private final Queue<JMXServiceURL> urls;

  private final Collection<Agent> acquired = new CopyOnWriteArrayList<>();

  private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public AgentPool(final Collection<JMXServiceURL> urls) {
    this.urls = new ConcurrentLinkedQueue<>(checkNotNull(urls));
  }

  public Collection<Agent> acquireAll() {
    return acquire(urls.size());
  }

  public Collection<Agent> acquire(final int size) {
    log.info("Acquiring {} agents...", size);
    try {
      List<Agent> agents = executor.invokeAll(
          IntStream.range(0, size)
              .mapToObj(i -> (Callable<Agent>) () -> {
                Agent agent = new Agent(urls.poll());
                log.info("Acquired {}", agent);
                return agent;
              })
              .collect(Collectors.toList())
      ).stream()
          .map(f -> {
            try {
              return f.get();
            }
            catch (Exception e) {
              throw Throwables.propagate(e);
            }
          }).collect(Collectors.toList());

      checkState(
          size == agents.size(), "Acquired number of agents (%s) is not of expected size (%s)", agents.size(), size
      );
      acquired.addAll(agents);
      return agents;
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }

  public void releaseAll() {
    release(acquired);
  }

  public void release(final Collection<Agent> agents) {
    log.info("Releasing {} agents...", agents.size());

    ConcurrentLinkedQueue<Agent> toRelease = new ConcurrentLinkedQueue<>(checkNotNull(agents));
    try {
      executor.invokeAll(
          IntStream.range(0, agents.size())
              .mapToObj(i -> Executors.callable(() -> {
                Agent agent = toRelease.poll();
                try {
                  log.info("Releasing {}", agent);
                  agent.stop();
                  acquired.remove(agent);
                }
                catch (Exception e) {
                  log.error("Could not stop {}", agent, e);
                }
              }))
              .collect(Collectors.toList())
      );
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }

}
