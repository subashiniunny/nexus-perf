package com.sonatype.nexus.perftest.controller;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.sonatype.nexus.perftest.ClientSwarmMBean;

public class Swarm
    extends Client
{
  private final ObjectName objectName;

  private ClientSwarmMBean control;

  public Swarm(final MBeanServerConnection connection, final ObjectName objectName) {
    setConnection(connection);
    this.objectName = objectName;
  }

  public ClientSwarmMBean getControl() {
    if (control == null) {
      control = JMX.newMBeanProxy(getConnection(), objectName, ClientSwarmMBean.class, false);
    }
    return control;
  }

  @Override
  public <T> T get(final Attribute<T> attribute) {
    String canonicalName = attribute.getName().getCanonicalName();
    if (canonicalName.contains("${domain}")) {
      canonicalName = canonicalName.replace("${domain}", getControl().getMetricsDomain());
      return super.get(new Attribute<T>(canonicalName, attribute.getAttribute()));
    }
    return super.get(attribute);
  }

  public static class Success
  {
    public static final Attribute<Long> count = new Attribute<>(
        "${domain}:name=success", "Count"
    );
  }
}

