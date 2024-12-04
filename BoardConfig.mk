#
# Copyright (C) 2024 The Android Open Source Project
#
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/xiaomi/aurora

# A/B
AB_OTA_UPDATER := true

AB_OTA_PARTITIONS += \
    boot \
    init_boot \
    vendor_boot \
    dtbo \
    vbmeta \
    vbmeta_system \
    odm \
    product \
    recovery \
    system \
    system_dlkm \
    system_ext \
    vendor \
    vendor_dlkm

# Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv9-a
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_VARIANT := generic
TARGET_CPU_VARIANT_RUNTIME := kryo300

# Audio
$(call soong_config_set, android_hardware_audio, run_64bit, true)

AUDIO_FEATURE_ENABLED_DLKM := true
AUDIO_FEATURE_ENABLED_PROXY_DEVICE := true
AUDIO_FEATURE_ENABLED_DTS_EAGLE := false
AUDIO_FEATURE_ENABLED_PAL_HIDL := true
AUDIO_FEATURE_ENABLED_AGM_HIDL := true
AUDIO_FEATURE_ENABLED_LSM_HIDL := true
AUDIO_FEATURE_ENABLED_HW_ACCELERATED_EFFECTS := false
AUDIO_FEATURE_ENABLED_DLKM := true
AUDIO_FEATURE_ENABLED_INSTANCE_ID := true
AUDIO_FEATURE_ENABLED_KEEP_ALIVE := true
AUDIO_FEATURE_ENABLED_GEF_SUPPORT := true
AUDIO_FEATURE_ENABLED_SVA_MULTI_STAGE := true
BOARD_SUPPORTS_OPENSOURCE_STHAL := false
BOARD_SUPPORTS_SOUND_TRIGGER := true
BOARD_USES_ALSA_AUDIO := true
TARGET_USES_QCOM_MM_AUDIO := true

# Boot control
SOONG_CONFIG_NAMESPACES += ufsbsg
SOONG_CONFIG_ufsbsg += ufsframework
SOONG_CONFIG_ufsbsg_ufsframework := bsg

# Build
BUILD_BROKEN_DUP_RULES := true
BUILD_BROKEN_ELF_PREBUILT_PRODUCT_COPY_FILES := true

# Display
TARGET_SCREEN_DENSITY := 560

# DTB
BOARD_INCLUDE_DTB_IN_BOOTIMG := true

# HIDL
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += \
    $(DEVICE_PATH)/configs/vintf/compatibility_matrix.device.xml

DEVICE_MATRIX_FILE := $(DEVICE_PATH)/configs/vintf/compatibility_matrix.xml

DEVICE_MANIFEST_SKUS := pineapple
DEVICE_MANIFEST_PINEAPPLE_FILES := $(DEVICE_PATH)/configs/vintf/manifest_pineapple.xml

# Init Boot
BOARD_INIT_BOOT_HEADER_VERSION := 4
BOARD_MKBOOTIMG_INIT_ARGS += --header_version $(BOARD_INIT_BOOT_HEADER_VERSION)

# Kernel
BOARD_BOOTCONFIG := \
    androidboot.hardware=qcom \
    androidboot.load_modules_parallel=true \
    androidboot.memcg=1 \
    androidboot.usbcontroller=a600000.dwc3 \
    androidboot.vendor.qspa=true \
    androidboot.console=0

BOARD_KERNEL_CMDLINE := \
    bootinfo.fingerprint=$(AOSPA_VERSION) \
    disable_dma32=on \
    mtdoops.fingerprint=$(AOSPA_VERSION) \
    video=vfb:640x400,bpp=32,memsize=3072000

BOARD_KERNEL_BASE := 0x00000000
BOARD_KERNEL_PAGESIZE := 4096
BOARD_KERNEL_IMAGE_NAME := Image
BOARD_USES_GENERIC_KERNEL_IMAGE := true
BOARD_RAMDISK_USE_LZ4 := true
TARGET_NEEDS_DTBOIMAGE := true

BOARD_BOOT_HEADER_VERSION := 4
BOARD_MKBOOTIMG_ARGS := --header_version $(BOARD_BOOT_HEADER_VERSION)

include $(DEVICE_PATH)/kernel/kernel-platform.mk

# Metadata
BOARD_USES_METADATA_PARTITION := true

# OTA
TARGET_OTA_ASSERT_DEVICE := aurora

# Partitions
BOARD_FLASH_BLOCK_SIZE := 262144

BOARD_BOOTIMAGE_PARTITION_SIZE := 100663296
BOARD_DTBOIMG_PARTITION_SIZE := 25165824
BOARD_INIT_BOOT_IMAGE_PARTITION_SIZE := 8388608
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 104857600
BOARD_VENDOR_BOOTIMAGE_PARTITION_SIZE := 100663296

BOARD_SUPER_PARTITION_SIZE := 8321499136
BOARD_SUPER_PARTITION_GROUPS := qti_dynamic_partitions
BOARD_QTI_DYNAMIC_PARTITIONS_PARTITION_LIST := odm product system system_dlkm system_ext vendor vendor_dlkm
BOARD_QTI_DYNAMIC_PARTITIONS_SIZE := 8317304832

BOARD_PARTITION_LIST := $(call to-upper, $(BOARD_QTI_DYNAMIC_PARTITIONS_PARTITION_LIST))
$(foreach p, $(BOARD_PARTITION_LIST), $(eval BOARD_$(p)IMAGE_FILE_SYSTEM_TYPE := erofs))
$(foreach p, $(BOARD_PARTITION_LIST), $(eval TARGET_COPY_OUT_$(p) := $(call to-lower, $(p))))

BOARD_USES_SYSTEM_DLKMIMAGE := true
TARGET_USERIMAGES_USE_F2FS := true
BOARD_USES_VENDOR_DLKMIMAGE := true
TARGET_USERIMAGES_USE_EXT4 := true
BOARD_USERDATAIMAGE_FILE_SYSTEM_TYPE := f2fs

# Platform
BOARD_USES_QCOM_HARDWARE := true

# Properties
TARGET_ODM_PROP += $(DEVICE_PATH)/configs/props/odm.prop
TARGET_PRODUCT_PROP += $(DEVICE_PATH)/configs/props/product.prop
TARGET_SYSTEM_PROP += $(DEVICE_PATH)/configs/props/system.prop
TARGET_SYSTEM_EXT_PROP += $(DEVICE_PATH)/configs/props/system_ext.prop
TARGET_VENDOR_PROP += $(DEVICE_PATH)/configs/props/vendor.prop

# Recovery
TARGET_RECOVERY_PIXEL_FORMAT := RGBX_8888
TARGET_RECOVERY_FSTAB := $(DEVICE_PATH)/init/fstab.qcom
BOARD_USES_RECOVERY_AS_BOOT := false
$(call soong_config_set, ufsbsg, ufsframework, bsg)
BOARD_EXCLUDE_KERNEL_FROM_RECOVERY_IMAGE := true
BOARD_MOVE_RECOVERY_RESOURCES_TO_VENDOR_BOOT := false

# Sepolicy
include device/qcom/sepolicy_vndr/SEPolicy.mk
BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/private
SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/public

# Verified Boot
BOARD_AVB_ENABLE := true
BOARD_AVB_MAKE_VBMETA_IMAGE_ARGS += --flags 3
BOARD_MOVE_GSI_AVB_KEYS_TO_VENDOR_BOOT := true

BOARD_AVB_BOOT_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_BOOT_ALGORITHM := SHA256_RSA4096
BOARD_AVB_BOOT_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_BOOT_ROLLBACK_INDEX_LOCATION := 4

BOARD_AVB_DTBO_ALGORITHM := SHA256_RSA4096
BOARD_AVB_DTBO_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_DTBO_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_DTBO_ROLLBACK_INDEX_LOCATION := 3

BOARD_AVB_RECOVERY_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_RECOVERY_ALGORITHM := SHA256_RSA4096
BOARD_AVB_RECOVERY_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_RECOVERY_ROLLBACK_INDEX_LOCATION := 1

BOARD_AVB_VBMETA_SYSTEM := system system_dlkm system_ext product
BOARD_AVB_VBMETA_SYSTEM_ALGORITHM := SHA256_RSA4096
BOARD_AVB_VBMETA_SYSTEM_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX_LOCATION := 2

BOARD_AVB_VENDOR_BOOT_ALGORITHM := SHA256_RSA4096
BOARD_AVB_VENDOR_BOOT_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_VENDOR_BOOT_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_VENDOR_BOOT_ROLLBACK_INDEX_LOCATION := 5

BOARD_AVB_SYSTEM_DLKM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_VENDOR_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_VENDOR_DLKM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_ODM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256

# Vibrator
SOONG_CONFIG_NAMESPACES += XIAOMI_VIBRATOR
SOONG_CONFIG_XIAOMI_VIBRATOR := USE_EFFECT_STREAM
SOONG_CONFIG_XIAOMI_VIBRATOR_USE_EFFECT_STREAM := true

# WiFi
BOARD_WLAN_DEVICE := qcwcn
BOARD_HOSTAPD_DRIVER := NL80211
BOARD_HOSTAPD_PRIVATE_LIB := lib_driver_cmd_$(BOARD_WLAN_DEVICE)
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd_$(BOARD_WLAN_DEVICE)
CONFIG_IEEE80211AX := true
WIFI_DRIVER_DEFAULT := qca_cld3
WIFI_DRIVER_STATE_CTRL_PARAM := "/dev/wlan"
WIFI_DRIVER_STATE_OFF := "OFF"
WIFI_DRIVER_STATE_ON := "ON"
WIFI_HIDL_FEATURE_DUAL_INTERFACE := true
WIFI_HIDL_UNIFIED_SUPPLICANT_SERVICE_RC_ENTRY := true
WPA_SUPPLICANT_VERSION := VER_0_8_X

# Include the proprietary files BoardConfig.
include vendor/xiaomi/aurora/BoardConfigVendor.mk
