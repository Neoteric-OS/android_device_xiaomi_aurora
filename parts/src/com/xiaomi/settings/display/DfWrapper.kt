/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature

object DfWrapper {

    private const val TAG = "XiaomiPartsDisplayFeatureWrapper"

    private var displayFeature: IDisplayFeature? = null

    private val deathRecipient = IBinder.DeathRecipient {
        Log.d(TAG, "serviceDied")
        displayFeature = null
    }

    @JvmStatic
    fun getDisplayFeature(): IDisplayFeature? {
        if (displayFeature == null) {
            Log.d(TAG, "getDisplayFeature: mDisplayFeature=null")
            try {
                val name = "default"
                val fqName = "${IDisplayFeature.DESCRIPTOR}/$name"
                val binder = android.os.Binder.allowBlocking(
                    ServiceManager.waitForDeclaredService(fqName)
                )
                displayFeature = IDisplayFeature.Stub.asInterface(binder)
                displayFeature?.asBinder()?.linkToDeath(deathRecipient, 0)
            } catch (e: Exception) {
                Log.e(TAG, "getDisplayFeature failed!", e)
            }
        }
        return displayFeature
    }

    @JvmStatic
    fun setDisplayFeature(params: DfParams) {
        val feature = getDisplayFeature()
        if (feature == null) {
            Log.e(TAG, "setDisplayFeatureParams: displayFeature is null!")
            return
        }
        Log.d(TAG, "setDisplayFeatureParams: $params")
        try {
            feature.setFeature(0, params.mode, params.value, params.cookie)
        } catch (e: Exception) {
            Log.e(TAG, "setDisplayFeatureParams failed!", e)
        }
    }

    data class DfParams(val mode: Int, val value: Int, val cookie: Int) {
        override fun toString(): String = "DisplayFeatureParams($mode, $value, $cookie)"
    }
}
