package com.sonatype.nexus.perftest.controller;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AttributeTrigger
{
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

  private Duration granularityPeriod = Duration.ofSeconds(1);

  private ScheduledFuture<?> scheduledFuture;

  private AttributeSource attributeSource;

  public void bind(final AttributeSource attributeSource) {
    this.attributeSource = checkNotNull(attributeSource);
    schedule();
  }

  private void schedule() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    scheduledFuture = scheduler.scheduleWithFixedDelay(
        () -> {
          try {
            AttributeTrigger.this.check();
          }
          catch (Exception e) {
            log.warn("Exception during execution of {}", this, e);
          }
        }, 0, granularityPeriod.toMillis(), TimeUnit.MILLISECONDS
    );
  }

  protected abstract void check();

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

}

