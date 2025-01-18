/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.system

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import com.android.settings.R

import org.nameless.custom.preference.SwitchPreferenceCompat
import org.nameless.provider.SettingsExt.System.POP_UP_NOTIFICATION_BLACKLIST
import org.nameless.settings.fragment.PerAppSwitchConfigFragment

class PopUpNotificationBlacklistFragment : PerAppSwitchConfigFragment() {

    private var checkedList = listOf<String>()

    override fun getTitleResId() = R.string.pop_up_view_light_weight_notificiation_blacklist_title

    override fun getTopInfoResId() = R.string.pop_up_view_light_weight_notificiation_blacklist_summary

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        checkedList = Settings.System.getStringForUser(
            requireContext().contentResolver,
            POP_UP_NOTIFICATION_BLACKLIST,
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
                POP_UP_NOTIFICATION_BLACKLIST,
                pkgList.joinToString(";"),
                UserHandle.USER_CURRENT
            )
        }
    }
}
