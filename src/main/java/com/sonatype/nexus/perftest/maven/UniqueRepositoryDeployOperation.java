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
 * Creates new repository, then deploys set of artifacts to the repository.
 */
public class UniqueRepositoryDeployOperation
    extends AbstractNexusOperation
    implements Operation
{

  private final File basedir;

  private final File pomTemplate;

  private final boolean deleteRepository;

  private final boolean disableIndexing;

  private final Repositories repositories;

  @JsonCreator
  public UniqueRepositoryDeployOperation(@JacksonInject Nexus nexus,
                                         @JsonProperty("artifactsBasedir") File basedir,
                                         @JsonProperty("pomTemplate") File pomTemplate,
                                         @JsonProperty("deleteRepository") boolean deleteRepository,
                                         @JsonProperty("disableIndexing") boolean disableIndexing)
  {
    super(nexus);
    this.basedir = basedir;
    this.pomTemplate = pomTemplate;
    this.deleteRepository = deleteRepository;
    this.disableIndexing = disableIndexing;
    this.repositories = getNexusClient(newRepositoryFactories()).getSubsystem(Repositories.class);
  }

  @Override
  public void perform(ClientRequestInfo requestInfo) throws Exception {
    final String repoId =
        String.format("%d.%d.%d", requestInfo.getClientId(), requestInfo.getRequestId(), System.currentTimeMillis());
    MavenHostedRepository repository = repositories.create(MavenHostedRepository.class, repoId);
    if (disableIndexing) {
      repository.excludeFromSearchResults();
    }
    repository.save();

    final ArtifactDeployer deployer = new ArtifactDeployer(requestInfo.getHttpClient(), repository.contentUri());
    final String groupId = "test.uniquerepodeploy"; // always the same groupId
    final String version = repoId;

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

    if (deleteRepository) {
      repository.remove().save();
    }
  }
}
