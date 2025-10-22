/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.UserHandle
import android.util.Log
import android.view.Display
import android.view.Display.HdrCapabilities
import com.xiaomi.settings.display.ColorModeService
import com.xiaomi.settings.display.DcDimmingService
import com.xiaomi.settings.doze.AodBrightnessService
import com.xiaomi.settings.edgesuppression.EdgeSuppressionService
import com.xiaomi.settings.touch.DoubleTapToWakeService
import com.xiaomi.settings.touch.TouchOrientationService
import com.xiaomi.settings.touch.TouchPollingRateService

class BootCompletedReceiver : BroadcastReceiver() {

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

        context.startServiceAsUser(Intent(context, AodBrightnessService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, DcDimmingService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, EdgeSuppressionService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, TouchOrientationService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, TouchPollingRateService::class.java), UserHandle.CURRENT)
        context.startServiceAsUser(Intent(context, DoubleTapToWakeService::class.java), UserHandle.CURRENT)
    }

    private fun onBootCompleted(context: Context) {
        // Intentionally left blank; add post-boot logic here if needed.
    }

    companion object {
        private const val TAG = "XiaomiParts"
        private const val DEBUG = true
    }
}
