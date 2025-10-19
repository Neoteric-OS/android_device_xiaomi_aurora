/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.edgesuppression

import android.app.Fragment
import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class EdgeSuppressionSettingsActivity : CollapsingToolbarBaseActivity() {

    private var edgeSuppressionSettingsFragment: EdgeSuppressionSettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentManager = fragmentManager
        val fragment: Fragment? = fragmentManager.findFragmentById(R.id.content_frame)
        if (fragment == null) {
            edgeSuppressionSettingsFragment = EdgeSuppressionSettingsFragment()
            fragmentManager.beginTransaction()
                .add(R.id.content_frame, edgeSuppressionSettingsFragment, TAG)
                .commit()
        } else {
            edgeSuppressionSettingsFragment = fragment as EdgeSuppressionSettingsFragment
        }
    }

    companion object {
        private const val TAG = "edgesuppression"
    }
}
