/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.maven;

import java.io.File;

import com.sonatype.nexus.perftest.ClientSwarm.ClientRequestInfo;
import com.sonatype.nexus.perftest.ClientSwarm.Operation;
import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.operation.AbstractNexusOperation;

import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deploys set of artifacts to an existing repository.
 */
public class RepositoryDeployOperation
    extends AbstractNexusOperation
    implements Operation
{
  private final File basedir;

  private final File pomTemplate;

  private final String groupId;

  private final String repo;

  private final Repositories repositories;

  @JsonCreator
  public RepositoryDeployOperation(@JacksonInject Nexus nexus,
                                   @JsonProperty("artifactsBasedir") File basedir,
                                   @JsonProperty("pomTemplate") File pomTemplate,
                                   @JsonProperty(value = "groupId", required = false) String groupId,
                                   @JsonProperty("repo") String repo)
  {
    super(nexus);
    this.basedir = basedir;
    this.pomTemplate = pomTemplate;
    this.groupId = groupId == null ? "test.group" : groupId;
    this.repo = repo;
    this.repositories = getNexusClient(newRepositoryFactories()).getSubsystem(Repositories.class);
  }

  @Override
  public void perform(ClientRequestInfo requestInfo) throws Exception {
    MavenHostedRepository repository = repositories.get(MavenHostedRepository.class, repo);

    final ArtifactDeployer deployer = new ArtifactDeployer(requestInfo.getHttpClient(), repository.contentUri());
    final String version = requestInfo.getClientId() + "." + requestInfo.getRequestId();

    Meter uploadedBytesMeter = requestInfo.getContextValue("metric.uploadedBytesMeter");
    long uploaded = 0;
    int artifactNo = 0;
    for (File file : basedir.listFiles()) {
      if (file.getName().endsWith(".jar")) {
        final String artifactId = String.format("artifact-%d", artifactNo++);
        uploaded += deployer.deployPom(groupId, artifactId, version, pomTemplate);
        uploaded += deployer.deployJar(groupId, artifactId, version, file);
      }
    }
    uploadedBytesMeter.mark(uploaded);
  }
}
