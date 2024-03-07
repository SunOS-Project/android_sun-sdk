/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.policy

import android.content.Context

import javax.inject.Inject
import javax.inject.Singleton

import org.nameless.view.AppFocusManager
import org.nameless.view.IAppFocusObserver

@Singleton
class ForegroundActivityListener @Inject constructor(
    context: Context
) {

    private val appFocusManager = context.getSystemService(AppFocusManager::class.java)!!

    private var fullscreenPackageName = ""
    private var fullscreenActivityName = ""

    init {
        appFocusManager.registerAppFocusObserver(object : IAppFocusObserver.Stub() {
            override fun onFullscreenFocusChanged(packageName: String, activityName: String) {
                fullscreenPackageName = packageName
                fullscreenActivityName = packageName
            }
        }, true)
    }

    fun getTopPackageName() = fullscreenPackageName

    fun getTopActivityName() = fullscreenActivityName
}
