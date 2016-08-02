package com.sonatype.nexus.perftest.controller;

import java.util.function.Consumer;

public class QueuedThreadPoolUnhealthy
    extends NotificationTrigger
{
  public QueuedThreadPoolUnhealthy(final Consumer<String> consumer)
  {
    super(
        Nexus.ObjectNames.healthCheckNotifier(),
        notification -> {
          if ("jetty-qtp".equals(notification.getType())) {
            consumer.accept(notification.getMessage());
          }
        }
    );
  }

  @Override
  public String toString() {
    return "Queued thread pool health check";
  }

}

