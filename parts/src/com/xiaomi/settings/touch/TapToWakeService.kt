/*
 * Copyright (C) 2026 Neoteric OS
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log

// The touch driver reports single taps as KEY_GOTO while TOUCH_AOD_ENABLE or TOUCH_FODICON_ENABLE is set.
class TapToWakeService : Service() {

    private var registered = false

    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = updateTapMode()
        }

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = updateTapMode()
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        if (!registered) {
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOZE_TAP_SCREEN_GESTURE),
                false,
                settingsObserver
            )
            registerReceiver(
                screenReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_DISPLAY_STATE_CHANGED)
                }
            )
            registered = true
        }
        updateTapMode()
        return START_STICKY
    }

    override fun onDestroy() {
        if (registered) {
            contentResolver.unregisterContentObserver(settingsObserver)
            unregisterReceiver(screenReceiver)
            registered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateTapMode() {
        val enabled =
            Settings.Secure.getInt(contentResolver, Settings.Secure.DOZE_TAP_SCREEN_GESTURE, 0) != 0
        val value = if (enabled) 1 else 0
        TouchFeatureWrapper.setTouchMode(TOUCH_AOD_ENABLE, value)
        TouchFeatureWrapper.setTouchMode(TOUCH_FODICON_ENABLE, value)
    }

    companion object {
        private const val TAG = "TapToWakeService"
        private const val DEBUG = true
        private const val TOUCH_AOD_ENABLE = 11
        private const val TOUCH_FODICON_ENABLE = 16
    }
}
