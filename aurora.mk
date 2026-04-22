#
# Copyright (C) 2024 The Android Open Source Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the Neoteric configuration.
$(call inherit-product, vendor/neoteric/target/product/neoteric-target.mk)

# Inherit from aurora device.
$(call inherit-product, device/xiaomi/aurora/pineapple.mk)

# Device identifier
PRODUCT_DEVICE := aurora
PRODUCT_NAME := aurora
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := 24031PN0DC
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="aurora_global-user 15 AQ3A.240627.003 OS2.0.102.0.VNAEUXM release-keys" \
    BuildFingerprint=Xiaomi/aurora_global/aurora:15/AQ3A.240627.003/OS2.0.102.0.VNAEUXM:user/release-keys \
    DeviceName=aurora \
    DeviceProduct=aurora \
    SystemDevice=aurora \
    SystemName=aurora

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
