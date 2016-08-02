package com.sonatype.nexus.perftest.npm;

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
import com.sonatype.nexus.perftest.paths.DownloadPaths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkArgument;

public class NPMELogParser
    extends CircularIterator<String>
    implements DownloadPaths
{
  @JsonCreator
  public NPMELogParser(final @JsonProperty(value = "logfile", required = true) File logfile)
      throws IOException
  {
    super(parse(logfile));
    checkArgument(getSize() > 0, "No paths loaded");
  }

  private static List<String> parse(final File logfile) throws IOException {
    ArrayList<String> paths = new ArrayList<>();
    try (BufferedReader br =
             new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(logfile))))) {
      String str;
      while ((str = br.readLine()) != null) {
        if (str.contains("\"GET ")) {
          StringTokenizer st = new StringTokenizer(str, "[]\" ");
          st.nextToken(); // ip
          st.nextToken(); // not sure
          st.nextToken(); // username
          st.nextToken(); // [date:time
          st.nextToken(); // timezoneoffset]
          String method = st.nextToken(); // "METHOD
          if ("GET".equals(method)) {
            String path = st.nextToken(); // path
            path = path.substring(2).replace("_attachments", "-");
            paths.add(path);
          }
        }
      }
    }
    return Collections.unmodifiableList(paths);
  }
}