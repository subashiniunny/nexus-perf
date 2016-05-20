package com.sonatype.nexus.perftest.controller;

import java.util.function.BiConsumer;

import javax.management.Notification;
import javax.management.ObjectName;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationTrigger
    implements Trigger
{
  private final ObjectName objectName;

  private final BiConsumer<NotificationTrigger, Notification> consumer;

  public NotificationTrigger(final ObjectName objectName,
                             final BiConsumer<NotificationTrigger, Notification> consumer)
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
            ringTheAlarm(notification);
          },
          null,
          null
      );
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected void ringTheAlarm(final Notification notification) {
    consumer.accept(this, notification);
  }

  @Override
  public String toString() {
    return "Notification " + objectName;
  }

}

