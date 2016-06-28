package com.sonatype.nexus.perftest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ClientSample
{
  private static final String DATA = "all/target/all-1.0.4-SNAPSHOT-data/";

  @Test
  public void sample() throws Exception {
    AgentPool pool = new AgentPool(JMXServiceURLs.of(
        /*"192.168.1.6:5001",
        "192.168.1.7:5001",*/
        "localhost:5001",
        "localhost:5002",
        "localhost:5003"
    ));

    //Nexus nexus = new Nexus(jmxServiceURL("localhost:1099"));
    //nexus.addTrigger(new QueuedThreadPoolUnhealthy(
    //    (message) -> {
    //      System.out.println();
    //      System.out.println(
    //          "!!!!!!!!!!!!!!!!! Nexus is dead (" + message + ")"
    //      );
    //      System.out.println();
    //      pool.releaseAll();
    //    }
    //));
    //nexus.addTrigger(new GaugeTrigger<>(
    //        Nexus.QueuedThreadPool.activeThreads,
    //        (state, activeThreads) -> {
    //          if (State.HIGH == state) {
    //            System.out.println();
    //            System.out.println(
    //                "!!!!!!!!!!!!!!!!! Nexus is dead (" + activeThreads + ")"
    //            );
    //            System.out.println();
    //            //pool.releaseAll();
    //          }
    //          else {
    //            System.out.println();
    //            System.out.println(
    //                "!!!!!!!!!!!!!!!!! Nexus is back to life (" + activeThreads + ")"
    //            );
    //            System.out.println();
    //          }
    //        }).setHighThreshold(200)
    //);

    try {
      Collection<Agent> m01Agents = pool.acquire(2);
      assertThat(m01Agents, hasSize(2));
      Collection<Agent> m02Agents = pool.acquire(1);
      assertThat(m02Agents, hasSize(1));

      Map<String, String> overrides = new HashMap<>();
      overrides.put("nexus.baseurl", "http://localhost:8081/nexus");
      overrides.put("test.duration", "20 SECONDS");

      m01Agents.parallelStream().forEach(agent -> agent.load(DATA + "maven02-1.0.4-SNAPSHOT", overrides));
      m02Agents.parallelStream().forEach(agent -> agent.load(DATA + "maven01-1.0.4-SNAPSHOT", overrides));

      m01Agents.parallelStream().forEach(Agent::start);
      m02Agents.parallelStream().forEach(Agent::start);

      List<Swarm> m01Swarms = m01Agents.stream().map(Agent::getSwarms).flatMap(Collection::stream)
          .collect(Collectors.toList());
      m01Swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.setRateMultiplier(5);
        control.setRateSleepMillis(7);
      });

      List<Swarm> m02Swarms = m02Agents.stream().map(Agent::getSwarms).flatMap(Collection::stream)
          .collect(Collectors.toList());

      m01Agents.parallelStream().forEach(Agent::waitToFinish);
      m02Agents.parallelStream().forEach(Agent::waitToFinish);

      m01Swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.getFailures().stream().forEach(failure -> System.out.println("-------- " + failure));
      });

      m01Swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Failure.count), is(equalTo(0L))));
      m02Swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Failure.count), is(equalTo(0L))));
      //assertThat(nexus.get(Nexus.Requests.count), is(greaterThan(500L)));
    }
    finally {
      pool.releaseAll();
    }
  }
}
