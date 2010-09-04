# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Common definitions for host or target builds of libdvm.
#
# If you enable or disable optional features here, make sure you do
# a "clean" build -- not everything depends on Dalvik.h.  (See Android.mk
# for the exact command.)
#


#
# Compiler defines.
#
LOCAL_CFLAGS += -fstrict-aliasing -Wstrict-aliasing=2 -fno-align-jumps
#LOCAL_CFLAGS += -DUSE_INDIRECT_REF
LOCAL_CFLAGS += -include "dalvikdefines.h"

#
# Optional features.  These may impact the size or performance of the VM.
#
LOCAL_CFLAGS += -DWITH_PROFILER -DWITH_DEBUGGER

# 0=full cache, 1/2=reduced, 3=no cache
LOCAL_CFLAGS += -DDVM_RESOLVER_CACHE=0

ifeq ($(WITH_DEADLOCK_PREDICTION),true)
  LOCAL_CFLAGS += -DWITH_DEADLOCK_PREDICTION
  WITH_MONITOR_TRACKING := true
endif
ifeq ($(WITH_MONITOR_TRACKING),true)
  LOCAL_CFLAGS += -DWITH_MONITOR_TRACKING
endif

# Make a debugging version when building the simulator (if not told
# otherwise) and when explicitly asked.
dvm_make_debug_vm := false
ifeq ($(strip $(DEBUG_DALVIK_VM)),)
  ifeq ($(dvm_simulator),true)
    dvm_make_debug_vm := true
  endif
else
  dvm_make_debug_vm := $(DEBUG_DALVIK_VM)
endif

ifeq ($(dvm_make_debug_vm),true)
  #
  # "Debug" profile:
  # - debugger enabled
  # - profiling enabled
  # - tracked-reference verification enabled
  # - allocation limits enabled
  # - GDB helpers enabled
  # - LOGV
  # - assert()
  # - full ELF symbols
  #
  LOCAL_CFLAGS += -DWITH_INSTR_CHECKS
  LOCAL_CFLAGS += -DWITH_EXTRA_OBJECT_VALIDATION
  LOCAL_CFLAGS += -DWITH_TRACKREF_CHECKS
  LOCAL_CFLAGS += -DWITH_ALLOC_LIMITS
  LOCAL_CFLAGS += -DWITH_EXTRA_GC_CHECKS=1
  #LOCAL_CFLAGS += -DCHECK_MUTEX
  #LOCAL_CFLAGS += -DPROFILE_FIELD_ACCESS
  LOCAL_CFLAGS += -DDVM_SHOW_EXCEPTION=3
  # add some extra stuff to make it easier to examine with GDB
  LOCAL_CFLAGS += -DEASY_GDB
  # overall config may be for a "release" build, so reconfigure these
  LOCAL_CFLAGS += -UNDEBUG -DDEBUG=1 -DLOG_NDEBUG=1 -DWITH_DALVIK_ASSERT
else  # !dvm_make_debug_vm
  #
  # "Performance" profile:
  # - all development features disabled
  # - compiler optimizations enabled (redundant for "release" builds)
  # - (debugging and profiling still enabled)
  # - minimize ELF symbols to reduce code size by 10%
  #
  #LOCAL_CFLAGS += -DNDEBUG -DLOG_NDEBUG=1
  # "-O2" is redundant for device (release) but useful for sim (debug)
  #LOCAL_CFLAGS += -O2 -Winline
  #LOCAL_CFLAGS += -DWITH_EXTRA_OBJECT_VALIDATION
  LOCAL_CFLAGS += -DDVM_SHOW_EXCEPTION=1
  # if you want to try with assertions on the device, add:
  #LOCAL_CFLAGS += -UNDEBUG -DDEBUG=1 -DLOG_NDEBUG=1 -DWITH_DALVIK_ASSERT
  # use GCC Visibility to reduce the footprint of runtime library
  LOCAL_CFLAGS += -fvisibility=hidden
endif  # !dvm_make_debug_vm

# bug hunting: checksum and verify interpreted stack when making JNI calls
#LOCAL_CFLAGS += -DWITH_JNI_STACK_CHECK

LOCAL_SRC_FILES := \
	AllocTracker.c \
	AtomicCache.c \
	CheckJni.c \
	Ddm.c \
	Debugger.c \
	DvmDex.c \
	Exception.c \
	Hash.c \
	IndirectRefTable.c.arm \
	Init.c \
	InlineNative.c.arm \
	Inlines.c \
	Intern.c \
	Jni.c \
	JarFile.c \
	LinearAlloc.c \
	Misc.c.arm \
	Native.c \
	PointerSet.c \
	Profile.c \
	Properties.c \
	RawDexFile.c \
	ReferenceTable.c \
	SignalCatcher.c \
	StdioConverter.c \
	Sync.c \
	TestCompability.c \
	Thread.c \
	UtfString.c \
	alloc/clz.c.arm \
	alloc/Alloc.c \
	alloc/HeapBitmap.c.arm \
	alloc/HeapDebug.c \
	alloc/HeapSource.c \
	alloc/HeapTable.c \
	alloc/HeapWorker.c \
	alloc/Heap.c.arm \
	alloc/MarkSweep.c.arm \
	alloc/DdmHeap.c \
	analysis/CodeVerify.c \
	analysis/DexOptimize.c \
	analysis/DexVerify.c \
	analysis/ReduceConstants.c \
	analysis/RegisterMap.c \
	analysis/VerifySubs.c \
	interp/Interp.c.arm \
	interp/Stack.c \
	jdwp/ExpandBuf.c \
	jdwp/JdwpAdb.c \
	jdwp/JdwpConstants.c \
	jdwp/JdwpEvent.c \
	jdwp/JdwpHandler.c \
	jdwp/JdwpMain.c \
	jdwp/JdwpSocket.c \
	mterp/Mterp.c.arm \
	mterp/out/InterpC-portstd.c.arm \
	mterp/out/InterpC-portdbg.c.arm \
	native/InternalNative.c \
	native/dalvik_system_DexFile.c \
	native/dalvik_system_SamplingProfiler.c \
	native/dalvik_system_VMDebug.c \
	native/dalvik_system_VMRuntime.c \
	native/dalvik_system_VMStack.c \
	native/dalvik_system_Zygote.c \
	native/java_lang_Class.c \
	native/java_lang_Object.c \
	native/java_lang_Runtime.c \
	native/java_lang_String.c \
	native/java_lang_System.c \
	native/java_lang_SystemProperties.c \
	native/java_lang_Throwable.c \
	native/java_lang_VMClassLoader.c \
	native/java_lang_VMThread.c \
	native/java_lang_reflect_AccessibleObject.c \
	native/java_lang_reflect_Array.c \
	native/java_lang_reflect_Constructor.c \
	native/java_lang_reflect_Field.c \
	native/java_lang_reflect_Method.c \
	native/java_lang_reflect_Proxy.c \
	native/java_security_AccessController.c \
	native/java_util_concurrent_atomic_AtomicLong.c \
	native/org_apache_harmony_dalvik_NativeTestTarget.c \
	native/org_apache_harmony_dalvik_ddmc_DdmServer.c \
	native/org_apache_harmony_dalvik_ddmc_DdmVmInternal.c \
	native/sun_misc_Unsafe.c \
	native/SystemThread.c \
	oo/AccessCheck.c \
	oo/Array.c \
	oo/Class.c \
	oo/Object.c \
	oo/Resolve.c \
	oo/TypeCheck.c \
	reflect/Annotation.c \
	reflect/Proxy.c \
	reflect/Reflect.c \
	test/AtomicSpeed.c \
	test/TestHash.c \
	test/TestIndirectRefTable.c

WITH_JIT := $(strip $(WITH_JIT))

ifeq ($(WITH_JIT),true)
  LOCAL_CFLAGS += -DWITH_JIT
  LOCAL_SRC_FILES += \
	../dexdump/OpCodeNames.c \
	compiler/Compiler.c \
	compiler/Frontend.c \
	compiler/Utility.c \
	compiler/IntermediateRep.c \
	compiler/Dataflow.c \
	compiler/Loop.c \
	compiler/Ralloc.c \
	interp/Jit.c
endif

WITH_HPROF := $(strip $(WITH_HPROF))
ifeq ($(WITH_HPROF),)
  WITH_HPROF := true
endif
ifeq ($(WITH_HPROF),true)
  LOCAL_SRC_FILES += \
	hprof/Hprof.c \
	hprof/HprofClass.c \
	hprof/HprofHeap.c \
	hprof/HprofOutput.c \
	hprof/HprofString.c
  LOCAL_CFLAGS += -DWITH_HPROF=1

  ifeq ($(strip $(WITH_HPROF_UNREACHABLE)),true)
    LOCAL_CFLAGS += -DWITH_HPROF_UNREACHABLE=1
  endif

  ifeq ($(strip $(WITH_HPROF_STACK)),true)
    LOCAL_SRC_FILES += \
	hprof/HprofStack.c \
	hprof/HprofStackFrame.c
    LOCAL_CFLAGS += -DWITH_HPROF_STACK=1
  endif # WITH_HPROF_STACK
endif   # WITH_HPROF

ifeq ($(strip $(DVM_TRACK_HEAP_MARKING)),true)
  LOCAL_CFLAGS += -DDVM_TRACK_HEAP_MARKING=1
endif

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	dalvik \
	dalvik/vm \
	external/zlib \
	$(KERNEL_HEADERS)


ifeq ($(dvm_simulator),true)
  LOCAL_LDLIBS += -lpthread -ldl
  ifeq ($(HOST_OS),linux)
    # need this for clock_gettime() in profiling
    LOCAL_LDLIBS += -lrt
  endif
else
  ifeq ($(dvm_os),linux)
    LOCAL_SHARED_LIBRARIES += libdl
  endif
endif

MTERP_ARCH_KNOWN := false

ifeq ($(dvm_arch),arm)
  #dvm_arch_variant := armv7-a
  #LOCAL_CFLAGS += -march=armv7-a -mfloat-abi=softfp -mfpu=vfp
  MTERP_ARCH_KNOWN := true
  # Select architecture-specific sources (armv4t, armv5te etc.)
  LOCAL_SRC_FILES += \
		arch/arm/CallOldABI.S \
		arch/arm/CallEABI.S \
		arch/arm/HintsEABI.c \
		mterp/out/InterpC-$(dvm_arch_variant).c.arm \
		mterp/out/InterpAsm-$(dvm_arch_variant).S

  ifeq ($(WITH_JIT),true)
    LOCAL_SRC_FILES += \
		compiler/codegen/arm/RallocUtil.c \
		compiler/codegen/arm/$(dvm_arch_variant)/Codegen.c \
		compiler/codegen/arm/$(dvm_arch_variant)/CallingConvention.S \
		compiler/codegen/arm/Assemble.c \
		compiler/codegen/arm/ArchUtility.c \
		compiler/codegen/arm/LocalOptimizations.c \
		compiler/codegen/arm/GlobalOptimizations.c \
		compiler/template/out/CompilerTemplateAsm-$(dvm_arch_variant).S
  endif
endif

ifeq ($(dvm_arch),x86)
  ifeq ($(dvm_os),linux)
    MTERP_ARCH_KNOWN := true
    LOCAL_SRC_FILES += \
		arch/$(dvm_arch_variant)/Call386ABI.S \
		arch/$(dvm_arch_variant)/Hints386ABI.c \
		mterp/out/InterpC-$(dvm_arch_variant).c \
		mterp/out/InterpAsm-$(dvm_arch_variant).S
  endif
endif

ifeq ($(dvm_arch),sh)
  MTERP_ARCH_KNOWN := true
  LOCAL_SRC_FILES += \
		arch/sh/CallSH4ABI.S \
		arch/generic/Hints.c \
		mterp/out/InterpC-allstubs.c \
		mterp/out/InterpAsm-allstubs.S
endif

ifeq ($(MTERP_ARCH_KNOWN),false)
  # unknown architecture, try to use FFI
  LOCAL_C_INCLUDES += external/libffi/$(dvm_os)-$(dvm_arch)

  ifeq ($(dvm_os)-$(dvm_arch),darwin-x86)
      # OSX includes libffi, so just make the linker aware of it directly.
      LOCAL_LDLIBS += -lffi
  else
      LOCAL_SHARED_LIBRARIES += libffi
  endif

  LOCAL_SRC_FILES += \
		arch/generic/Call.c \
		arch/generic/Hints.c \
		mterp/out/InterpC-allstubs.c

  # The following symbols are usually defined in the asm file, but
  # since we don't have an asm file in this case, we instead just
  # peg them at 0 here, and we add an #ifdef'able define for good
  # measure, too.
  LOCAL_CFLAGS += -DdvmAsmInstructionStart=0 -DdvmAsmInstructionEnd=0 \
	-DdvmAsmSisterStart=0 -DdvmAsmSisterEnd=0 -DDVM_NO_ASM_INTERP=1
endif

ifeq ($(TEST_VM_IN_ECLAIR),true)
  LOCAL_CFLAGS += -DTEST_VM_IN_ECLAIR
endif
