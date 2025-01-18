/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import com.android.settings.R

import org.nameless.custom.preference.SwitchPreferenceCompat
import org.nameless.provider.SettingsExt.System.STATUS_BAR_NOTIFICATION_TICKER_BLACKLIST
import org.nameless.settings.fragment.PerAppSwitchConfigFragment

class TickerBlacklistSettingsFragment : PerAppSwitchConfigFragment() {

    private var checkedList = listOf<String>()

    override fun getTitleResId() = R.string.status_bar_notification_ticker_blacklist_title

    override fun getTopInfoResId() = R.string.status_bar_notification_ticker_blacklist_summary

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        checkedList = Settings.System.getStringForUser(
            requireContext().contentResolver,
            STATUS_BAR_NOTIFICATION_TICKER_BLACKLIST,
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
                STATUS_BAR_NOTIFICATION_TICKER_BLACKLIST,
                pkgList.joinToString(";"),
                UserHandle.USER_CURRENT
            )
        }
    }
}
