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
   * Loads the performance test with given scenario and returns the {@link ObjectName}s of the swarms started in
   * given scenario.
   *
   * @see #load(String, Map)
   */
  List<ObjectName> load(String scenario);

  /**
   * Loads the performance test with given scenario and returns the {@link ObjectName}s of the swarms started in
   * given scenario. It also may receive a map with overrides, that will override values from scenario map.
   */
  List<ObjectName> load(String scenario, @Nullable Map<String, String> overrides);

  /**
   * Starts the loaded performance test.
   */
  void start();

  /**
   * Stops the performance test, returns {@code true} if it was running.
   */
  boolean stop();

  /**
   * Returns {@code true} if performance test is running, {@code false} otherwise.
   */
  boolean isRunning();

  /**
   * @return a string representation of test duration using ISO-8601 seconds based representation, such as
   * PT8H6M12.345S.
   */
  String getDuration();

  /**
   * Stops the performance test if runs, and exits the app, thus JVM too, with given code.
   */
  void exit(int code);
}
