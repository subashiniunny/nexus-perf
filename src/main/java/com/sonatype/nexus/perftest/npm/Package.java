package com.sonatype.nexus.perftest.npm;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Package
{
  private final String name;

  private final File json;

  public Package(
      @JsonProperty(value = "name", required = true) String name,
      @JsonProperty(value = "json") File jsonFile)
  {
    this.name = name;
    this.json = jsonFile;
  }

  public String getName() {
    return name;
  }

  public File getJsonFile() {
    return json;
  }
}
