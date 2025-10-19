/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.util.Log

class TouchOrientationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        updateOrientation()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged")
        updateOrientation()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateOrientation() {
        val rotation = display?.rotation ?: 0
        Log.d(TAG, "updateTpOrientation: rotation=$rotation")
        TfWrapper.setTouchFeature(TfWrapper.TfParams(8, rotation))
    }

    companion object {
        private const val TAG = "XiaomiPartsTouchOrientationService"
        @Suppress("unused")
        private const val DEBUG = true
    }
}
