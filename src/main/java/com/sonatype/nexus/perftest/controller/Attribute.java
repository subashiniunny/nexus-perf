package com.sonatype.nexus.perftest.controller;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.google.common.base.Throwables;

public class Attribute<T>
{
  private final ObjectName name;

  private final String attribute;

  public Attribute(final ObjectName objectName, final String attribute) {
    this.name = objectName;
    this.attribute = attribute;
  }

  public Attribute(final String objectName, final String attribute) {
    this(objectName(objectName), attribute);
  }

  private static ObjectName objectName(final String name)
  {
    try {
      return new ObjectName(name);
    }
    catch (MalformedObjectNameException e) {
      throw Throwables.propagate(e);
    }
  }

  public ObjectName getName() {
    return name;
  }

  public String getAttribute() {
    return attribute;
  }
}

