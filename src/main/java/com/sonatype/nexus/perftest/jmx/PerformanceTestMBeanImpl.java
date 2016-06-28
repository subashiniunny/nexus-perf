/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.jmx;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.StandardEmitterMBean;

import com.sonatype.nexus.perftest.PerformanceTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.perftest.PerformanceTestRunner.create;

/**
 * JMX endpoint for PerformanceTestMBean client.
 */
public class PerformanceTestMBeanImpl
    extends NotificationBroadcasterSupport
    implements PerformanceTestMBean
{
  private static final Logger log = LoggerFactory.getLogger(PerformanceTestMBeanImpl.class);

  private PerformanceTest performanceTest;

  private String scenario;

  private Thread performanceTestThread;

  private int notificationSequence;

  public PerformanceTestMBeanImpl() throws Exception
  {
    ObjectName objectName = ObjectName.getInstance(getClass().getPackage().getName(), "name", "control");
    StandardEmitterMBean mbean = new StandardEmitterMBean(this, PerformanceTestMBean.class, this);
    ManagementFactory.getPlatformMBeanServer().registerMBean(mbean, objectName);
  }

  @Override
  public List<ObjectName> load(final String scenario) {
    return load(scenario, null);
  }

  @Override
  public synchronized List<ObjectName> load(final String scenario, @Nullable final Map<String, String> overrides) {
    if (performanceTest != null) {
      throw new IllegalStateException("A scenario has already been loaded");
    }
    try {
      this.scenario = scenario;
      performanceTest = create(new File(scenario), overrides);
      return performanceTest.getObjectNames();
    }
    catch (Exception e) {
      log.error("Error", e);
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public synchronized void start() {
    if (performanceTest == null) {
      throw new IllegalStateException("A scenario must be loaded before starting it");
    }
    if (performanceTestThread == null || !performanceTestThread.isAlive()) {
      try {
        performanceTestThread = new Thread(
            () -> {
              try {
                sendNotification(new AttributeChangeNotification(
                    getClass().getSimpleName(),
                    notificationSequence++,
                    System.currentTimeMillis(),
                    scenario,
                    "running",
                    "boolean",
                    Boolean.FALSE,
                    Boolean.TRUE)
                );

                performanceTest.run();
              }
              finally {
                sendNotification(new AttributeChangeNotification(
                    getClass().getSimpleName(),
                    notificationSequence++,
                    System.currentTimeMillis(),
                    scenario,
                    "running",
                    "boolean",
                    Boolean.TRUE,
                    Boolean.FALSE)
                );
              }
            },
            "test"
        );
        performanceTestThread.start();
      }
      catch (Exception e) {
        log.error("Error", e);
        throw new RuntimeException(e.getMessage());
      }
    }
  }

  @Override
  public synchronized boolean stop() {
    try {
      if (performanceTest != null) {
        performanceTest.stop();
        performanceTest = null;
        if (performanceTestThread != null) {
          performanceTestThread.join();
          performanceTestThread = null;
          return true;
        }
      }
      return false;
    }
    catch (Exception e) {
      log.error("Error", e);
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return new MBeanNotificationInfo[]{
        new MBeanNotificationInfo(
            new String[]{"running"},
            AttributeChangeNotification.class.getName(),
            "Swarm running state"
        )
    };
  }

  @Override
  public boolean isRunning() {
    return performanceTestThread != null && performanceTestThread.isAlive();
  }

  @Override
  public String getDuration() {
    if (performanceTest == null) {
      return null;
    }
    return java.time.Duration.ofMillis(performanceTest.getDuration().toMillis()).toString();
  }

  @Override
  public void exit(final int code) {
    try {
      stop();
    }
    finally {
      log.info("Exit {}", code);
      System.exit(code);
    }
  }
}
