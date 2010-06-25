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
# Android.mk for Dalvik VM.
#
# This makefile builds both for host and target, and so the very large
# swath of common definitions are factored out into a separate file to
# minimize duplication.
#
# If you enable or disable optional features here (or in Dvm.mk),
# rebuild the VM with:
#
#  make clean-libdvm clean-libdvm_assert clean-libdvm_sv clean-libdvm_interp
#  make -j4 libdvm
#

LOCAL_PATH:= $(call my-dir)

#
# Build for the target (device).
#

ifeq ($(TARGET_ARCH_VARIANT),armv5te)
    WITH_JIT := false
endif

# Build the installed version (libdvm.so) first
include $(LOCAL_PATH)/ReconfigureDvm.mk

# Overwrite default settings
ifneq ($(TARGET_ARCH),x86)
ifeq ($(TARGET_SIMULATOR),false)
    LOCAL_PRELINK_MODULE := true
endif
endif
LOCAL_MODULE_TAGS := user
LOCAL_MODULE := libdvm
include $(BUILD_SHARED_LIBRARY)

# If WITH_JIT is configured, build multiple versions of libdvm.so to facilitate
# correctness/performance bugs triage
ifeq ($(WITH_JIT),true)

    # Derivation #1
    # Enable assert and JIT tuning
    include $(LOCAL_PATH)/ReconfigureDvm.mk

    # Enable assertions and JIT-tuning
    LOCAL_CFLAGS += -UNDEBUG -DDEBUG=1 -DLOG_NDEBUG=1 -DWITH_DALVIK_ASSERT \
				    -DWITH_JIT_TUNING -DJIT_STATS
    LOCAL_MODULE := libdvm_assert
    include $(BUILD_SHARED_LIBRARY)

    # Derivation #2
    # Enable assert and self-verification
    include $(LOCAL_PATH)/ReconfigureDvm.mk

    # Enable assertions and JIT self-verification
    LOCAL_CFLAGS += -UNDEBUG -DDEBUG=1 -DLOG_NDEBUG=1 -DWITH_DALVIK_ASSERT \
					-DWITH_SELF_VERIFICATION
    LOCAL_MODULE := libdvm_sv
    include $(BUILD_SHARED_LIBRARY)

    # Devivation #3
    # Compile out the JIT
    WITH_JIT := false
    include $(LOCAL_PATH)/ReconfigureDvm.mk

    LOCAL_MODULE := libdvm_interp
    include $(BUILD_SHARED_LIBRARY)

endif

#
# Build for the host.
#

ifeq ($(WITH_HOST_DALVIK),true)

    include $(CLEAR_VARS)

    # Variables used in the included Dvm.mk.
    dvm_os := $(HOST_OS)
    dvm_arch := $(HOST_ARCH)
    dvm_arch_variant := $(HOST_ARCH_VARIANT)
    dvm_simulator := false

    include $(LOCAL_PATH)/Dvm.mk

    # We need to include all of these libraries. The end result of this
    # section is a static library, but LOCAL_STATIC_LIBRARIES doesn't
    # actually cause any code from the specified libraries to be included,
    # whereas LOCAL_WHOLE_STATIC_LIBRARIES does. No, I (danfuzz) am not
    # entirely sure what LOCAL_STATIC_LIBRARIES is even supposed to mean
    # in this context, but it is in (apparently) meaningfully used in
    # other parts of the build.
    LOCAL_WHOLE_STATIC_LIBRARIES += \
	libnativehelper-host libdex liblog libcutils

    # The libffi from the source tree should never be used by host builds.
    # The recommendation is that host builds should always either
    # have sufficient custom code so that libffi isn't needed at all,
    # or they should use the platform's provided libffi. So, if the common
    # build rules decided to include it, axe it back out here.
    ifneq (,$(findstring libffi,$(LOCAL_SHARED_LIBRARIES)))
        LOCAL_SHARED_LIBRARIES := \
            $(patsubst libffi, ,$(LOCAL_SHARED_LIBRARIES))
    endif

    LOCAL_MODULE := libdvm-host

    include $(BUILD_HOST_STATIC_LIBRARY)

endif
