/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fragment

import android.content.Context

import androidx.preference.Preference

import org.nameless.settings.widget.AppListPreference

abstract class PerAppListConfigFragment : BasePerAppConfigFragment() {

    private var ignorePreferenceChange = false

    override fun createAppPreference(prefContext: Context, appData: AppData): Preference {
        return AppListPreference(prefContext).apply {
            icon = getIcon(appData.packageName)
            title = appData.label
            dialogTitle = appData.label

            entries = this@PerAppListConfigFragment.getEntries().toTypedArray()
            entryValues = getValues().map { it.toString() }.toTypedArray()

            getCurrentValue(appData.packageName, appData.uid).toString().let { v ->
                value = v
                entryValues.indexOfFirst { it == v }.let { idx ->
                    summary = if (idx >= 0) entries[idx] else String()
                }
            }

            setOnPreferenceChangeListener { _, v ->
                if (ignorePreferenceChange) {
                    return@setOnPreferenceChangeListener true
                }
                v.toString().let { newValue ->
                    onValueChanged(appData.packageName, appData.uid, newValue.toInt())
                    entryValues.indexOfFirst { it == newValue }.let { idx ->
                        summary = if (idx >= 0) entries[idx] else String()
                    }
                }
                return@setOnPreferenceChangeListener true
            }
        }
    }

    abstract fun getEntries(): List<String>

    abstract fun getValues(): List<Int>

    abstract fun getCurrentValue(packageName: String, uid: Int): Int

    abstract fun getDefaultValue(): Int

    abstract fun onValueChanged(packageName: String, uid: Int, value: Int)

    override fun getResetRunnable(): Runnable? {
        return Runnable {
            ignorePreferenceChange = true
            allPreference.forEachIndexed { i, p ->
                (p as AppListPreference).apply {
                    findIndexOfValue(getDefaultValue().toString()).let { idx ->
                        setValueIndex(idx)
                        summary = if (idx >= 0) entries[idx] else String()
                    }
                }
                onValueChanged(allAppData[i].packageName, allAppData[i].uid, getDefaultValue())
            }
            ignorePreferenceChange = false
            showResetSuccessToast()
        }
    }
}
