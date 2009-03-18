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
# dexdump, similar in purpose to objdump.
#
LOCAL_PATH:= $(call my-dir)

dexdump_src_files := \
		DexDump.c \
		OpCodeNames.c

dexdump_c_includes := \
		dalvik \
		$(JNI_H_INCLUDE)

dexdump_shared_libraries :=

dexdump_static_libraries := \
		libdex

##
##
## Build the device command line tool dexdump
##
##
ifneq ($(SDK_ONLY),true)  # SDK_only doesn't need device version

include $(CLEAR_VARS)
LOCAL_MODULE := dexdump
LOCAL_SRC_FILES := $(dexdump_src_files)
LOCAL_C_INCLUDES := $(dexdump_c_includes)
LOCAL_SHARED_LIBRARIES := $(dexdump_shared_libraries) libz liblog
LOCAL_STATIC_LIBRARIES := $(dexdump_static_libraries)
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := debug
LOCAL_LDLIBS +=
include $(BUILD_EXECUTABLE)

endif # !SDK_ONLY


##
##
## Build the host command line tool dexdump
##
##
ifneq ($(TARGET_SIMULATOR),true) # not 64 bit clean
include $(CLEAR_VARS)
LOCAL_MODULE := dexdump
LOCAL_SRC_FILES := $(dexdump_src_files)
LOCAL_C_INCLUDES := $(dexdump_c_includes)
LOCAL_SHARED_LIBRARIES := $(dexdump_shared_libraries)
LOCAL_STATIC_LIBRARIES := $(dexdump_static_libraries) liblog
LOCAL_LDLIBS += -lpthread -lz
include $(BUILD_HOST_EXECUTABLE)
endif # !TARGET_SIMULATOR

