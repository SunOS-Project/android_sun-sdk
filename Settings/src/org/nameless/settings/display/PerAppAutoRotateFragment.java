/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import static org.nameless.os.RotateManager.ROTATE_FOLLOW_SYSTEM;
import static org.nameless.os.RotateManager.ROTATE_FORCE_OFF;
import static org.nameless.os.RotateManager.ROTATE_FORCE_ON;

import android.os.Bundle;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import org.nameless.os.RotateManager;
import org.nameless.settings.fragment.PerAppListConfigFragment;

public class PerAppAutoRotateFragment extends PerAppListConfigFragment {

    private RotateManager mRotateManager;

    private List<String> mEntries;
    private List<Integer> mValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRotateManager = getActivity().getSystemService(RotateManager.class);

        mEntries = new ArrayList<>();
        mEntries.add(getString(R.string.per_app_auto_rotate_default));
        mEntries.add(getString(R.string.per_app_auto_rotate_off));
        mEntries.add(getString(R.string.per_app_auto_rotate_on));

        mValues = new ArrayList<>();
        mValues.add(ROTATE_FOLLOW_SYSTEM);
        mValues.add(ROTATE_FORCE_OFF);
        mValues.add(ROTATE_FORCE_ON);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.per_app_auto_rotate_config;
    }

    @Override
    protected int getTopInfoResId() {
        return R.string.per_app_auto_rotate_summary;
    }

    @Override
    protected List<String> getEntries() {
        return mEntries;
    }

    @Override
    protected List<Integer> getValues() {
        return mValues;
    }

    @Override
    protected int getCurrentValue(String packageName, int uid) {
        return mRotateManager.getRotateConfigForPackage(packageName);
    }

    @Override
    protected void onValueChanged(String packageName, int uid, int value) {
        mRotateManager.setRotateConfigForPackage(packageName, value);
    }
}
