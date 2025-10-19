/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.os.UserHandle
import android.util.Log

/**
 * This service relays the current device orientation to the touchscreen. This automatically
 * configures touchscreen parameters such as edge supression and sensitivity, to match the current
 * orientation.
 */
class TouchOrientationService : Service() {

    private var rotation: Int = 0
        set(value) {
            if (field == value) return
            field = value
            if (DEBUG) Log.d(TAG, "rotation=$value")
            runCatching {
                    // Lucky for us, Surface.ROTATION_* directly translates into touchpanel values
                    TouchFeatureWrapper.setTouchMode(MODE_TOUCH_PANEL_ORIENTATION, value)
                }
                .onFailure { e -> Log.e(TAG, "Failed to set touch panel orientation", e) }
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        updateOrientation()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (DEBUG) Log.d(TAG, "onConfigurationChanged")
        updateOrientation()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateOrientation() {
        rotation = display.rotation
    }

    companion object {
        private const val TAG = "TouchOrientationService"
        //private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
        private val DEBUG = true

        private const val MODE_TOUCH_PANEL_ORIENTATION = 8

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, TouchOrientationService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
