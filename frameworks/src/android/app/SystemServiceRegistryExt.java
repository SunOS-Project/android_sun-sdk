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

import org.sun.app.AppPropsManager;
import org.sun.app.IAppPropsManagerService;
import org.sun.app.GameModeManager;
import org.sun.app.IGameModeManagerService;
import org.sun.content.ContextExt;
import org.sun.content.IOnlineConfigManagerService;
import org.sun.content.OnlineConfigManager;
import org.sun.display.IRefreshRateManagerService;
import org.sun.display.RefreshRateManager;
import org.sun.hardware.ISensorBlockService;
import org.sun.hardware.SensorBlockManager;
import org.sun.os.IRotateManagerService;
import org.sun.os.RotateManager;
import org.sun.view.DisplayResolutionManager;
import org.sun.view.IDisplayResolutionManagerService;

/** @hide */
class SystemServiceRegistryExt {

    private SystemServiceRegistryExt() {}

    static void registerExtServices() {
        registerService(ContextExt.APP_PROPS_MANAGER_SERVICE, AppPropsManager.class,
                new CachedServiceFetcher<AppPropsManager>() {
            @Override
            public AppPropsManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.APP_PROPS_MANAGER_SERVICE);
                IAppPropsManagerService service = IAppPropsManagerService.Stub.asInterface(binder);
                return new AppPropsManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.DISPLAY_RESOLUTION_MANAGER_SERVICE, DisplayResolutionManager.class,
                new CachedServiceFetcher<DisplayResolutionManager>() {
            @Override
            public DisplayResolutionManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.DISPLAY_RESOLUTION_MANAGER_SERVICE);
                IDisplayResolutionManagerService service = IDisplayResolutionManagerService.Stub.asInterface(binder);
                return new DisplayResolutionManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.GAME_MODE_SERVICE, GameModeManager.class,
                new CachedServiceFetcher<GameModeManager>() {
            @Override
            public GameModeManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.GAME_MODE_SERVICE);
                IGameModeManagerService service = IGameModeManagerService.Stub.asInterface(binder);
                return new GameModeManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.LINEARMOTOR_VIBRATOR_SERVICE, LinearmotorVibrator.class,
                new CachedServiceFetcher<LinearmotorVibrator>() {
            @Override
            public LinearmotorVibrator createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.LINEARMOTOR_VIBRATOR_SERVICE);
                ILinearmotorVibratorService service = ILinearmotorVibratorService.Stub.asInterface(binder);
                return new LinearmotorVibrator(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.ONLINE_CONFIG_MANAGER_SERVICE, OnlineConfigManager.class,
                new CachedServiceFetcher<OnlineConfigManager>() {
            @Override
            public OnlineConfigManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.ONLINE_CONFIG_MANAGER_SERVICE);
                IOnlineConfigManagerService service = IOnlineConfigManagerService.Stub.asInterface(binder);
                return new OnlineConfigManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.REFRESH_RATE_MANAGER_SERVICE, RefreshRateManager.class,
                new CachedServiceFetcher<RefreshRateManager>() {
            @Override
            public RefreshRateManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.REFRESH_RATE_MANAGER_SERVICE);
                IRefreshRateManagerService service = IRefreshRateManagerService.Stub.asInterface(binder);
                return new RefreshRateManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.ROTATE_MANAGER_SERVICE, RotateManager.class,
                new CachedServiceFetcher<RotateManager>() {
            @Override
            public RotateManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.ROTATE_MANAGER_SERVICE);
                IRotateManagerService service = IRotateManagerService.Stub.asInterface(binder);
                return new RotateManager(ctx.getOuterContext(), service);
            }});

        registerService(ContextExt.SENSOR_BLOCK_MANAGER_SERVICE, SensorBlockManager.class,
                new CachedServiceFetcher<SensorBlockManager>() {
            @Override
            public SensorBlockManager createService(ContextImpl ctx) {
                IBinder binder = ServiceManager.getService(ContextExt.SENSOR_BLOCK_MANAGER_SERVICE);
                ISensorBlockService service = ISensorBlockService.Stub.asInterface(binder);
                return new SensorBlockManager(ctx.getOuterContext(), service);
            }});
    }
}
