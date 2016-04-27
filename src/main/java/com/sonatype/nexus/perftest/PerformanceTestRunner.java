/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.File;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class PerformanceTestRunner
{
  public static void main(String[] args) throws Exception {
    if (args == null || args.length < 1) {
      throw new IllegalArgumentException("You must specify the dataDirectory");
    }

    new PerformanceTestRunner().run(new File(args[0]));
  }

  public void run(File dataDirectory) throws Exception {
    Context context = new Context(dataDirectory);
    runTest(context);
  }

  private void runTest(final Context context) throws Exception {
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
    System.out.format("Using scenario %s\n", context.getScenario());
    PerformanceTest test = mapper.readValue(context.getScenario(), PerformanceTest.class);
    test.run();
    System.out.println("Exit");
    System.exit(0);
  }
}
