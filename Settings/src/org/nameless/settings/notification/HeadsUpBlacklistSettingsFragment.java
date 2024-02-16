/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.notification;

import static org.nameless.provider.SettingsExt.System.HEADS_UP_BLACKLIST;

import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import org.nameless.custom.preference.SwitchPreference;
import org.nameless.settings.fragment.PerAppSwitchConfigFragment;

public class HeadsUpBlacklistSettingsFragment extends PerAppSwitchConfigFragment {

    private List<String> mCheckedList;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mCheckedList = new ArrayList<>();
        final String blacklist = Settings.System.getStringForUser(
                mContext.getContentResolver(),
                HEADS_UP_BLACKLIST, UserHandle.USER_CURRENT);
        if (blacklist != null) {
            for (String pkg : blacklist.split(";")) {
                mCheckedList.add(pkg);
            }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.heads_up_blacklist;
    }

    @Override
    protected int getTopInfoResId() {
        return R.string.heads_up_blacklist_summary;
    }

    @Override
    protected boolean isChecked(String packageName, int uid) {
        return mCheckedList.contains(packageName);
    }

    @Override
    protected boolean onSetChecked(SwitchPreference pref, String packageName, int uid, boolean checked) {
        return true;
    }

    @Override
    protected void onCheckedListUpdated(List<String> pkgList) {
        mCheckedList = pkgList;

        StringBuilder sb = new StringBuilder();
        for (String pkg : mCheckedList) {
            sb.append(pkg).append(";");
        }
        Settings.System.putStringForUser(mContext.getContentResolver(),
                HEADS_UP_BLACKLIST, sb.toString(), UserHandle.USER_CURRENT);
    }
}
