/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fragment

import android.content.Context

import androidx.preference.Preference

import org.nameless.custom.preference.SwitchPreferenceCompat
import org.nameless.settings.widget.AppSwitchPreference

abstract class PerAppSwitchConfigFragment : BasePerAppConfigFragment() {

    private val pkgCheckState = mutableMapOf<String, Boolean>()

    private var ignorePreferenceChange = false

    override fun createAppPreference(prefContext: Context, appData: AppData): Preference {
        return AppSwitchPreference(prefContext).apply {
            icon = getIcon(appData.packageName)
            title = appData.label
            summary = appData.packageName
            isChecked = isChecked(appData.packageName, appData.uid)

            pkgCheckState[appData.packageName] = isChecked

            setOnPreferenceChangeListener { p, v ->
                if (ignorePreferenceChange) {
                    return@setOnPreferenceChangeListener true
                }
                onSetChecked(
                    p as SwitchPreferenceCompat,
                    appData.packageName,
                    appData.uid,
                    v.toString().toBoolean()
                ).let {
                    if (it) {
                        pkgCheckState[appData.packageName] = v.toString().toBoolean()
                        onCheckedListUpdated(pkgCheckState.filter { it.value }.map { it.key })
                    }
                    return@setOnPreferenceChangeListener it
                }
            }
        }
    }

    override fun getSelectAllRunnable(): Runnable? {
        return Runnable {
            ignorePreferenceChange = true
            allPreference.forEach {
                (it as AppSwitchPreference).isChecked = true
            }
            pkgCheckState.mapValues { true }
            onCheckedListUpdated(pkgCheckState.filter { it.value }.map { it.key })
            ignorePreferenceChange = false
        }
    }

    override fun getDeselectAllRunnable(): Runnable? {
        return Runnable {
            ignorePreferenceChange = true
            allPreference.forEach {
                (it as AppSwitchPreference).isChecked = false
            }
            pkgCheckState.mapValues { false }
            onCheckedListUpdated(pkgCheckState.filter { it.value }.map { it.key })
            ignorePreferenceChange = false
        }
    }

    override fun getResetRunnable(): Runnable? {
        return Runnable {
            ignorePreferenceChange = true
            allPreference.forEach {
                (it as AppSwitchPreference).isChecked = getDefaultChecked()
            }
            pkgCheckState.mapValues { getDefaultChecked() }
            onCheckedListUpdated(pkgCheckState.filter { it.value }.map { it.key })
            ignorePreferenceChange = false
            showResetSuccessToast()
        }
    }

    open fun getDefaultChecked() = false

    abstract fun isChecked(packageName: String, uid: Int): Boolean

    abstract fun onSetChecked(pref: SwitchPreferenceCompat, packageName: String, uid: Int, checked: Boolean): Boolean

    abstract fun onCheckedListUpdated(pkgList: List<String>)
}
