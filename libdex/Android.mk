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

LOCAL_PATH:= $(call my-dir)

dex_src_files := \
	CmdUtils.c \
	DexCatch.c \
	DexClass.c \
	DexDataMap.c \
	DexFile.c \
	DexInlines.c \
	DexProto.c \
	DexSwapVerify.c \
	InstrUtils.c \
	Leb128.c \
	OptInvocation.c \
	sha1.c \
	SysUtil.c \
	ZipArchive.c

dex_include_files := \
	dalvik \
	$(JNI_H_INCLUDE) \
	external/zlib \
	external/safe-iop/include

##
##
## Build the device version of libdex
##
##
ifneq ($(SDK_ONLY),true)  # SDK_only doesn't need device version

include $(CLEAR_VARS)

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

LOCAL_SRC_FILES := $(dex_src_files)
LOCAL_C_INCLUDES += $(dex_include_files)
LOCAL_CFLAGS += -include "dalvikdefines.h"
ifeq ($(dvm_make_debug_vm),false)
  # hide ELF symbols to reduce code size
  LOCAL_CFLAGS += -fvisibility=hidden
endif
LOCAL_MODULE := libdex
include $(BUILD_STATIC_LIBRARY)

endif # !SDK_ONLY


##
##
## Build the host version of libdex
##
##
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(dex_src_files)
LOCAL_C_INCLUDES += $(dex_include_files)
LOCAL_CFLAGS += -include "dalvikdefines.h"
LOCAL_MODULE := libdex
include $(BUILD_HOST_STATIC_LIBRARY)
