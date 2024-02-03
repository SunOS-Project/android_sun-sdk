/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.phone

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.UserHandle

import com.android.systemui.R
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.settings.UserTracker
import com.android.systemui.util.settings.SystemSettings

import java.util.concurrent.Executor

import javax.inject.Inject

import org.nameless.provider.SettingsExt.System.GESTURE_NAVBAR_IME_SPACE
import org.nameless.provider.SettingsExt.System.GESTURE_NAVBAR_IMMERSIVE
import org.nameless.provider.SettingsExt.System.GESTURE_NAVBAR_LENGTH_MODE
import org.nameless.provider.SettingsExt.System.GESTURE_NAVBAR_RADIUS_MODE
import org.nameless.provider.SettingsExt.System.NAVBAR_INVERSE_LAYOUT

@SysUISingleton
class NavbarAppearanceController @Inject constructor(
    @Main mainExecutor: Executor,
    @Main mainHandler: Handler,
    private val context: Context,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker
) : UserTracker.Callback {

    private val callbacks = mutableListOf<NavbarAppearanceChangeCallback>()

    private var barLengthMode = LENGTH_MODE_NORMAL
    private var barRadiusMode = RADIUS_MODE_NORMAL
    private var imeSpaceVisible = true
    private var immersive = false
    private var inverseLayout = false

    init {
        val settingsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    GESTURE_NAVBAR_LENGTH_MODE -> updateLengthMode(true)
                    GESTURE_NAVBAR_RADIUS_MODE -> updateRadiusMode(true)
                    GESTURE_NAVBAR_IME_SPACE -> updateImeSpaceVisibility(true)
                    GESTURE_NAVBAR_IMMERSIVE -> updateImmersiveState(true)
                    NAVBAR_INVERSE_LAYOUT -> updateInverseLayoutState(true)
                }
            }
        }
        systemSettings.registerContentObserverForUser(
                GESTURE_NAVBAR_LENGTH_MODE,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                GESTURE_NAVBAR_RADIUS_MODE,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                GESTURE_NAVBAR_IME_SPACE,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                GESTURE_NAVBAR_IMMERSIVE,
                settingsObserver, UserHandle.USER_ALL)
        systemSettings.registerContentObserverForUser(
                NAVBAR_INVERSE_LAYOUT,
                settingsObserver, UserHandle.USER_ALL)
        updateSettings()

        userTracker.addCallback(this, mainExecutor)
    }

    override fun onUserChanged(newUser: Int, userContext: Context) {
        updateSettings()
    }

    private fun updateSettings() {
        updateLengthMode(false)
        updateRadiusMode(false)
        updateImeSpaceVisibility(false)
        updateImmersiveState(false)
        updateInverseLayoutState(false)
        notifyAppearanceChanged()
    }

    private fun updateLengthMode(notifyChange: Boolean) {
        barLengthMode = systemSettings.getIntForUser(GESTURE_NAVBAR_LENGTH_MODE,
                LENGTH_MODE_NORMAL, userTracker.userId)
        if (notifyChange) {
            notifyAppearanceChanged()
        }
    }

    private fun updateRadiusMode(notifyChange: Boolean) {
        barRadiusMode = systemSettings.getIntForUser(GESTURE_NAVBAR_RADIUS_MODE,
                RADIUS_MODE_NORMAL, userTracker.userId)
        if (notifyChange) {
            notifyAppearanceChanged()
        }
    }

    private fun updateImeSpaceVisibility(notifyChange: Boolean) {
        imeSpaceVisible = systemSettings.getIntForUser(GESTURE_NAVBAR_IME_SPACE,
                1, userTracker.userId) == 1
        if (notifyChange) {
            notifyAppearanceChanged()
        }
    }

    private fun updateImmersiveState(notifyChange: Boolean) {
        immersive = systemSettings.getIntForUser(GESTURE_NAVBAR_IMMERSIVE,
                0, userTracker.userId) == 1
        if (notifyChange) {
            notifyAppearanceChanged()
        }
    }

    private fun updateInverseLayoutState(notifyChange: Boolean) {
        inverseLayout = systemSettings.getIntForUser(NAVBAR_INVERSE_LAYOUT,
                0, userTracker.userId) == 1
        if (notifyChange) {
            notifyAppearanceChanged()
        }
    }

    private fun notifyAppearanceChanged() {
        val length = getNavbarLength()
        val radius = getNavbarRadius()
        val height = getNavbarHeight()
        val frameHeight = getNavbarFrameHeight()
        val inverseLayout = isInverseLayoutEnabled()

        callbacks.forEach {
            it.onNavbarAppearanceChanged(
                length,
                radius,
                height,
                frameHeight,
                inverseLayout
            )
        }
    }

    fun getNavbarLength(): Int {
        return if (barLengthMode == LENGTH_MODE_HIDDEN) {
            0
        } else if (barLengthMode == LENGTH_MODE_NORMAL) {
            -1
        } else if (barLengthMode == LENGTH_MODE_MEDIUM) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_home_handle_width_medium)
        } else if (barLengthMode == LENGTH_MODE_LONG) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_home_handle_width_long)
        } else {
            0
        }
    }

    fun getNavbarRadius(): Float {
        return (if (barRadiusMode == RADIUS_MODE_NORMAL) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_handle_radius)
        } else if (barRadiusMode == RADIUS_MODE_MEDIUM) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_handle_radius_medium)
        } else if (barRadiusMode == RADIUS_MODE_THICKEST) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_handle_radius_thickest)
        } else {
            context.resources.getDimensionPixelSize(R.dimen.navigation_handle_radius)
        }).toFloat()
    }

    fun getNavbarHeight(): Int {
        return if (barLengthMode == LENGTH_MODE_HIDDEN) {
            0
        } else if (immersive) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_bar_height_immersive)
        } else {
            -1
        }
    }

    fun getNavbarFrameHeight(): Int {
        return if (!imeSpaceVisible) {
            context.resources.getDimensionPixelSize(R.dimen.navigation_bar_frame_height_hide_ime)
        } else {
            -1
        }
    }

    fun isInverseLayoutEnabled(): Boolean {
        return inverseLayout
    }

    fun addCallback(callback: NavbarAppearanceChangeCallback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: NavbarAppearanceChangeCallback) {
        callbacks.remove(callback)
    }

    companion object {
        private const val LENGTH_MODE_HIDDEN = 0
        private const val LENGTH_MODE_NORMAL = 1
        private const val LENGTH_MODE_MEDIUM = 2
        private const val LENGTH_MODE_LONG = 3

        private const val RADIUS_MODE_NORMAL = 0
        private const val RADIUS_MODE_MEDIUM = 1
        private const val RADIUS_MODE_THICKEST = 2
    }
}

interface NavbarAppearanceChangeCallback {
    fun onNavbarAppearanceChanged(
        length: Int,
        radius: Float,
        height: Int,
        frameHeight: Int,
        inverseLayout: Boolean
    )
}
