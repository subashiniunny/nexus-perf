/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.jmx;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.sonatype.nexus.perftest.ClientSwarm;

/**
 * JMX endpoint for ClientSwarm.
 */
public class ClientSwarmMBeanImpl
    extends StandardMBean
    implements ClientSwarmMBean
{
  private final ObjectName objectName;

  private final ClientSwarm clientSwarm;

  private final String metricsDomain;

  public ClientSwarmMBeanImpl(final ObjectName objectName, final ClientSwarm clientSwarm, String metricsDomain) {
    super(ClientSwarmMBean.class, false);
    this.objectName = objectName;
    this.clientSwarm = clientSwarm;
    this.metricsDomain = metricsDomain;
  }

  @Override
  public ObjectName getObjectName() {
    return objectName;
  }

  @Override
  public String getSwarmName() {
    return clientSwarm.getSwarmName();
  }

  @Override
  public int getRateSleepMillis() {
    return clientSwarm.getRate().getPeriodMillis();
  }

  @Override
  public void setRateSleepMillis(final int sp) {
    clientSwarm.getRate().setPeriodMillis(sp);
  }

  @Override
  public String getRatePeriod() {
    int period = ((int) TimeUnit.SECONDS.toMillis(1) / getRateSleepMillis()) * getRateMultiplier();
    return period + "/" + TimeUnit.SECONDS.name();
  }

  @Override
  public void setRatePeriod(final String value) {
    setRateSleepMillis(RequestRate.parsePeriod(value));
  }

  @Override
  public int getToDoCount() { return clientSwarm.getRate().getToDoCount(); }

  @Override
  public int getWaitingCount() { return clientSwarm.getRate().getWaitingCount(); }

  @Override
  public String getMetricsDomain() {
    return metricsDomain;
  }

  @Override
  public List<String> getFailures() {
    return clientSwarm.getFailures();
  }

  @Override
  public int getRateMultiplier() {
    return clientSwarm.getRate().getMultiplier();
  }

  @Override
  public void setRateMultiplier(final int multiplier) {
    clientSwarm.getRate().setMultiplier(multiplier);
  }
}
