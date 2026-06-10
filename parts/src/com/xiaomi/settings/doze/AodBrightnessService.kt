/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.doze

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.AmbientDisplayConfiguration
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.Display
import com.xiaomi.settings.display.DisplayFeatureWrapper
import com.xiaomi.settings.utils.FileUtils

class AodBrightnessService : Service() {

    private var sensorManager: SensorManager? = null
    private var aodSensor: Sensor? = null
    private lateinit var ambientConfig: AmbientDisplayConfiguration
    private var isDozing = false
    private var isDozeHbm = false
    private var displayState = Display.STATE_ON

    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values[0]
            isDozeHbm = value == AOD_SENSOR_EVENT_BRIGHT
            Log.d(TAG, "onSensorChanged: type=${event.sensor.type} value=$value")
            updateDozeBrightness()
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: ${intent.action}")
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (isDozing) {
                        isDozing = false
                        updateDozeBrightness()
                        sensorManager?.unregisterListener(sensorListener, aodSensor)
                    }
                }

                Intent.ACTION_SCREEN_OFF -> {
                    if (!ambientConfig.alwaysOnEnabled(UserHandle.USER_CURRENT)) {
                        Log.d(TAG, "AOD is not enabled.")
                        isDozing = false
                        return
                    }
                    if (!isDozing) {
                        isDozing = true
                        setInitialDozeHbmState()
                        sensorManager?.registerListener(
                            sensorListener,
                            aodSensor,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                    }
                }

                Intent.ACTION_DISPLAY_STATE_CHANGED -> {
                    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    displayState = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.state ?: displayState
                    updateDozeBrightness()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating service")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        aodSensor = sensorManager?.getDefaultSensor(SENSOR_TYPE_AOD, true)
        ambientConfig = AmbientDisplayConfiguration(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting service")
        val screenStateFilter = IntentFilter(Intent.ACTION_DISPLAY_STATE_CHANGED).apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying service")
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (ignored: IllegalArgumentException) {
            Log.d(TAG, "Screen state receiver already unregistered")
        }
        sensorManager?.unregisterListener(sensorListener, aodSensor)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setInitialDozeHbmState() {
        val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
        isDozeHbm = brightness > DOZE_HBM_BRIGHTNESS_THRESHOLD
        Log.d(TAG, "setInitialDozeHbmState: brightness=$brightness mIsDozeHbm=$isDozeHbm")
        updateDozeBrightness()
    }

    private fun updateDozeBrightness() {
        Log.d(
            TAG,
            "updateDozeBrightness: mIsDozing=$isDozing mDisplayState=$displayState mIsDozeHbm=$isDozeHbm"
        )
        if (FileUtils.readLineInt(FOD_PRESS_STATUS_PATH) == 1) {
            if (DEBUG) Log.d(TAG, "updateDozeBrightness: FOD active, aborting!")
            return
        }
        val isDozeState = isDozing && (displayState == Display.STATE_DOZE || displayState == Display.STATE_DOZE_SUSPEND)
        val mode = if (!isDozeState) 0 else if (isDozeHbm) 1 else 2
        try {
            DisplayFeatureWrapper.setFeature(25, mode, 0)
        } catch (e: Exception) {
            Log.e(TAG, "updateDozeBrightness failed!", e)
        }
    }

    companion object {
        private const val TAG = "XiaomiPartsAodBrightnessService"
        private const val DEBUG = true

        private const val SENSOR_TYPE_AOD = 33171029
        private const val AOD_SENSOR_EVENT_BRIGHT = 4f
        @Suppress("unused")
        private const val AOD_SENSOR_EVENT_DIM = 5f
        @Suppress("unused")
        private const val AOD_SENSOR_EVENT_DARK = 3f

        private const val FOD_PRESS_STATUS_PATH = "/sys/class/touch/touch_dev/fod_press_status"
        private const val DOZE_HBM_BRIGHTNESS_THRESHOLD = 18
    }
}
