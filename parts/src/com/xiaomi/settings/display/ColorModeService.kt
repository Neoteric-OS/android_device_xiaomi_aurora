/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.display.AmbientDisplayConfiguration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

class ColorModeService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var ambientConfig: AmbientDisplayConfiguration
    private var isDozing = false

    private val settingObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            Log.e(TAG, "SettingObserver: onChange")
            setCurrentColorMode()
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e(TAG, "onReceive: ${intent.action}")
            handleScreenStateChanged(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
        setupService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        teardownService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupService() {
        ambientConfig = AmbientDisplayConfiguration(this)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(DISPLAY_COLOR_MODE),
            false,
            settingObserver,
            UserHandle.USER_CURRENT
        )

        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)

        setCurrentColorMode()
    }

    private fun teardownService() {
        contentResolver.unregisterContentObserver(settingObserver)
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (ignored: IllegalArgumentException) {
            Log.e(TAG, "Receiver already unregistered", ignored)
        }
    }

    private fun setCurrentColorMode() {
        if (isDozing) {
            Log.e(TAG, "Skipping color mode change in AOD")
            return
        }

        val colorMode = Settings.System.getIntForUser(
            contentResolver,
            DISPLAY_COLOR_MODE,
            DEFAULT_COLOR_MODE,
            UserHandle.USER_CURRENT
        )

        val params = COLOR_MAP[colorMode] ?: STANDARD_PARAMS
        Log.e(TAG, "Setting color mode: $colorMode, params=$params")

        DfWrapper.setDisplayFeature(if (params.mode == EXPERT_MODE) EXPERT_PARAMS else params)
    }

    private fun handleScreenStateChanged(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> handleScreenOn()
            Intent.ACTION_SCREEN_OFF -> handleScreenOff()
        }
    }

    private fun handleScreenOn() {
        if (isDozing) {
            isDozing = false
            restoreColorModeAfterDoze()
        }
    }

    private fun restoreColorModeAfterDoze() {
        handler.postDelayed({
            Log.e(TAG, "Restoring color mode after AOD")
            setCurrentColorMode()
        }, 100)
    }

    private fun handleScreenOff() {
        if (!ambientConfig.alwaysOnEnabled(UserHandle.USER_CURRENT)) {
            Log.e(TAG, "AOD not enabled")
            isDozing = false
            return
        }
        isDozing = true
        setStandardColorModeForDoze()
    }

    private fun setStandardColorModeForDoze() {
        handler.removeCallbacksAndMessages(null)
        Log.e(TAG, "Setting standard color mode for AOD")
        DfWrapper.setDisplayFeature(STANDARD_PARAMS)
    }

    companion object {
        private const val TAG = "XiaomiPartsColorModeService"
        @Suppress("unused")
        private const val DEBUG = true

        private val DEFAULT_COLOR_MODE =
            SystemProperties.getInt("persist.sys.sf.native_mode", 0)
        private val STANDARD_PARAMS = DfWrapper.DfParams(2, 2, 255)

        private const val EXPERT_MODE = 26
        private val EXPERT_PARAMS = DfWrapper.DfParams(EXPERT_MODE, 0, 10)

        private val COLOR_MAP = mapOf(
            258 to DfWrapper.DfParams(0, 2, 255),  // Vivid
            256 to DfWrapper.DfParams(1, 2, 255),  // Saturated
            257 to STANDARD_PARAMS,                // Standard
            269 to DfWrapper.DfParams(26, 1, 0),   // Original
            268 to DfWrapper.DfParams(26, 2, 0),   // P3
            267 to DfWrapper.DfParams(26, 3, 0)    // sRGB
        )
    }
}
