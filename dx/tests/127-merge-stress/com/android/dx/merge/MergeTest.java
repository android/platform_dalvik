/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.merge;

import com.android.dex.Dex;
import com.android.dex.DexIndexOverflowException;
import com.android.dx.command.dexer.DxContext;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Random;
import java.util.HashSet;

/**
 * This test tries to merge given dex files at random, a first pass at 2 by 2, followed by
 * a second pass doing multi-way merges.
 */
public class MergeTest {

  private static final int NUMBER_OF_TRIES = 1000;

  private static final int WORKER_THREADS = 4;

  private static final ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);

  // Helper task to concurrently run merge tests.
  static class MergeTask implements Runnable {
    private final DexMerger dexMerger;
    private final String[] dexFiles;

    MergeTask(String[] dexFiles, Dex[] dexesToMerge) throws IOException {
      this.dexMerger = new DexMerger(dexesToMerge, CollisionPolicy.KEEP_FIRST, new DxContext());
      this.dexFiles = dexFiles;
    }

    public void run() {
      try {
        dexMerger.merge();
      } catch (DexIndexOverflowException e) {
        // ignore index overflow
      } catch (Throwable t) {
        System.err.println("Exception processing DEX files: " + t);
        System.err.println("Problem merging those dexes: " + Arrays.toString(dexFiles));
        System.exit(1);
      }
    }
  }

  public static void main(String[] args) throws Throwable {
    Random random = new Random();
    HashSet<Integer> seen = new HashSet<>();
    for (int pass = 0; pass < 2; pass++) {
      for (int i = 0; i < NUMBER_OF_TRIES; i++) {
        // On the first pass only do 2-way merges, then do from 3 to 10 way merges
        // but not more to avoid dex index overflow.
        int numDex = pass == 0 ? 2 : random.nextInt(8) + 3;
        numDex = Math.min(numDex, args.length);
        String[] fileNames = new String[numDex];
        for (int j = 0; j < numDex; ++j) {
          int fileIndex = random.nextInt(args.length);
          fileNames[j] = args[fileIndex];
        }

        if (!seen.add(fileNames.hashCode())) {
          // Skip, already seen set of file names with the same hash.
          continue;
        }

        Dex[] dexesToMerge = new Dex[numDex];
        for (int j = 0; j < numDex; ++j) {
          try {
            dexesToMerge[j] = new Dex(new File(fileNames[j]));
          } catch (IOException e) {
            System.err.println("Error opening " + fileNames[j]);
            System.err.println(e);
            System.exit(1);
          }
        }
        executor.execute(new MergeTask(fileNames, dexesToMerge));
      }
    }
    executor.shutdown();
    executor.awaitTermination(8, TimeUnit.HOURS);
  }
}
