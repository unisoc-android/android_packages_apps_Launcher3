###############################################################################
LOCAL_PATH:= $(my-dir)

# NativeLauncherConfigOverlay
include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := NativeLauncherConfigOverlay
LOCAL_MODULE_OWNER := unisoc
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_SDK_VERSION := current
include $(BUILD_RRO_PACKAGE)
