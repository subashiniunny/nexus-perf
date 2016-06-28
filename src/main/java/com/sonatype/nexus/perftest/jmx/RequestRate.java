/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.jmx;

import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Schedules test client requests according to specified rate.
 */
public class RequestRate
{
  private final Random rnd = new Random(1);

  private final AtomicInteger count = new AtomicInteger();

  private final Semaphore semaphore = new Semaphore(0);

  private final long startTimeMillis;

  private volatile int periodMillis;

  private volatile int multiplier = 1;

  /**
   * @param rate average number of requests per time {@code unit}
   * @param unit time unit of {@code rate} parameter
   */
  public RequestRate(int rate, TimeUnit unit) {
    this((int) (unit.toMillis(1) / rate));
  }

  private RequestRate(int periodMillis) {
    this(System.currentTimeMillis(), periodMillis);
  }

  private RequestRate(long startTimeMillis, int periodMillis) {
    // TODO assert period is at least 10
    this.startTimeMillis = startTimeMillis;
    this.periodMillis = periodMillis;

    // feeder
    new Thread(
        () -> {
          try {
            Thread.sleep(nextDelayMillis()); // obey offsetStart
            while (true) {
              Thread.sleep(this.periodMillis);
              semaphore.release(multiplier);
            }
          }
          catch (InterruptedException e) {
            // just bail out silently
          }
        }
    ).start();
  }

  @JsonCreator
  public RequestRate(String value) {
    this(parsePeriod(value));
  }

  public static int parsePeriod(String value) {
    StringTokenizer st = new StringTokenizer(value, " /");
    int time = Integer.parseInt(st.nextToken());
    TimeUnit unit = TimeUnit.valueOf(st.nextToken() + "S");
    return (int) (unit.toMillis(1) / time);
  }

  public long nextDelayMillis() {
    // time of the next event, in millis from test start
    long relativeTimeMillis = (((long) periodMillis) * ((long) count.incrementAndGet()));

    // add some randomness. not sure how much this matters
    relativeTimeMillis -= rnd.nextInt(periodMillis);

    // "real-world" time of the next event
    long absoluteTimeMillis = startTimeMillis + relativeTimeMillis;

    // delay to the next event
    return Math.max(0, absoluteTimeMillis - System.currentTimeMillis());
  }

  public void waitForWork() throws InterruptedException {
    semaphore.acquire();
  }

  public int getPeriodMillis() {
    return periodMillis;
  }

  /**
   * This method can be used only if clients are blocked using {@link #waitForWork()}. This method along with {@link
   * #nextDelayMillis()} will not work as expected.
   */
  public void setPeriodMillis(int pm) {
    periodMillis = pm;
    semaphore.drainPermits();
  }

  public RequestRate offsetStart(long millis) {
    return new RequestRate(startTimeMillis + millis, periodMillis);
  }

  public long getStartTimeMillis() {
    return startTimeMillis;
  }

  public int getToDoCount() { return semaphore.availablePermits(); }

  public int getWaitingCount() { return semaphore.getQueueLength(); }

  public int getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(final int multiplier) {
    checkArgument(multiplier > 0, "Must be greater than zero: %s", multiplier);
    this.multiplier = multiplier;
    semaphore.drainPermits();
  }
}
