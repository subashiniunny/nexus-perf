package com.sonatype.nexus.perftest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

public class ClientPool
{
  private static final Logger log = LoggerFactory.getLogger(ClientPool.class);

  private final Collection<JMXServiceURL> urls;

  private final Collection<Client> acquired = new ArrayList<>();

  public ClientPool(final Collection<JMXServiceURL> urls) {
    this.urls = urls;
  }

  public Collection<Client> acquire(final int size) {
    log.info("Acquiring {} clients...", size);
    Collection<Client> result = new ArrayList<>();
    Iterator<JMXServiceURL> it = urls.iterator();
    while (result.size() < size && it.hasNext()) {
      JMXServiceURL url = it.next();
      it.remove();
      Client client = new Client(url);
      result.add(client);
      log.info("Acquired {}", client);
    }
    checkState(size == result.size());
    acquired.addAll(result);
    return result;
  }

  public void releaseAll() {
    release(acquired);
  }

  public void release(final Collection<Client> clients) {
    log.info("Releasing {} clients...", clients.size());
    for (Iterator<Client> i = clients.iterator(); i.hasNext(); ) {
      Client client = i.next();
      try {
        log.info("Releasing {}", client);
        client.stop();
        i.remove();
      }
      catch (Exception e) {
        log.error("Could not stop {}", client, e);
      }
    }
  }

}
