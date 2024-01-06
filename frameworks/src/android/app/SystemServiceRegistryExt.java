/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.app;

import static android.app.SystemServiceRegistry.registerService;

import android.app.SystemServiceRegistry.CachedServiceFetcher;
import android.os.IBinder;
import android.os.ServiceManager;

import org.nameless.content.ContextExt;
import org.nameless.view.AppFocusManager;
import org.nameless.view.IAppFocusManagerService;

/** @hide */
public class SystemServiceRegistryExt {

    private SystemServiceRegistryExt() {}

    public static void registerExtServices() {
        registerService(ContextExt.APP_FOCUS_MANAGER_SERVICE, AppFocusManager.class,
                new CachedServiceFetcher<AppFocusManager>() {
            @Override
            public AppFocusManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.APP_FOCUS_MANAGER_SERVICE);
                IAppFocusManagerService service = IAppFocusManagerService.Stub.asInterface(binder);
                return new AppFocusManager(ctx.getOuterContext(), service);
            }});
    }
}
