/*
 * Copyright (C) 2022 crDroid Android Project
 * Copyright (C) 2024 Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nameless.settings.display.theme;

import static android.os.UserHandle.USER_SYSTEM;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.PathParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class ThemeUtils {

    private static final String TAG = "ThemeUtils";

    static final String FONT_KEY = "android.theme.customization.font";
    static final String ICON_SHAPE_KEY = "android.theme.customization.adaptive_icon_shape";

    private static final Comparator<OverlayInfo> OVERLAY_INFO_COMPARATOR =
            Comparator.comparingInt(a -> a.priority);

    private final Context mContext;
    private final IOverlayManager mOverlayManager;
    private final PackageManager mPackageManager;

    ThemeUtils(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));   
    }

    void setOverlayEnabled(String category, String packageName) {
        final String currentPackageName = getOverlayInfos(category, "android").stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse(null);

        try {
            if ("android".equals(packageName)) {
                mOverlayManager.setEnabled(currentPackageName, false, USER_SYSTEM);
            } else {
                mOverlayManager.setEnabledExclusiveInCategory(packageName,
                        USER_SYSTEM);
            }
            writeSettings(category, packageName, "android".equals(packageName));
        } catch (RemoteException e) {}
    }

    private void writeSettings(String category, String packageName, boolean disable) {
        final String overlayPackageJson = Settings.Secure.getStringForUser(
                mContext.getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, USER_SYSTEM);
        JSONObject object;
        try {
            if (overlayPackageJson == null) {
                object = new JSONObject();
            } else {
                object = new JSONObject(overlayPackageJson);
            }
            if (disable) {
                if (object.has(category)) object.remove(category);
            } else {
                object.put(category, packageName);
            }
            Settings.Secure.putStringForUser(mContext.getContentResolver(),
                    Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                    object.toString(), USER_SYSTEM);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e);
        }
    }

    List<String> getOverlayPackagesForCategory(String category, String target) {
        final List<String> overlays = new ArrayList<>();
        overlays.add("android");
        for (OverlayInfo info : getOverlayInfos(category, target)) {
            if (category.equals(info.getCategory())) {
                overlays.add(info.getPackageName());
            }
        }
        return overlays;
    }

    List<OverlayInfo> getOverlayInfos(String category, String target) {
        final List<OverlayInfo> filteredInfos = new ArrayList<>();
        try {
            final List<OverlayInfo> overlayInfos = mOverlayManager
                    .getOverlayInfosForTarget(target, USER_SYSTEM);
            for (OverlayInfo overlayInfo : overlayInfos) {
                if (category.equals(overlayInfo.category)) {
                    filteredInfos.add(overlayInfo);
                }
            }
        } catch (RemoteException e) {}
        filteredInfos.sort(OVERLAY_INFO_COMPARATOR);
        return filteredInfos;
    }

    ShapeDrawable createShapeDrawable(String overlayPackage) {
        Resources res = null;
        try {
            if (overlayPackage.equals("android")) {
                res = Resources.getSystem();
            } else {
                if (overlayPackage.equals("default")) {
                    overlayPackage = "android";
                }
                res = mPackageManager.getResourcesForApplication(overlayPackage);
            }
        } catch (NameNotFoundException | NotFoundException e) {}
        if (res == null) {
            return null;
        }
        final String shape = res.getString(
                res.getIdentifier("config_icon_mask",
                "string", overlayPackage));
        final Path path = TextUtils.isEmpty(shape) ? null : PathParser.createPathFromPathData(shape);
        final PathShape pathShape = new PathShape(path, 100f, 100f);
        final ShapeDrawable shapeDrawable = new ShapeDrawable(pathShape);
        final int mThumbSize = (int) (mContext.getResources().getDisplayMetrics().density * 72);
        shapeDrawable.setIntrinsicHeight(mThumbSize);
        shapeDrawable.setIntrinsicWidth(mThumbSize);
        return shapeDrawable;
    }
}
