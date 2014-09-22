/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.dx.merge;

import java.util.Iterator;

/** Guava's peeking iterator, with dependencies stripped. */
class PeekingIterator<E> implements Iterator<E> {

  private final Iterator<? extends E> iterator;
  private boolean hasPeeked;
  private E peekedElement;

  public PeekingIterator(Iterator<? extends E> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return hasPeeked || iterator.hasNext();
  }

  @Override
  public E next() {
    if (!hasPeeked) {
      return iterator.next();
    }
    E result = peekedElement;
    hasPeeked = false;
    peekedElement = null;
    return result;
  }

  @Override
  public void remove() {
    if (hasPeeked) throw new IllegalStateException("Can't remove after you've peeked at next");
    iterator.remove();
  }

  public E peek() {
    if (!hasPeeked) {
      peekedElement = iterator.next();
      hasPeeked = true;
    }
    return peekedElement;
  }
}
