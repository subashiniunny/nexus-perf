/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class PerformanceTestRunner
{
  public static void main(String[] args) throws Exception {
    File perfProperties = new File("perf.properties");
    if (!perfProperties.isFile()) {
      System.out.printf("No data?");
      System.exit(1);
    }

    Properties perf = new Properties();
    try (InputStream is = new FileInputStream(perfProperties)) {
      perf.load(is);
    }
    String scenario = perf.getProperty("perftest.scenario");
    File scenarioFile = new File(scenario).getCanonicalFile();
    if (!scenarioFile.isFile()) {
      System.out.printf("Scenario " + scenario + " found at " + scenarioFile);
      System.exit(1);
    }

    perf.forEach((k, v) -> {
      System.setProperty((String) k, (String) v);
    });

    new PerformanceTestRunner().run(scenarioFile);
  }

  private void run(final File dataDirectory) throws Exception {
    final Nexus nexus = new Nexus();
    ObjectMapper mapper = new XmlMapper();
    mapper.setInjectableValues(new InjectableValues()
    {
      @Override
      public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty,
                                        Object beanInstance)
      {
        if (Nexus.class.getName().equals(valueId)) {
          return nexus;
        }
        return null;
      }
    });
    System.out.format("Using test configuration %s\n", dataDirectory);
    PerformanceTest test = mapper.readValue(dataDirectory, PerformanceTest.class);
    test.run();
    System.out.println("Exit");
    System.exit(0);
  }
}
