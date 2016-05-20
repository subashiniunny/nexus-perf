package com.sonatype.nexus.perftest.controller;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class MonitorCondition
    implements Condition
{
  private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private Duration granularityPeriod = Duration.ofSeconds(1);

  private ScheduledFuture<?> scheduledFuture;

  private AttributeSource attributeSource;

  @Override
  public void bind(final AttributeSource attributeSource) {
    this.attributeSource = checkNotNull(attributeSource);
    schedule();
  }

  public AttributeSource getAttributeSource() {
    return attributeSource;
  }

  public Duration getGranularityPeriod() {
    return granularityPeriod;
  }

  public void setGranularityPeriod(final Duration granularityPeriod) {
    this.granularityPeriod = checkNotNull(granularityPeriod);
    if (attributeSource != null) {
      schedule();
    }
  }

  private void schedule() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    scheduledFuture = scheduler.scheduleWithFixedDelay(
        this::check, 0, granularityPeriod.toMillis(), TimeUnit.MILLISECONDS
    );
  }

  protected abstract void check();

}

