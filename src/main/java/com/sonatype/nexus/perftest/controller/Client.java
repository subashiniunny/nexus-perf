package com.sonatype.nexus.perftest.controller;

import java.util.ArrayList;
import java.util.Collection;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class Client
{
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private MBeanServerConnection connection;

  private Collection<Trigger> triggers = new ArrayList<>();

  public <T> T get(final Attribute<T> attribute) {
    return get(attribute.getName(), attribute.getAttribute());
  }

  public <T> T get(final ObjectName name, final String attribute) {
    try {
      return (T) connection.getAttribute(name, attribute);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  <T extends Trigger> void addTrigger(final T condition) {
    triggers.add(condition);
    if (connection != null) {
      condition.bind(this);
    }
  }

  protected MBeanServerConnection getConnection() {
    return connection;
  }

  protected void setConnection(final MBeanServerConnection connection) {
    this.connection = checkNotNull(connection);
    triggers.parallelStream().forEach(c -> c.bind(this));
  }

}

