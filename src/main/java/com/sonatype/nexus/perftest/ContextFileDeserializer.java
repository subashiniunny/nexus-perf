/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class ContextFileDeserializer
    extends JsonDeserializer<File>
{
  private final Context context;

  public ContextFileDeserializer(final Context context) {
    this.context = context;
  }

  @Override
  public File deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException, JsonProcessingException
  {
    JsonNode node = p.getCodec().readTree(p);
    return context.resolve(node.asText());
  }
}
