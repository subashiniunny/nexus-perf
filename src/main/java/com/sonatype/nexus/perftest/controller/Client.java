package com.sonatype.nexus.perftest.controller;

import java.time.Duration;
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

public class Client
{
  private static final Logger log = LoggerFactory.getLogger(Client.class);

  private final JMXServiceURL serviceURL;

  private MBeanServerConnection connection;

  private PerformanceTestMBean controlBean;

  private List<Swarm> swarms;

  private CountDownLatch finishSignal = new CountDownLatch(1);

  public Client(final JMXServiceURL serviceURL) {
    this.serviceURL = serviceURL;
    try {
      log.info("Connecting to {}...", serviceURL);
      JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
      connection = connector.getMBeanServerConnection();
      ObjectName controlBeanName = new ObjectName(PerformanceTest.class.getPackage().getName(), "name", "control");
      controlBean = JMX.newMBeanProxy(connection, controlBeanName, PerformanceTestMBean.class, false);
      connection.addNotificationListener(controlBeanName, (notification, handback) -> {
        if (notification instanceof AttributeChangeNotification) {
          AttributeChangeNotification acn = (AttributeChangeNotification) notification;
          if ("running".equals(acn.getAttributeName())
              && Boolean.FALSE.equals(acn.getNewValue()) && Boolean.TRUE.equals(acn.getOldValue())) {
            finishSignal.countDown();
          }
        }
      }, null, null);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public List<Swarm> getSwarms() {
    return swarms;
  }

  public Client start(final String scenario) {
    return start(scenario, null);
  }

  public Client start(final String scenario, @Nullable Map<String, String> overrides) {
    log.info("Starting scenario {} on {}", scenario, this);
    swarms = controlBean.start(scenario, overrides).stream()
        .map(name -> new Swarm(connection, name))
        .collect(Collectors.toList());
    return this;
  }

  public Client stop() {
    log.info("Stopping {}", this);
    controlBean.stop();
    return this;
  }

  public Client waitToFinish() {
    return waitToFinish(null);
  }

  public Client waitToFinish(final @Nullable Duration timeout) {
    try {
      if (timeout == null) {
        log.info("Waiting for {} to finish", this);
        finishSignal.await();
      }
      else {
        log.info("Waiting for {} to finish for {}", this, timeout);
        finishSignal.await(timeout.getNano(), TimeUnit.NANOSECONDS);
      }
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
    return this;
  }

  @Override
  public String toString() {
    return serviceURL.toString();
  }
}

