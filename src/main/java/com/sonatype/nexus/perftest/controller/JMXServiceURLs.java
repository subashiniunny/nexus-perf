package com.sonatype.nexus.perftest.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.management.remote.JMXServiceURL;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class JMXServiceURLs
{
  public static Collection<JMXServiceURL> of(final JMXServiceURL... urls) {
    return Lists.newArrayList(urls);

  }

  public static Collection<JMXServiceURL> of(final String... targets) {
    return Arrays.stream(targets).map(JMXServiceURLs::jmxServiceURL).collect(Collectors.toList());
  }

  public static JMXServiceURL jmxServiceURL(final String target) {
    try {
      return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + target + "/jmxrmi");
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public static JMXServiceURL jmxServiceURL(final int servicePort) {
    try {
      return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + servicePort + "/jmxrmi");
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
