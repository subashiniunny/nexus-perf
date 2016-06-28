/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.sonatype.nexus.perftest.maven.DownloadOperation;
import com.sonatype.nexus.perftest.paths.CsvLogParser;
import com.sonatype.nexus.perftest.paths.DownloadPaths;
import com.sonatype.nexus.perftest.paths.HttpdLogParser;

public class PrimeNexusScenario
{

  public static final int DOWNLOAD_TCOUNT = 20;

  public static void main(String[] args) throws Exception {
    Nexus nexus = new Nexus();

    String data = System.getProperty("data.file", "maven-3.1-build-artifact-access.log.gz");

    DownloadPaths paths = null;

    if (data.contains("csv")) {
      paths = new CsvLogParser(new File(data));
    }
    else {
      paths = new HttpdLogParser(new File(data));
    }


    final DownloadOperation download = new DownloadOperation(nexus, "public", paths);
    final RequestRate downloadRate = new RequestRate(5, TimeUnit.SECONDS);
    final ClientSwarm downloaders = new ClientSwarm(new Nexus(), "download", download, null, downloadRate, DOWNLOAD_TCOUNT);
    final Metric downloadMetric = downloaders.getMetric();

    downloaders.start();

    final Metric[] metrics = new Metric[]{downloadMetric};
    new ProgressTickThread(metrics).start();

    Thread.sleep(100L * 1000L);
  }
}
