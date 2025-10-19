/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.edgesuppression

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.android.settingslib.widget.MainSwitchPreference
import com.xiaomi.settings.R

class EdgeSuppressionSettingsFragment :
    PreferenceFragment(),
    Preference.OnPreferenceChangeListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var edgeSuppressionManager: EdgeSuppressionManager
    private lateinit var sharedPreferences: SharedPreferences

    private var switchBar: MainSwitchPreference? = null
    private var widthPreference: SeekBarPreference? = null

    private var leftView: View? = null
    private var rightView: View? = null

    private val leftLayoutParams = FrameLayout.LayoutParams(-1, -1, 51)
    private val rightLayoutParams = FrameLayout.LayoutParams(-1, -1, 53)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        edgeSuppressionManager =
            EdgeSuppressionManager.getInstance(requireActivity().applicationContext)
        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireActivity().applicationContext)

        addPreferencesFromResource(R.xml.settings_edgesuppression)

        switchBar = findPreference("edgesuppression_enable") as? MainSwitchPreference
        switchBar?.addOnSwitchChangeListener(this)

        widthPreference = findPreference("edgesuppression_width") as? SeekBarPreference
        widthPreference?.apply {
            setUpdatesContinuously(true)
            onPreferenceChangeListener = this@EdgeSuppressionSettingsFragment
        }

        edgeSuppressionManager.handleEdgeSuppressionChange()
        setupEdgeSuppressionPreview(switchBar?.isChecked == true)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val value = (newValue.toString().toFloat() + 20f) / 100f
        updateEdgeSuppression(value)
        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val storedWidth = sharedPreferences.getInt("edgesuppression_width", 60)
        val value = if (isChecked) (storedWidth + 20f) / 100f else 0f
        updateEdgeSuppression(value)
    }

    private fun updateEdgeSuppression(value: Float) {
        val suppressionSize = edgeSuppressionManager.getSuppressionSize(false, value)
        leftLayoutParams.width = suppressionSize
        rightLayoutParams.width = suppressionSize
        leftView?.layoutParams = leftLayoutParams
        rightView?.layoutParams = rightLayoutParams

        sharedPreferences.edit()
            .putFloat("edgesuppression_width_value", value)
            .apply()

        edgeSuppressionManager.handleEdgeSuppressionChange()
    }

    private fun setupEdgeSuppressionPreview(enabled: Boolean) {
        val activity = requireActivity()
        val context = requireContext()
        val rootView = activity.window?.decorView as? ViewGroup ?: return
        leftView = View(context)
        rightView = View(context)

        val storedWidth = sharedPreferences.getInt("edgesuppression_width", 60)
        val widthValue = if (enabled) (storedWidth + 20f) / 100f else 0f
        val suppressionSize = edgeSuppressionManager.getSuppressionSize(false, widthValue)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenHeight = maxOf(metrics.widthPixels, metrics.heightPixels) - 1

        leftLayoutParams.height = screenHeight
        rightLayoutParams.height = screenHeight
        leftLayoutParams.width = suppressionSize
        rightLayoutParams.width = suppressionSize

        rootView.addView(leftView, leftLayoutParams)
        rootView.addView(rightView, rightLayoutParams)

        val restrictedColor = resources.getColor(R.color.restricted_tip_area_color, null)
        leftView?.setBackgroundColor(restrictedColor)
        rightView?.setBackgroundColor(restrictedColor)
    }

    companion object {
        private const val TAG = "XiaomiPartsEdgeSuppressionSettingsFragment"
        @Suppress("unused")
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }
}
