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
    PRIVATE_BUILD_DESC="aurora_global-user 14 UKQ1.231003.002 V816.0.11.3.UNACNXM release-keys"

BUILD_FINGERPRINT := Xiaomi/aurora_global/aurora:14/UKQ1.231003.002/V816.0.11.3.UNACNXM:user/release-keys

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
