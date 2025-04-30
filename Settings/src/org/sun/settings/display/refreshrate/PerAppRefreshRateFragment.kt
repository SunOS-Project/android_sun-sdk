/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display.refreshrate

import android.os.Bundle

import com.android.internal.util.sun.DisplayRefreshRateHelper

import com.android.settings.R

import org.sun.display.RefreshRateManager
import org.sun.settings.fragment.PerAppListConfigFragment

class PerAppRefreshRateFragment : PerAppListConfigFragment() {

    private val helper by lazy { DisplayRefreshRateHelper.getInstance(requireContext()) }

    private val refreshRateManager by lazy { requireContext().getSystemService(RefreshRateManager::class.java)!! }

    private val entries = mutableListOf<String>()
    private val values = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        entries.add(getString(R.string.per_app_refresh_rate_default))
        values.add(-1)

        helper.getSupportedRefreshRateList()?.forEach {
            entries.add("$it Hz")
            values.add(it)
        }
    }

    override fun getTitleResId() = R.string.per_app_refresh_rate_title

    override fun getTopInfoResId() = R.string.per_app_refresh_rate_summary

    override fun getEntries() = entries

    override fun getValues() = values

    override fun getCurrentValue(packageName: String, uid: Int): Int {
        return refreshRateManager.getRefreshRateForPackage(packageName)
    }

    override fun getDefaultValue() = -1

    override fun onValueChanged(packageName: String, uid: Int, value: Int) {
        if (value > 0) {
            refreshRateManager.setRefreshRateForPackage(packageName, value)
        } else {
            refreshRateManager.unsetRefreshRateForPackage(packageName)
        }
    }
}
