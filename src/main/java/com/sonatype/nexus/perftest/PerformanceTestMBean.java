/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.management.ObjectName;

/**
 * JMX endpoint for Performance client.
 */
public interface PerformanceTestMBean
{
  /**
   * Starts the performance test with given scenario and returns the {@link ObjectName}s of the swarms started in
   * given scenario. If already running, method does nothing and returns {@code null}.
   *
   * @see #start(String, Map)
   */
  List<ObjectName> start(String scenario);

  /**
   * Starts the performance test with given scenario and returns the {@link ObjectName}s of the swarms started in
   * given scenario. If already running, method does nothing and returns {@code null}. It also may receive a map
   * with overrides, that will override values from scenario map.
   */
  List<ObjectName> start(String scenario, @Nullable Map<String, String> overrides);

  /**
   * Stops the performance test, returns {@code true} if it was running.
   */
  boolean stop();

  /**
   * Returns {@code true} if performce test is running, {@code false} otherwise.
   */
  boolean isRunning();

  /**
   * Stops the performance test if runs, and exits the app, thus JVM too, with given code.
   */
  void exit(int code);
}
