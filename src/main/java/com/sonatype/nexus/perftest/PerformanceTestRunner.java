/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class PerformanceTestRunner
{
  private static final Logger log = LoggerFactory.getLogger(PerformanceTestRunner.class);

  public static void main(String[] args) throws Exception {
    if (args == null || args.length < 1) {
      throw new IllegalArgumentException(
          "You must specify the dataDirectory OR string 'remote' for remote JXM control");
    }

    String argument = args[0];
    if ("remote".equals(argument)) {
      // JMX remote control
      log.info("JMX controlled nexus-perf client");
      new PerformanceTestMBeanImpl();
      new CountDownLatch(1).await();
    }
    else {
      // old behaviour
      PerformanceTest test = create(new File(args[0]), null);
      test.run();
      log.info("Exit");
      System.exit(0);
    }
  }

  public static PerformanceTest create(final File dataDirectory, @Nullable final Map<String, String> overrides)
      throws Exception
  {
    checkArgument(dataDirectory.isDirectory(), "Not a directory: %s", dataDirectory);
    Context context = new Context(dataDirectory, overrides);
    final Nexus nexus = new Nexus();
    ObjectMapper mapper = new XmlMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(File.class, new ContextFileDeserializer(context));
    mapper.registerModule(module);
    mapper.setInjectableValues(new InjectableValues()
    {
      @Override
      public Object findInjectableValue(Object valueId,
                                        DeserializationContext ctxt,
                                        BeanProperty forProperty,
                                        Object beanInstance)
      {
        if (Nexus.class.getName().equals(valueId)) {
          return nexus;
        }
        return null;
      }
    });
    log.info("Using scenario {}", context.getScenario());
    PerformanceTest performanceTest = mapper.readValue(context.getScenario(), PerformanceTest.class);
    if (overrides != null) {
      String duration = overrides.get("test.duration");
      if (duration != null) {
        performanceTest.setDuration(new Duration(duration));
      }
    }
    return performanceTest;
  }
}
