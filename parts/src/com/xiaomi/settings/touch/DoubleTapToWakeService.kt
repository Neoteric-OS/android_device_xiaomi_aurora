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
                updateWakeModes(contentResolver)
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
        updateWakeModes(contentResolver)
    }

    private fun registerObserverIfNeeded(resolver: ContentResolver) {
        synchronized(lock) {
            if (isObserverRegistered) return
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE),
                true,
                settingsObserver
            )
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOZE_TAP_SCREEN_GESTURE),
                true,
                settingsObserver
            )
            isObserverRegistered = true
        }
    }

    private fun updateWakeModes(resolver: ContentResolver) {
        updateWakeMode(
            resolver = resolver,
            secureKey = Settings.Secure.DOUBLE_TAP_TO_WAKE,
            mode = DOUBLE_TAP_TO_WAKE_MODE,
            label = "Double Tap to Wake"
        )
        updateWakeMode(
            resolver = resolver,
            secureKey = Settings.Secure.DOZE_TAP_SCREEN_GESTURE,
            mode = SINGLE_TAP_TO_WAKE_MODE,
            label = "Single Tap to Wake"
        )
    }

    private fun updateWakeMode(
        resolver: ContentResolver,
        secureKey: String,
        mode: Int,
        label: String
    ) {
        runCatching {
            val enabled = Settings.Secure.getInt(
                resolver,
                secureKey,
                0
            ) == 1
            TouchFeatureWrapper.setTouchMode(
                mode,
                if (enabled) 1 else 0
            )
            if (DEBUG) {
                Log.i(
                    TAG,
                    "$label set to ${if (enabled) "enabled" else "disabled"}"
                )
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to update $label mode", e)
        }
    }

    companion object {
        private const val TAG = "DoubleTapToWakeService"
        private const val DEBUG = true
        private const val DOUBLE_TAP_TO_WAKE_MODE = 14
        private const val SINGLE_TAP_TO_WAKE_MODE = 11

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, DoubleTapToWakeService::class.java),
                UserHandle.CURRENT
            )
        }
    }
}
