
.class multidimensional
.super java/lang/Object


; Output from some versions of javac on:
; public static Object test_getObjectArray() {
;     Object[][] array = null;
;     return array[1][1];
; }
.method public static test_getObjectArray()Ljava/lang/Object;
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    aaload
    areturn
.end method

; Output from some versions of javac on:
; public static void test_setObjectArray() {
;     Object[][] array = null;
;     array[1][1] = null;
; }
.method public static test_setObjectArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    aconst_null
    aastore
    return
.end method


; Output from some versions of javac on:
; public static boolean test_getBooleanArray() {
;     boolean[][] array = null;
;     return array[1][1];
; }
.method public static test_getBooleanArray()Z
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    baload
    ireturn
.end method

; Output from some versions of javac on:
; public static void test_setBooleanArray() {
;     boolean[][] array = null;
;     array[1][1] = false;
; }
.method public static test_setBooleanArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    iconst_0
    bastore
    return
.end method


; Output from some versions of javac on:
; public static byte test_getByteArray() {
;     byte[][] array = null;
;     return array[1][1];
; }
.method public static test_getByteArray()B
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    baload
    ireturn
.end method

; Output from some versions of javac on:
; public static void test_setByteArray() {
;     byte[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setByteArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    iconst_0
    bastore
    return
.end method


; Output from some versions of javac on:
; public static char test_getCharArray() {
;     char[][] array = null;
;     return array[1][1];
; }
.method public static test_getCharArray()C
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    caload
    ireturn
.end method

; Output from some versions of javac on:
; public static void test_setCharArray() {
;     char[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setCharArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    iconst_0
    castore
    return
.end method


; Output from some versions of javac on:
; public static short test_getShortArray() {
;     short[][] array = null;
;     return array[1][1];
; }
.method public static test_getShortArray()S
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    saload
    ireturn
.end method

; Output from some versions of javac on:
; public static void test_setShortArray() {
;     short[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setShortArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    iconst_0
    sastore
    return
.end method


; Output from some versions of javac on:
; public static int test_getIntArray() {
;     int[][] array = null;
;     return array[1][1];
; }
.method public static test_getIntArray()I
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    iaload
    ireturn
.end method

; Output from some versions of javac on:
; public static void test_setIntArray() {
;     int[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setIntArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    iconst_0
    iastore
    return
.end method


; Output from some versions of javac on:
; public static long test_getLongArray() {
;     long[][] array = null;
;     return array[1][1];
; }
.method public static test_getLongArray()J
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    laload
    lreturn
.end method

; Output from some versions of javac on:
; public static void test_setLongArray() {
;     long[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setLongArray()V
    .limit locals 1
    .limit stack 4

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    lconst_0
    lastore
    return
.end method


; Output from some versions of javac on:
; public static float test_getFloatArray() {
;     float[][] array = null;
;     return array[1][1];
; }
.method public static test_getFloatArray()F
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    faload
    freturn
.end method

; Output from some versions of javac on:
; public static void test_setFloatArray() {
;     float[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setFloatArray()V
    .limit locals 1
    .limit stack 3

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    fconst_0
    fastore
    return
.end method


; Output from some versions of javac on:
; public static double test_getDoubleArray() {
;     double[][] array = null;
;     return array[1][1];
; }
.method public static test_getDoubleArray()D
    .limit locals 1
    .limit stack 2

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    daload
    dreturn
.end method

; Output from some versions of javac on:
; public static void test_setDoubleArray() {
;     double[][] array = null;
;     array[1][1] = 0;
; }
.method public static test_setDoubleArray()V
    .limit locals 1
    .limit stack 4

    aconst_null
    astore_0
    aload_0
    iconst_1
    aaload
    iconst_1
    dconst_0
    dastore
    return
.end method

