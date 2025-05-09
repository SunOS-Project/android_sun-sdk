/*
 * Copyright (C) 2022 FlamingoOS Project
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
 * limitations under the License
 */

package org.sun.systemui.statusbar.phone

import android.content.Context
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.UserHandle
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.animation.Animation

import androidx.annotation.GuardedBy

import com.android.settingslib.Utils

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.doze.DozeLog
import com.android.systemui.keyguard.ScreenLifecycle
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.NotificationListener
import com.android.systemui.statusbar.StatusBarState
import com.android.systemui.statusbar.SysuiStatusBarStateController
import com.android.systemui.statusbar.phone.DozeParameters
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.android.systemui.util.settings.SystemSettings

import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import org.sun.provider.SettingsExt
import org.sun.systemui.statusbar.EdgeLightView

@SysUISingleton
class EdgeLightViewController @Inject constructor(
    @Main private val mainHandler: Handler,
    private val context: Context,
    private val keyguardStateController: KeyguardStateController,
    private val systemSettings: SystemSettings,
    private val sysuiStatusBarStateController: SysuiStatusBarStateController,
    private val userTracker: UserTracker,
    screenLifecycle: ScreenLifecycle,
    dozeParameters: DozeParameters,
    configurationController: ConfigurationController,
) : ScreenLifecycle.Observer,
    NotificationListener.NotificationHandler,
    ConfigurationController.ConfigurationListener,
    KeyguardStateController.Callback,
    UserTracker.Callback {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val animationDuration =
        (dozeParameters.pulseVisibleDuration / 3).toLong() - COLLAPSE_ANIMATION_DURATION

    private var screenOn = false
    private var edgeLightView: EdgeLightView? = null
    private var pulsing = false

    private val settingsMutex = Mutex()

    @GuardedBy("settingsMutex")
    private var edgeLightEnabled = false

    @GuardedBy("settingsMutex")
    private var colorMode = ColorMode.THEME

    // Whether to always trigger edge light on pulse even if it
    // is not because notification was posted. For example: tap to wake
    // for ambient display.
    @GuardedBy("settingsMutex")
    private var alwaysTriggerOnPulse = false

    private val settingsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            logD {
                "setting changed for ${uri?.lastPathSegment}"
            }
            coroutineScope.launch {
                settingsMutex.withLock {
                    when (uri?.lastPathSegment) {
                        SettingsExt.System.EDGE_LIGHT_ENABLED ->
                            edgeLightEnabled = isEdgeLightEnabled()
                        SettingsExt.System.EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE ->
                            alwaysTriggerOnPulse = alwaysTriggerOnPulse()
                        SettingsExt.System.EDGE_LIGHT_REPEAT_ANIMATION ->
                            edgeLightView?.setRepeatCount(getRepeatCount())
                        SettingsExt.System.EDGE_LIGHT_COLOR_MODE -> {
                            colorMode = getColorMode()
                            edgeLightView?.setColor(getColorForMode(colorMode))
                        }
                        SettingsExt.System.EDGE_LIGHT_CUSTOM_COLOR ->
                            edgeLightView?.setColor(getCustomColor())
                    }
                    return@withLock
                }
            }
        }
    }

    init {
        coroutineScope.launch {
            loadSettings()
            register(
                SettingsExt.System.EDGE_LIGHT_ENABLED,
                SettingsExt.System.EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE,
                SettingsExt.System.EDGE_LIGHT_REPEAT_ANIMATION,
                SettingsExt.System.EDGE_LIGHT_COLOR_MODE,
                SettingsExt.System.EDGE_LIGHT_CUSTOM_COLOR,
            )
        }
        screenLifecycle.addObserver(this)
        configurationController.addCallback(this)
        keyguardStateController.addCallback(this)
        userTracker.addCallback(this) {
            coroutineScope.launch {
                it.run()
            }
        }
    }

    private fun register(vararg keys: String) {
        keys.forEach {
            systemSettings.registerContentObserverForUserSync(
                it,
                settingsObserver,
                UserHandle.USER_ALL
            )
        }
    }

    private fun loadSettings() {
        coroutineScope.launch {
            settingsMutex.withLock {
                edgeLightEnabled = isEdgeLightEnabled()
                alwaysTriggerOnPulse = alwaysTriggerOnPulse()
                edgeLightView?.let {
                    it.setRepeatCount(getRepeatCount())
                    it.setColor(getColorForMode(getColorMode()))
                }
            }
        }
    }

    private fun canPulse(): Boolean {
        return sysuiStatusBarStateController.isDozing() &&
                sysuiStatusBarStateController.getState() == StatusBarState.KEYGUARD &&
                !keyguardStateController.isUnlocked
    }

    private suspend fun isEdgeLightEnabled(): Boolean {
        return withContext(Dispatchers.IO) {
            systemSettings.getIntForUser(SettingsExt.System.EDGE_LIGHT_ENABLED, 0, userTracker.userId) == 1
        }
    }

    private suspend fun alwaysTriggerOnPulse(): Boolean {
        return withContext(Dispatchers.IO) {
            systemSettings.getIntForUser(
                SettingsExt.System.EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE,
                0,
                userTracker.userId
            ) == 1
        }
    }

    private suspend fun getRepeatCount(): Int {
        val repeat = withContext(Dispatchers.IO) {
            systemSettings.getIntForUser(
                SettingsExt.System.EDGE_LIGHT_REPEAT_ANIMATION,
                0,
                userTracker.userId
            ) == 1
        }
        return if (repeat) Animation.INFINITE else 0
    }

    private suspend fun getColorMode(): ColorMode {
        val colorModeInt = withContext(Dispatchers.IO) {
            systemSettings.getIntForUser(
                SettingsExt.System.EDGE_LIGHT_COLOR_MODE,
                0,
                userTracker.userId
            )
        }
        return ColorMode.values().find { it.ordinal == colorModeInt } ?: ColorMode.THEME
    }

    private suspend fun getCustomColor(): Int {
        val colorString = withContext(Dispatchers.IO) {
            systemSettings.getStringForUser(
                SettingsExt.System.EDGE_LIGHT_CUSTOM_COLOR,
                userTracker.userId
            )
        } ?: return Color.WHITE
        return try {
            Color.parseColor(colorString)
        } catch (_: IllegalArgumentException) {
            Log.e(TAG, "Custom color $colorString is invalid")
            Color.WHITE
        }
    }

    private suspend fun getThemeColor(): Int {
        return Utils.getColorAttrDefaultColor(context,
                com.android.internal.R.attr.colorAccentPrimary)
    }

    // Theme color is returned for notification color mode
    // as well since the color is set when notification is posted.
    private suspend fun getColorForMode(mode: ColorMode): Int =
        when (mode) {
            ColorMode.CUSTOM -> getCustomColor()
            else -> getThemeColor()
        }

    override fun onScreenTurnedOn() {
        logD {
            "onScreenTurnedOn"
        }
        screenOn = true
        if (pulsing) {
            logD {
                "onScreenTurnedOn: pulsing: show()"
            }
            show()
        }
    }

    override fun onScreenTurnedOff() {
        logD {
            "onScreenTurnedOff"
        }
        screenOn = false
    }

    override fun onNotificationsInitialized() {
        // No-op
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        logD {
            "onNotificationPosted, sbn = $sbn"
        }
        coroutineScope.launch {
            settingsMutex.withLock {
                if (colorMode == ColorMode.NOTIFICATION) {
                    edgeLightView?.setColor(sbn.notification.color)
                }
                if (screenOn && pulsing) show()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {
        // No-op
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        onNotificationRemoved(sbn, rankingMap)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        // No-op
    }

    override fun onUiModeChanged() {
        // Reload theme color
        coroutineScope.launch {
            settingsMutex.withLock {
                edgeLightView?.setColor(getColorForMode(colorMode))
            }
        }
    }

    override fun onKeyguardGoingAwayChanged() {
        if (!canPulse()) {
            hide()
        }
    }

    override fun onKeyguardFadingAwayChanged() {
        if (!canPulse()) {
            hide()
        }
    }

    override fun onUserChanged(newUser: Int, userContext: Context) {
        loadSettings()
    }

    fun attach(notificationListener: NotificationListener) {
        logD {
            "attach"
        }
        notificationListener.addNotificationHandler(this)
    }

    fun updateColor() {
        coroutineScope.launch {
            settingsMutex.withLock {
                if (colorMode == ColorMode.THEME) {
                    edgeLightView?.setColor(getColorForMode(colorMode))
                }
            }
        }
    }

    fun setEdgeLightView(edgeLightView: EdgeLightView) {
        this.edgeLightView = edgeLightView.apply {
            setExpandAnimationDuration(animationDuration)
            setCollapseAnimationDuration(COLLAPSE_ANIMATION_DURATION)
        }.also {
            coroutineScope.launch {
                settingsMutex.withLock {
                    it.setRepeatCount(getRepeatCount())
                    it.setColor(getColorForMode(colorMode))
                }
            }
        }
    }

    fun setPulsing(pulsing: Boolean, reason: Int) {
        coroutineScope.launch {
            settingsMutex.withLock {
                if (pulsing && (alwaysTriggerOnPulse ||
                        reason == DozeLog.PULSE_REASON_NOTIFICATION)) {
                    this@EdgeLightViewController.pulsing = true
                    // Use theme color if color mode is set to notification color
                    // and pulse is not because of notification.
                    if (colorMode == ColorMode.NOTIFICATION && reason != DozeLog.PULSE_REASON_NOTIFICATION) {
                        edgeLightView?.setColor(getThemeColor())
                    }
                    if (screenOn) {
                        logD {
                            "setPulsing: screenOn: show()"
                        }
                        show()
                    }
                } else {
                    this@EdgeLightViewController.pulsing = false
                    hide()
                }
            }
        }
    }

    private fun show() {
        if (canPulse()) {
            coroutineScope.launch {
                settingsMutex.withLock {
                    if (edgeLightEnabled) edgeLightView?.show()
                }
            }
        }
    }

    private fun hide() {
        edgeLightView?.hide()
    }

    companion object {
        private const val COLLAPSE_ANIMATION_DURATION = 700L

        private val TAG = EdgeLightViewController::class.simpleName

        private inline fun logD(crossinline msg: () -> String) {
            if (Log.isLoggable(TAG, Log.DEBUG))  {
                Log.d(TAG, msg())
            }
        }
    }
}

private enum class ColorMode {
    THEME,
    NOTIFICATION,
    CUSTOM
}
