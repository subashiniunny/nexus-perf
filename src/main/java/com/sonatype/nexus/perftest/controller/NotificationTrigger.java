package com.sonatype.nexus.perftest.controller;

import java.util.function.Consumer;

import javax.management.Notification;
import javax.management.ObjectName;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationTrigger
    implements Trigger
{
  private final ObjectName objectName;

  private final Consumer<Notification> consumer;

  public NotificationTrigger(final ObjectName objectName,
                             final Consumer<Notification> consumer)
  {
    this.objectName = checkNotNull(objectName);
    this.consumer = checkNotNull(consumer);
  }

  @Override
  public void bind(final Client client) {
    try {
      client.getConnection().addNotificationListener(
          objectName,
          (notification, handback) -> {
            consumer.accept(notification);
          },
          null,
          null
      );
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return "Notification " + objectName;
  }

}

