package com.sonatype.nexus.perftest.npm;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import com.sonatype.nexus.perftest.ClientSwarm.ClientRequestInfo;
import com.sonatype.nexus.perftest.ClientSwarm.Operation;
import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.operation.AbstractNexusOperation;
import com.sonatype.nexus.perftest.operation.CircularIterator;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Installs packages using OS "npm install".
 */
public class InstallOperation
    extends AbstractNexusOperation
    implements Operation
{
  private final CircularIterator<Package> packages;

  private final String repositoryUrl;

  public InstallOperation(
      @JacksonInject Nexus nexus,
      @JsonProperty(value = "repo", required = true) String repo,
      @JsonProperty(value = "packages", required = true) Collection<Package> packages)
  {
    super(nexus);
    this.repositoryUrl = getRepoBaseurl(repo);
    this.packages = new CircularIterator<>(packages);
  }

  @Override
  public void perform(ClientRequestInfo requestInfo) throws Exception {
    Path tempDir = null;
    try {
      Package npmPackage = packages.next();
      tempDir = Files.createTempDirectory("nexus-perf-");
      Path packageDir = Files.createDirectories(tempDir.resolve(npmPackage.getName()));
      NpmClient npm = NpmClient.builder()
          .directory(packageDir.toFile())
          .cache(packageDir.resolve("cache").toFile())
          .registry(repositoryUrl)
          .build();
      if (npmPackage.getJsonFile() != null) {
        Files.copy(npmPackage.getJsonFile().toPath(), packageDir.resolve("package.json"));
        npm.install();
      }
      else {
        npm.install(npmPackage.getName());
      }
    }
    finally {
      if (tempDir != null) {
        try {
          deleteDirectory(tempDir);
        }
        catch (Exception e) {
          // ignore
        }
      }
    }
  }

  private void deleteDirectory(final Path packageDir) throws IOException {
    Files.walkFileTree(packageDir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
