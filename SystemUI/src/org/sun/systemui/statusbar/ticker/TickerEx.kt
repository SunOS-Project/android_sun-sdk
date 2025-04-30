/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.systemui.statusbar.ticker

import android.app.Notification.FLAG_ONLY_ALERT_ONCE
import android.util.Log
import androidx.collection.LruCache
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import org.sun.os.DebugConstants.DEBUG_TICKER

object TickerEx {

    private const val TAG = "Ticker::TickerEx"

    private val tickLruMap = object : LruCache<String, CharSequence>(1024) {
        override fun sizeOf(key: String, value: CharSequence): Int {
            return value.length
        }
    }

    @JvmStatic
    fun tickFilter(entry: NotificationEntry, isFirstTickThisNotification: Boolean, tickTask: Runnable) {
        val notification = entry.sbn.notification ?: return
        val tickerText = notification.tickerText ?: return
        if (!isFirstTickThisNotification) {
            val alertOnce = (notification.flags and FLAG_ONLY_ALERT_ONCE) != 0
            if (alertOnce) {
                return
            }
        }
        val keyInLruMap = entry.sbn.key
        if (tickerText == tickLruMap.put(keyInLruMap, tickerText)) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "Skip ticker because of duplicate content, content=${tickerText}")
            }
        } else {
            tickTask.run()
        }
    }

    @JvmStatic
    fun removeTickFilter(entry: NotificationEntry) {
        tickLruMap.remove(entry.sbn.key)
    }
}
