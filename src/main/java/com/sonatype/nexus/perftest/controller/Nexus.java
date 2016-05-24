package com.sonatype.nexus.perftest.controller;

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

  public Nexus(final JMXServiceURL serviceURL) {
    try {
      log.info("Connecting to {} ...", serviceURL);
      JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
      setConnection(connector.getMBeanServerConnection());
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
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

