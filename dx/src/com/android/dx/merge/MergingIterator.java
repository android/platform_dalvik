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

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

/** Guava's merging iterator, with dependencies stripped. */
class MergingIterator<T extends Comparable<T>> implements Iterator<T> {
  final Queue<PeekingIterator<T>> queue;

  public MergingIterator(Iterable<? extends Iterator<? extends T>> iterators) {
    // A comparator that's used by the heap, allowing the heap
    // to be sorted based on the top of each iterator.
    Comparator<PeekingIterator<T>> heapComparator =
        new Comparator<PeekingIterator<T>>() {
          @Override
          public int compare(PeekingIterator<T> o1, PeekingIterator<T> o2) {
            return o1.peek().compareTo(o2.peek());
          }
        };

    queue = new PriorityQueue<PeekingIterator<T>>(2, heapComparator);

    for (Iterator<? extends T> iterator : iterators) {
      if (iterator.hasNext()) {
        queue.add(new PeekingIterator<T>(iterator));
      }
    }
  }

  @Override
  public boolean hasNext() {
    return !queue.isEmpty();
  }

  @Override
  public T next() {
    PeekingIterator<T> nextIter = queue.remove();
    T next = nextIter.next();
    if (nextIter.hasNext()) {
      queue.add(nextIter);
    }
    return next;
  }

  @Override public void remove() {
    throw new UnsupportedOperationException();
  }
}
