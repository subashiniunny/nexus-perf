package com.sonatype.nexus.perftest.controller;

import javax.management.MBeanServerConnection;

public interface Trigger
{

  void bind(AttributeSource attributeSource);

}

