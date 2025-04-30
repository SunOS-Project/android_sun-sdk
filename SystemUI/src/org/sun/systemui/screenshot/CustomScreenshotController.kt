/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.systemui.screenshot

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.UserHandle
import android.os.VibrationAttributes
import android.os.VibrationExtInfo

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.settings.UserTracker
import com.android.systemui.shared.system.FullscreenTaskStackChangeListener
import com.android.systemui.shared.system.TaskStackChangeListeners
import com.android.systemui.statusbar.VibratorHelper
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.sun.provider.SettingsExt.System.SCREENSHOT_SOUND

import vendor.sun.hardware.vibratorExt.Effect.HEAVY_CLICK
import vendor.sun.hardware.vibratorExt.Effect.SCREENSHOT

@SysUISingleton
class CustomScreenshotController @Inject constructor(
    context: Context,
    @Main private val mainExecutor: Executor,
    @Main mainHandler: Handler,
    private val systemSettings: SystemSettings,
    private val taskStackChangeListeners: TaskStackChangeListeners,
    private val userTracker: UserTracker,
    private val vibratorHelper: VibratorHelper
) {

    private val fullscreenTaskStackChangeListener = FullscreenTaskStackChangeListener(context)

    private var screenshotSound = true

    init {
        val settingsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                updateSettings()
            }
        }
        systemSettings.registerContentObserverForUserSync(
                SCREENSHOT_SOUND,
                settingsObserver, UserHandle.USER_ALL)
        updateSettings()

        taskStackChangeListeners.registerTaskStackListener(fullscreenTaskStackChangeListener)

        userTracker.addCallback(object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                updateSettings()
            }
        }, mainExecutor)
    }

    private fun updateSettings() {
        screenshotSound = systemSettings.getIntForUser(SCREENSHOT_SOUND, 1,
                userTracker.userId) == 1
    }

    fun interceptPlayCameraSound(): Boolean {
        vibratorHelper.vibrateExt(VibrationExtInfo.Builder().apply {
            setEffectId(SCREENSHOT)
            setFallbackEffectId(HEAVY_CLICK)
            setVibrationAttributes(HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
        }.build())
        return !screenshotSound
    }

    fun getForegroundAppLabel() = fullscreenTaskStackChangeListener.topPackageName

    companion object {
        private val HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
    }
}
