   /* Copyright (C) 2008 The Android Open Source Project
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

   /*
    * File: CallABI.S
    *
    * Code: facilitates call to native code C and C++ routines.
    *
    */

   /*
    * Function prototype:
    *
    * void dvmPlatformInvoke(void* pEnv, ClassObject* clazz, int argInfo, int argc,
    * const u4* argv, const char* signature, void* func, JValue* pReturn)
    *
    * The method we are calling has the form:
    *
    * return_type func(JNIEnv* pEnv, ClassObject* clazz, ...)
    * -or-
    * return_type func(JNIEnv* pEnv, Object* this, ...)
    *
    * We receive a collection of 32-bit values which correspond to arguments from
    * the interpreter (e.g. float occupies one, double occupies two).  It's up to
    * us to convert these into local calling conventions.
    */

   /*
    * On entry:
    *   4(%sp)    JNIEnv (can be left alone)
    *   8(%esp)   clazz (NULL for virtual method calls, non-NULL for static)
    *   12(%esp)  arg info
    *   16(%esp)  argc (number of 32-bit values in argv)
    *   20(%esp)  argv
    *   24(%esp)  short signature
    *   28(%esp)  func
    *   32(%esp)  pReturn
    *
    * For a virtual method call, the "this" reference is in argv[0].
    *
    * argInfo (32-bit int) layout:
    *
    *   SRRRHHHH HHHHHHHH HHHHHHHH HHHHHHHH
    *
    *   S - if set, argInfo hints are invalid
    *   R - return type enumeration (see jniInternal.h)
    *       VOID   -> 0
    *       FLOAT  -> 1
    *       DOUBLE -> 2
    *       S8     -> 3
    *       S4     -> 4
    *    H - target-specific hints (see below for details)
    *
    * IA32 ABI JNI hint format
    *
    *       ZZZZ ZZZZZZZZ AAAAAAAA AAAAAAAA
    *
    *   Z - reserved
    *   A - size of the variable argument block in 32-bit words
    */

    .text
    .align  4
    .global dvmPlatformInvoke
    .type   dvmPlatformInvoke, %function


dvmPlatformInvoke:
CallABI_ENTER:

   /*
    * Save registers.
    */

    movl        %ebp, -4(%esp)
    movl        %ebx, -8(%esp)          # save %ebx
    movl        %esi, -12(%esp)         # save %esi
    movl        %edi, -16(%esp)         # save %edi
    lea         (%esp), %ebp

   /*
    * Check if argInfo is valid. Is always valid so should remove this check?
    */

    movzwl      12(%ebp), %ecx          # %ecx<- argsize in words
    movl        12(%ebp), %ebx          # %ebx<- argInfo

    shl         $2, %ecx                # %ecx<- argsize in bytes
    subl        %ecx, %esp              # %esp<- expanded for arg region

   /*
    * Prepare for 16 byte alignment
    */

    and         $0xfffffff0, %esp
    subl        $24, %esp


    movl        8(%ebp), %eax           # %eax<- clazz
    cmpl        $0, %eax                # Check virtual or static
    movl        4(%ebp), %ecx           # %ecx<- JNIEnv
    movl        20(%ebp), %esi          # %esi<- argV
    jne         1f                      # Branch if static
    movl        (%esi), %eax            # get the this pointer
    addl        $4, %esi                # %esi<- update past this

1:
    movl        %ecx, -8(%esp)          # push JNIEnv as arg #1
    movl        %eax, -4(%esp)          # push clazz or this as arg #2
    lea         -8(%esp), %esp

   /*
    * Copy arguments
    */

    movzwl      %bx, %ecx               # %ecx<- %bx; argsize in words
    lea         8(%esp), %edi           # %edi<- stack location for arguments
    cld
    rep         movsl                   # move %ecx arguments to 8(%esp)
    call        *28(%ebp)
    sarl        $28, %ebx               # %ebx<- SRRR (low 4 bits)
    je          CallABI_EXIT            # exit call
    cmpl        $2, %ebx
    movl        32(%ebp), %ecx          # %ecx<- return pointer
    je          2f                      # handle double return
    jl          1f                      # handle float return

   /*
    *  If a native function returns a result smaller than 8-bytes
    *  then higher bytes may contain garbage.
    *  This code does type-checking based on size of return result.
    *  We zero higher bytes instead of allowing the garbage to go through.
    */

    cmpl        $3,%ebx
    je          S8
    cmpl        $4,%ebx
    je          S4
    cmpl        $7,%ebx
    je          S1
    cmpl        $6,%ebx
    jne         S2
U2:
    movzwl      %ax, %eax
    movl        %eax, (%ecx)            # save 32-bit return
    jmp         CallABI_EXIT            # exit call

S1:
    movsbl      %al, %eax
    movl        %eax, (%ecx)            # save 32-bit return
    jmp         CallABI_EXIT            # exit call
S2:
    movswl      %ax, %eax
    movl        %eax, (%ecx)            # save 32-bit return
    jmp         CallABI_EXIT            # exit call
S4:
    cltd
    movl        %eax, (%ecx)            # save 32-bit return
    jmp         CallABI_EXIT            # exit call
S8:
    movl        %edx, 4(%ecx)           # save 64-bit return
    movl        %eax, (%ecx)            # save 32-bit return
    jmp         CallABI_EXIT            # exit call

2:
    fstpl       (%ecx)                  # save double return
    jmp         CallABI_EXIT            # exit call
1:
    fstps       (%ecx)                  # save float return

CallABI_EXIT:
    lea         (%ebp), %esp
    movl        -16(%ebp), %edi         # restore %edi
    movl        -12(%ebp), %esi         # restore %esi
    movl        -8(%ebp), %ebx          # restore %ebx
    movl        -4(%ebp), %ebp          # restore caller base pointer
    ret                                 # return
