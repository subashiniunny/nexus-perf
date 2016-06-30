package com.sonatype.nexus.perftest.controller;

import java.util.HashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nexus
    extends Client
{
  private static final Logger log = LoggerFactory.getLogger(Nexus.class);

  public Nexus(final JMXServiceURL serviceURL, final String user, final String password) {
    try {
      log.info("Connecting to {} ...", serviceURL);
      Map<String, String[]> env = new HashMap<>();
      if (user != null) {
        String[] credentials = {user, password};
        env.put(JMXConnector.CREDENTIALS, credentials);
      }
      JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);
      setConnection(connector.getMBeanServerConnection());
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Nexus(final JMXServiceURL serviceURL) {
    this(serviceURL, null, null);
  }

  public static class ObjectNames
  {
    public static ObjectName healthCheckNotifier() {
      try {
        return new ObjectName("io.takari.nexus.healthcheck.jmx:name=JmxHealthCheckNotifierMBeanImpl");
      }
      catch (MalformedObjectNameException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  public static class Requests
  {
    public static final Attribute<Long> count = new Attribute<>(
        "\"com.yammer.metrics.web\":type=\"WebappMetricsFilter\",name=\"requests\"", "Count"
    );
  }

  public static class QueuedThreadPool
  {
    public static final Attribute<Integer> activeThreads = new Attribute<>(
        "\"org.eclipse.jetty.util.thread\":type=\"QueuedThreadPool\",name=\"active-threads\"", "Value"
    );

    public static final Attribute<Double> percentIdle = new Attribute<>(
        "\"org.eclipse.jetty.util.thread\":type=\"QueuedThreadPool\",name=\"percent-idle\"", "Value"
    );
  }

  public static class OperatingSystem
  {
    public static final Attribute<Long> openFileDescriptorCount = new Attribute<>(
        "java.lang:type=OperatingSystem", "OpenFileDescriptorCount"
    );

    public static final Attribute<Long> maxFileDescriptorCount = new Attribute<>(
        "java.lang:type=OperatingSystem", "MaxFileDescriptorCount"
    );

    public static final Attribute<Double> processCpuLoad = new Attribute<>(
        "java.lang:type=OperatingSystem", "ProcessCpuLoad"
    );

    public static final Attribute<Double> systemCpuLoad = new Attribute<>(
        "java.lang:type=OperatingSystem", "SystemCpuLoad"
    );
  }

}

