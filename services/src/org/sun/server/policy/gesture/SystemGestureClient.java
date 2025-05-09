/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.policy.gesture;

import android.os.IBinder;

import org.sun.view.ISystemGestureListener;

class SystemGestureClient {

    IBinder key;
    String pkg;
    int uid;
    int gesture;
    ISystemGestureListener listener;

    SystemGestureClient(IBinder key, String pkg, int uid, int gesture, ISystemGestureListener listener) {
        this.key = key;
        this.pkg = pkg;
        this.uid = uid;
        this.gesture = gesture;
        this.listener = listener;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof SystemGestureClient) && equals((SystemGestureClient) o);
    }

    private boolean equals(SystemGestureClient o) {
        return o != null && o.pkg.equals(pkg) && uid == o.uid && gesture == o.gesture && key == o.key;
    }

    @Override
    public String toString() {
        return "SystemGestureClient{key=" + key + ", pkg='" + pkg + "', gesture=" + gesture +
                ", uid=" + uid + ", listener=" + listener + '}';
    }
}
