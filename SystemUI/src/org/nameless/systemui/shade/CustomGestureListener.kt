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

package org.nameless.systemui.shade

import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.StatusBarState
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.nameless.provider.SettingsExt.System.DOUBLE_TAP_SLEEP_LOCKSCREEN
import org.nameless.provider.SettingsExt.System.DOUBLE_TAP_SLEEP_STATUSBAR
import org.nameless.provider.SettingsExt.System.STATUSBAR_BRIGHTNESS_CONTROL
import org.nameless.provider.SettingsExt.System.STATUSBAR_GESTURE_PORTRAIT_ONLY

@SysUISingleton
class CustomGestureListener @Inject constructor(
    private val context: Context,
    @Background private val bgExecutor: Executor,
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
        val settingsObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    DOUBLE_TAP_SLEEP_LOCKSCREEN -> updateDoubleTapSleepLockscreen()
                    DOUBLE_TAP_SLEEP_STATUSBAR -> updateDoubleTapSleepStatusbar()
                    STATUSBAR_BRIGHTNESS_CONTROL -> updateStatusbarBrightnessControl()
                    STATUSBAR_GESTURE_PORTRAIT_ONLY -> updateGesturePortraitOnly()
                }
            }
        }
        systemSettings.registerContentObserver(
                Settings.System.getUriFor(DOUBLE_TAP_SLEEP_LOCKSCREEN),
                true, settingsObserver)
        systemSettings.registerContentObserver(
                Settings.System.getUriFor(DOUBLE_TAP_SLEEP_STATUSBAR),
                true, settingsObserver)
        systemSettings.registerContentObserver(
                Settings.System.getUriFor(STATUSBAR_BRIGHTNESS_CONTROL),
                true, settingsObserver)
        systemSettings.registerContentObserver(
                Settings.System.getUriFor(STATUSBAR_GESTURE_PORTRAIT_ONLY),
                true, settingsObserver)
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
        updateDoubleTapSleepLockscreen()
        updateDoubleTapSleepStatusbar()
        updateStatusbarBrightnessControl()
        updateGesturePortraitOnly()
    }

    private fun updateDoubleTapSleepLockscreen() {
        doubleTapSleepLockscreen = Settings.System.getIntForUser(
                context.contentResolver,
                DOUBLE_TAP_SLEEP_LOCKSCREEN,
                1, userTracker.userId) != 0
    }

    private fun updateDoubleTapSleepStatusbar() {
        doubleTapSleepStatusbar = Settings.System.getIntForUser(
                context.contentResolver,
                DOUBLE_TAP_SLEEP_STATUSBAR,
                1, userTracker.userId) != 0
    }

    private fun updateStatusbarBrightnessControl() {
        statusbarBrightnessControl = Settings.System.getIntForUser(
                context.contentResolver,
                STATUSBAR_BRIGHTNESS_CONTROL,
                0, userTracker.userId) != 0
    }

    private fun updateGesturePortraitOnly() {
        gesturePortraitOnly = Settings.System.getIntForUser(
                context.contentResolver,
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
