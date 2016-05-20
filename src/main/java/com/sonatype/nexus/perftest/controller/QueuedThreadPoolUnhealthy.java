package com.sonatype.nexus.perftest.controller;

import java.util.function.Consumer;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.google.common.base.Throwables;

public class QueuedThreadPoolUnhealthy
    extends NotificationTrigger
{
  public QueuedThreadPoolUnhealthy(final Consumer<String> consumer)
  {
    super(
        objectName(),
        (notificationTrigger, notification) -> {
          if ("jetty-qtp".equals(notification.getType())) {
            consumer.accept(notification.getMessage());
          }
        }
    );
  }

  private static ObjectName objectName() {
    try {
      return new ObjectName("io.takari.nexus.healthcheck.jmx:name=JmxHealthCheckNotifierMBeanImpl");
    }
    catch (MalformedObjectNameException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return "Queued thread pool health check";
  }

}

