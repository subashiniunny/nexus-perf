package com.sonatype.nexus.perftest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

public class AgentPool
{
  private static final Logger log = LoggerFactory.getLogger(AgentPool.class);

  private final Collection<JMXServiceURL> urls;

  private final Collection<Agent> acquired = new ArrayList<>();

  public AgentPool(final Collection<JMXServiceURL> urls) {
    this.urls = urls;
  }

  public Collection<Agent> acquireAll() {
    return acquire(urls.size());
  }

  public Collection<Agent> acquire(final int size) {
    log.info("Acquiring {} agents...", size);
    Collection<Agent> result = new ArrayList<>();
    Iterator<JMXServiceURL> it = urls.iterator();
    while (result.size() < size && it.hasNext()) {
      JMXServiceURL url = it.next();
      it.remove();
      Agent agent = new Agent(url);
      result.add(agent);
      log.info("Acquired {}", agent);
    }
    checkState(size == result.size());
    acquired.addAll(result);
    return result;
  }

  public void releaseAll() {
    release(acquired);
  }

  public void release(final Collection<Agent> agents) {
    log.info("Releasing {} agents...", agents.size());
    for (Iterator<Agent> i = agents.iterator(); i.hasNext(); ) {
      Agent agent = i.next();
      try {
        log.info("Releasing {}", agent);
        agent.stop();
        i.remove();
      }
      catch (Exception e) {
        log.error("Could not stop {}", agent, e);
      }
    }
  }

}
