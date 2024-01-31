/*
 * Copyright (C) 2023 The LineageOS Project
 * Copyright (C) 2024 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nameless.systemui.statusbar.phone

import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.UserHandle

import com.android.internal.util.nameless.PopUpSettingsHelper

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.nameless.provider.SettingsExt.System.POP_UP_NOTIFICATION_JUMP_PORTRAIT
import org.nameless.provider.SettingsExt.System.POP_UP_NOTIFICATION_JUMP_LANDSCAPE

@SysUISingleton
class PopUpViewController @Inject constructor(
    private val context: Context,
    @Background private val bgExecutor: Executor,
    private val configurationController: ConfigurationController,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker
) {

    private var notificationJumpPortrait = false
    private var notificationJumpLandscape = false

    private var isLandscape: Boolean

    init {
        val settingsObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    POP_UP_NOTIFICATION_JUMP_PORTRAIT -> updateNotificationJumpPort()
                    POP_UP_NOTIFICATION_JUMP_LANDSCAPE -> updateNotificationJumpLand()
                }
            }
        }
        systemSettings.registerContentObserverForUser(
                POP_UP_NOTIFICATION_JUMP_PORTRAIT,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                POP_UP_NOTIFICATION_JUMP_LANDSCAPE,
                settingsObserver, UserHandle.USER_ALL)
        updateSettings()

        userTracker.addCallback(object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                updateSettings()
            }
        }, bgExecutor)

        isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        configurationController.addCallback(object : ConfigurationController.ConfigurationListener {
            override fun onConfigChanged(newConfig: Configuration?) {
                isLandscape = newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
        })
    }

    private fun updateSettings() {
        updateNotificationJumpPort()
        updateNotificationJumpLand()
    }

    private fun updateNotificationJumpPort() {
        notificationJumpPortrait = PopUpSettingsHelper.isNotificationJumpEnabled(
                context, false, userTracker.userId)
    }

    private fun updateNotificationJumpLand() {
        notificationJumpLandscape = PopUpSettingsHelper.isNotificationJumpEnabled(
                context, true, userTracker.userId)
    }

    fun shouldJumpNotificationWithPopUp(): Boolean {
        return if (isLandscape) notificationJumpLandscape else notificationJumpPortrait
    }
}
