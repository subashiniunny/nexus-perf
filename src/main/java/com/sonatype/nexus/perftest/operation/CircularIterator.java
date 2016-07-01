/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.operation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class CircularIterator<T>
{
  private final List<T> elements;

  private final AtomicInteger nextIndex = new AtomicInteger(0);

  public CircularIterator(final Collection<T> elements) {
    checkNotNull(elements);
    this.elements = new LinkedList<T>(elements);
  }

  public T getNext() {
    return elements.get(nextIndex.getAndIncrement() % elements.size());
  }

  public Iterable<T> getAll() {
    return elements;
  }

  public int getSize() {
    return elements.size();
  }
}
