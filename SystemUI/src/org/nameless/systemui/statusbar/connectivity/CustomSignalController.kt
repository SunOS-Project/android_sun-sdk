/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.connectivity

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.UserHandle

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.settings.UserTracker
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.nameless.provider.SettingsExt.System.DATA_DISABLED_ICON
import org.nameless.provider.SettingsExt.System.SHOW_FOURG_ICON

@SysUISingleton
class CustomSignalController @Inject constructor(
    private val context: Context,
    @Main private val mainExecutor: Executor,
    @Main mainHandler: Handler,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker
) {

    private val callbacks = mutableListOf<Callback>()

    private var dataDisabledIcon = true
    private var show4gIcon = false

    init {
        val settingsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    DATA_DISABLED_ICON -> updateDataDisabledIcon(true)
                    SHOW_FOURG_ICON -> updateShow4gIcon(true)
                }
            }
        }
        systemSettings.registerContentObserverForUser(
                DATA_DISABLED_ICON,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                SHOW_FOURG_ICON,
                settingsObserver, UserHandle.USER_ALL)
        updateSettings()

        userTracker.addCallback(object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                updateSettings()
            }
        }, mainExecutor)
    }

    private fun updateSettings() {
        updateDataDisabledIcon(false)
        updateShow4gIcon(false)
        notifySettingsChanged()
    }

    private fun updateDataDisabledIcon(notifyChange: Boolean) {
        dataDisabledIcon = systemSettings.getIntForUser(DATA_DISABLED_ICON,
                1, userTracker.userId) == 1
        if (notifyChange) {
            notifySettingsChanged()
        }
    }

    private fun updateShow4gIcon(notifyChange: Boolean) {
        show4gIcon = systemSettings.getIntForUser(SHOW_FOURG_ICON,
                0, userTracker.userId) == 1
        if (notifyChange) {
            notifySettingsChanged()
        }
    }

    fun showDataDisabledIcon() = dataDisabledIcon

    fun show4gIcon() = show4gIcon

    fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    private fun notifySettingsChanged() {
        callbacks.forEach {
            it.onSettingsChanged(
                dataDisabledIcon,
                show4gIcon
            )
        }
    }

    interface Callback {
        fun onSettingsChanged(
            dataDisabledIcon: Boolean,
            show4gIcon: Boolean
        )
    }
}
