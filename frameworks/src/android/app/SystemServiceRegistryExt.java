/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.app;

import static android.app.SystemServiceRegistry.registerService;

import android.app.SystemServiceRegistry.CachedServiceFetcher;
import android.os.IBinder;
import android.os.ServiceManager;

import com.oplus.os.ILinearmotorVibratorService;
import com.oplus.os.LinearmotorVibrator;

import org.nameless.content.ContextExt;
import org.nameless.display.IRefreshRateManagerService;
import org.nameless.display.RefreshRateManager;
import org.nameless.os.IPocketService;
import org.nameless.os.PocketManager;
import org.nameless.view.AppFocusManager;
import org.nameless.view.DisplayResolutionManager;
import org.nameless.view.IAppFocusManagerService;
import org.nameless.view.IDisplayResolutionManagerService;

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

        registerService(ContextExt.DISPLAY_RESOLUTION_MANAGER_SERVICE, DisplayResolutionManager.class,
                new CachedServiceFetcher<DisplayResolutionManager>() {
            @Override
            public DisplayResolutionManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.DISPLAY_RESOLUTION_MANAGER_SERVICE);
                IDisplayResolutionManagerService service = IDisplayResolutionManagerService.Stub.asInterface(binder);
                return new DisplayResolutionManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.LINEARMOTOR_VIBRATOR_SERVICE, LinearmotorVibrator.class,
                new CachedServiceFetcher<LinearmotorVibrator>() {
            @Override
            public LinearmotorVibrator createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.LINEARMOTOR_VIBRATOR_SERVICE);
                ILinearmotorVibratorService service = ILinearmotorVibratorService.Stub.asInterface(binder);
                return new LinearmotorVibrator(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.POCKET_SERVICE, PocketManager.class,
                new CachedServiceFetcher<PocketManager>() {
            @Override
            public PocketManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.POCKET_SERVICE);
                IPocketService service = IPocketService.Stub.asInterface(binder);
                return new PocketManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.REFRESH_RATE_MANAGER_SERVICE, RefreshRateManager.class,
                new CachedServiceFetcher<RefreshRateManager>() {
            @Override
            public RefreshRateManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.REFRESH_RATE_MANAGER_SERVICE);
                IRefreshRateManagerService service = IRefreshRateManagerService.Stub.asInterface(binder);
                return new RefreshRateManager(ctx.getOuterContext(), service);
            }});
    }
}
