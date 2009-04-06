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
# Android.mk for Dalvik VM.  If you enable or disable optional features here,
# rebuild the VM with "make clean-libdvm && make -j4 libdvm".
#
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)


#
# Compiler defines.
#
LOCAL_CFLAGS += -fstrict-aliasing -Wstrict-aliasing=2 -fno-align-jumps

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

#
# "Debug" profile:
# - debugger enabled
# - profiling enabled
# - tracked-reference verification enabled
# - allocation limits enabled
# - GDB helpers enabled
# - LOGV
# - assert()  (NDEBUG is handled in the build system)
#
ifeq ($(TARGET_BUILD_TYPE),debug)
LOCAL_CFLAGS += -DWITH_INSTR_CHECKS -DWITH_EXTRA_OBJECT_VALIDATION
LOCAL_CFLAGS += -DWITH_TRACKREF_CHECKS
LOCAL_CFLAGS += -DWITH_ALLOC_LIMITS
#LOCAL_CFLAGS += -DCHECK_MUTEX
#LOCAL_CFLAGS += -DPROFILE_FIELD_ACCESS
LOCAL_CFLAGS += -DDVM_SHOW_EXCEPTION=3
# add some extra stuff to make it easier to examine with GDB
LOCAL_CFLAGS += -DEASY_GDB
endif


#
# "Performance" profile:
# - all development features disabled
# - compiler optimizations enabled (redundant for "release" builds)
# - (debugging and profiling still enabled)
#
ifeq ($(TARGET_BUILD_TYPE),release)
#LOCAL_CFLAGS += -DNDEBUG -DLOG_NDEBUG=1
# "-O2" is redundant for device (release) but useful for sim (debug)
#LOCAL_CFLAGS += -O2 -Winline
LOCAL_CFLAGS += -DDVM_SHOW_EXCEPTION=1
# if you want to try with assertions on the device, add:
#LOCAL_CFLAGS += -UNDEBUG -DDEBUG=1 -DLOG_NDEBUG=1 -DWITH_DALVIK_ASSERT
endif

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
	oo/AccessCheck.c \
	oo/Array.c \
	oo/Class.c \
	oo/Object.c \
	oo/Resolve.c \
	oo/TypeCheck.c \
	reflect/Annotation.c \
	reflect/Proxy.c \
	reflect/Reflect.c \
	test/TestHash.c

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


ifeq ($(TARGET_SIMULATOR),true)
  LOCAL_LDLIBS += -lpthread -ldl
  ifeq ($(HOST_OS),linux)
    # need this for clock_gettime() in profiling
    LOCAL_LDLIBS += -lrt
  endif
else
  LOCAL_SHARED_LIBRARIES += libdl
endif

# Uncomment for fast IA interpreter
# X86-ATOM=YES

ifeq ($(TARGET_ARCH),arm)
  LOCAL_SRC_FILES += \
		arch/arm/CallOldABI.S \
		arch/arm/CallEABI.S \
		arch/arm/HintsEABI.c
  #
  # The armv4 configation in mterp is actually armv4t, it's just
  # wrongly named
  #
  # TODO: Rename mterp/config-armv4 and mterp/armv4 (to armv4t)
  #       then remove this ifeq.
  #
  ifeq ($(TARGET_ARCH_VERSION),armv4t)
    LOCAL_SRC_FILES += \
		mterp/out/InterpC-armv4.c.arm \
		mterp/out/InterpAsm-armv4.S
  else
    # Select architecture specific sources (armv4,armv5te etc)
    LOCAL_SRC_FILES += \
		mterp/out/InterpC-$(TARGET_ARCH_VERSION).c.arm \
		mterp/out/InterpAsm-$(TARGET_ARCH_VERSION).S
  endif
  LOCAL_SHARED_LIBRARIES += libdl
else
   ifdef X86-ATOM
	# use FFI for now, then use custom
	# version rather than FFI
 	$(warning compiling fast IA interpreter)

	LOCAL_SRC_FILES += arch/x86-atom/CallABI.S \
			   arch/x86-atom/HintsABI.c

	LOCAL_SRC_FILES += \
			mterp/out/InterpC-x86-atom.c \
			mterp/out/InterpAsm-x86-atom.S

    ifneq ($(TARGET_SIMULATOR),true)
	LOCAL_SHARED_LIBRARIES += libdl
    endif

  else ifeq ($(TARGET_ARCH),x86)
    LOCAL_SRC_FILES += \
		arch/x86/Call386ABI.S \
		arch/x86/Hints386ABI.c
    LOCAL_SRC_FILES += \
		mterp/out/InterpC-x86.c \
		mterp/out/InterpAsm-x86.S
  else
	# unknown architecture, try to use FFI
    LOCAL_C_INCLUDES += external/libffi/$(TARGET_OS)-$(TARGET_ARCH)
    LOCAL_SRC_FILES += \
		arch/generic/Call.c \
		arch/generic/Hints.c
    LOCAL_SHARED_LIBRARIES += libffi
	
    LOCAL_SRC_FILES += \
		mterp/out/InterpC-allstubs.c \
		mterp/out/InterpAsm-allstubs.S
  endif
endif


LOCAL_MODULE := libdvm

LOCAL_SHARED_LIBRARIES += \
	liblog \
	libcutils \
	libnativehelper \
	libz

LOCAL_STATIC_LIBRARIES += \
	libdex

include $(BUILD_SHARED_LIBRARY)
