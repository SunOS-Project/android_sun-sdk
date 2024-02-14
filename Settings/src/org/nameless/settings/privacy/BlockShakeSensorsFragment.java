/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.privacy;

import static org.nameless.hardware.SensorBlockManager.SHAKE_SENSORS_ALLOW;
import static org.nameless.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_ALWAYS;
import static org.nameless.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_FIRST_SCREEN;

import android.os.Bundle;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import org.nameless.hardware.SensorBlockManager;
import org.nameless.settings.fragment.PerAppListConfigFragment;

public class BlockShakeSensorsFragment extends PerAppListConfigFragment {

    private SensorBlockManager mSensorBlockManager;

    private List<String> mEntries;
    private List<Integer> mValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorBlockManager = getActivity().getSystemService(SensorBlockManager.class);

        mEntries = new ArrayList<>();
        mEntries.add(getString(R.string.shake_sensors_allow));
        mEntries.add(getString(R.string.shake_sensors_block_first_screen));
        mEntries.add(getString(R.string.shake_sensors_block_always));

        mValues = new ArrayList<>();
        mValues.add(SHAKE_SENSORS_ALLOW);
        mValues.add(SHAKE_SENSORS_BLOCK_FIRST_SCREEN);
        mValues.add(SHAKE_SENSORS_BLOCK_ALWAYS);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.block_shake_sensors_config;
    }

    @Override
    protected int getTopInfoResId() {
        return R.string.block_shake_sensors_summary;
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
    protected int getCurrentValue(String packageName) {
        return mSensorBlockManager.getShakeSensorsConfigForPackage(packageName);
    }

    @Override
    protected void onValueChanged(String packageName, int value) {
        mSensorBlockManager.setShakeSensorsConfigForPackage(packageName, value);
    }
}
