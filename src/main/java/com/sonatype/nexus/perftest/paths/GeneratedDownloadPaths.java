/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.paths;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generated download paths that most probably does not exists on remote. Useful to exercise HttpClient and proxy
 * capabilities of Nexus.
 */
public class GeneratedDownloadPaths
    implements DownloadPaths
{
  private final AtomicInteger nextIndex = new AtomicInteger(0);

  private final String prefix;

  @JsonCreator
  public GeneratedDownloadPaths(@JsonProperty(value = "prefix", required = true) String prefix) {
    this.prefix = prefix;
  }

  @Override
  public String getNext() {
    return prefix + "/" + System.nanoTime();
  }

  @Override
  public Iterable<String> getAll() {
    throw new RuntimeException("getAll() or " + getClass().getSimpleName() + " cannot be invoked");
  }
}
