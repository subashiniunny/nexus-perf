/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.paths;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import com.sonatype.nexus.perftest.operation.CircularIterator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkArgument;

public class CsvLogParser
    extends CircularIterator<String>
    implements DownloadPaths
{
  @JsonCreator
  public CsvLogParser(final @JsonProperty(value = "logfile", required = true) File logfile) throws IOException {
    super(parse(logfile));
    checkArgument(getSize() > 0, "No paths loaded");
  }

  private static List<String> parse(final File logfile) throws IOException {
    ArrayList<String> paths = new ArrayList<>();
    try (BufferedReader br =
             new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(logfile))))) {
      String str;
      while ((str = br.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(str, ",");
        paths.add(st.nextToken());  // full path
      }
    }
    return Collections.unmodifiableList(paths);
  }
}
