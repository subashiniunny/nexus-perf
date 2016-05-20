package com.sonatype.nexus.perftest.controller;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nexus
    extends AttributeSource
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
  }

}

