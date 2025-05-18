/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display

import androidx.appcompat.app.AlertDialog

import com.android.settings.R

import org.sun.custom.preference.SwitchPreferenceCompat
import org.sun.settings.fragment.PerAppSwitchConfigFragment

class ForceFullscreenDisplayFragment : PerAppSwitchConfigFragment() {

    override fun getAllowedSystemAppListResId() = R.array.config_fullscreenDisplayAllowedSystemApps

    override fun getBlacklistAppListResId() = com.android.internal.R.array.config_forceFullBlacklistApps

    override fun getTitleResId() = R.string.app_force_fullscreen_title

    override fun getTopInfoResId() = R.string.app_force_fullscreen_info

    override fun isChecked(packageName: String, uid: Int): Boolean {
        return packageManager.isForceFull(packageName)
    }

    override fun onSetChecked(pref: SwitchPreferenceCompat, packageName: String, uid: Int, checked: Boolean): Boolean {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(requireContext().getText(R.string.switch_fullscreen_display_warn_title))
            setMessage(requireContext().getText(R.string.switch_fullscreen_display_warn_content))
            setPositiveButton(R.string.okay, { _, _ ->
                packageManager.setForceFull(packageName, checked)
                pref.isChecked = checked
            })
            setNegativeButton(R.string.cancel, null)
        }.show()
        return false
    }

    override fun onCheckedListUpdated(pkgList: List<String>) {}

    override fun getSelectAllRunnable(): Runnable? {
        return Runnable {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(requireContext().getText(R.string.switch_fullscreen_display_warn_title))
                setMessage(requireContext().getText(R.string.switch_fullscreen_display_warn_content))
                setPositiveButton(R.string.okay, { _, _ ->
                    super.getSelectAllRunnable()?.run()
                    allAppData.forEach {
                        packageManager.setForceFull(it.packageName, true)
                    }
                })
                setNegativeButton(R.string.cancel, null)
            }.show()
        }
    }

    override fun getDeselectAllRunnable(): Runnable? {
        return Runnable {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(requireContext().getText(R.string.switch_fullscreen_display_warn_title))
                setMessage(requireContext().getText(R.string.switch_fullscreen_display_warn_content))
                setPositiveButton(R.string.okay, { _, _ ->
                    super.getDeselectAllRunnable()?.run()
                    allAppData.forEach {
                        packageManager.setForceFull(it.packageName, false)
                    }
                })
                setNegativeButton(R.string.cancel, null)
            }.show()
        }
    }

    override fun getResetRunnable(): Runnable? {
        return Runnable {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(requireContext().getText(R.string.switch_fullscreen_display_warn_title))
                setMessage(requireContext().getText(R.string.switch_fullscreen_display_warn_content))
                setPositiveButton(R.string.okay, { _, _ ->
                    super.getSelectAllRunnable()?.run()
                    allAppData.forEach {
                        packageManager.setForceFull(it.packageName, true)
                    }
                    showResetSuccessToast()
                })
                setNegativeButton(R.string.cancel, null)
            }.show()
        }
    }
}
