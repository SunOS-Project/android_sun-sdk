/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.app;

import static org.sun.os.DebugConstants.DEBUG_APP_PROPS;

import android.annotation.SystemService;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.RemoteException;
import android.util.Slog;

import com.android.internal.util.sun.KeyProviderManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import org.sun.content.ContextExt;

/** @hide */
@SystemService(ContextExt.APP_PROPS_MANAGER_SERVICE)
public class AppPropsManager {

    private static final String TAG = "AppPropsManager";

    private final IAppPropsManagerService mService;

    private static volatile boolean sIsFinsky = false;

    public AppPropsManager(Context context, IAppPropsManagerService service) {
        mService = service;
    }

    /** @hide */
    public Map<String, String> getAppSpoofMap(ComponentName componentName) {
        if (mService == null) {
            return null;
        }
        try {
            return mService.getAppSpoofMap(componentName);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static void setProps(Context context) {
        final AppPropsManager manager = context.getSystemService(AppPropsManager.class);
        if (manager == null) {
            return;
        }
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();
        if (packageName == null || processName == null) {
            return;
        }
        final ComponentName component = new ComponentName(packageName, processName);
        sIsFinsky = packageName.equals("com.android.vending");
        final Map<String, String> spoofMap = manager.getAppSpoofMap(component);
        if (spoofMap == null) {
            return;
        }
        for (String key : spoofMap.keySet()) {
            if (key.startsWith("[VERSION_STRING]")) {
                setVersionFieldString(key.substring(16), spoofMap.get(key));
            } else if (key.startsWith("[VERSION_INT]")) {
                setVersionFieldInt(key.substring(13), Integer.parseInt(spoofMap.get(key)));
            } else if (key.startsWith("[PROP_LONG]")) {
                setPropValue(key.substring(11), Long.parseLong(spoofMap.get(key)));
            } else {
                setPropValue(key, spoofMap.get(key));
            }
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            logD("Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Slog.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionFieldString(String key, String value) {
        try {
            logD("Defining version " + key + " to " + value);
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Slog.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionFieldInt(String key, int value) {
        try {
            logD("Defining version " + key + " to " + value);
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Slog.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().toLowerCase()
                    .contains("droidguard"));
    }

    public static boolean isComponentGms(ComponentName component) {
        final String packageName = component.getPackageName();
        final String processNameLowerCase = component.getClassName().toLowerCase();
        return "com.google.android.gms".equals(packageName) &&
                (processNameLowerCase.contains("unstable") ||
                processNameLowerCase.contains("pixelmigrate") ||
                processNameLowerCase.contains("instrumentation"));
    }

    public static void onEngineGetCertificateChain() {

        // If a keybox is found, don't block key attestation
        if (KeyProviderManager.isKeyboxAvailable()) {
            logD("Key attestation blocking is disabled because a keybox is defined to spoof");
            return;
        }

        // Check stack for SafetyNet
        if (isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }

        // Check stack for PlayIntegrity
        if (sIsFinsky) {
            throw new UnsupportedOperationException();
        }
    }

    private static void logD(String msg) {
        if (DEBUG_APP_PROPS) {
            Slog.d(TAG, msg);
        }
    }
}
