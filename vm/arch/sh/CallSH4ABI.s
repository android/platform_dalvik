/*
 * Copyright (C) Hitachi, Ltd. 2009, All rights reserved.
 * Copyright (C) Renesas Solutions Corp. 2009, All rights reserved.
 *
 *   Author : Shin-ichiro KAWASAKI  <shinichiro.kawasaki.mg@hitachi.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

	/*
	 * Invoking JNI native method via SH4 ABI.
	 * This inplementation follows the spec found in following URL.
	 * http://www.ecos.sourceware.org/docs-1.3.1/ref/gnupro-ref/sh/SH_ch01.html#pgfId-461254

	 * TODO :
	 *  (1) Big endian support.
	 *  (2) SH3 ABI support.  Which includes non-float calling convention
	 *      for SH4.
	 */
	.text
	.align 4
	.type  dvmPlatformInvoke, #function
	.globl dvmPlatformInvoke

	/*
	 * @param r4 void* pEnv  (used as scrach after invoking method)
	 * @param r5 ClassObject* clazz
	 * @param r6 int argInfo
	 * @param r7 int argc
	 * @param r15[0] const u4 * argv
	 * @param r15[1] const char * shorty
	 * @param r15[2] void * func
	 * @param r15[3] JValue * pReturn
	 *
	 * @remark r0,r1  Scratch before invoking method.
	 *                Return value after invoking method.
	 * @remark r2  shorty pointer
	 * @remark r3  argv pointer before invoking method.
	 *             pReturn after invoking method.
	 * @remark r8-11 Don't touch.
	 * @remark r12 status of r5-7
	 * @remark r13 status of fr4-11
	 * @remark r14 Keep stack pointer.
	 */
dvmPlatformInvoke:
	## save preserved regsiters
	mov.l	r14, @-r15
	mov	r15, r14
	add	#4, r14		/* r14 = original r15 = stack pointer */
	mov.l	r13, @-r15
	mov.l	r12, @-r15
	sts.l	pr, @-r15
	
	# fetch arguments
	mov.l	@r14, r3 	/* argv */
	mov.l	@(4,r14), r2 	/* shorty for argumnets */
	mov	#1, r0		/* shorty's 1st byte specify ret value type. */
	add	r0, r2

### initialize local variables

	## r12 ... status of r6, and r7
	##          bit 1 << 0 : if r6 is available, it contains 1.
	##          bit 1 << 1 : if r7 is available, it contains 1.
	##  Note : r4 is always used to pass pEnv.
	##         r5 is always used for clazz or object
	mov	#3, r12		/* b0000-0111 : r5-7 avialble. */
	
	## r13 ... status of fr4-fr11
	##          bit 1 << 0 : if fr4 is available, it contains 1.
	##          bit 1 << 1 : if fr5 is available, it contains 1.
	## 		... 
	##          bit 1 << 7 : if fr11 is available, it contains 1.
	mov	#0xFF, r13	/* b1111-1111 : fr4-11 avialble. */

### put arguments

	## ... keep pEnv in r4 as is.

	## check clazz
	mov	#0, r0
	cmp/eq	r0, r5
	bf	arg_loop	/* if r5 has clazz, keep it as is */
	mov.l	@r3+, r5	/* put object arg in r5 */	

	## other args
arg_loop:	
one_arg_handled:
	mov.b	@r2+, r0
	cmp/eq	#0, r0		/* if (*shorty == '\0) */
	bf	process_one_arg
	bra	arg_end		/* no argument left */
	nop

process_one_arg:

	## check arg type

	cmp/eq	#'F', r0
	bt	jfloat_arg

	cmp/eq	#'D', r0
	bt	jdouble_arg

	cmp/eq	#'J', r0
	bt	jlong_arg

	## other 32bit arg types
	mov	r12, r0
	cmp/eq	#0, r0
	bt	put_32bit_on_stack /* r6-7 not available */

	tst	#1, r0
	bt	j32_arg_1
	mov.l	@r3+, r6	/* put one arg in r6 */
	mov	#1, r0		/* r6 is not available now. */
	not	r0, r0
	and	r0, r12	
	bra	one_arg_handled
	nop
j32_arg_1:	
	tst	#2, r0
	bt	j32_arg_fatal_error
	mov.l	@r3+, r7	/* put one arg in r7 */
	mov	#2, r0		/* r7 is not available now. */
	not	r0, r0
	and	r0, r12	
	bra	one_arg_handled
	nop

j32_arg_fatal_error:	
	bra	j32_arg_fatal_error
	nop

jlong_arg:	
	mov	r12, r0
	cmp/eq	#0, r0
	bt	put_64bit_on_stack /* r6-7 not available */

	and	#3, r0
	cmp/eq	#3, r0
	bf	put_64bit_on_stack /* consequent two registers not available. */
	mov.l	@r3+, r6	/* put one arg in r6 and r7 */
	mov.l	@r3+, r7	/* TODO : consider big endian */
	mov	#3, r0		/* r6 and r7 are not available now. */
	not	r0, r0
	and	r0, r12	
	bra	one_arg_handled
	nop

	# utility routines are placed here make short range jumps available.
put_32bit_on_stack:	
	mov.l	@r3+, r0
	mov.l	r0, @-r15
	bra	one_arg_handled
	nop

put_64bit_on_stack:	
	mov.l	@r3+, r0	/* TODO : consider endian. */
	mov.l	r0, @-r15	/* Pay attention that the endianness is */
	mov.l	@r3+, r0	/* once reversed.  It is corrected when the */
	mov.l	r0, @-r15	/* arguments on stack are revesred before */
	bra	one_arg_handled /* jni call */
	nop
		
jdouble_arg:	
	mov	r13, r0
	cmp/eq	#0, r0
	bt	put_64bit_on_stack /* fr4-11 not available */

	and	#3, r0
	cmp/eq	#3, r0
	bf	jdouble_arg_1
	
	fmov.s	@r3+, fr5	/* put one arg to drX */
	fmov.s	@r3+, fr4
	mov	#3, r0		/* fr4-frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jdouble_arg_1:	
	mov	r13, r0
	and	#12, r0
	cmp/eq	#12, r0
	bf	jdouble_arg_2

	fmov.s	@r3+, fr7	/* put one arg to drX */
	fmov.s	@r3+, fr6	
	mov	#15, r0		/* fr4-frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jdouble_arg_2:	
	mov	r13, r0
	and	#48, r0
	cmp/eq	#48, r0
	bf	jdouble_arg_3
	fmov.s	@r3+, fr9	/* put one arg to drX */
	fmov.s	@r3+, fr8	
	mov	#63, r0		/* fr4-frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jdouble_arg_3:	
	mov	r13, r0
	and	#192, r0
	cmp/eq	#192, r0
	bf	put_64bit_on_stack
	fmov.s	@r3+, fr11	/* put one arg to drX */
	fmov.s	@r3+, fr10	
	mov	#0, r13		/* fr4-fr11 all not available now. */
	bra	one_arg_handled
	nop

	## TODO : for bigendian, the register usage sequence should be modified.
jfloat_arg:
	mov	r13, r0
	cmp/eq	#0, r0
	bt	put_32bit_on_stack /* fr4-11 not available */

	tst	#2, r0
	bt	jfloat_arg_1
	fmov.s	@r3+, fr5	/* put one arg to frX */
	mov	#2, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_1:	
	tst	#1, r0
	bt	jfloat_arg_2
	fmov.s	@r3+, fr4	/* put one arg to frX */
	mov	#1, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_2:	
	tst	#8, r0
	bt	jfloat_arg_3
	fmov.s	@r3+, fr7	/* put one arg to frX */
	mov	#8, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_3:	
	tst	#4, r0
	bt	jfloat_arg_4
	fmov.s	@r3+, fr6	/* put one arg to frX */
	mov	#4, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_4:	
	tst	#32, r0
	bt	jfloat_arg_5
	fmov.s	@r3+, fr9	/* put one arg to frX */
	mov	#32, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_5:	
	tst	#16, r0
	bt	jfloat_arg_6
	fmov.s	@r3+, fr8	/* put one arg to frX */
	mov	#16, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_6:	
	tst	#128, r0
	bt	jfloat_arg_7
	fmov.s	@r3+, fr11	/* put one arg to frX */
	mov	#127, r0	/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_arg_7:	
	tst	#64, r0
	bt	jfloat_fatal_error
	fmov.s	@r3+, fr10	/* put one arg to frX */
	mov	#64, r0		/* frX not available now. */
	not	r0, r0
	and	r0, r13
	bra	one_arg_handled
	nop

jfloat_fatal_error:
	bra	jfloat_fatal_error:
	nop

arg_end:	
	
	
### reverse the variables on stack
	mov	r14, r12	/* points to first arg on stack */
	add	#-20, r12
	mov	r15, r13	/* points to last arg on stack */
arg_rev_loop:	
	cmp/hs	r12, r13	/* When r13 >= r12 (unsigned), 1->T */
	bt	arg_rev_end
	mov.l	@r12, r0
	mov.l	@r13, r1
	mov.l	r0, @r13
	mov.l	r1, @r12
	add	#-4, r12
	add	#4, r13
	bra	arg_rev_loop
	nop

arg_rev_end:
	
### invoke the JNI function.

	mov.l	@(8,r14), r0
	jsr	@r0
	nop

### pass the return value

	/*
	 * r0 and r1 keep return value.
	 */

	## fetch data
	mov.l	@(4,r14), r2 	/* reload shorty */
	mov.b	@r2, r2		/* first byte specifyes return value type. */
	mov.l	@(12,r14), r3 	/* pReturn */
	
	## check return value types

	mov	#'V', r4
	cmp/eq	r4, r2
	bt	end

	mov	#'F', r4
	cmp/eq	r4, r2
	bt	jfloat_ret

	mov	#'D', r4
	cmp/eq	r4, r2
	bt	jdouble_ret

	mov	#'J', r4
	cmp/eq	r4, r2
	bt	jlong_ret

	## fall-through for other 32 bit java types.

	## load return values
j32_ret:
	bra	end
	mov.l	r0, @r3		/* delay slot */

jfloat_ret:
	bra	end
	fmov.s	fr0, @r3	/* delay slot */
	
jdouble_ret:
	fmov.s	fr1, @r3	/* TODO : consider endian */
	mov	#4, r0
	bra	end
	fmov.s	fr0, @(r0,r3)	/* delay slot */
	
jlong_ret:
	mov.l	r0, @r3		/* TODO : consider endian */
	bra	end
	mov.l	r1, @(4,r3)	/* delay slot */
	
end:
	## restore preserved registers
	mov	r14, r15
	add	#-16, r15
	lds.l	@r15+, pr
	mov.l	@r15+, r12
	mov.l	@r15+, r13
	mov.l	@r15+, r14
	
	rts			/* dvmPlatformInvoke returns void. */
	nop
