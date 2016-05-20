package com.sonatype.nexus.perftest.controller;

import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThresholdTrigger<T extends Comparable>
    extends AttributeTrigger
    implements Trigger
{
  private final Attribute<T> attribute;

  private final BiConsumer<ThresholdTrigger<T>, T> consumer;

  private T threshold;

  private boolean notified;

  public ThresholdTrigger(final Attribute<T> attribute, final BiConsumer<ThresholdTrigger<T>, T> consumer) {
    this.attribute = checkNotNull(attribute);
    this.consumer = checkNotNull(consumer);
  }

  @Override
  protected void check() {
    T current = getAttributeSource().get(attribute);

    if (threshold != null && current != null) {
      if (current.compareTo(threshold) >= 0) {
        if (!notified) {
          notified = true;
          ringTheAlarm(current);
        }
      }
      else {
        if (notified) {
          notified = false;
          ringTheAlarm(current);
        }
      }
    }
    else {
      notified = false;
    }
  }

  protected void ringTheAlarm(final T current) {
    consumer.accept(this, current);
  }

  public T getThreshold() {
    return threshold;
  }

  public ThresholdTrigger<T> setThreshold(final T threshold) {
    this.threshold = threshold;
    return this;
  }

  @Override
  public String toString() {
    return "Threshold " + attribute + "(" + threshold + ")";
  }
}

