/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.system

import com.android.settings.R

import org.sun.app.GameModeManager
import org.sun.custom.preference.SwitchPreferenceCompat
import org.sun.settings.fragment.PerAppSwitchConfigFragment

class ManageGamesFragment : PerAppSwitchConfigFragment() {

    private val gameModeManager by lazy { requireContext().getSystemService(GameModeManager::class.java)!! }

    override fun getAllowedSystemAppListResId() = 0

    override fun getTitleResId() = R.string.game_mode_manage_apps_title

    override fun getTopInfoResId() = R.string.game_mode_manage_apps_summary

    override fun isChecked(packageName: String, uid: Int): Boolean {
        return gameModeManager.isAppGame(packageName)
    }

    override fun onSetChecked(pref: SwitchPreferenceCompat, packageName: String, uid: Int, checked: Boolean): Boolean {
        if (checked) {
            gameModeManager.addGame(packageName)
        } else {
            gameModeManager.removeGame(packageName)
        }
        return true;
    }

    override fun onCheckedListUpdated(pkgList: List<String>) {}

    override fun getSelectAllRunnable(): Runnable? {
        return Runnable {
            super.getSelectAllRunnable()?.run()
            allAppData.forEach {
                gameModeManager.addGame(it.packageName)
            }
        }
    }

    override fun getDeselectAllRunnable(): Runnable? {
        return Runnable {
            super.getDeselectAllRunnable()?.run()
            allAppData.forEach {
                gameModeManager.removeGame(it.packageName)
            }
        }
    }
}
