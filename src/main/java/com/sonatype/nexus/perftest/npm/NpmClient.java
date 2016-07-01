package com.sonatype.nexus.perftest.npm;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Charsets;
import com.google.common.collect.ObjectArrays;
import com.google.common.io.Files;
import io.airlift.command.Command;
import io.airlift.command.CommandFailedException;
import io.airlift.command.CommandResult;

public class NpmClient
{

  private final File directory;

  private final File cache;

  private final String registry;

  public NpmClient(File directory, File cache, String registry) {
    this.directory = directory;
    this.cache = cache != null ? cache : new File(System.getProperty("user.home"), ".npm");
    this.registry = registry != null ? registry : "https://registry.npmjs.org";
    if (!directory.exists()) {
      directory.mkdirs();
    }
    writeNpmrc();
  }

  public String publish(String... additionalArgs) throws CommandFailedException {
    String[] args = ObjectArrays.concat(new String[]{"npm", "publish", "--silent"}, additionalArgs, String.class);
    Executor executor = Executors.newFixedThreadPool(2);
    CommandResult npm = new Command(args)
        .setDirectory(directory)
        .setTimeLimit(5, TimeUnit.MINUTES)
        .execute(executor);
    return npm.getCommandOutput().trim();
  }

  public String install(String... additionalArgs) throws CommandFailedException {
    String[] args = ObjectArrays.concat(new String[]{"npm", "install", "--silent"}, additionalArgs, String.class);
    Executor executor = Executors.newFixedThreadPool(2);
    CommandResult npm = new Command(args)
        .setDirectory(directory)
        .setTimeLimit(5, TimeUnit.MINUTES)
        .execute(executor);
    return npm.getCommandOutput().trim();
  }

  private void writeNpmrc() {
    File npmrc = new File(directory, ".npmrc");
    try {
      Files.write(String.format("cache=%s\n", cache), npmrc, Charsets.UTF_8);
      Files.append(String.format("registry=%s", registry), npmrc, Charsets.UTF_8);
    }
    catch (IOException e) {
      throw new RuntimeException("Cannot generate");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {

    File directory;

    File cache;

    String registry;

    public Builder directory(File directory) {
      this.directory = directory;
      return this;
    }

    public Builder cache(File cache) {
      this.cache = cache;
      return this;
    }

    public Builder registry(String registry) {
      this.registry = registry;
      return this;
    }

    public NpmClient build() {
      return new NpmClient(directory, cache, registry);
    }
  }

  public static void main(String[] args) {
    File directory = new File("/tmp/poop1");
    File cache = new File(directory, "cache");
    NpmClient client = NpmClient.builder()
        .directory(directory)
        .registry("http://localhost:8081/nexus/content/groups/npmjs-group/")
        .cache(cache)
        .build();
  }
}
