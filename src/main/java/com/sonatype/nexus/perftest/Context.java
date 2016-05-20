/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

public class Context
{
  private final File dataDirectory;

  private final File scenarioFile;

  private final Properties properties;

  public Context(final File dir, @Nullable final Map<String, String> overrides) throws IOException {
    this.dataDirectory = dir.getCanonicalFile();
    if (!dataDirectory.isDirectory()) {
      throw new IllegalArgumentException(dataDirectory + "is not a directory");
    }
    File perfProperties = new File(dataDirectory, "perf.properties");
    if (!perfProperties.isFile()) {
      throw new IllegalArgumentException(perfProperties + " not found");
    }

    properties = new Properties();
    try (InputStream is = new FileInputStream(perfProperties)) {
      properties.load(is);
    }
    if (overrides != null) {
      overrides.forEach((k, v) -> properties.setProperty(k, v));
    }
    String scenario = properties.getProperty("perftest.scenario");
    this.scenarioFile = resolve(scenario);
    if (!scenarioFile.isFile()) {
      throw new IllegalArgumentException(scenarioFile + " not found");
    }

    properties.forEach((k, v) -> {
      if (v != null && v.toString().trim().length() != 0) {
        System.setProperty(k.toString(), v.toString());
      }
    });
  }

  public File getDataDirectory() {
    return dataDirectory;
  }

  public File getScenario() {
    return scenarioFile;
  }

  public Properties getProperties() {
    return properties;
  }

  public File resolve(String path) throws IOException {
    return new File(getDataDirectory(), path).getCanonicalFile();
  }
}
