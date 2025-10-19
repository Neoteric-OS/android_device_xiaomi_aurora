/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.System.DC_DIMMING_STATE
import android.util.Log

class DcDimmingService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val settingObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            Log.e(TAG, "SettingObserver: onChange")
            updateDcDimming()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating service")
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(DC_DIMMING_STATE),
            false,
            settingObserver,
            UserHandle.USER_CURRENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting service")
        updateDcDimming()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying service")
        contentResolver.unregisterContentObserver(settingObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateDcDimming() {
        val enabled = Settings.System.getIntForUser(
            contentResolver,
            DC_DIMMING_STATE,
            0,
            UserHandle.USER_CURRENT
        ) == 1
        Log.d(TAG, "updateDcDimming: enabled=$enabled")
        DisplayFeatureWrapper.setFeature(20, if (enabled) 1 else 0, 0)
    }

    companion object {
        private const val TAG = "XiaomiPartsDcDimmingService"
        @Suppress("unused")
        private const val DEBUG = true
    }
}
