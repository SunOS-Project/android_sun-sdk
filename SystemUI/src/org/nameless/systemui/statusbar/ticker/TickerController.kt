/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.ticker

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

import org.nameless.provider.SettingsExt.System.STATUS_BAR_NOTIFICATION_TICKER

@SysUISingleton
class TickerController @Inject constructor(
    private val context: Context,
    @Main private val mainExecutor: Executor,
    @Main mainHandler: Handler,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker
) {

    private val callbacks = mutableListOf<Callback>()

    private var notificationTicker = true

    init {
        val settingsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    STATUS_BAR_NOTIFICATION_TICKER -> updateNotificationTicker(true)
                }
            }
        }
        systemSettings.registerContentObserverForUserSync(
                STATUS_BAR_NOTIFICATION_TICKER,
                settingsObserver, UserHandle.USER_ALL)
        updateSettings()

        userTracker.addCallback(object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                updateSettings()
            }
        }, mainExecutor)
    }

    private fun updateSettings() {
        updateNotificationTicker(false)
        notifySettingsChanged()
    }

    private fun updateNotificationTicker(notifyChange: Boolean) {
        notificationTicker = systemSettings.getIntForUser(STATUS_BAR_NOTIFICATION_TICKER,
                1, userTracker.userId) == 1
        if (notifyChange) {
            notifySettingsChanged()
        }
    }

    fun showNotificationTicker() = notificationTicker

    fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    private fun notifySettingsChanged() {
        callbacks.forEach {
            it.onSettingsChanged(
                notificationTicker
            )
        }
    }

    interface Callback {
        fun onSettingsChanged(
            notificationTicker: Boolean
        )
    }
}
