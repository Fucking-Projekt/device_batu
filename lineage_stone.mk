#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Device uses AIDL CAS HAL (com.android.hardware.cas APEX) instead of
# deprecated HIDL CAS 1.2 service. Set this before inheriting base products,
# where build/target/product/base_vendor.mk decides default vendor packages.
TARGET_REQUIRES_HIDL_CAS_HAL := false

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
TARGET_SUPPORTS_OMX_SERVICE := false
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from device.
$(call inherit-product, $(LOCAL_PATH)/device.mk)
$(call inherit-product, $(LOCAL_PATH)/overrides.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Gamebar
$(call inherit-product, packages/apps/GameBar/gamebar.mk)

# Sign keys
include vendor/lineage-priv/keys/keys.mk

# Boot animation
TARGET_SCREEN_HEIGHT := 2400
TARGET_SCREEN_WIDTH := 1080

# Flags
TARGET_EXCLUDES_AUDIOFX := true
PERF_ANIM_OVERRIDE := true
PRODUCT_NO_CAMERA := false
TARGET_DISABLE_EPPE := true
TARGET_ENABLE_FP_OVERRIDE := false
WITH_GMS := false

PRODUCT_BRAND := Redmi
PRODUCT_DEVICE := stone
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_MODEL := Redmi Note 12 5G
PRODUCT_NAME := lineage_stone

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildSystemFingerprint=$(SysBrand)/$(SysName)/$(SysDevice):$(PLATFORM_VERSION)/$(BUILD_ID)/$(BuildNumber):$(BuildVariant)/$(BuildKeys) \
    DeviceProduct=stone \
    DeviceName=stone \
    VdrProductBrand=Xiaomi \
    VdrDeviceName=mivendor \
    VdrProductManufacturer=Xiaomi \
    VdrProductModel="Xiaomi for ARM64" \
    VdrDeviceProduct=mivendor_holi \
    OdmProductBrand=Xiaomi \
    OdmDeviceName=miodm \
    OdmProductManufacturer=Xiaomi \
    OdmProductModel="Xiaomi for ARM64" \
    OdmDeviceProduct=miodm_holi \
    SysBrand=Android \
    SysName=generic_system \
    SysManufacturer=Xiaomi \
    SysModel="Generic System" \
    SysDevice=generic \
    PdtProductBrand=Xiaomi \
    PdtDeviceName=miproduct \
    PdtProductManufacturer=QUALCOMM \
    PdtProductModel=miproduct \
    PdtDeviceProduct=miproduct_stone \
    SexProductBrand=Xiaomi \
    SexDeviceName=missi \
    SexProductManufacturer=QUALCOMM \
    SexProductModel=missi \
    SexDeviceProduct=missi \
    BuildNumber=OS4.0.0.1.XMPMIXM \
    BuildUsername=Rimaki \
    BuildHostname=Corporate-Commisioner-of-Pertamina \
    BuildVariant=user \
    BuildKeys=release-keys
