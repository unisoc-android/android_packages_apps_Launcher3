###############################################################################
LOCAL_PATH:= $(my-dir)

# NativeLauncherLayout
include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := NativeLauncherLayout
LOCAL_MODULE_OWNER := unisoc
LOCAL_MODULE_TAGS := optional
LOCAL_PRODUCT_MODULE := true
LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_SDK_VERSION := current
include $(BUILD_PACKAGE)

