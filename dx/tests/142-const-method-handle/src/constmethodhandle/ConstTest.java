/*
 * Copyright (C) 2017 The Android Open Source Project
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

package constmethodhandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class ConstTest {
  private static void displayMethodHandle(MethodHandle mh) throws Throwable {
    System.out.println("MethodHandle " + mh + " => " +
                       (Class) mh.invoke((Object) Float.valueOf(1.23e4f)));
  }

  private static void displayMethodType(MethodType mt) {
    System.out.println("MethodType " + mt);
  }
}
