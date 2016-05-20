package com.sonatype.nexus.perftest.controller;


import org.junit.Test;

import static com.sonatype.nexus.perftest.controller.JMXServiceURLs.jmxServiceURL;

public class NexusSample
{
  @Test
  public void queryNexus() throws Exception {
    Nexus nexus = new Nexus(jmxServiceURL("localhost:1099"));

    System.out.println(nexus.get(Nexus.QueuedThreadPool.activeThreads));

    nexus.addTrigger(new ThresholdTrigger<>(Nexus.QueuedThreadPool.activeThreads,
        (condition, activeThreads) -> {
          System.out.println();
          System.out.println(
              "!!!!!!!!!!!!!!!!! To many threads (" + activeThreads + ")"
          );
          System.out.println();
        }).setThreshold(200));

    Thread.sleep(100);
  }
}
