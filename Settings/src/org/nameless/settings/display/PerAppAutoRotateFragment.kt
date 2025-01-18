/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display

import com.android.settings.R

import org.nameless.os.RotateManager
import org.nameless.os.RotateManager.ROTATE_FOLLOW_SYSTEM
import org.nameless.os.RotateManager.ROTATE_FORCE_OFF
import org.nameless.os.RotateManager.ROTATE_FORCE_ON
import org.nameless.settings.fragment.PerAppListConfigFragment

class PerAppAutoRotateFragment : PerAppListConfigFragment() {

    private val rotateManager by lazy { requireContext().getSystemService(RotateManager::class.java)!! }

    override fun getTitleResId() = R.string.per_app_auto_rotate_title

    override fun getTopInfoResId() = R.string.per_app_auto_rotate_summary

    override fun getEntries() = listOf(
        getString(R.string.per_app_auto_rotate_default),
        getString(R.string.per_app_auto_rotate_off),
        getString(R.string.per_app_auto_rotate_on)
    )

    override fun getValues() = listOf(
        ROTATE_FOLLOW_SYSTEM,
        ROTATE_FORCE_OFF,
        ROTATE_FORCE_ON
    )

    override fun getCurrentValue(packageName: String, uid: Int): Int {
        return rotateManager.getRotateConfigForPackage(packageName)
    }

    override fun getDefaultValue() = ROTATE_FOLLOW_SYSTEM

    override fun onValueChanged(packageName: String, uid: Int, value: Int) {
        rotateManager.setRotateConfigForPackage(packageName, value)
    }
}
