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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

//import org.codehaus.jackson.map.ObjectMapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.nexus.perftest.maven.DownloadPaths;

public class NPMELogParser implements DownloadPaths{
  
  private static final String PREFIX = "/content/groups/npm-all/";

  // ossrh ssl public repo access log for 2013-08-01 contains 214895 paths 15458118 chars in total
  // this fits in ~30M of heap, so heap should not be a problem for any meaningful test.
  private final List<String> paths;

  private final AtomicInteger nextIndex = new AtomicInteger(0);

  public NPMELogParser(File logfile) throws IOException {
    this(logfile, PREFIX);
  }

  @JsonCreator
  public NPMELogParser(@JsonProperty("logfile") File logfile, @JsonProperty(value = "prefix") String prefix) throws IOException {
    ArrayList<String> paths = new ArrayList<>();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(logfile))))) {
      String str;
      while ((str = br.readLine()) != null) {
        //NPMELogMessage logMessage = new ObjectMapper().readValue(str,NPMELogMessage.class); 
        //String message = logMessage.getMessage();
        if (str != null && str.contains("\"GET ")){
          StringTokenizer st = new StringTokenizer(str, "[]\" ");
          st.nextToken(); // ip
          st.nextToken(); // not sure
          st.nextToken(); // username
          st.nextToken(); // [date:time
          st.nextToken(); // timezoneoffset]
          String method = st.nextToken(); // "METHOD
          if ("GET".equals(method)) {
            String path = st.nextToken(); // path
            if (!path.contains("@walmart")) {
              path = path.substring(2).replace("_attachments", "-");
              //System.out.println("path after fix="+path);
              paths.add(path);
            }
          }
        }
      }
    }
    this.paths = Collections.unmodifiableList(paths);
  }
  
  @Override
  public String getNext() {
    if (paths != null && paths.size() > 0){
      return paths.get(nextIndex.getAndIncrement() % paths.size());
    }
    return null;
  }

  @Override
  public Iterable<String> getAll() {
    return paths;
  }

}
