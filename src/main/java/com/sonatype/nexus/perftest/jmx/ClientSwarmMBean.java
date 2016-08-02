/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.jmx;

import java.util.List;

import javax.management.ObjectName;

/**
 * JMX endpoint for ClientSwarm.
 */
public interface ClientSwarmMBean
{
  /**
   * Returns {@link ObjectName} of this swarm.
   */
  ObjectName getObjectName();

  /**
   * Swarm name as set in scenario.
   */
  String getSwarmName();

  /**
   * Returns the multiplier of to-do's, basically controls how many permits are put into Semaphore at given rate,
   * hence, it multiplies to available work.
   */
  int getRateMultiplier();

  /**
   * Sets the multiplier, it must be positive integer (>0).
   */
  void setRateMultiplier(final int multiplier);

  /**
   * Gets the sleep time in millis, the period rate thread sleeps to release permit(s) to semaphore. This number is in
   * direct ratio with {@link #getRatePeriod()} and is exposed just as a convenience.
   */
  int getRateSleepMillis();

  /**
   * Sets the sleep time in millis, the period rate thread sleeps to release permit(s) to semaphore. This number is in
   * direct ratio with {@link #setRatePeriod(String)} and is exposed just as a convenience.
   */
  void setRateSleepMillis(int sp);

  /**
   * Returns the rate "human" description as {@code 5/SECOND} which means "five per second".
   */
  String getRatePeriod();

  /**
   * Sets the rate in "human", if form of {@code integer + "/" + TimeUnit}. Example: {@code 5/SECOND}.
   */
  void setRatePeriod(String value);

  /**
   * Returns the to-do's available to be worked on. In normal circumstances this should be zero. If this
   * number is larger than 0, it means the swarm is depleted, and your rate parameters are out of range for given
   * swarm.
   */
  int getToDoCount();

  /**
   * Returns the swarm members count that are waiting for to-do's. Best if this number is non-zero, meaning you still
   * have some reserve in the swarm. If zero, it means you probably depleted your swarm, and to-do's will start
   * growing too, see {@link #getToDoCount()}.
   */
  int getWaitingCount();

  String getMetricsDomain();

  List<String> getFailures();
}
