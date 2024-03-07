/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.notification;

import static org.nameless.provider.SettingsExt.System.HEADS_UP_STOPLIST;

import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import org.nameless.custom.preference.SwitchPreferenceCompat;
import org.nameless.settings.fragment.PerAppSwitchConfigFragment;

public class HeadsUpStoplistSettingsFragment extends PerAppSwitchConfigFragment {

    private List<String> mCheckedList;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mCheckedList = new ArrayList<>();
        final String stoplist = Settings.System.getStringForUser(
                mContext.getContentResolver(),
                HEADS_UP_STOPLIST, UserHandle.USER_CURRENT);
        if (stoplist != null) {
            for (String pkg : stoplist.split(";")) {
                mCheckedList.add(pkg);
            }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.heads_up_stoplist;
    }

    @Override
    protected int getTopInfoResId() {
        return R.string.heads_up_stoplist_summary;
    }

    @Override
    protected boolean isChecked(String packageName, int uid) {
        return mCheckedList.contains(packageName);
    }

    @Override
    protected boolean onSetChecked(SwitchPreferenceCompat pref, String packageName, int uid, boolean checked) {
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
                HEADS_UP_STOPLIST, sb.toString(), UserHandle.USER_CURRENT);
    }
}
