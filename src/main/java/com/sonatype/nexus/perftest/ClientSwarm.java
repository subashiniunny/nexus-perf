/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.ImmutableMap;

/**
 * Models a group of similar clients. The clients performs the same operation. Request rate is
 * configured for the swarm.
 */
public class ClientSwarm
{
  private final String swarmName;

  private final List<ClientThread> threads;

  private final Metric metric;

  private final Meter requestsMeter;

  private final Timer requestDurationTimer;

  private final Meter successMeter;

  private final Meter failureMeter;

  private final Meter downloadedBytesMeter;

  private final Meter uploadedBytesMeter;

  private final RequestRate rate;

  public interface ClientRequestInfo
  {
    String getSwarmName();

    int getClientId();

    int getRequestId();

    <T> void setContextValue(String key, T value);

    <T> T getContextValue(String key);

    /**
     * timestamp of the request relative to the test start time
     */
    long getTestTimeMillis();
  }

  @JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "class")
  public interface Operation
  {
    void perform(ClientRequestInfo requestInfo) throws Exception;
  }

  private class ClientThread
      extends Thread
      implements ClientRequestInfo
  {
    private final String swarmName;

    private final int clientId;

    private final Operation operation;

    private final RequestRate rate;

    private final Metric metric;

    private int requestId;

    private final Map<String, Object> context = new HashMap<>();

    public ClientThread(String swarmName, int clientId, Operation operation, Metric metric, RequestRate rate, Meter downloadedBytesMeter, Meter uploadedBytesMeter) {
      super(String.format("%s-%d", swarmName, clientId));
      this.swarmName = swarmName;
      this.clientId = clientId;
      this.operation = operation;
      this.metric = metric;
      this.rate = rate;
      this.context.put("metric.downloadedBytesMeter", downloadedBytesMeter);
      this.context.put("metric.uploadedBytesMeter", uploadedBytesMeter);
    }

    @Override
    public final void run() {
      while (true) {
        requestId++;
        try {
          rate.waitForWork();
        }
        catch (InterruptedException e) {
          break;
        }

        requestsMeter.mark();
        Timer.Context timerContext = requestDurationTimer.time();
        Metric.Context context = metric.time();
        boolean success = false;
        String failureMessage = null;
        try {
          operation.perform(this);
          success = true;
        }
        catch (InterruptedException | InterruptedIOException e) {
          // TODO more graceful shutdown
          printStackTrace(e);
          break;
        }
        catch (Exception e) {
          failureMessage = e.getMessage();
          printStackTrace(e);
        }
        finally {
          timerContext.stop();
          if (success) {
            successMeter.mark();
            context.success();
          }
          else {
            failureMeter.mark();
            context.failure(failureMessage);
          }
        }
      }
    }

    private void printStackTrace(final Exception e) {
      System.err.println(Thread.currentThread().getName() + " " + e.getMessage());
    }

    @Override
    public String getSwarmName() {
      return swarmName;
    }

    @Override
    public int getClientId() {
      return clientId;
    }

    @Override
    public int getRequestId() {
      return requestId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContextValue(String key) {
      return (T) context.get(key);
    }

    @Override
    public <T> void setContextValue(String key, T value) {
      context.put(key, value);
    }

    @Override
    public long getTestTimeMillis() {
      return System.currentTimeMillis() - rate.getStartTimeMillis();
    }
  }

  @JsonCreator
  public ClientSwarm( //
                      @JsonProperty("name") String name, //
                      @JsonProperty("operation") Operation operation, //
                      @JsonProperty(value = "initialDelay", required = false) Duration initialDelay, //
                      @JsonProperty("rate") RequestRate rate, //
                      @JsonProperty("numberOfClients") int clientCount)
  {
    this.requestsMeter = new Meter();
    this.requestDurationTimer = new Timer();
    this.successMeter = new Meter();
    this.failureMeter = new Meter();
    this.downloadedBytesMeter = new Meter();
    this.uploadedBytesMeter = new Meter();

    MetricSet metricSet = new MetricSet() {
      @Override
      public Map<String, com.codahale.metrics.Metric> getMetrics() {
        return ImmutableMap.<String, com.codahale.metrics.Metric>builder()
            .put("requests", requestsMeter)
            .put("requestDuration", requestDurationTimer)
            .put("success", successMeter)
            .put("failure", failureMeter)
            .put("downloadedBytes", downloadedBytesMeter)
            .put("uploadedBytes", uploadedBytesMeter)
            .build();
      }
    };
    SharedMetricRegistries.getOrCreate(name).registerAll(metricSet);

    swarmName = name;
    this.rate = initialDelay != null ? rate.offsetStart(initialDelay.toMillis()) : rate;

    metric = new Metric(name);
    List<ClientThread> threads = new ArrayList<>();
    for (int i = 0; i < clientCount; i++) {
      ClientThread clientThread = new ClientThread(name, i, operation, metric, rate, downloadedBytesMeter, uploadedBytesMeter);
      clientThread.setName(swarmName + i);
      threads.add(clientThread);
    }
    this.threads = Collections.unmodifiableList(threads);
  }

  public void start() {
    for (Thread thread : threads) {
      thread.start();
    }
  }

  public void stop() throws InterruptedException {
    for (ClientThread thread : threads) {
      for (int i = 0; i < 3 && thread.isAlive(); i++) {
        thread.interrupt();
        thread.join(1000L);
      }
      if (thread.isAlive()) {
        StringBuilder sb = new StringBuilder(String.format("Thread %s ignored interrupt flag\n", thread.getName()));
        for (StackTraceElement f : thread.getStackTrace()) {
          sb.append("\t").append(f.toString()).append("\n");
        }
        System.err.println(sb.toString());
      }
    }
  }

  public String getSwarmName() {
    return swarmName;
  }

  public Metric getMetric() {
    return metric;
  }

  public RequestRate getRate() {
    return rate;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Swarm{");
    sb.append("name='").append(metric.getName()).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
