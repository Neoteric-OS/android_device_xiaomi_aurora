/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.os.IHwBinder.DeathRecipient
import android.util.Log
import vendor.xiaomi.hw.touchfeature.ITouchFeature

object TfWrapper {

    private const val TAG = "XiaomiPartsTouchFeatureWrapper"

    private var touchFeature: ITouchFeature? = null

    @Suppress("unused")
    private val deathRecipient = DeathRecipient {
        Log.d(TAG, "serviceDied")
        touchFeature = null
    }

    @JvmStatic
    fun getTouchFeature(): ITouchFeature? {
        if (touchFeature == null) {
            Log.d(TAG, "getTouchFeature: mTouchFeature=null")
            try {
                val name = "default"
                val fqName = "${ITouchFeature.DESCRIPTOR}/$name"
                val binder = android.os.Binder.allowBlocking(
                    android.os.ServiceManager.waitForDeclaredService(fqName)
                )
                touchFeature = ITouchFeature.Stub.asInterface(binder)
            } catch (e: Exception) {
                Log.e(TAG, "getTouchFeature failed!", e)
            }
        }
        return touchFeature
    }

    @JvmStatic
    fun setTouchFeature(params: TfParams) {
        val feature = getTouchFeature()
        if (feature == null) {
            Log.e(TAG, "setTouchFeatureParams: touchfeature is null!")
            return
        }
        Log.d(TAG, "setTouchFeatureParams: $params")
        try {
            if (params.valueArray != null) {
                feature.setEdgeMode(0, params.mode, params.valueArray, params.valueArray.size)
            } else {
                feature.setTouchMode(0, params.mode, params.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setTouchFeatureParams failed!", e)
        }
    }

    data class TfParams(val mode: Int, val value: Int, val valueArray: IntArray?) {
        constructor(mode: Int, value: Int) : this(mode, value, null)
        constructor(mode: Int, valueArray: IntArray) : this(mode, 0, valueArray)

        override fun toString(): String {
            return if (valueArray != null) {
                "TouchFeatureParams($mode, ${valueArray.joinToString(prefix = \"[\", postfix = \"]\")})"
            } else {
                "TouchFeatureParams($mode, $value)"
            }
        }
    }
}
