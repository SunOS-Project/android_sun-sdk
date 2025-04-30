/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.privacy

import com.android.settings.R

import org.sun.hardware.SensorBlockManager
import org.sun.hardware.SensorBlockManager.SHAKE_SENSORS_ALLOW
import org.sun.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_ALWAYS
import org.sun.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_FIRST_SCREEN
import org.sun.settings.fragment.PerAppListConfigFragment

class BlockShakeSensorsFragment : PerAppListConfigFragment() {

    private val sensorBlockManager by lazy { requireContext().getSystemService(SensorBlockManager::class.java)!! }

    override fun getAllowedSystemAppListResId() = 0

    override fun getTitleResId() = R.string.block_shake_sensors_title

    override fun getTopInfoResId() = R.string.block_shake_sensors_summary

    override fun getEntries() = listOf(
        getString(R.string.shake_sensors_allow),
        getString(R.string.shake_sensors_block_first_screen),
        getString(R.string.shake_sensors_block_always)
    )

    override fun getValues() = listOf(
        SHAKE_SENSORS_ALLOW,
        SHAKE_SENSORS_BLOCK_FIRST_SCREEN,
        SHAKE_SENSORS_BLOCK_ALWAYS
    )

    override fun getCurrentValue(packageName: String, uid: Int): Int {
        return sensorBlockManager.getShakeSensorsConfigForPackage(packageName)
    }

    override fun getDefaultValue() = SHAKE_SENSORS_ALLOW

    override fun onValueChanged(packageName: String, uid: Int, value: Int) {
        sensorBlockManager.setShakeSensorsConfigForPackage(packageName, value)
    }
}
