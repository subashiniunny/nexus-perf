/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License Version 1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class RequestRateTest {
  @Test
  public void testStringParsing() {
    Assert.assertEquals(TimeUnit.DAYS.toMillis(1) / 10, new RequestRate("10/DAY").getPeriodMillis());
    Assert.assertEquals(TimeUnit.DAYS.toMillis(1) / 10, new RequestRate(" 10 / DAY ").getPeriodMillis());
  }

  @Test
  public void testNextDelay() {
    RequestRate rate = new RequestRate("100/HOUR");
    int count = 100;
    long delay = 0;
    double total = 0;
    for (int i = 0; i < count; i++) {
      long nextDelay = rate.nextDelayMillis();
      Assert.assertTrue(Integer.toString(i) + " next=" + nextDelay + " curr=" + delay, nextDelay > delay);
      total += (nextDelay - delay);
      delay = nextDelay;
    }
    Assert.assertEquals((double) rate.getPeriodMillis(), total / count, rate.getPeriodMillis() / 100);
  }

}
