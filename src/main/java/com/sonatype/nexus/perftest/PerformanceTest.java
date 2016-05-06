/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sonatype.nexus.perftest.db.PerformanceMetricDescriptor;
import com.sonatype.nexus.perftest.db.TestExecution;
import com.sonatype.nexus.perftest.db.TestExecutions;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

public class PerformanceTest
{

  @JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "class")
  public interface NexusConfigurator
  {
    void cleanup() throws Exception;
  }

  private static final String buildId = System.getProperty("perftest.buildId");

  private static final String baselineId = System.getProperty("perftest.baselineId");

  private final String name;

  private final Duration duration;

  private final Collection<NexusConfigurator> configurators;

  private final Collection<ClientSwarm> swarms;

  @JsonCreator
  public PerformanceTest(
      @JsonProperty("name") String name,
      @JsonProperty("duration") Duration duration,
      @JsonProperty("configurators") Collection<NexusConfigurator> configurators, //
      @JsonProperty("swarms") Collection<ClientSwarm> swarms)
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

  }

  public void run() throws InterruptedException {
    TestExecution baseline = null;
    if (baselineId != null) {
      baseline = TestExecutions.select(name, baselineId);
      if (baseline == null) {
        throw new RuntimeException(String.format("Baseline build %s is not found", baselineId));
      }
    }

    List<Metric> metrics = new ArrayList<>();
    for (ClientSwarm swarm : swarms) {
      JmxReporter.forRegistry(SharedMetricRegistries.getOrCreate(swarm.getSwarmName()))
          .inDomain(name + "." + swarm.getSwarmName()).build().start();
      metrics.add(swarm.getMetric());
      swarm.start();
    }

    ProgressTickThread progressTickThread = new ProgressTickThread(metrics.toArray(new Metric[metrics.size()]));

    Thread.sleep(duration.toMillis());

    System.err.println("Stopping...");
    for (ClientSwarm swarm : swarms) {
      swarm.stop();
    }
    progressTickThread.interrupt();
    progressTickThread.join();
    progressTickThread.printTick();
    System.err.println("Stopped");

    for (NexusConfigurator configurator : configurators) {
      try {
        configurator.cleanup();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    assertPerformance(metrics, baseline);
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

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PerformanceTest{");
    sb.append("name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }

}
