/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.maven;

import com.sonatype.nexus.perftest.AbstractNexusOperation;
import com.sonatype.nexus.perftest.ClientSwarm.ClientRequestInfo;
import com.sonatype.nexus.perftest.ClientSwarm.Operation;
import com.sonatype.nexus.perftest.Nexus;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Downloads series of artifacts from a maven2 repository.
 */
public class DownloadOperation extends AbstractNexusOperation implements Operation {
  private final String repo;

  private final DownloadPaths paths;

  public DownloadOperation(@JacksonInject Nexus nexus, @JsonProperty("repo") String repo,
      @JsonProperty("paths") DownloadPaths paths ) {
    super(nexus);
    this.repo = repo;
    this.paths = paths;
  }

  @Override
  public void perform(ClientRequestInfo requestInfo) throws Exception {
    final String repoBaseUrl = getRepoBaseurl(repo);
    new DownloadAction(getHttpClient(), repoBaseUrl).download(paths.getNext());
  }
}
