/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.sonatype.nexus.perftest.db.PerformanceMetricDescriptor;
import com.sonatype.nexus.perftest.db.TestExecution;
import com.sonatype.nexus.perftest.db.TestExecutions;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.ObjectNameFactory;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTest
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

  @JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "class")
  public interface NexusConfigurator
  {
    void cleanup() throws Exception;
  }

  private static final String buildId = System.getProperty("perftest.buildId");

  private static final String baselineId = System.getProperty("perftest.baselineId");

  private final String name;

  private Duration duration;

  private final Collection<NexusConfigurator> configurators;

  private final Collection<ClientSwarm> swarms;

  private final CountDownLatch stopLatch;

  private final List<ObjectName> objectNames;

  private final List<JmxReporter> jmxReporters;

  @JsonCreator
  public PerformanceTest(
      @JsonProperty("name") String name,
      @JsonProperty("duration") Duration duration,
      @JsonProperty("configurators") Collection<NexusConfigurator> configurators, //
      @JsonProperty("swarms") Collection<ClientSwarm> swarms) throws Exception
  {
    this.name = name;
    this.duration = duration;
    if (configurators != null) {
      this.configurators = Collections.unmodifiableCollection(new ArrayList<>(configurators));
    }
    else {
      this.configurators = Collections.emptyList();
    }
    this.swarms = Collections.unmodifiableCollection(new ArrayList<>(swarms));
    this.stopLatch = new CountDownLatch(1);

    this.objectNames = new ArrayList<>(swarms.size());
    this.jmxReporters = new ArrayList<>(swarms.size());
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    for (ClientSwarm swarm : swarms) {
      String metricsDomain = getClass().getPackage().getName() + "." + swarm.getSwarmName();
      JmxReporter jmxReporter = JmxReporter.forRegistry(SharedMetricRegistries.getOrCreate(swarm.getSwarmName()))
          .inDomain(metricsDomain).build();
      jmxReporter.start();
      jmxReporters.add(jmxReporter);
      ObjectName objectName = ObjectName.getInstance(getClass().getPackage().getName(), "name", swarm.getSwarmName());
      server.registerMBean(new ClientSwarmMBeanImpl(objectName, swarm,metricsDomain), objectName);
      objectNames.add(objectName);
    }
  }

  public void run() {
    log.info("Starting...");
    TestExecution baseline = null;
    if (baselineId != null) {
      baseline = TestExecutions.select(name, baselineId);
      if (baseline == null) {
        throw new RuntimeException(String.format("Baseline build %s is not found", baselineId));
      }
    }

    List<Metric> metrics = new ArrayList<>();
    for (ClientSwarm swarm : swarms) {
      metrics.add(swarm.getMetric());
      swarm.start();
    }

    ProgressTickThread progressTickThread = new ProgressTickThread(metrics.toArray(new Metric[metrics.size()]));
    progressTickThread.start();

    log.info("Started");

    boolean stopped = false;
    try {
      stopped = !stopLatch.await(duration.toMillis(), TimeUnit.MILLISECONDS);

      log.info("Stopping...");
      for (ClientSwarm swarm : swarms) {
        swarm.stop();
      }
      progressTickThread.interrupt();
      progressTickThread.join();
      progressTickThread.printTick();
      log.info("Stopped");
    }
    catch (Exception e) {
      log.error("Error", e);
    }

    for (NexusConfigurator configurator : configurators) {
      try {
        configurator.cleanup();
      }
      catch (Exception e) {
        log.error("Configurator error", e);
      }
    }

    if (!stopped) {
      assertPerformance(metrics, baseline);
    }
  }

  public void stop() {
    stopLatch.countDown();

    // remove JMX and metric bits
    for (JmxReporter jmxReporter : jmxReporters) {
      jmxReporter.stop();
    }

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    for (ObjectName objectName : objectNames) {
      try {
        server.unregisterMBean(objectName);
      }
      catch (Exception e) {
        log.error("JMX Error", e);
      }
    }
  }

  private void assertPerformance(List<Metric> metrics, TestExecution baseline) {
    TestExecution execution = new TestExecution(name, buildId);
    Collection<PerformanceMetricDescriptor> descriptors = new ArrayList<>();
    for (Metric metric : metrics) {
      descriptors.add(new PerformanceMetricDescriptor(metric.getName() + ".successCount", 0.9f, 1.1f));
      execution.addMetric(metric.getName() + ".successCount", metric.getSuccesses());

      descriptors.add(new PerformanceMetricDescriptor(metric.getName() + ".successDuration", 0.9f, 1.1f));
      execution.addMetric(metric.getName() + ".successDuration", metric.getSuccessDuration());

      descriptors.add(new PerformanceMetricDescriptor(metric.getName() + ".failureCount", 0.9f, 1.1f));
      execution.addMetric(metric.getName() + ".failureCount", metric.getFailures());
    }

    if (buildId != null && !buildId.equals("-")) {
      TestExecutions.insert(execution);
    }

    if (baseline != null) {
      TestExecutions.assertPerformance(descriptors, baseline, execution);
    }
  }

  public Collection<ClientSwarm> getSwarms() {
    return Collections.unmodifiableCollection(new ArrayList<>(swarms));
  }

  public List<ObjectName> getObjectNames() {
    return objectNames;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(final Duration duration) {
    this.duration = duration;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PerformanceTest{");
    sb.append("name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }

}
