package com.sonatype.nexus.perftest.config;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.nexus.perftest.AbstractNexusOperation;
import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.PerformanceTest.NexusConfigurator;

import org.sonatype.nexus.client.core.subsystem.repository.GroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.HostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenGroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;

import com.bolyuba.nexus.plugin.npm.client.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Operation creating and optionally dropping the repository.
 */
public class CreateRepository
    extends AbstractNexusOperation
    implements NexusConfigurator
{
  // format -> type -> Class
  // formats: hosted, proxy, group
  // type: maven, npm
  private static final Map<String, Map<String, Class<? extends Repository>>> repoTypes;

  static {
    repoTypes = new HashMap<>();
    repoTypes.put("maven", new HashMap<>());
    repoTypes.put("npm", new HashMap<>());

    repoTypes.get("maven").put("hosted", MavenHostedRepository.class);
    repoTypes.get("maven").put("proxy", MavenProxyRepository.class);
    repoTypes.get("maven").put("group", MavenGroupRepository.class);
    repoTypes.get("npm").put("hosted", NpmHostedRepository.class);
    repoTypes.get("npm").put("proxy", NpmProxyRepository.class);
    repoTypes.get("npm").put("group", NpmGroupRepository.class);
  }

  private final String repo;

  private final String format;

  private final String type;

  private final String proxyOf;

  private final String members;

  private final boolean deleteRepository;

  private final boolean failIfExists;

  private final Repositories repositories;

  private Repository repository;

  private Exception createException;

  @JsonCreator
  public CreateRepository(@JacksonInject final Nexus nexus,
                          @JsonProperty("repo") String repo,
                          @JsonProperty("format") String format,
                          @JsonProperty("type") String type,
                          @JsonProperty("proxyOf") String proxyOf,
                          @JsonProperty("members") String members,
                          @JsonProperty("deleteRepository") boolean deleteRepository,
                          @JsonProperty("failIfExists") boolean failIfExists)
  {
    super(nexus);
    this.repo = repo;
    this.format = format;
    this.type = type;
    this.proxyOf = proxyOf;
    this.members = members;
    this.deleteRepository = deleteRepository;
    this.failIfExists = failIfExists;

    this.repositories = getNexusClient(newRepositoryFactories()).getSubsystem(Repositories.class);
  }

  @Override
  public void prepare() throws Exception {
    try {
      repository = create();
      repository.save();
      System.out.println("Created repository: " + repo + "(" + format + ", " + type + ")");
    }
    catch (Exception e) {
      if (!failIfExists) {
        createException = e;
        repository = repositories.get(repo);
        System.out.println("Using existing repository: " + repo);
      }
      else {
        throw e;
      }
    }
  }

  @Override
  public void cleanup() throws Exception {
    if (createException == null && deleteRepository) {
      repository.remove().save();
    }
  }

  private Repository create() {
    if ("hosted".equals(type)) {
      Class<HostedRepository> repositoryClass = (Class<HostedRepository>) repoTypes.get(format).get(type);
      HostedRepository<HostedRepository> repository = repositories.create(repositoryClass, repo);
      return repository;
    }
    else if ("proxy".equals(type)) {
      Class<ProxyRepository> repositoryClass = (Class<ProxyRepository>) repoTypes.get(format).get(type);
      ProxyRepository<ProxyRepository> repository = repositories.create(repositoryClass, repo);
      repository.asProxyOf(proxyOf);
      return repository;
    }
    else if ("group".equals(type)) {
      Class<GroupRepository> repositoryClass = (Class<GroupRepository>) repoTypes.get(format).get(type);
      GroupRepository<GroupRepository> repository = repositories.create(repositoryClass, repo);
      repository.addMember(members.split(","));
      return repository;
    }
    else {
      throw new IllegalArgumentException("Unknown type " + type);
    }
  }
}
