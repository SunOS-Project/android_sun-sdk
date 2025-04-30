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

package org.sun.systemui.shade

import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.PowerManager
import android.os.UserHandle
import android.view.GestureDetector
import android.view.MotionEvent

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.StatusBarState
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.sun.provider.SettingsExt.System.DOUBLE_TAP_SLEEP_LOCKSCREEN
import org.sun.provider.SettingsExt.System.DOUBLE_TAP_SLEEP_STATUSBAR
import org.sun.provider.SettingsExt.System.STATUSBAR_BRIGHTNESS_CONTROL
import org.sun.provider.SettingsExt.System.STATUSBAR_GESTURE_PORTRAIT_ONLY

@SysUISingleton
class CustomGestureListener @Inject constructor(
    context: Context,
    @Main private val mainExecutor: Executor,
    @Main mainHandler: Handler,
    private val configurationController: ConfigurationController,
    private val powerManager: PowerManager,
    private val statusBarStateController: StatusBarStateController,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker
) : GestureDetector.SimpleOnGestureListener() {

    private var doubleTapSleepLockscreen = false
    private var doubleTapSleepStatusbar = false
    private var statusbarBrightnessControl = false
    private var gesturePortraitOnly = false

    private var isLandscape: Boolean

    init {
        val settingsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    DOUBLE_TAP_SLEEP_LOCKSCREEN -> updateDoubleTapSleepLockscreen()
                    DOUBLE_TAP_SLEEP_STATUSBAR -> updateDoubleTapSleepStatusbar()
                    STATUSBAR_BRIGHTNESS_CONTROL -> updateStatusbarBrightnessControl()
                    STATUSBAR_GESTURE_PORTRAIT_ONLY -> updateGesturePortraitOnly()
                }
            }
        }
        systemSettings.registerContentObserverForUserSync(
                DOUBLE_TAP_SLEEP_LOCKSCREEN,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUserSync(
                DOUBLE_TAP_SLEEP_STATUSBAR,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUserSync(
                STATUSBAR_BRIGHTNESS_CONTROL,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUserSync(
                STATUSBAR_GESTURE_PORTRAIT_ONLY,
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
    }

    private fun updateSettings() {
        updateDoubleTapSleepLockscreen()
        updateDoubleTapSleepStatusbar()
        updateStatusbarBrightnessControl()
        updateGesturePortraitOnly()
    }

    private fun updateDoubleTapSleepLockscreen() {
        doubleTapSleepLockscreen = systemSettings.getIntForUser(
                DOUBLE_TAP_SLEEP_LOCKSCREEN,
                1, userTracker.userId) != 0
    }

    private fun updateDoubleTapSleepStatusbar() {
        doubleTapSleepStatusbar = systemSettings.getIntForUser(
                DOUBLE_TAP_SLEEP_STATUSBAR,
                1, userTracker.userId) != 0
    }

    private fun updateStatusbarBrightnessControl() {
        statusbarBrightnessControl = systemSettings.getIntForUser(
                STATUSBAR_BRIGHTNESS_CONTROL,
                0, userTracker.userId) != 0
    }

    private fun updateGesturePortraitOnly() {
        gesturePortraitOnly = systemSettings.getIntForUser(
                STATUSBAR_GESTURE_PORTRAIT_ONLY,
                0, userTracker.userId) != 0
    }

    fun shouldSleepFromLockscreen(): Boolean {
        return doubleTapSleepLockscreen
    }

    fun shouldSleepFromStatusbar(): Boolean {
        return doubleTapSleepStatusbar && (!gesturePortraitOnly || !isLandscape)
    }

    fun isStatusbarBrightnessControlEnabled(): Boolean {
        return statusbarBrightnessControl && (!gesturePortraitOnly || !isLandscape)
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        if (!statusBarStateController.isDozing &&
                statusBarStateController.state == StatusBarState.KEYGUARD &&
                shouldSleepFromLockscreen()
        ) {
            powerManager.goToSleep(e.getEventTime())
            return true
        }
        return false
    }
}
