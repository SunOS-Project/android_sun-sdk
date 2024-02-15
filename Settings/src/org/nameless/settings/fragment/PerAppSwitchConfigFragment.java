/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fragment;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

import org.nameless.custom.preference.SwitchPreference;
import org.nameless.settings.widget.AppSwitchPreference;

public abstract class PerAppSwitchConfigFragment extends BasePerAppConfigFragment {

    private final LinkedHashMap<String, Boolean> mPkgCheckState = new LinkedHashMap<>();

    @Override
    protected Preference createAppPreference(Context prefContext, AppData appData) {
        final SwitchPreference pref = new AppSwitchPreference(prefContext);
        pref.setIcon(getIcon(appData.packageName));
        pref.setTitle(appData.label);
        pref.setSummary(appData.packageName);

        final boolean checked = isChecked(appData.packageName, appData.uid);
        pref.setChecked(checked);
        mPkgCheckState.put(appData.packageName, checked);

        pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean ret = onSetChecked(appData.packageName, appData.uid, (Boolean) newValue);
                if (ret) {
                    mPkgCheckState.put(appData.packageName, (Boolean) newValue);
                    onCheckedListUpdated(generateCheckedPkgList());
                }
                return ret;
            }
        });

        return pref;
    }

    private List<String> generateCheckedPkgList() {
        final List<String> ret = new ArrayList<>();
        for (String pkg : mPkgCheckState.keySet()) {
            if (mPkgCheckState.get(pkg)) {
                ret.add(pkg);
            }
        }
        return ret;
    }

    protected abstract boolean isChecked(String packageName, int uid);

    protected abstract boolean onSetChecked(String packageName, int uid, boolean checked);

    protected abstract void onCheckedListUpdated(List<String> pkgList);
}
