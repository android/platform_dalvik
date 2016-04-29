# Copyright 2006 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)

# This tool is prebuilt if we're doing an app-only build.
ifeq ($(TARGET_BUILD_APPS)$(filter true,$(TARGET_BUILD_PDK)),)

dx_src_files := \
  $(call all-subdir-java-files) \
  $(call all-java-files-under,../../../libcore/dex/src/main/java)

# dx java library
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(dx_src_files)
LOCAL_JAR_MANIFEST := ../etc/manifest.txt
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= dx

# Force java 7 target because we want java 7 runtimes to run dx, at least as long as the android SDK
# requirement JDK 7.
LOCAL_JAVA_LANGUAGE_VERSION := 1.7

include $(BUILD_HOST_JAVA_LIBRARY)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

endif # No TARGET_BUILD_APPS or TARGET_BUILD_PDK

# the documentation
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(dx_src_files) $(call all-subdir-html-files)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= dx
LOCAL_DROIDDOC_OPTIONS := -hidden
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_IS_HOST_MODULE := true

include $(BUILD_DROIDDOC)

dx_src_files :=
