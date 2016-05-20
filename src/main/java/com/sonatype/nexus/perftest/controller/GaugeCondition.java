package com.sonatype.nexus.perftest.controller;

import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class GaugeCondition<T extends Comparable>
    extends MonitorCondition
{
  private final Attribute<T> attribute;

  private final BiConsumer<GaugeCondition<T>, T> consumer;

  private T highThreshold;

  private boolean notified;

  public GaugeCondition(final Attribute<T> attribute, final BiConsumer<GaugeCondition<T>, T> consumer) {
    this.attribute = checkNotNull(attribute);
    this.consumer = checkNotNull(consumer);
  }

  @Override
  protected void check() {
    T current = getAttributeSource().get(attribute);
    System.out.println("------- " + current);
    if (highThreshold != null && current != null && current.compareTo(highThreshold) > 0) {
      if (!notified) {
        notified = true;
        ringTheAlarm(current);
      }
    }
    else {
      notified = false;
    }
  }

  protected void ringTheAlarm(final T current) {
    consumer.accept(this, current);
  }

  public T getHighThreshold() {
    return highThreshold;
  }

  public GaugeCondition setHighThreshold(final T highThreshold) {
    this.highThreshold = highThreshold;
    return this;
  }
}

