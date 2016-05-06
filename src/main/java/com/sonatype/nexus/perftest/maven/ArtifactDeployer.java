/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import com.sonatype.nexus.perftest.Digests;

import com.google.common.base.Charsets;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

/**
 * Helper to deploy artifacts to a maven2 repository.
 */
public class ArtifactDeployer
{
  private final HttpClient httpclient;

  private final String repoUrl;

  public ArtifactDeployer(HttpClient httpclient, String repoUrl) {
    this.httpclient = httpclient;
    this.repoUrl = repoUrl.endsWith("/") ? repoUrl : (repoUrl + "/");
  }

  /**
   * Deploys provided pom.xml file under specified groupId, artifactId and version. The contents of the pom is updated
   * to match specified groupId, artifactId and version.
   */
  public long deployPom(String groupId, String artifactId, String version, File pomTemplate) throws IOException {
    final Document pom = XMLParser.parse(pomTemplate);

    pom.getRootElement().getChild("groupId").setText(groupId);
    pom.getRootElement().getChild("artifactId").setText(artifactId);
    pom.getRootElement().getChild("version").setText(version);
    // pom.getRootElement().getChild( "packaging" ).setText( "pom" );
    StringWriter buf = new StringWriter();
    XMLWriter writer = new XMLWriter(buf);
    pom.toXML(writer);
    String body = buf.toString();
    HttpEntity pomEntity = new StringEntity(body, ContentType.TEXT_XML);

    deploy(pomEntity, groupId, artifactId, version, ".pom");
    return body.getBytes(Charsets.UTF_8).length;
  }

  /**
   * Deploys provided file under specified groupId, artifactId and version with packaging=jar.
   */
  public long deployJar(String groupId, String artifactId, String version, File jar) throws IOException {
    HttpEntity jarEntity = new FileEntity(jar, ContentType.DEFAULT_BINARY);
    deploy(jarEntity, groupId, artifactId, version, ".jar");
    return jar.length();
  }

  private void deploy(HttpEntity entity, String groupId, String artifactId, String version, String extension)
      throws IOException
  {
    deploy0(entity, groupId, artifactId, version, extension);
    deploy0(getDigest(entity, "sha1"), groupId, artifactId, version, extension + ".sha1");
    deploy0(getDigest(entity, "md5"), groupId, artifactId, version, extension + ".md5");
  }

  private void deploy0(HttpEntity entity, String groupId, String artifactId, String version, String extension)
      throws IOException
  {
    StringBuilder path = new StringBuilder();

    path.append(groupId.replace('.', '/')).append('/');
    path.append(artifactId).append('/');
    path.append(version).append('/');
    path.append(artifactId).append('-').append(version).append(extension);

    HttpPut httpPut = new HttpPut(repoUrl + path);
    httpPut.setEntity(entity);

    HttpResponse response;
    try {
      response = httpclient.execute(httpPut);

      try {
        EntityUtils.consume(response.getEntity());
      }
      finally {
        httpPut.releaseConnection();
      }
    }
    catch (IOException e) {
      throw new IOException("IOException executing " + httpPut.toString(), e);
    }

    if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() > 299) {
      throw new IOException(httpPut.toString() + " : " + response.getStatusLine().toString());
    }
  }

  public static StringEntity getDigest(HttpEntity entity, String algorithm) throws IOException {
    try (InputStream inputStream = entity.getContent()) {
      return new StringEntity(Digests.getDigest(inputStream, algorithm), ContentType.TEXT_PLAIN);
    }
  }
}
