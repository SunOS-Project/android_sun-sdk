/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.notification

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import com.android.settings.R

import org.sun.custom.preference.SwitchPreferenceCompat
import org.sun.provider.SettingsExt.System.HEADS_UP_BLACKLIST
import org.sun.settings.fragment.PerAppSwitchConfigFragment

class HeadsUpBlacklistSettingsFragment : PerAppSwitchConfigFragment() {

    private var checkedList = listOf<String>()

    override fun getTitleResId() = R.string.heads_up_blacklist_title

    override fun getTopInfoResId() = R.string.heads_up_blacklist_summary

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        checkedList = Settings.System.getStringForUser(
            requireContext().contentResolver,
            HEADS_UP_BLACKLIST,
            UserHandle.USER_CURRENT
        )?.split(";")?.toList() ?: emptyList()
    }

    override fun isChecked(packageName: String, uid: Int): Boolean {
        return checkedList.contains(packageName)
    }

    override fun onSetChecked(pref: SwitchPreferenceCompat, packageName: String, uid: Int, checked: Boolean): Boolean {
        return true
    }

    override fun onCheckedListUpdated(pkgList: List<String>) {
        checkedList = pkgList.also {
            Settings.System.putStringForUser(
                requireContext().contentResolver,
                HEADS_UP_BLACKLIST,
                pkgList.joinToString(";"),
                UserHandle.USER_CURRENT
            )
        }
    }
}
