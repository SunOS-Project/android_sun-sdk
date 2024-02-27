/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.system;

import android.os.Bundle;

import com.android.settings.R;

import java.util.List;

import org.nameless.app.GameModeManager;
import org.nameless.custom.preference.SwitchPreference;
import org.nameless.settings.fragment.PerAppSwitchConfigFragment;

public class ManageGamesFragment extends PerAppSwitchConfigFragment {

    private GameModeManager mGameModeManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGameModeManager = getActivity().getSystemService(GameModeManager.class);
    }

    @Override
    protected int getAllowedSystemAppListResId() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.manage_games;
    }

    @Override
    protected int getTopInfoResId() {
        return R.string.game_mode_manage_apps_summary;
    }

    @Override
    protected boolean isChecked(String packageName, int uid) {
        return mGameModeManager.isAppGame(packageName);
    }

    @Override
    protected boolean onSetChecked(SwitchPreference pref, String packageName, int uid, boolean checked) {
        if (checked) {
            mGameModeManager.addGame(packageName);
        } else {
            mGameModeManager.removeGame(packageName);
        }
        return true;
    }

    @Override
    protected void onCheckedListUpdated(List<String> pkgList) {}
}
