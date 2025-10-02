#!/bin/bash
#
# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2017-2020 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

set -e

DEVICE=aurora
VENDOR=xiaomi

# Load extract_utils and do some sanity checks
MY_DIR="${BASH_SOURCE%/*}"
if [[ ! -d "${MY_DIR}" ]]; then MY_DIR="${PWD}"; fi

ANDROID_ROOT="${MY_DIR}/../../.."

HELPER="${ANDROID_ROOT}/tools/extract-utils/extract_utils.sh"
if [ ! -f "${HELPER}" ]; then
    echo "Unable to find helper script at ${HELPER}"
    exit 1
fi
source "${HELPER}"

# Default to sanitizing the vendor folder before extraction
CLEAN_VENDOR=true

KANG=
SECTION=

while [ "${#}" -gt 0 ]; do
    case "${1}" in
    -n | --no-cleanup)
        CLEAN_VENDOR=false
        ;;
    -k | --kang)
        KANG="--kang"
        ;;
    -s | --section)
        SECTION="${2}"
        shift
        CLEAN_VENDOR=false
        ;;
    *)
        SRC="${1}"
        ;;
    esac
    shift
done

if [ -z "${SRC}" ]; then
    SRC="adb"
fi

function blob_fixup() {
    case "${1}" in
        odm/etc/camera/enhance_motiontuning.xml | odm/etc/camera/night_motiontuning.xml | odm/etc/camera/motiontuning.xml | odm/etc/camera/night_enhance_motiontuning.xml)
            sed -i 's/<?xml=/<?xml /g' "${2}"
            ;;
        system_ext/etc/vintf/manifest/vendor.qti.qesdsys.service.xml)
            sed -i '/\/\*\*/,/\*\//c\<!--\n * Copyright (c) 2021 Qualcomm Technologies, Inc.\n * All Rights Reserved.\n * Confidential and Proprietary - Qualcomm Technologies, Inc.\n-->' "${2}"
            ;;
        system/priv-app/MiuiCamera/MiuiCamera.apk)
            tmp_dir="${EXTRACT_TMP_DIR}/MiuiCamera"
            mkdir -p "$tmp_dir"

            if [ ! -f "$2" ]; then
                echo "Error: File $2 does not exist."
                exit 1
            fi

            java -jar "${APKTOOL}" d -q "$2" -o "$tmp_dir" -f || {
                echo "Error running apktool."
                exit 1
            }

            if grep -rl "com.miui.gallery" "$tmp_dir"; then
                grep -rl "com.miui.gallery" "$tmp_dir" | xargs sed -i 's|"com.miui.gallery"|"com.google.android.apps.photos"|g'
            fi

            java -jar "${APKTOOL}" b -q "$tmp_dir" -o "$2" || {
                echo "Error rebuilding APK."
                exit 1
            }

            rm -rf "$tmp_dir"
            ;;
        vendor/bin/hw/vendor.qti.media.c2@1.0-service | vendor/bin/hw/vendor.dolby.media.c2@1.0-service | vendor/bin/hw/vendor.qti.media.c2audio@1.0-service)
            "${PATCHELF}" --add-needed "lib-mediac2.so" "${2}"
            ;;
        vendor/etc/audio/sku_pineapple/audio_effects.xml)
            sed -i 's|<library name="misoundfx" path="libmisoundfx.so"/>|<library name="misoundfx" path="libmisoundfx_ext.so"/>|' "${2}"
            ;;
        vendor/etc/init/hw/init.qcom.rc)
            sed -i '/interface vendor\.qti\.hardware\.wigig\.netperftuner@1\.0::INetPerfTuner default/d' "${2}"
            ;;
        vendor/etc/seccomp_policy/atfwd@2.0.policy | vendor/etc/seccomp_policy/wfdhdcphalservice.policy | vendor/etc/seccomp_policy/qsap_sensors.policy | vendor/etc/seccomp_policy/qesdk.policy | vendor/etc/seccomp_policy/qesdksec.policy)
            [ "$2" = "" ] && return 0
            [ -n "$(tail -c 1 "${2}")" ] && echo >> "${2}"
            grep -q "gettid: 1" "${2}" || echo "gettid: 1" >> "${2}"
            ;;
        vendor/etc/seccomp_policy/c2audio.vendor.ext-arm64.policy)
            [ "$2" = "" ] && return 0
            grep -q "setsockopt: 1" "${2}" || echo "setsockopt: 1" >> "${2}"
            ;;
        vendor/etc/seccomp_policy/gnss@2.0-qsap-location.policy)
            [ "$2" = "" ] && return 0
            [ -n "$(tail -c 1 "${2}")" ] && echo >> "${2}"
            grep -q "gettid: 1" "${2}" || echo "gettid: 1" >> "${2}"
            grep -q "sched_get_priority_min: 1" "${2}" || echo "sched_get_priority_min: 1" >> "${2}"
            grep -q "sched_get_priority_max: 1" "${2}" || echo "sched_get_priority_max: 1" >> "${2}"
            ;;
        odm/lib64/libaudioroute_ext.so | vendor/lib64/libar-pal.so)
            "${PATCHELF}" --replace-needed "libaudioroute.so" "libaudioroute-v34.so" "${2}"
            ;;
        vendor/lib64/hw/camera.qcom.so | vendor/lib64/hw/com.qti.chi.override.so | vendor/lib64/libchifeature2.so | vendor/lib64/libcameraopt.so | vendor/lib64/libcamxcommonutils.so | vendor/lib64/libmialgoengine.so)
            "${PATCHELF}" --add-needed "libprocessgroup_shim.so" "$2"
            ;;
        vendor/lib64/hw/camera.xiaomi.so)
            "${PATCHELF}" --add-needed "libprocessgroup_shim.so" "$2"
            ;;
        vendor/lib64/vendor.libdpmframework.so)
            "${PATCHELF}" --add-needed "libhidlbase_shim.so" "$2"
            "${PATCHELF}" --add-needed "libbinder_shim.so" "${2}"
            ;;
        vendor/lib64/libqcodec2_core.so)
            grep -q "libcodec2_shim.so" "${2}" || "${PATCHELF}" --add-needed "libcodec2_shim.so" "${2}"
            ;;
        vendor/etc/init/vendor.xiaomi.hardware.vibratorfeature.service.rc)
            sed -i "s/\/odm\/bin\//\/vendor\/bin\//g" "${2}"
            sed -i "s/\/odm\/etc\//\/vendor\/etc\//g" "${2}"
            ;;
        vendor/lib64/libqcc_sdk.so | vendor/lib64/libqms_xiaomi.so | vendor/lib64/libqms_client.so | vendor/bin/qcc-vendor | vendor/bin/xtra-daemon | vendor/bin/qms | vendor/bin/cnd | vendor/lib64/libcne.so)
            "${PATCHELF}" --add-needed "libbinder_shim.so" "${2}"
            ;;
        vendor/lib64/libdlbdsservice.so | vendor/lib64/libdlbpreg.so | vendor/lib64/libdlbdsservice.so | vendor/lib64/libswspatializer_ext.so | vendor/lib64/soundfx/libdlbvol.so | vendor/lib64/soundfx/libhwdap.so | vendor/lib64/soundfx/libswspatializer.so)
            "${PATCHELF}" --replace-needed "libstagefright_foundation.so" "libstagefright_foundation-v33.so" "${2}"
            ;;
        vendor/bin/hw/vendor.dolby.hardware.dms@2.0-service)
            grep -q "libstagefright_foundation-v33.so" "${2}" || "${PATCHELF}" --add-needed "libstagefright_foundation-v33.so" "${2}"
            ;;
    esac
}

# Initialize the helper
setup_vendor "${DEVICE}" "${VENDOR}" "${ANDROID_ROOT}" false "${CLEAN_VENDOR}"

extract "${MY_DIR}/proprietary-files.txt" "${SRC}" "${KANG}" --section "${SECTION}"

"${MY_DIR}/setup-makefiles.sh"
