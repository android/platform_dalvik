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

/**
 * AOSP JFuzz Tester.
 * Automatically generated program.
 * jfuzz -s 3674365851 -d 1 -l 8 -i 2 -n 3 (version 1.3)
 */

import java.util.Arrays;

public class Test {

  private interface X {
    int x();
  }

  private class A {
    public int a() {
      return (mI--);
    }
  }

  private class B extends A implements X {
    public int a() {
      return super.a() + ((235022827 >> mI) & 574409782);
    }
    public int x() {
      return ((mZ) ? mI : ((Math.addExact(852067216, 246625693)) % mI));
    }
  }

  private static class C implements X {
    public static int s() {
      return 1878004320;
    }
    public int c() {
      return -618864356;
    }
    public int x() {
      return -556993081;
    }
  }

  private A mA  = new B();
  private B mB  = new B();
  private X mBX = new B();
  private C mC  = new C();
  private X mCX = new C();

  private boolean mZ = false;
  private int     mI = 0;
  private long    mJ = 0;
  private float   mF = 0;
  private double  mD = 0;

  private float[][][][][][][] mArray = new float[2][2][2][2][2][2][2];

  private Test() {
    float a = -774156645.0f;
    for (int i0 = 0; i0 < 2; i0++) {
      for (int i1 = 0; i1 < 2; i1++) {
        for (int i2 = 0; i2 < 2; i2++) {
          for (int i3 = 0; i3 < 2; i3++) {
            for (int i4 = 0; i4 < 2; i4++) {
              for (int i5 = 0; i5 < 2; i5++) {
                for (int i6 = 0; i6 < 2; i6++) {
                  mArray[i0][i1][i2][i3][i4][i5][i6] = a;
                  a++;
                }
              }
            }
          }
        }
      }
    }
  }

  private float testMethod() {
    {
      long lJ0 = (Long.MIN_VALUE);
      if (((boolean) new Boolean(mZ))) {
        lJ0 |= ((-609721394L | -601487228L) + (--mJ));
        for (int i0 = mArray.length - 1; i0 >= 0; i0--) {
          mF = ((float) new Float(921021787.0f));
          mZ ^= ((mZ ? (mZ) : (Boolean.logicalXor((Boolean.logicalAnd(mZ, mZ)), mZ))) ^ true);
          for (int i1 = 2 - 1; i1 >= 0; i1--) {
            {
              int i2 = -1;              while (++i2 < mArray.length) {
                mI = ((int) new Integer(((int) mD)));
                mI <<= (mI++);
                mZ = ((boolean) new Boolean(true));
                mJ >>= (- (~ (lJ0++)));
              }
            }
          }
          mI >>= (Integer.MIN_VALUE);
        }
        mD *= (mC.x());
        mI *= (Math.multiplyExact(61363273, (-1448306837 | mI)));
      } else {
        mZ = (mZ);
        for (int i0 = 0; i0 < 2; i0++) {
          for (int i1 = 2 - 1; i1 >= 0; i1--) {
            mArray[i0][i1][mArray.length - 1][i0][i1][1][i1] *= ((! mZ) ? -1041059197.0f : (mF / -1370212878.0f));
            lJ0 += (~ mJ);
            for (int i2 = 2 - 1; i2 >= 0; i2--) {
              {
                int i3 = -1;                while (++i3 < 2) {
                  return ((mZ) ? ((float) new Float((--mF))) : (++mF));
                }
              }
              mI = (--mI);
            }
            mJ &= (--lJ0);
          }
          if (((mZ) ^ false)) {
            mI >>>= (mC.c());
          } else {
            if (((+ (Long.reverseBytes(mJ))) >= 1517367973L)) {
              mI <<= (mI--);
            } else {
              {
                int i1 = 0;                do {
                  mZ |= (true && true);
                  if ((Boolean.logicalAnd(mZ, (! true)))) {
                    mI %= ((mI ^ mI) ^ mI);
                  } else {
                    mI = (mI << mI);
                  }
                } while (++i1 < 2);
              }
            }
          }
        }
        switch (mArray.length - 1) {
          case 1: {
            mF *= (-1336843462.0f - ((804606312.0f * 1709271074.0f) - (Float.MIN_NORMAL)));
            break;
          }
          default: {
            mF /= (mF--);
            break;
          }
        }
      }
      lJ0 >>>= (lJ0 | mJ);
    }
    mJ %= ( (-1288553765 ^ mI));
    return (--mArray[1][0][1][1][0][1][0]);
  }

  public static void main(String[] args) {
    Test t = new Test();
    float r = -985685074.0f;
    try {
      r = t.testMethod();
    } catch (Exception e) {
      // Arithmetic, null pointer, index out of bounds, etc.
      System.out.println("An exception was caught.");
    }
    System.out.println("r  = " + r);
    System.out.println("mZ = " + t.mZ);
    System.out.println("mI = " + t.mI);
    System.out.println("mJ = " + t.mJ);
    System.out.println("mF = " + t.mF);
    System.out.println("mD = " + t.mD);
    System.out.println("mArray = " + Arrays.deepToString(t.mArray));
  }
}

