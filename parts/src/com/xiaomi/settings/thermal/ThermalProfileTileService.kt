/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xiaomi.settings.R
import com.xiaomi.settings.utils.FileUtils

class ThermalProfileTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val profile = FileUtils.readLineInt(THERMAL_PROFILE_PATH)
        updateUi(profile)
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val currentProfile = FileUtils.readLineInt(THERMAL_PROFILE_PATH)
        val newProfile =
            if (currentProfile == THERMAL_PROFILE_DEFAULT) THERMAL_PROFILE_MGAME else THERMAL_PROFILE_DEFAULT
        FileUtils.writeLine(THERMAL_PROFILE_PATH, newProfile)
        updateUi(newProfile)
    }

    private fun updateUi(profile: Int) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.thermalprofile_title)
        val subtitle = when (profile) {
            THERMAL_PROFILE_DEFAULT -> getString(R.string.thermalprofile_default)
            THERMAL_PROFILE_MGAME -> getString(R.string.thermalprofile_game)
            else -> getString(R.string.thermalprofile_unknown)
        }
        tile.subtitle = subtitle
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    companion object {
        private const val THERMAL_PROFILE_PATH = "/sys/class/thermal/thermal_message/sconfig"
        private const val THERMAL_PROFILE_DEFAULT = 0
        private const val THERMAL_PROFILE_MGAME = 19
    }
}
