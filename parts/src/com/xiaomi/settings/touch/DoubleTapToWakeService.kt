/*
 * Copyright (C) 2024 Neoteric OS
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

class DoubleTapToWakeService : Service() {

    private val lock = Any()
    private var isObserverRegistered = false

    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                updateTapToWake(contentResolver)
            }
        }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")
        initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        initialize()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        synchronized(lock) {
            if (isObserverRegistered) {
                contentResolver.unregisterContentObserver(settingsObserver)
                isObserverRegistered = false
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initialize() {
        registerObserverIfNeeded(contentResolver)
        updateTapToWake(contentResolver)
    }

    private fun registerObserverIfNeeded(resolver: ContentResolver) {
        synchronized(lock) {
            if (isObserverRegistered) return
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE),
                true,
                settingsObserver
            )
            isObserverRegistered = true
        }
    }

    private fun updateTapToWake(resolver: ContentResolver) {
        runCatching {
            val enabled = Settings.Secure.getInt(
                resolver,
                Settings.Secure.DOUBLE_TAP_TO_WAKE,
                0
            ) == 1
            TouchFeatureWrapper.setTouchMode(
                DOUBLE_TAP_TO_WAKE_MODE,
                if (enabled) 1 else 0
            )
            if (DEBUG) {
                Log.i(
                    TAG,
                    "Tap to Wake set to ${if (enabled) "enabled" else "disabled"}"
                )
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to update Tap to Wake mode", e)
        }
    }

    companion object {
        private const val TAG = "DoubleTapToWakeService"
        private const val DEBUG = true
        private const val DOUBLE_TAP_TO_WAKE_MODE = 14

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, DoubleTapToWakeService::class.java),
                UserHandle.CURRENT
            )
        }
    }
}
