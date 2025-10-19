/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.edgesuppression

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.IBinder
import android.util.Log

class EdgeSuppressionService : Service() {

    private var edgeSuppressionManager: EdgeSuppressionManager? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (DEBUG) Log.d(TAG, "onReceive: ${intent.action}")
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                edgeSuppressionManager?.handleEdgeSuppressionChange()
            }
        }
    }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        super.onCreate()
        try {
            val screenStateFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
            registerReceiver(screenStateReceiver, screenStateFilter)
            edgeSuppressionManager = EdgeSuppressionManager.getInstance(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while initializing EdgeSuppressionManager", e)
            stopSelf()
            return
        }

        packageManager.setComponentEnabledSetting(
            ComponentName(this, EdgeSuppressionSettingsActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        val manager = edgeSuppressionManager
        if (manager != null) {
            try {
                manager.handleEdgeSuppressionChange()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling edge suppression change", e)
            }
        } else {
            Log.e(TAG, "EdgeSuppressionManager is null, skipping edge suppression change")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (ignored: IllegalArgumentException) {
            if (DEBUG) Log.d(TAG, "Receiver already unregistered")
        }
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (DEBUG) Log.d(TAG, "onConfigurationChanged")
        val manager = edgeSuppressionManager
        if (manager != null) {
            try {
                manager.handleEdgeSuppressionChange()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling configuration change", e)
            }
        } else {
            Log.e(TAG, "EdgeSuppressionManager is null, skipping configuration change handling")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "XiaomiPartsEdgeSuppressionService"
        @Suppress("unused")
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }
}
