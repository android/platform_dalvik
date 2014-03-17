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

/*
 * JNI method invocation.  This is used to call a C/C++ JNI method.  The
 * argument list has to be pushed onto the native stack according to
 * local calling conventions.
 *
 */

/* AArch64 Procedure Call Standard Notes
 *
 * x0-x7   - Parameter + results registers
 * x9-x15  - Caller saved registers
 * x19-x28 - Callee saved registers
 * x29     - Frame pointer (FP)
 * x30     - Link register (LR)
 * SP      - Stack pointer
 *
 * d0-d7   - FP  parameters registers
 * d8-d15  - Callee preserved FP registers
 * d16-d31 - Caller preserved (scratch)
 *
 * No aggregate types are passed by JNI.
 *
 */

/*
 * Function prototype:
 *
 * void dvmPlatformInvoke(void* pEnv, ClassObject* clazz, int argInfo, int argc,
 *  const u8* argv, const char* signature, void* func, JValue* pReturn)
 *
 * The method called has the form:
 *
 * return_type func(JNIEnv* pEnv, ClassObject* clazz, ...)
 *   -or-
 * return_type func(JNIEnv* pEnv, Object* this, ...)
 *
 * We receive a collection of 32-bit values which correspond to arguments from
 * the interpreter (e.g. float occupies one, double occupies two).  It's up to
 * us to convert these into local calling conventions.
 */

    .text
    .align  2
    .global dvmPlatformInvoke
    .type   dvmPlatformInvoke, %function

/*
 * On entry all parameters are in registers:
 *   x0 - void *pEnv
 *   x1 - ClassObject* clazz
 *   w2 - int argInfo
 *   w3 - int argc
 *   x4 - const u8* argv
 *   x5 - const char* signature
 *   x6 - void *func
 *   x7 - JValue* pReturn
 *
 * For a virtual method call, the "this" reference is in argv[0].
 *
 */
dvmPlatformInvoke:
    .cfi_startproc

    add x5, x5, #1  // Skip return type in signature, argInfo is used instead.

    // x1 (clazz) is 0 if this is a instance method. If so, load x1 with argv[0]
    // otherwise, leave x1 asis, as that will be passed to the JNI function as
    // its second argument (jclass).
    cbnz x1, .LcheckHint

    // Load jobject into x1 from argv[0]. Increment argv one slot.
    ldr x1, [x4], #8

    // Either get stack slots from hints in argInfo or calculate from signature.
.LcheckHint:
    tbz x2, #31, .LuseHint // Branch if there is a hint to useHint.

    // At this point, the signature matches the parameters from argv (x4) onwards.
    mov x9, x5    // Copy signature
    mov x10, #2   // number of ints, x0 and x1 taken already by JNIEnv* and jobject/jclass.
    mov x11, #0   // number of floats
    mov x12, #0   // number of stack slots

.LfirstScan:
    ldrb w14, [x9], #1   // Load a character from signature, and point signature at next character.
    cbz w14, .LdoneCounting        // \0 at end of signature indicates end.

    cmp x14, #'F'        // is this a float?
    beq .LisAFloat
    cmp x14, #'D'        // is this a double?
    beq .LisAFloat

    // Otherwise, it is one of ZBCSIJL, held in integer registers.
    cmp x10, #8          // If there are more than 8 integer registers used, start using stack slots.
    bge .LstackIncrement
    add x10, x10, #1     // One more register used...
    b .LfirstScan

.LstackIncrement:
    add x12, x12, #1     // Increment the # of stack slots used.
    b .LfirstScan

.LisAFloat:              // Handle doubles and floats.
    cmp x11, #8          // are there less than eight floats?
    bge .LstackIncrement
    add x11, x11, #1     // Increment number of floats.
    b .LfirstScan

.LuseHint:
    and x12, x2, 0x0fffffff // Retrieve number of stack slots from jhints - omit top 4 bits.


.LdoneCounting:
    mov x10, sp         // For calculating new SP.

SAVED_REGISTERS = 8     // Save LR, FP, x9, x19-x23 in the frame.

    add x12, x12, #SAVED_REGISTERS    // Add to SP size of the frame.
    lsl x12, x12, #3    // Multiply # of stack slots by 8 to get size of slots.
    sub x10, x10, x12   // Subtract from SP the number of slots & space for frame.
    and x10, x10, #0xfffffffffffffff0  // mask out bottom 4 bits to align SP to 16 bytes.
    mov x9, sp          // Save old SP for saving later.
    mov sp, x10         // set SP - we can now store LR and FP. (SP always at bottom!)

    sub x12, x9, #SAVED_REGISTERS*8   // Calculate new frame pointer
    stp x29, x30, [x12] // Store old LR and FP at new stack pointer location
    mov x29, x12        // Set FP to new FP

    // We don't know in advance where sp is relative to fp, so this is the
    // earliest we can set up the eh_frame.
    .cfi_def_cfa_register   29
    .cfi_offset 29, 0
    .cfi_offset 30, 8

    // Save these callee saved registers before we use them.
    stp   x19,x20, [x29, #16]
    .cfi_offset x19, 16
    .cfi_offset x20, 24
    stp   x21,x22, [x29, #32]
    .cfi_offset x21, 32
    .cfi_offset x22, 40

    // Save x23 and x9 - x23 is callee saved.
    // Save the old stack pointer (x9) on the stack.
    // As the stack frame is not a fix size, this needs to be stored explicitly.
    stp   x23,x9, [x29, #48]
    .cfi_offset x23, 48
    .cfi_offset x9, 56



    // Scan signature again.
    // x10 has the new stack pointer. Store at that and increment as values are stored.
    // At this point, the signature matches the paramters from argv (x4) onwards.

    /* Save some of the parameter registers in other registers/stack, as
     * these will be the parameters for the JNI function.
     * x0 and x1 are left asis.
     *
     * x0 - void *pEnv           <- Already correct
     * x1 - ClassObject* clazz   <- Already correct.
     * w2 - int argInfo          <- Copied to x23
     * w3 - int argc             <- Scrap - we use \0 at end of signature.
     * x4 - const u8* argv       <- Copied to x19
     * x5 - const char* signature  <- Copied to x20
     * x6 - void *func           <- Copied to x21
     * x7 - JValue* pReturn      <- Copied to e have saved this on the stack.
     */
    // x11 has address to call to store integer value. Incremented as necessary.
    adr   x11, .LstoreX2

    // x12 has address to call to store single FP value. Use x14 as an offset.
    adr   x12, .LstoreSingle0

    // x13 has address to call to store double FP value. Use x14 as an offset.
    adr   x13, .LstoreDouble0

    mov   x14, #0    // Index of floating point register (single or double)

    // These 5 registers have been saved on the stack and need to be restored
    // after the JNI function is called.
    mov   x19, x4           // Copy argv
    mov   x20, x5           // Copy signature
    mov   x21, x6           // Copy function
    mov   x22, x7           // return value
    ubfm  x23, x2, #28, #30 // Extract top bits 28-30 for return type.

    // x9  <- old stack pointer value.
    // x10 <- Current address on stack to store a value.
    // x11 <- address to jump to to store an integer.
    // x12 <- address to jump to store float
    // x13 <- address to jump to store a double.
    // x14 <- index of float or double routine to jump to.
    // x15 <- Scratch
    // s/d16 <- For passing floats to registers.

.LsecondScan:
    ldrb w15, [x20], #1  // Load next character in signature, and increment.
    cbz w15, .LcallFunction   // Exit at end of signature

    cmp x15, #'F'        // is this a float?
    bne .LisADouble2
    ldr s16, [x19], #8   // Pull float from argv and increment argv
    add x15, x12, x14    // Calculate subroutine to jump to
    br  x15

.LisADouble2:
    cmp x15, #'D'        // is this a double?
    bne .LisALong
    ldr d16, [x19], #16   // Pull double from argv and increment argv by two slots
    add x15, x13, x14    // Calculate subroutine to jump to
    br x15
.LisALong:
    cmp x15, #'J'        // is this a long?
    bne .LisAnInteger
    ldr x15, [x19], #16   // Pull out a long from argv and increment argv by two slots
    br  x11              // Branch to next appropriate integer store routine

.LisAnInteger:
    ldr x15, [x19], #8   // Pull out an integer value from argv and increment argv
    br  x11              // Branch to next appropriate integer store routine

.LstoreX2:
    mov x2, x15        // Store in x2
    add x11, x11, 12   // next jump to x11 will store x15 into x3, etc...
    b .LsecondScan
.LstoreX3:
    mov x3, x15
    add x11, x11, 12
    b .LsecondScan
.LstoreX4:
    mov x4, x15
    add x11, x11, 12
    b .LsecondScan
.LstoreX5:
    mov x5, x15
    add x11, x11, 12
    b .LsecondScan
.LstoreX6:
    mov x6, x15
    add x11, x11, 12
    b .LsecondScan
.LstoreX7:
    mov x7, x15
    add x11, x11, 12
    b .LsecondScan
.LstoreIntStack:         // Ran out of registers, so store integers on stack.
    str x15, [x10], #8   // Store value in stack slot and increment.
    b .LsecondScan


// Store singles.
.LstoreSingle0:
    fmov s0, s16
    add x14, x14, #12    // Increment index by 3 instructions.
    b .LsecondScan
.LstoreSingle1:
    fmov s1, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingle2:
    fmov s2, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingle3:
    fmov s3, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingle4:
    fmov s4, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingle5:
    fmov s5, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingle6:
    fmov s6, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingle7:
    fmov s7, s16
    add x14, x14, #12
    b .LsecondScan
.LstoreSingleStack:         // Ran out of registers, so store floats on stack.
    str s16, [x10], #8      // Store value in stack slot and increment.
    b .LsecondScan

// Store doubles.
.LstoreDouble0:
    fmov d0, d16            // Increment index by 3 instructions.
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble1:
    fmov d1, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble2:
    fmov d2, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble3:
    fmov d3, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble4:
    fmov d4, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble5:
    fmov d5, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble6:
    fmov d6, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDouble7:
    fmov d7, d16
    add x14, x14, #12
    b .LsecondScan
.LstoreDoubleStack:         // Ran out of registers, so store doubles on stack.
    str d16, [x10], #8      // Store value in stack slot and increment.
    b .LsecondScan


.LcallFunction:
    // Jump to JNI function.
    blr x21

    ldr   x9,  [x29,#56]    // Load old stack pointer.
    .cfi_restore x9

    // Store return value from correct register, depending on type, if at all.
    // Do this by jumping to one of the labels below.
    adr   x11, .LreturnTypeIs0
    add   x11, x11, x23, lsl #3
    br    x11

.LreturnTypeIs0:            // 0 DALVIK_JNI_RETURN_VOID
    b .Lreturn
    nop                     // Need 2 instructions per label
.LreturnTypeIs1:            // 1 DALVIK_JNI_RETURN_FLOAT
    str s0, [x22]
    b .Lreturn
.LreturnTypeIs2:            // 2 DALVIK_JNI_RETURN_DOUBLE
    str d0, [x22]
    b .Lreturn
.LreturnTypeIs3:            // 3 DALVIK_JNI_RETURN_S8
    b .LreturnInteger
    nop                     // Need 2 instructions per label
.LreturnTypeIs4:            // 4 DALVIK_JNI_RETURN_S4
    sxtw x0, w0
    b .LreturnInteger
.LreturnTypeIs5:            // 5 DALVIK_JNI_RETURN_S2
    sxth x0, w0
    b .LreturnInteger
.LreturnTypeIs6:            // 6 DALVIK_JNI_RETURN_U2
    uxth x0, w0
    b .LreturnInteger
.LreturnTypeIs7:            // 7 DALVIK_JNI_RETURN_S1
    sxtb x0, w0

.LreturnInteger:
    str x0, [x22]
.Lreturn:
    ldp x19,x20, [x29, #16]     // Restore these callee saved registers
    .cfi_restore x19
    .cfi_restore x20
    ldp x21, x22, [x29, #32]
    .cfi_restore x21
    .cfi_restore x22
    ldr x23, [x29, #48]
    .cfi_restore x23

    ldp x29, x30,[x29]      // Restore old frame pointer.
    .cfi_restore x29
    .cfi_restore x30
    mov sp, x9              // Restore stack pointer.
    .cfi_restore sp

    // Done.
    ret
    .cfi_endproc
