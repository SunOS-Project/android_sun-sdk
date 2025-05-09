/*
 * Copyright (C) 2021 The Android Open Source Project
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

package org.sun.systemui.qs.tiles

import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings

import com.android.internal.logging.MetricsLogger

import com.android.systemui.animation.Expandable
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.plugins.qs.QSTile
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.qs.QSHost
import com.android.systemui.qs.QsEventLogger
import com.android.systemui.qs.logging.QSLogger
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.statusbar.policy.KeyguardStateController

import org.sun.provider.SettingsExt.Secure.QSTILE_REQUIRES_UNLOCKING

internal abstract class SecureQSTile<TState : QSTile.State> protected constructor(
    host: QSHost, uiEventLogger: QsEventLogger, backgroundLooper: Looper, mainHandler: Handler,
    falsingManager: FalsingManager, metricsLogger: MetricsLogger,
    statusBarStateController: StatusBarStateController, activityStarter: ActivityStarter,
    qsLogger: QSLogger,
    private val mKeyguard: KeyguardStateController,
) : QSTileImpl<TState>(
    host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
    metricsLogger, statusBarStateController, activityStarter, qsLogger,
) {
    abstract override fun newTileState(): TState

    protected abstract fun handleClick(expandable: Expandable?, keyguardShowing: Boolean)

    override fun handleClick(expandable: Expandable?) {
        val disableOnLockscreen = Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                QSTILE_REQUIRES_UNLOCKING, 1,
                UserHandle.USER_CURRENT) == 1
        handleClick(expandable, mKeyguard.isMethodSecure && mKeyguard.isShowing && disableOnLockscreen)
    }

    protected fun checkKeyguard(expandable: Expandable?, keyguardShowing: Boolean): Boolean {
        return if (keyguardShowing) {
            mActivityStarter.postQSRunnableDismissingKeyguard {
                handleClick(expandable, false)
            }
            true
        } else {
            false
        }
    }
}
