/*
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.app;

import static org.nameless.content.ContextExt.APP_PROPS_MANAGER_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_APP_PROPS;

import android.os.Build;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.nameless.app.IAppPropsManagerService;
import org.nameless.server.NamelessSystemExService;

import org.xmlpull.v1.XmlPullParser;

public class AppPropsController {

    private static final String TAG = "AppPropsController";

    private static class InstanceHolder {
        private static AppPropsController INSTANCE = new AppPropsController();
    }

    public static AppPropsController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final String SYSTEM_CONFIG_FILE = "/system_ext/etc/custom_props_config.xml";

    private static final String KEY_DEFAULT = "Default";
    private static final String KEY_GENERIC = "Generic";
    private static final String KEY_GMS = "GMS";

    // Key: model (OnePlus 69)             Val: prop keys, prop vals (BRAND, ONEPLUS)
    private final HashMap<String, ArrayMap<String, String>> mPropsToChange = new HashMap<>();
    // Key: packageName (com.google.gms)   Val: prop keys (FINGERPRINT)
    private final HashMap<String, HashSet<String>> mPropsToKeep = new HashMap<>();
    // Key: model (OnePlus 69)             Val: packageName (com.google.gms, ...)
    private final HashMap<String, HashSet<String>> mPackagesToChange = new HashMap<>();
    private final HashMap<String, HashSet<String>> mGamePackagesToChange = new HashMap<>();
    // Packages in this set will only get generic props spoofed.
    private final HashSet<String> mPackagesToKeep = new HashSet<>();
    // Packages in this set will be considered to be spoofed.
    private final HashSet<String> mExtraPackagesToChange = new HashSet<>();
    // Packages in this set will be considered as Google Camera package.
    private final HashSet<String> mCustomGoogleCameraPackages = new HashSet<>();

    private final Object mLock = new Object();

    private final class AppPropsManagerService extends IAppPropsManagerService.Stub {
        @Override
        public Map<String, String> getAppSpoofMap(String packageName, String processName, boolean isGms) {
            synchronized (mLock) {
                logD("getAppSpoofMap, packageName=" + packageName + ", processName=" + processName + ", isGms=" + isGms);
                return getAppSpoofMapLocked(packageName, processName, isGms);
            }
        }
    }

    private boolean mInitialized = false;

    public void initSystemExService(NamelessSystemExService service) {
        service.publishBinderService(APP_PROPS_MANAGER_SERVICE, new AppPropsManagerService());
    }

    public void onSystemServicesReady() {
        initConfig();
        mInitialized = true;
    }

    private void initConfig() {
        synchronized (mLock) {
            String name;
            String packageName;
            String packagesStr;
            String propsStr;
            String[] packages;
            String[] props;
            boolean isGame;
            try {
                FileReader fr = new FileReader(new File(SYSTEM_CONFIG_FILE));
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fr);
                int event = parser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        switch (parser.getName()) {
                            case "propsToChange":
                                name = parser.getAttributeValue(null, "name");
                                propsStr = parser.getAttributeValue(null, "props");
                                props = propsStr.split(";");
                                for (String prop : props) {
                                    final int colonIdx = prop.indexOf(":");
                                    final String key = prop.substring(0, colonIdx).trim();
                                    final String val = prop.substring(colonIdx + 1).trim();
                                    if (!mPropsToChange.containsKey(name)) {
                                        mPropsToChange.put(name, new ArrayMap<>());
                                    }
                                    mPropsToChange.get(name).put(key, val);
                                    logD("Added propsToChange, model=" + name + ", key=" + key + ", val=" + val);
                                }
                                break;
                            case "propsToKeep":
                                packageName = parser.getAttributeValue(null, "package");
                                propsStr = parser.getAttributeValue(null, "props");
                                props = propsStr.split(";");
                                for (String prop : props) {
                                    final String trimedProp = prop.trim();
                                    if (!mPropsToKeep.containsKey(packageName)) {
                                        mPropsToKeep.put(packageName, new HashSet<>());
                                    }
                                    mPropsToKeep.get(packageName).add(trimedProp);
                                    logD("Added propsToKeep, packageName=" + packageName + ", prop=" + trimedProp);
                                }
                                break;
                            case "packagesToChange":
                                name = parser.getAttributeValue(null, "name");
                                isGame = "true".equals(parser.getAttributeValue(null, "game"));
                                packagesStr = parser.getAttributeValue(null, "packages");
                                packages = packagesStr.split(";");
                                for (String pkg : packages) {
                                    final String trimedPkg = pkg.trim();
                                    if (isGame) {
                                        if (!mGamePackagesToChange.containsKey(name)) {
                                            mGamePackagesToChange.put(name, new HashSet<>());
                                        }
                                        mGamePackagesToChange.get(name).add(trimedPkg);
                                    } else {
                                        if (!mPackagesToChange.containsKey(name)) {
                                            mPackagesToChange.put(name, new HashSet<>());
                                        }
                                        mPackagesToChange.get(name).add(trimedPkg);
                                    }
                                    logD("Added packagesToChange, model=" + name + ", isGame=" + isGame + ", packageName=" + trimedPkg);
                                }
                                break;
                            case "packagesToKeep":
                                packagesStr = parser.getAttributeValue(null, "packages");
                                packages = packagesStr.split(";");
                                for (String pkg : packages) {
                                    final String trimedPkg = pkg.trim();
                                    mPackagesToKeep.add(trimedPkg);
                                    logD("Added packagesToKeep, packageName=" + trimedPkg);
                                }
                                break;
                            case "extraPackagesToChange":
                                packagesStr = parser.getAttributeValue(null, "packages");
                                packages = packagesStr.split(";");
                                for (String pkg : packages) {
                                    final String trimedPkg = pkg.trim();
                                    mExtraPackagesToChange.add(trimedPkg);
                                    logD("Added extraPackagesToChange, packageName=" + trimedPkg);
                                }
                                break;
                            case "customGoogleCamera":
                                packagesStr = parser.getAttributeValue(null, "packages");
                                packages = packagesStr.split(";");
                                for (String pkg : packages) {
                                    final String trimedPkg = pkg.trim();
                                    mCustomGoogleCameraPackages.add(trimedPkg);
                                    logD("Added customGoogleCamera, packageName=" + trimedPkg);
                                }
                                break;
                        }
                    }
                    event = parser.next();
                }
                fr.close();
            } catch (Exception e) {
                Slog.e(TAG, "exception on initConfig", e);
            }
        }
    }

    private Map<String, String> getAppSpoofMapLocked(String packageName, String processName, boolean isGms) {
        Map<String, String> spoofMap = new ArrayMap<>();

        if (!mInitialized) {
            return spoofMap;
        }

        Map<String, String> genericMap = mPropsToChange.getOrDefault(KEY_GENERIC, null);
        if (genericMap != null) {
            spoofMap.putAll(genericMap);
        }

        if (mPackagesToKeep.contains(packageName)) {
            return spoofMap;
        }

        if (packageName.startsWith("com.google.android.GoogleCamera") ||
                mCustomGoogleCameraPackages.contains(packageName)) {
            return spoofMap;
        }

        if ("com.google.android.gms".equals(packageName)) {
            spoofMap.put("[PROP_LONG]TIME", String.valueOf(System.currentTimeMillis()));
        }

        if (isGms) {
            Map<String, String> gmsMap = mPropsToChange.getOrDefault(KEY_GMS, null);
            if (gmsMap != null) {
                spoofMap.putAll(gmsMap);
            }
        } else if (packageName.startsWith("com.google.") ||
                packageName.startsWith("com.samsung.") ||
                mExtraPackagesToChange.contains(packageName)) {
            String spoofModel = KEY_DEFAULT;
            for (String model : mPackagesToChange.keySet()) {
                if (mPackagesToChange.get(model).contains(packageName)) {
                    spoofModel = model;
                    break;
                }
            }
            spoofMap.putAll(mPropsToChange.get(spoofModel));
        } else {
            String spoofModel = null;
            for (String model : mGamePackagesToChange.keySet()) {
                if (mGamePackagesToChange.get(model).contains(packageName)) {
                    spoofModel = model;
                    break;
                }
            }
            if (spoofModel != null) {
                spoofMap.putAll(mPropsToChange.get(spoofModel));
            }
        }

        if (mPropsToKeep.containsKey(packageName)) {
            for (String key : mPropsToKeep.get(packageName)) {
                spoofMap.remove(key);
            }
        }

        if ("com.google.android.settings.intelligence".equals(packageName)) {
            spoofMap.put("FINGERPRINT", Build.VERSION.INCREMENTAL);
        }

        return spoofMap;
    }

    private static void logD(String msg) {
        if (DEBUG_APP_PROPS) {
            Slog.d(TAG, msg);
        }
    }
}
