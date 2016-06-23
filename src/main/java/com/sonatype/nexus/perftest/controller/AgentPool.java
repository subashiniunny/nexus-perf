package com.sonatype.nexus.perftest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

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

    Semaphore nrOfAgents = new Semaphore(size, true);
    Collection<Callable<Agent>> acquirers = new ArrayList<>();
    Collection<JMXServiceURL> failed = new ArrayList<>();
    Collection<Agent> agents = new ArrayList<>();
    JMXServiceURL head;
    while ((head = urls.poll()) != null) {
      final JMXServiceURL jmxServiceURL = head;
      acquirers.add(() -> {
        if (nrOfAgents.availablePermits() > 0) {
          try {
            Agent agent = new Agent(jmxServiceURL);
            if (nrOfAgents.tryAcquire()) {
              log.info("Acquired {}", agent);
              agents.add(agent);
              return agent;
            }
          }
          catch (Exception e) {
            // ignore
          }
        }
        failed.add(jmxServiceURL);
        return null;
      });
    }
    try {
      executor.invokeAll(acquirers);
    }
    catch (InterruptedException e) {
      log.warn("Interrupted, ignoring...", e);
    }
    urls.addAll(failed);
    acquired.addAll(agents);

    log.info("Acquired {} agents", agents.size());
    return agents;
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
                  urls.offer(agent.getJmxServiceURL());
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
      log.warn("Interrupted, ignoring...", e);
    }
  }
}
