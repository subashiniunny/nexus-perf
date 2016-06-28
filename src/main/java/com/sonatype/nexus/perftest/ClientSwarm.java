/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.nexus.perftest.jmx.RequestRate;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.ImmutableMap;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models a group of similar clients. The clients performs the same operation. Request rate is
 * configured for the swarm.
 */
public class ClientSwarm
{
  public static final int HTTP_TIMEOUT = Integer.parseInt(System.getProperty("perftest.http.timeout", "60000"));

  private static final Logger log = LoggerFactory.getLogger(ClientSwarm.class);

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

  private final List<String> failures = new ArrayList<>();

  public interface ClientRequestInfo
  {
    String getSwarmName();

    int getClientId();

    int getRequestId();

    <T> void setContextValue(String key, T value);

    <T> T getContextValue(String key);

    HttpClient getHttpClient();

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

    private final CloseableHttpClient httpClient;

    private int requestId;

    private final Map<String, Object> context = new HashMap<>();

    private boolean interrupted;

    public ClientThread(Nexus nexus,
                        String swarmName,
                        int clientId,
                        Operation operation,
                        Metric metric,
                        RequestRate rate,
                        Meter downloadedBytesMeter,
                        Meter uploadedBytesMeter)
    {
      super(String.format("%s-%d", swarmName, clientId));
      this.swarmName = swarmName;
      this.clientId = clientId;
      this.operation = operation;
      this.metric = metric;
      this.rate = rate;
      this.context.put("metric.downloadedBytesMeter", downloadedBytesMeter);
      this.context.put("metric.uploadedBytesMeter", uploadedBytesMeter);

      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(nexus.getUsername(), nexus.getPassword()));
      BasicHttpClientConnectionManager clientConnectionManager = new BasicHttpClientConnectionManager();
      this.httpClient = HttpClients.custom()
          .setConnectionManager(clientConnectionManager)
          .setDefaultRequestConfig(
              RequestConfig.custom()
                  .setConnectTimeout(HTTP_TIMEOUT)
                  .setSocketTimeout(HTTP_TIMEOUT).build()
          )
          .setDefaultCredentialsProvider(credsProvider).build();
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
          log.warn("Unexpected exception", e);
          break;
        }
        catch (Exception e) {
          if (!interrupted) {
            failureMessage = e.toString();
            log.warn("Unexpected exception", e);
          }
        }
        finally {
          timerContext.stop();
          if (success) {
            successMeter.mark();
            context.success();
          }
          else if (!interrupted) {
            failureMeter.mark();
            failures.add(failureMessage);
            context.failure(failureMessage);
          }
        }
      }
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
    public HttpClient getHttpClient() {
      return httpClient;
    }

    @Override
    public long getTestTimeMillis() {
      return System.currentTimeMillis() - rate.getStartTimeMillis();
    }

    @Override
    public void interrupt() {
      interrupted = true;
      try {
        this.httpClient.close();
      }
      catch (IOException e) {
        log.error("Could not close HttpClient", e);
      }
      finally {
        super.interrupt();
      }
    }
  }

  @JsonCreator
  public ClientSwarm(@JacksonInject Nexus nexus,
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

    MetricSet metricSet = new MetricSet()
    {
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
    SharedMetricRegistries.remove(name);
    SharedMetricRegistries.getOrCreate(name).registerAll(metricSet);

    swarmName = name;
    this.rate = initialDelay != null ? rate.offsetStart(initialDelay.toMillis()) : rate;

    metric = new Metric(name);
    List<ClientThread> threads = new ArrayList<>();
    for (int i = 0; i < clientCount; i++) {
      ClientThread clientThread = new ClientThread(nexus, name, i, operation, metric, rate, downloadedBytesMeter,
          uploadedBytesMeter);
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
    SharedMetricRegistries.remove(swarmName);

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
        log.error("{}", sb);
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

  public List<String> getFailures() {
    return failures;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Swarm{");
    sb.append("name='").append(metric.getName()).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
