package com.sonatype.nexus.perftest.operation;

import java.io.IOException;
import java.io.InputStream;

import com.sonatype.nexus.perftest.ClientSwarm.ClientRequestInfo;
import com.sonatype.nexus.perftest.ClientSwarm.Operation;
import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.paths.DownloadPaths;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

/**
 * Downloads series of url from nexus repository.
 */
public class DownloadOperation
    extends AbstractNexusOperation
    implements Operation
{
  private final String prefix;

  private final String userAgent;

  private final DownloadPaths paths;

  private final String baseUrl;

  public DownloadOperation(
      @JacksonInject Nexus nexus,
      @JsonProperty(value = "repo", required = true) String repo,
      @JsonProperty(value = "context-root", defaultValue = "nexus") String contextRoot,
      @JsonProperty(value = "user-agent") String userAgent,
      @JsonProperty(value = "paths", required = true) DownloadPaths paths)
  {
    super(nexus);
    this.baseUrl = getRepoBaseurl(repo);
    this.prefix = "/" + contextRoot + "/";
    this.userAgent = userAgent;
    this.paths = paths;
  }

  @Override
  public void perform(ClientRequestInfo requestInfo) throws Exception {
    Meter downloadedBytesMeter = requestInfo.getContextValue("metric.downloadedBytesMeter");
    long downloaded = download(requestInfo.getHttpClient(), paths.getNext());
    downloadedBytesMeter.mark(downloaded);
  }

  private long download(HttpClient httpClient, String path) throws IOException {
    if (path != null && path.startsWith(prefix)) {
      path = path.substring(prefix.length());
    }

    final String url = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    final HttpGet httpGet = new HttpGet(url);
    if (userAgent != null) {
      httpGet.addHeader(HttpHeaders.USER_AGENT, userAgent);
    }
    final HttpResponse response = httpClient.execute(httpGet);

    long size = 0;
    if (isSuccess(response)) {
      size = getEntitySize(response.getEntity());
    }
    else {
      EntityUtils.consume(response.getEntity());
      if (response.getStatusLine().getStatusCode() != 404) {
        throw new IOException(response.getStatusLine().toString());
      }
    }
    return size;
  }

  private long getEntitySize(final HttpEntity entity) throws IOException {
    long size = 0;
    int numRead;
    try (InputStream in = entity.getContent()) {
      do {
        numRead = in.read();
        if (numRead > 0) {
          size += numRead;
        }
      }
      while (numRead != -1);
    }
    return size;
  }
}
