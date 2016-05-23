/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.sonatype.nexus.perftest.Digests;

import com.google.common.io.CountingInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

/**
 * Downloads specified artifact, verifies checksum, throws IOException if downloads fails or checksum is invalid
 */
public class DownloadAction
{
  private final String baseUrl;

  private static class Checksumer
  {
    private final HttpEntity entity;

    private String sha1;

    private long length;

    public Checksumer(HttpEntity entity) {
      this.entity = entity;
    }

    public void consumeEntity() throws IOException {
      try (InputStream inputStream = entity.getContent()) {
        CountingInputStream cis = new CountingInputStream(inputStream);
        this.sha1 = Digests.getDigest(cis, "sha1");
        this.length = cis.getCount();
      }
    }

    public String getSha1() {
      return sha1;
    }

    public long getLength() { return length; }
  }

  public DownloadAction(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public long download(HttpClient httpClient, String path) throws IOException {

    String pref = "/nexus/";
    if (path != null && path.startsWith(pref)){
      path = path.substring(pref.length());
    }

    final String url = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    final HttpGet httpGet = new HttpGet(url);
    final HttpResponse response = httpClient.execute(httpGet);

    if (!isSuccess(response)) {
      EntityUtils.consume(response.getEntity());

      if (response.getStatusLine().getStatusCode() != 404) {
        throw new IOException(response.getStatusLine().toString());
      }

      return 0;
    }

    // consume entity entirely
    final Checksumer checksumer = new Checksumer(response.getEntity());
    checksumer.consumeEntity();

    if (!url.contains(".meta/nexus-smartproxy-plugin/handshake/") && !url.endsWith(".sha1")) {
      final String sha1 = getUrlContents(httpClient, url + ".sha1");
      if (sha1 != null) {
        if (!sha1.startsWith(checksumer.getSha1())) {
          throw new IOException("Wrong SHA1 " + url);
        }
      }
    }
    return checksumer.getLength();
  }

  protected boolean isSuccess(HttpResponse response) {
    return response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299;
  }

  private String getUrlContents(HttpClient httpClient, String url) throws IOException {
    final HttpGet httpGet = new HttpGet(url);
    final HttpResponse response = httpClient.execute(httpGet);
    if (!isSuccess(response)) {
      EntityUtils.consume(response.getEntity());
      return null;
    }
    return EntityUtils.toString(response.getEntity(), (Charset) null);
  }

}
