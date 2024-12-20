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
        vendor/etc/media_codecs_pineapple.xml|vendor/etc/media_codecs_pineapple_vendor.xml)
            sed -i "/vp9.decoder/,/<\/MediaCodec>/d" "${2}"
            sed -i "/av1.decoder/,/<\/MediaCodec>/d" "${2}"
            ;;
        vendor/etc/seccomp_policy/atfwd@2.0.policy | vendor/etc/seccomp_policy/wfdhdcphalservice.policy | vendor/etc/seccomp_policy/qsap_sensors.policy | vendor/etc/seccomp_policy/gnss@2.0-qsap-location.policy)
            [ "$2" = "" ] && return 0
            [ -n "$(tail -c 1 "${2}")" ] && echo >> "${2}"
            grep -q "gettid: 1" "${2}" || echo "gettid: 1" >> "${2}"
            ;;
        vendor/etc/seccomp_policy/c2audio.vendor.ext-arm64.policy)
            [ "$2" = "" ] && return 0
            grep -q "setsockopt: 1" "${2}" || echo "setsockopt: 1" >> "${2}"
            ;;
        vendor/lib64/vendor.libdpmframework.so)
            grep -q libhidlbase_shim.so "$2" || "$PATCHELF" --add-needed libhidlbase_shim.so "$2"
            ;;
        vendor/lib64/libqcodec2_core.so)
            grep -q "libcodec2_shim.so" "${2}" || "${PATCHELF}" --add-needed "libcodec2_shim.so" "${2}"
            ;;
        vendor/etc/init/vendor.xiaomi.hardware.vibratorfeature.service.rc)
            sed -i "s/\/odm\/bin\//\/vendor\/bin\//g" "${2}"
            sed -i "s/\/odm\/etc\//\/vendor\/etc\//g" "${2}"
            ;;
    esac
}

# Initialize the helper
setup_vendor "${DEVICE}" "${VENDOR}" "${ANDROID_ROOT}" false "${CLEAN_VENDOR}"

extract "${MY_DIR}/proprietary-files.txt" "${SRC}" "${KANG}" --section "${SECTION}"

"${MY_DIR}/setup-makefiles.sh"
