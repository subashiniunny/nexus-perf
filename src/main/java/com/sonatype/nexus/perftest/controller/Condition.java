package com.sonatype.nexus.perftest.controller;

import javax.management.MBeanServerConnection;

public interface Condition
{

  void bind(AttributeSource attributeSource);

}

