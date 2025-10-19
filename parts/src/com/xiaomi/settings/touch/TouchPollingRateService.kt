/*
 * Copyright (C) 2023 Paranoid Android
 * Copyright (C) 2025 Neoteric OS
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
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log

class TouchPollingRateService : Service() {

    private var isPollingEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private val screenStateReceiver = ScreenStateReceiver()
    private val settingsObserver = SettingsObserver(handler)

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Creating service")
        settingsObserver.update()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        screenStateReceiver.register()
        settingsObserver.register()
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service")
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (ignored: IllegalArgumentException) {
            Log.d(TAG, "ScreenStateReceiver already unregistered")
        }
        contentResolver.unregisterContentObserver(settingsObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class ScreenStateReceiver : BroadcastReceiver() {
        fun register() {
            if (DEBUG) Log.d(TAG, "ScreenStateReceiver: register")
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (DEBUG) Log.d(TAG, "Received ACTION_SCREEN_ON")
                    settingsObserver.update()
                }

                Intent.ACTION_USER_PRESENT -> {
                    if (DEBUG) Log.d(TAG, "Received ACTION_USER_PRESENT")
                    settingsObserver.update()
                }
            }
        }
    }

    private inner class SettingsObserver(handler: Handler) : ContentObserver(handler) {

        fun register() {
            if (DEBUG) Log.d(TAG, "SettingsObserver: register")
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(SECURE_KEY_POLLING),
                false,
                this
            )
        }

        fun update() {
            isPollingEnabled = Settings.Secure.getInt(contentResolver, SECURE_KEY_POLLING, 0) != 0
            if (DEBUG) Log.d(TAG, "SettingsObserver: SECURE_KEY_POLLING: $isPollingEnabled")
            TfWrapper.setTouchFeature(TfWrapper.TfParams(0, if (isPollingEnabled) 1 else 0))
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (DEBUG) Log.d(TAG, "SettingsObserver: onChange: ${uri?.toString()}")
            if (uri == Settings.Secure.getUriFor(SECURE_KEY_POLLING)) {
                update()
            }
        }
    }

    companion object {
        private const val TAG = "XiaomiPartsTouchPollingRateService"
        @Suppress("unused")
        private const val DEBUG = true
        private const val SECURE_KEY_POLLING = "touch_polling_enabled"
    }
}
