/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server;

import android.content.Context;

import com.android.server.SystemService;

public class NamelessSystemExService extends SystemService {

    private static final String TAG = "NamelessSystemExService";

    public NamelessSystemExService(Context context) {
        super(context);
    }
}
