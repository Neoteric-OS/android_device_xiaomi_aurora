/*
 * Copyright (C) 2026 Neoteric OS
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import android.util.Log

class SoFodTouchService : Service() {

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        enableSoFodModes()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enableSoFodModes() {
        TouchFeatureWrapper.setTouchMode(TOUCH_FOD_ENABLE, 1)
        TouchFeatureWrapper.setTouchMode(TOUCH_AOD_ENABLE, 1)
        TouchFeatureWrapper.setTouchMode(TOUCH_FODICON_ENABLE, 1)
    }

    companion object {
        private const val TAG = "SoFodTouchService"
        private const val DEBUG = true

        private const val TOUCH_FOD_ENABLE = 10
        private const val TOUCH_AOD_ENABLE = 11
        private const val TOUCH_FODICON_ENABLE = 16

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, SoFodTouchService::class.java),
                UserHandle.CURRENT
            )
        }
    }
}
