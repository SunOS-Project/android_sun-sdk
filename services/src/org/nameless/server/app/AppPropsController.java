/*
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.app;

import static org.nameless.content.ContextExt.APP_PROPS_MANAGER_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_APP_PROPS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.os.Build;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.R;
import com.android.internal.messages.SystemMessageExt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Map;

import org.nameless.app.AppPropsManager;
import org.nameless.app.IAppPropsManagerService;
import org.nameless.content.IOnlineConfigurable;
import org.nameless.content.OnlineConfigManager;
import org.nameless.server.NamelessSystemExService;

import org.xmlpull.v1.XmlPullParser;

public class AppPropsController extends IOnlineConfigurable.Stub {

    private static final String TAG = "AppPropsController";

    private static final int VERSION = 2;

    private static class InstanceHolder {
        private static AppPropsController INSTANCE = new AppPropsController();
    }

    public static AppPropsController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final String SYSTEM_CONFIG_FILE = "/system_ext/etc/custom_props_config.xml";
    private static final String LOCAL_CONFIG_FILE = "/data/nameless_configs/custom_props_config.xml";

    private static final String KEY_DEFAULT = "Default";
    private static final String KEY_GENERIC = "Generic";
    private static final String KEY_GMS = "GMS";

    // Key: model (OnePlus 69)             Val: prop keys, prop vals (BRAND, ONEPLUS)
    private final ArrayMap<String, ArrayMap<String, String>> mPropsToChange = new ArrayMap<>();
    // Key: packageName (com.google.gms)   Val: prop keys (FINGERPRINT)
    private final ArrayMap<String, ArraySet<String>> mPropsToKeep = new ArrayMap<>();
    // Key: model (OnePlus 69)             Val: packageName (com.google.gms, ...)
    private final ArrayMap<String, ArraySet<String>> mPackagesToChange = new ArrayMap<>();
    private final ArrayMap<String, ArraySet<String>> mGamePackagesToChange = new ArrayMap<>();
    // Packages in this set will only get generic props spoofed.
    private final ArraySet<String> mPackagesToKeep = new ArraySet<>();
    // Packages in this set will be considered to be spoofed.
    private final ArraySet<String> mExtraPackagesToChange = new ArraySet<>();
    // Packages in this set will be considered as Google Camera package.
    private final ArraySet<String> mCustomGoogleCameraPackages = new ArraySet<>();

    private final Object mLock = new Object();

    private final class AppPropsManagerService extends IAppPropsManagerService.Stub {
        @Override
        public Map<String, String> getAppSpoofMap(ComponentName componentName) {
            synchronized (mLock) {
                logD("getAppSpoofMap, component=" + componentName);
                return getAppSpoofMapLocked(componentName);
            }
        }
    }

    private NamelessSystemExService mSystemExService;

    private NotificationManager mNotificationManager;

    private boolean mInitialized = false;

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public String getOnlineConfigUri() {
        return SystemProperties.get("persist.sys.nameless.uri.props");
    }

    @Override
    public String getSystemConfigPath() {
        return SYSTEM_CONFIG_FILE;
    }

    @Override
    public String getLocalConfigPath() {
        return LOCAL_CONFIG_FILE;
    }

    @Override
    public void onConfigUpdated() {
        synchronized (mLock) {
            mInitialized = false;
            // We can use local config here safely because this is called after verification.
            initConfigLocked(LOCAL_CONFIG_FILE);
            mInitialized = true;
        }
        postConfigUpdatedNotification();
    }

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
        service.publishBinderService(APP_PROPS_MANAGER_SERVICE, new AppPropsManagerService());
    }

    public void onSystemServicesReady() {
        synchronized (mLock) {
            initConfigLocked(compareConfigTimestamp());
            mInitialized = true;
        }
        mSystemExService.getContext().getSystemService(OnlineConfigManager.class).registerOnlineConfigurable(this);

        mNotificationManager = mSystemExService.getContext().getSystemService(NotificationManager.class);
        final NotificationChannel channel = new NotificationChannel(TAG, TAG,
                NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(channel);
    }

    private Pair<Integer, Long> getConfigInfo(String path) {
        try {
            FileReader fr = new FileReader(new File(path));
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fr);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                   if ("info".equals(parser.getName())) {
                        final String versionStr = parser.getAttributeValue(null, "version");
                        final String timestampStr = parser.getAttributeValue(null, "timestamp");
                        return new Pair<>(Integer.parseInt(versionStr), Long.parseLong(timestampStr));
                    }
                }
                event = parser.next();
            }
            fr.close();
        } catch (Exception e) {
            if (!(e instanceof FileNotFoundException)) {
                Slog.e(TAG, "exception on get config info", e);
            }
        }
        return new Pair<>(Integer.MAX_VALUE, -1L);
    }

    private String compareConfigTimestamp() {
        final Pair<Integer, Long> systemConfigInfo = getConfigInfo(SYSTEM_CONFIG_FILE);
        final Pair<Integer, Long> localConfigInfo = getConfigInfo(LOCAL_CONFIG_FILE);

        // Online config requires higher framework version. Fallback to system config.
        if (localConfigInfo.first > VERSION) {
            return SYSTEM_CONFIG_FILE;
        }

        return localConfigInfo.second > systemConfigInfo.second ? LOCAL_CONFIG_FILE : SYSTEM_CONFIG_FILE;
    }

    private void initConfigLocked(String path) {
        mPropsToChange.clear();
        mPropsToKeep.clear();
        mPackagesToChange.clear();
        mGamePackagesToChange.clear();
        mPackagesToKeep.clear();
        mExtraPackagesToChange.clear();
        mCustomGoogleCameraPackages.clear();

        String name;
        String packageName;
        String packagesStr;
        String propsStr;
        String[] packages;
        String[] props;
        boolean isGame;
        try {
            FileReader fr = new FileReader(new File(path));
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
                                    mPropsToKeep.put(packageName, new ArraySet<>());
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
                                        mGamePackagesToChange.put(name, new ArraySet<>());
                                    }
                                    mGamePackagesToChange.get(name).add(trimedPkg);
                                } else {
                                    if (!mPackagesToChange.containsKey(name)) {
                                        mPackagesToChange.put(name, new ArraySet<>());
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

    private Map<String, String> getAppSpoofMapLocked(ComponentName component) {
        if (!mInitialized) {
            return null;
        }

        final String packageName = component.getPackageName();

        Map<String, String> spoofMap = new ArrayMap<>();

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

        if (AppPropsManager.isComponentGms(component)) {
            Map<String, String> gmsMap = mPropsToChange.getOrDefault(KEY_GMS, null);
            if (gmsMap != null) {
                spoofMap.putAll(gmsMap);
            }
        } else if (packageName.startsWith("com.google.") ||
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

    private void postConfigUpdatedNotification() {
        Notification.Builder builder = new Notification.Builder(mSystemExService.getContext(), TAG);
        builder.setContentTitle(
                mSystemExService.getContext().getString(R.string.app_prop_config_updated_title));
        builder.setContentText(
                mSystemExService.getContext().getString(R.string.app_prop_config_updated_content));
        builder.setSmallIcon(R.drawable.ic_update);
        builder.addAction(R.drawable.ic_update,
                mSystemExService.getContext().getString(R.string.reboot_now),
                mSystemExService.getRebootPendingIntent());

        mNotificationManager.notify(SystemMessageExt.NOTE_APP_PROPS_CONFIG_UPATED, builder.build());
    }

    private static void logD(String msg) {
        if (DEBUG_APP_PROPS) {
            Slog.d(TAG, msg);
        }
    }
}
