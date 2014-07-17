/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Duration {
  private final long value;

  private final TimeUnit unit;

  public Duration(long value, TimeUnit unit) {
    this.value = value;
    this.unit = unit;
  }

  @JsonCreator
  public Duration(String value) {
    StringTokenizer st = new StringTokenizer(value);
    this.value = Long.parseLong(st.nextToken());
    this.unit = TimeUnit.valueOf(st.nextToken());
  }

  public long toMillis() {
    return unit.toMillis(value);
  }
}