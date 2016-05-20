/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.sonatype.nexus.perftest;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.core.spi.subsystem.repository.RepositoryFactory;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.maven.JerseyMavenGroupRepositoryFactory;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.maven.JerseyMavenHostedRepositoryFactory;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.maven.JerseyMavenProxyRepositoryFactory;
import org.sonatype.nexus.client.rest.AuthenticationInfo;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.client.rest.ConnectionInfo;
import org.sonatype.nexus.client.rest.UsernamePasswordAuthenticationInfo;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClientFactory;
import org.sonatype.nexus.client.rest.jersey.subsystem.JerseyRepositoriesFactory;

import com.bolyuba.nexus.plugin.npm.client.internal.JerseyNpmGroupRepositoryFactory;
import com.bolyuba.nexus.plugin.npm.client.internal.JerseyNpmHostedRepositoryFactory;
import com.bolyuba.nexus.plugin.npm.client.internal.JerseyNpmProxyRepositoryFactory;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNexusOperation
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final String username;

  protected final String password;

  protected final String nexusBaseurl;

  public AbstractNexusOperation(Nexus nexus) {
    this.nexusBaseurl = sanitizeBaseurl(nexus.getBaseurl());
    this.username = nexus.getUsername();
    this.password = nexus.getPassword();
  }

  private static String sanitizeBaseurl(String nexusBaseurl) {
    if (nexusBaseurl == null || nexusBaseurl.trim().isEmpty()) {
      return null;
    }
    nexusBaseurl = nexusBaseurl.trim();
    return nexusBaseurl.endsWith("/") ? nexusBaseurl : (nexusBaseurl + "/");
  }

  protected boolean isSuccess(HttpResponse response) {
    return response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299;
  }

  protected NexusClient getNexusClient(final SubsystemFactory<?, JerseyNexusClient>... subsystemFactories) {
    if (nexusBaseurl == null) {
      throw new IllegalStateException();
    }
    BaseUrl baseUrl;
    try {
      baseUrl = BaseUrl.baseUrlFrom(this.nexusBaseurl);
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    AuthenticationInfo authenticationInfo = new UsernamePasswordAuthenticationInfo(this.username, this.password);
    ConnectionInfo connectionInfo = new ConnectionInfo(baseUrl, authenticationInfo, null /* proxyInfos */);
    return new JerseyNexusClientFactory(subsystemFactories).createFor(connectionInfo);
  }

  protected JerseyRepositoriesFactory newRepositoryFactories() {
    Set<RepositoryFactory> repositoryFactories = new HashSet<>();
    repositoryFactories.add(new JerseyMavenHostedRepositoryFactory());
    repositoryFactories.add(new JerseyMavenGroupRepositoryFactory());
    repositoryFactories.add(new JerseyMavenProxyRepositoryFactory());
    repositoryFactories.add(new JerseyNpmHostedRepositoryFactory());
    repositoryFactories.add(new JerseyNpmGroupRepositoryFactory());
    repositoryFactories.add(new JerseyNpmProxyRepositoryFactory());
    return new JerseyRepositoriesFactory(repositoryFactories);
  }

  protected String getRepoBaseurl(String repoid) {
    return getNexusClient(newRepositoryFactories()).getSubsystem(Repositories.class).get(repoid).contentUri();
  }
}
