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
# dexopt, the DEX file optimizer.  This is fully integrated with the VM,
# so it must be linked against the full VM shared library.
#
LOCAL_PATH:= $(call my-dir)

local_src_files := \
		OptMain.cpp

local_c_includes := \
		dalvik \
		dalvik/libdex \
		dalvik/vm

include $(CLEAR_VARS)
ifeq ($(TARGET_CPU_SMP),true)
    LOCAL_CFLAGS += -DANDROID_SMP=1
else
    LOCAL_CFLAGS += -DANDROID_SMP=0
endif

LOCAL_SRC_FILES := $(local_src_files)
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_SHARED_LIBRARIES := libssl libdvm libcrypto libicuuc libicui18n libcutils libexpat liblog libz
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := dexopt

LOCAL_C_INCLUDES += external/stlport/stlport bionic/ bionic/libstdc++/include
LOCAL_SHARED_LIBRARIES += libstlport

include $(BUILD_EXECUTABLE)

ifeq ($(WITH_HOST_DALVIK),true)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(local_src_files)
    LOCAL_C_INCLUDES := $(local_c_includes)
    LOCAL_SHARED_LIBRARIES := libssl-host libdvm libcrypto-host libicuuc-host libicui18n-host
    LOCAL_STATIC_LIBRARIES :=  libcutils libexpat liblog libz
    LOCAL_LDLIBS += -ldl -lpthread
    LOCAL_CFLAGS += -DANDROID_SMP=1
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := dexopt

    ifneq ($(strip $(BUILD_HOST_64bit)),)
        # libffi is currently needed in order to build Dalvik on the host. For
        # now, we pick the library from the host and we hard-code paths below.
        # The way to solve this is add for x86_64 custom code so that libffi is
        # not needed at all. Once this is done the code below can be removed.
        $(info TODOArm64: do not hard-code paths to host system libraries/includes)
        LOCAL_LDLIBS += -lffi
        LOCAL_LDFLAGS += -L/usr/lib/x86_64-linux-gnu
        LOCAL_C_INCLUDES += /usr/include/x86_64-linux-gnu
        LOCAL_CPPFLAGS += -I/usr/include/x86_64-linux-gnu -I/usr/include
    endif

    include $(BUILD_HOST_EXECUTABLE)
endif
