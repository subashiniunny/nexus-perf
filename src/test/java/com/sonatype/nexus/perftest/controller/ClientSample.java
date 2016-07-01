package com.sonatype.nexus.perftest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import com.sonatype.nexus.perftest.controller.Nexus.QoS;

import org.junit.Test;

import static com.sonatype.nexus.perftest.controller.JMXServiceURLs.jmxServiceURL;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ClientSample
{
  private static final Collection<JMXServiceURL> URLS = JMXServiceURLs.of(
      "192.168.1.6:5001",
      //"192.168.1.3:5001",
      "localhost:5001",
      "localhost:5002",
      "localhost:5003"
  );

  private static final String DATA = "all/target/all-1.0.4-SNAPSHOT-data/";

  @Test
  public void multipleScenarios() throws Exception {
    AgentPool pool = new AgentPool(URLS);

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

      List<Swarm> m01Swarms = m01Agents.stream().map(Agent::getSwarms).flatMap(Collection::stream).collect(toList());
      List<Swarm> m02Swarms = m02Agents.stream().map(Agent::getSwarms).flatMap(Collection::stream).collect(toList());

      m01Agents.parallelStream().forEach(Agent::waitToFinish);
      m02Agents.parallelStream().forEach(Agent::waitToFinish);

      m01Swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.getFailures().stream().forEach(failure -> System.out.println("-------- " + failure));
      });

      m01Swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Failure.count), is(equalTo(0L))));
      m02Swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Failure.count), is(equalTo(0L))));
    }
    finally {
      pool.releaseAll();
    }
  }

  @Test
  public void npm01() throws Exception {
    AgentPool pool = new AgentPool(URLS);

    try {
      Nexus nexus = new Nexus(jmxServiceURL("192.168.1.99:1099"));
      nexus.addTrigger(new GaugeTrigger<>(Nexus.QoS.Rejects.count,
          (state, value) -> {
            System.out.println("!!!!!!!!!!!!!!!!! Rejects " + value + " " + state);
          }).setHighThreshold(1L));
      nexus.addTrigger(new GaugeTrigger<>(Nexus.QoS.waitingForPermits,
          (state, value) -> {
            System.out.println("!!!!!!!!!!!!!!!!! Waits: " + value + " " + state);
          }).setHighThreshold(1));
      nexus.addTrigger(new GaugeTrigger<>(Nexus.QoS.queueSize(0),
          (state, value) -> {
            System.out.println("!!!!!!!!!!!!!!!!! Queue 0: " + value + " " + state);
          }).setHighThreshold(1));

      Collection<Agent> agents = pool.acquireAll();

      Map<String, String> overrides = new HashMap<>();
      overrides.put("nexus.baseurl", "http://192.168.1.99:8081/nexus");
      overrides.put("test.duration", "2 MINUTES");
      //overrides.put("test.duration", "20 SECONDS");

      agents.parallelStream().forEach(agent -> agent.load(DATA + "npm01-1.0.4-SNAPSHOT", overrides));
      agents.parallelStream().forEach(Agent::start);

      List<Swarm> swarms = agents.stream().map(Agent::getSwarms).flatMap(Collection::stream).collect(toList());
      swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.setRateMultiplier(10);
        control.setRateSleepMillis(5);
      });

      agents.parallelStream().forEach(Agent::waitToFinish);
      swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.getFailures().stream().forEach(failure -> System.out.println("-------- " + failure));
      });

      swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Failure.count), is(equalTo(0L))));
    }
    finally {
      pool.releaseAll();
    }
  }

  @Test
  public void stopAll() throws Exception {
    AgentPool pool = new AgentPool(URLS);

    try {
      pool.acquireAll();
    }
    finally {
      pool.releaseAll();
    }
  }
}
