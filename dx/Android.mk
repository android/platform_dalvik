# Copyright 2006 The Android Open Source Project
#
LOCAL_PATH := $(call my-dir)

# This tool is prebuilt if we're doing an app-only build.
ifeq ($(TARGET_BUILD_APPS)$(filter true,$(TARGET_BUILD_PDK)),)

# the dx script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := dx
LOCAL_SRC_FILES := etc/dx
LOCAL_ADDITIONAL_DEPENDENCIES := $(HOST_OUT_JAVA_LIBRARIES)/dx$(COMMON_JAVA_PACKAGE_SUFFIX)
include $(BUILD_PREBUILT)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

# the mainDexClasses rules
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := mainDexClasses.rules
LOCAL_SRC_FILES := etc/mainDexClasses.rules
LOCAL_REQUIRED_MODULES := dx
include $(BUILD_PREBUILT)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

installed_mainDexClasses.rules := $(LOCAL_INSTALLED_MODULE)

# the shrinkedAndroid jar is a library used by the mainDexClasses script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := shrinkedAndroid
LOCAL_BUILT_MODULE_STEM := shrinkedAndroid.jar
LOCAL_MODULE_SUFFIX := $(COMMON_JAVA_PACKAGE_SUFFIX)

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): PRIVATE_PROGUARD_FLAGS:= \
  -include $(addprefix $(LOCAL_PATH)/, shrinkedAndroid.proguard.flags)
$(LOCAL_BUILT_MODULE): $(call java-lib-files,android_stubs_current) \
                       $(addprefix $(LOCAL_PATH)/, shrinkedAndroid.proguard.flags)| $(PROGUARD)
	@echo Proguard: $@
	$(hide) $(PROGUARD) -injars "$<(**/*.class)" -outjars $@ $(PRIVATE_PROGUARD_FLAGS)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

installed_shrinkedAndroid := $(LOCAL_INSTALLED_MODULE)

# the mainDexClasses script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := mainDexClasses
LOCAL_SRC_FILES := etc/mainDexClasses
LOCAL_REQUIRED_MODULES := dx shrinkedAndroid mainDexClasses.rules
include $(BUILD_PREBUILT)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

endif # No TARGET_BUILD_APPS or TARGET_BUILD_PDK

# the dexmerger script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := dexmerger
LOCAL_SRC_FILES := etc/dexmerger
LOCAL_REQUIRED_MODULES := dx
include $(BUILD_PREBUILT)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

# the jasmin script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := jasmin
LOCAL_SRC_FILES := etc/jasmin
LOCAL_REQUIRED_MODULES := jasmin.jar
include $(BUILD_PREBUILT)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

# the jasmin lib
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := jasmin.jar
LOCAL_SRC_FILES := etc/jasmin.jar
include $(BUILD_PREBUILT)

INTERNAL_DALVIK_MODULES += $(LOCAL_INSTALLED_MODULE)

# the other stuff
# ============================================================
subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
		junit-tests \
		src \
	))

include $(subdirs)
