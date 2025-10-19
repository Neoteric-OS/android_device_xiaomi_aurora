/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Display.HdrCapabilities
import com.xiaomi.settings.display.ColorModeService
import com.xiaomi.settings.display.DcDimmingService
import com.xiaomi.settings.doze.AodBrightnessService
import com.xiaomi.settings.edgesuppression.EdgeSuppressionService
import com.xiaomi.settings.touch.TouchOrientationService
import com.xiaomi.settings.touch.TouchPollingRateService
import vendor.xiaomi.hw.touchfeature.ITouchFeature

class BootCompletedReceiver : BroadcastReceiver() {

    private var xiaomiTouchFeatureAidl: ITouchFeature? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.i(TAG, "Received intent: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> onLockedBootCompleted(context)
            Intent.ACTION_BOOT_COMPLETED -> onBootCompleted(context)
        }
    }

    private fun onLockedBootCompleted(context: Context) {
        context.startServiceAsUser(Intent(context, ColorModeService::class.java), UserHandle.CURRENT)

        val displayManager = context.getSystemService(DisplayManager::class.java)
        if (displayManager != null) {
            displayManager.overrideHdrTypes(
                Display.DEFAULT_DISPLAY,
                intArrayOf(
                    HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                    HdrCapabilities.HDR_TYPE_HDR10,
                    HdrCapabilities.HDR_TYPE_HLG,
                    HdrCapabilities.HDR_TYPE_HDR10_PLUS
                )
            )
        } else {
            Log.e(TAG, "DisplayManager service not available")
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateTapToWakeStatus(context)
            }
        }

        val resolver: ContentResolver = context.contentResolver
        resolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE),
            true,
            observer
        )

        context.startServiceAsUser(Intent(context, AodBrightnessService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, DcDimmingService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, EdgeSuppressionService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, TouchOrientationService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, TouchPollingRateService::class.java), UserHandle.CURRENT)

        updateTapToWakeStatus(context)
    }

    private fun updateTapToWakeStatus(context: Context) {
        try {
            if (xiaomiTouchFeatureAidl == null) {
                try {
                    val name = "default"
                    val fqName = "${ITouchFeature.DESCRIPTOR}/$name"
                    val binder: IBinder = android.os.Binder.allowBlocking(
                        android.os.ServiceManager.waitForDeclaredService(fqName)
                    )
                    xiaomiTouchFeatureAidl = ITouchFeature.Stub.asInterface(binder)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize Touch Feature service", e)
                    return
                }
            }

            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.DOUBLE_TAP_TO_WAKE,
                0
            ) == 1

            val touchFeature = xiaomiTouchFeatureAidl
            if (touchFeature != null) {
                touchFeature.setTouchMode(0, DOUBLE_TAP_TO_WAKE_MODE, if (enabled) 1 else 0)
                if (DEBUG) {
                    Log.i(TAG, "Tap to Wake set to ${if (enabled) "enabled" else "disabled"}")
                }
            } else {
                Log.e(TAG, "Touch Feature AIDL interface is not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Tap to Wake status", e)
        }
    }

    private fun onBootCompleted(context: Context) {
        // Intentionally left blank; add post-boot logic here if needed.
    }

    companion object {
        private const val TAG = "XiaomiParts"
        private const val DEBUG = true
        private const val DOUBLE_TAP_TO_WAKE_MODE = 14
    }
}
