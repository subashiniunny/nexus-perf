package com.sonatype.nexus.perftest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import static com.sonatype.nexus.perftest.controller.JMXServiceURLs.jmxServiceURL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class ClientSample
{
  @Test
  public void sample() throws Exception {
    ClientPool pool = new ClientPool(JMXServiceURLs.of(
        /*"192.168.1.6:5001",
        "192.168.1.7:5001",*/
        "localhost:5101",
        "localhost:5102",
        "localhost:5103"
    ));

    Nexus nexus = new Nexus(jmxServiceURL("localhost:1099"));
    nexus.addTrigger(new QueuedThreadPoolUnhealthy(
        (message) -> {
          System.out.println();
          System.out.println(
              "!!!!!!!!!!!!!!!!! Nexus is dead (" + message + ")"
          );
          System.out.println();
          pool.releaseAll();
        }
    ));
    nexus.addTrigger(new ThresholdTrigger<>(
            Nexus.QueuedThreadPool.activeThreads,
            (trigger, activeThreads) -> {
              System.out.println();
              System.out.println(
                  "!!!!!!!!!!!!!!!!! Nexus is dead (" + activeThreads + ")"
              );
              System.out.println();
              pool.releaseAll();
            }).setThreshold(395)
    );

    try {
      Collection<Client> m01Clients = pool.acquire(2);
      Collection<Client> m02Clients = pool.acquire(1);

      Map<String, String> overrides = new HashMap<>();
      overrides.put("nexus.baseurl", "http://localhost:8081/nexus");

      m01Clients.parallelStream().forEach(client -> client.start("maven01-1.0.3-SNAPSHOT", overrides));
      m02Clients.parallelStream().forEach(client -> client.start("maven01-1.0.3-SNAPSHOT", overrides));

      List<Swarm> m1Swarms = m01Clients.stream().map(Client::getSwarms).flatMap(Collection::stream)
          .collect(Collectors.toList());
      m1Swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.setRateMultiplier(5);
        control.setRateSleepMillis(7);
      });
      m01Clients.parallelStream().forEach(Client::waitToFinish);
      m02Clients.parallelStream().forEach(Client::waitToFinish);

      m1Swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Success.count), is(greaterThan(100L))));
      assertThat(nexus.get(Nexus.Requests.count), is(greaterThan(500L)));
    }
    finally {
      pool.releaseAll();
    }
  }
}
