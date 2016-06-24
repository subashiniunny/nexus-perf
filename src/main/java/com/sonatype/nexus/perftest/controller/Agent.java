package com.sonatype.nexus.perftest.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.management.AttributeChangeNotification;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sonatype.nexus.perftest.PerformanceTest;
import com.sonatype.nexus.perftest.PerformanceTestMBean;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Agent
{
  private static final Logger log = LoggerFactory.getLogger(Agent.class);

  private final JMXServiceURL jmxServiceURL;

  private MBeanServerConnection connection;

  private PerformanceTestMBean controlBean;

  private final List<Swarm> swarms = new ArrayList<>();

  private CountDownLatch finishSignal;

  public Agent(final JMXServiceURL jmxServiceURL) {
    this.jmxServiceURL = jmxServiceURL;
    try {
      log.info("Connecting to {}...", jmxServiceURL);
      JMXConnector connector = JMXConnectorFactory.connect(jmxServiceURL, null);
      connection = connector.getMBeanServerConnection();
      ObjectName controlBeanName = new ObjectName(PerformanceTest.class.getPackage().getName(), "name", "control");
      controlBean = JMX.newMBeanProxy(connection, controlBeanName, PerformanceTestMBean.class, false);
      connection.addNotificationListener(controlBeanName, (notification, handback) -> {
        if (notification instanceof AttributeChangeNotification) {
          AttributeChangeNotification acn = (AttributeChangeNotification) notification;
          if ("running".equals(acn.getAttributeName())
              && Boolean.FALSE.equals(acn.getNewValue()) && Boolean.TRUE.equals(acn.getOldValue())) {
            if (finishSignal != null) {
              finishSignal.countDown();
            }
          }
        }
      }, null, null);
    }
    catch (Exception e) {
      log.debug("Could not connect to {}: {}", jmxServiceURL, e.toString());
      throw Throwables.propagate(e);
    }
  }

  public List<Swarm> getSwarms() {
    return swarms;
  }

  public Agent load(final String scenario) {
    return load(scenario, null);
  }

  public Agent load(final String scenario, @Nullable Map<String, String> overrides) {
    if (controlBean.isRunning()) {
      throw new RuntimeException("Agent is currently running a scenario");
    }
    try {
      log.info("Loading scenario {} on {}", scenario, this);
      swarms.addAll(
          controlBean.load(scenario, overrides).stream()
              .map(name -> new Swarm(connection, name))
              .collect(Collectors.toList())
      );
      return this;
    }
    catch (Exception e) {
      log.debug("Could not load scenario {} on {}: {}", scenario, this, e.toString());
      throw e;
    }
  }

  public Agent start() {
    try {
      log.info("Starting {}", this);
      controlBean.start();
      finishSignal = new CountDownLatch(1);
      return this;
    }
    catch (Exception e) {
      log.debug("Could not start {}: {}", this, e.toString());
      throw e;
    }
  }

  public Agent stop() {
    try {
      log.info("Stopping {}", this);
      swarms.clear();
      controlBean.stop();
      if (finishSignal != null) {
        finishSignal.countDown();
      }
      return this;
    }
    catch (Exception e) {
      log.debug("Could not stop {}: {}", this, e.toString());
      throw e;
    }
  }

  public Agent waitToFinish() {
    return waitToFinish(null);
  }

  public Agent waitToFinish(final @Nullable Duration timeout) {
    try {
      if (timeout == null) {
        log.info("Waiting for {} to finish", this);
        if (finishSignal != null) {
          finishSignal.await();
        }
      }
      else {
        log.info("Waiting for {} to finish for {}", this, timeout);
        if (finishSignal != null) {
          finishSignal.await(timeout.getNano(), TimeUnit.NANOSECONDS);
        }
      }
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
    return this;
  }

  public JMXServiceURL getJmxServiceURL() {
    return jmxServiceURL;
  }

  @Override
  public String toString() {
    return jmxServiceURL.toString();
  }
}

