package com.sonatype.nexus.perftest.controller;


import org.junit.Test;

import static com.sonatype.nexus.perftest.controller.JMXServiceURLs.jmxServiceURL;

public class NexusSample
{
  @Test
  public void queryNexus() throws Exception {
    System.out.println(new Nexus(jmxServiceURL("localhost:1099")).get(Nexus.QueuedThreadPool.activeThreads));
  }
}
