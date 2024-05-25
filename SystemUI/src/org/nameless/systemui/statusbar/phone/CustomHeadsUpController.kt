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

import android.app.role.RoleManager
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.UserHandle

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.nameless.app.GameModeInfo
import org.nameless.app.GameModeManager
import org.nameless.app.IGameModeInfoListener
import org.nameless.provider.SettingsExt.System.DISABLE_LANDSCAPE_HEADS_UP
import org.nameless.provider.SettingsExt.System.HEADS_UP_BLACKLIST
import org.nameless.provider.SettingsExt.System.HEADS_UP_STOPLIST
import org.nameless.provider.SettingsExt.System.LESS_BORING_HEADS_UP
import org.nameless.systemui.statusbar.policy.ForegroundActivityListener

@SysUISingleton
class CustomHeadsUpController @Inject constructor(
    context: Context,
    @Main private val mainExecutor: Executor,
    @Main mainHandler: Handler,
    private val configurationController: ConfigurationController,
    private val foregroundActivityListener: ForegroundActivityListener,
    private val roleManager: RoleManager,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker
) {

    private val blacklistApps = mutableSetOf<String>()
    private val stoplistApps = mutableSetOf<String>()

    private var disableInLandscape = false
    private var lessBoring = false

    private var disabledByGame = false

    private var isLandscape: Boolean

    init {
        val settingsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    DISABLE_LANDSCAPE_HEADS_UP -> updateLandscapeHeadsUp()
                    HEADS_UP_BLACKLIST -> updateBlacklistApps()
                    HEADS_UP_STOPLIST -> updateStoplistApps()
                    LESS_BORING_HEADS_UP -> updateLessBoringHeadsUp()
                }
            }
        }
        systemSettings.registerContentObserverForUser(
                DISABLE_LANDSCAPE_HEADS_UP,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                HEADS_UP_BLACKLIST,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                HEADS_UP_STOPLIST,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                LESS_BORING_HEADS_UP,
                settingsObserver, UserHandle.USER_ALL)
        updateSettings()

        userTracker.addCallback(object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                updateSettings()
            }
        }, mainExecutor)

        isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        configurationController.addCallback(object : ConfigurationController.ConfigurationListener {
            override fun onConfigChanged(newConfig: Configuration?) {
                isLandscape = newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
        })

        context.getSystemService(GameModeManager::class.java)!!.registerGameModeInfoListener(
            object : IGameModeInfoListener.Stub() {
                override fun onGameModeInfoChanged(info: GameModeInfo) {
                    disabledByGame = info.isDanmakuNotificationEnabled()
                            || info.shouldDisableHeadsUp()
                }
            }
        )
    }

    private fun updateSettings() {
        updateLandscapeHeadsUp()
        updateBlacklistApps()
        updateStoplistApps()
        updateLessBoringHeadsUp()
    }

    private fun updateLandscapeHeadsUp() {
        disableInLandscape = systemSettings.getIntForUser(
                DISABLE_LANDSCAPE_HEADS_UP, 0, userTracker.userId) == 1
    }

    private fun updateBlacklistApps() {
        blacklistApps.clear()
        systemSettings.getStringForUser(
                HEADS_UP_BLACKLIST, userTracker.userId)
                ?.split(";")?.forEach { blacklistApps.add(it) }
    }

    private fun updateStoplistApps() {
        stoplistApps.clear()
        systemSettings.getStringForUser(
                HEADS_UP_STOPLIST, userTracker.userId)
                ?.split(";")?.forEach { stoplistApps.add(it) }
    }

    private fun updateLessBoringHeadsUp() {
        lessBoring = systemSettings.getIntForUser(
                LESS_BORING_HEADS_UP, 0, userTracker.userId) == 1
    }

    fun interceptHeadsUp(fromPackage: String): Boolean {
        if (disabledByGame) {
            return true
        }
        // Always allow heads up from dialer app
        if (fromPackage == roleManager.getRoleHolders(RoleManager.ROLE_DIALER).firstOrNull()) {
            return false
        }
        // Always allow heads up from sms app
        if (fromPackage == roleManager.getRoleHolders(RoleManager.ROLE_SMS).firstOrNull()) {
            return false
        }
        if (lessBoring) {
            return true
        }
        if (disableInLandscape && isLandscape) {
            return true
        }
        return blacklistApps.contains(fromPackage) ||
                stoplistApps.contains(foregroundActivityListener.getTopPackageName())
    }
}
