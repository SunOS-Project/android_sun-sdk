/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display;

import static org.sun.provider.SettingsExt.System.QQS_LAYOUT_CUSTOM;
import static org.sun.provider.SettingsExt.System.QS_LAYOUT_CUSTOM;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.internal.util.sun.TileUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settingslib.widget.LayoutPreference;

import org.sun.custom.preference.CustomSeekBarPreference;
import org.sun.custom.preference.SystemSettingSwitchPreference;

public class QsTileLayoutSettingsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_QS_HIDE_LABEL = "qs_tile_label_hide";
    private static final String KEY_QS_VERTICAL_LAYOUT = "qs_tile_vertical_layout";
    private static final String KEY_QS_COLUMN_PORTRAIT = "qs_layout_columns";
    private static final String KEY_QS_ROW_PORTRAIT = "qs_layout_rows";
    private static final String KEY_QQS_ROW_PORTRAIT = "qqs_layout_rows";
    private static final String KEY_APPLY_CHANGE_BUTTON = "apply_change_button";

    private Context mContext;

    private Button mApplyChange;

    private CustomSeekBarPreference mQsColumns;
    private CustomSeekBarPreference mQsRows;
    private CustomSeekBarPreference mQqsRows;

    private SystemSettingSwitchPreference mHide;
    private SystemSettingSwitchPreference mVertical;

    private int[] currentValue = new int[2];

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.qs_tile_layout);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQsColumns = (CustomSeekBarPreference) findPreference(KEY_QS_COLUMN_PORTRAIT);
        mQsColumns.setOnPreferenceChangeListener(this);

        mQsRows = (CustomSeekBarPreference) findPreference(KEY_QS_ROW_PORTRAIT);
        mQsRows.setOnPreferenceChangeListener(this);

        mQqsRows = (CustomSeekBarPreference) findPreference(KEY_QQS_ROW_PORTRAIT);
        mQqsRows.setOnPreferenceChangeListener(this);

        mContext = getContext();

        LayoutPreference preference = findPreference(KEY_APPLY_CHANGE_BUTTON);
        mApplyChange = (Button) preference.findViewById(R.id.apply_change);
        mApplyChange.setOnClickListener(v -> {
            if (mApplyChange.isEnabled()) {
                final int[] newValue = {
                    mQsRows.getValue() * 10 + mQsColumns.getValue(),
                    mQqsRows.getValue() * 10 + mQsColumns.getValue()
                };
                Settings.System.putIntForUser(getContentResolver(),
                        QS_LAYOUT_CUSTOM, newValue[0], UserHandle.USER_SYSTEM);
                Settings.System.putIntForUser(getContentResolver(),
                        QQS_LAYOUT_CUSTOM, newValue[1], UserHandle.USER_SYSTEM);
                if (TileUtils.updateLayout(mContext)) {
                    currentValue[0] = newValue[0];
                    currentValue[1] = newValue[1];
                    mApplyChange.setEnabled(false);
                } else {
                    Settings.System.putIntForUser(getContentResolver(),
                            QS_LAYOUT_CUSTOM, currentValue[0], UserHandle.USER_SYSTEM);
                    Settings.System.putIntForUser(getContentResolver(),
                            QQS_LAYOUT_CUSTOM, currentValue[1], UserHandle.USER_SYSTEM);
                    Toast.makeText(mContext, R.string.qs_apply_change_failed, Toast.LENGTH_LONG).show();
                }
            }
        });

        initPreference();

        final boolean hideLabel = TileUtils.getQSTileLabelHide(mContext);

        mHide = (SystemSettingSwitchPreference) findPreference(KEY_QS_HIDE_LABEL);
        mHide.setOnPreferenceChangeListener(this);

        mVertical = (SystemSettingSwitchPreference) findPreference(KEY_QS_VERTICAL_LAYOUT);
        mVertical.setEnabled(!hideLabel);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHide) {
            final boolean hideLabel = (Boolean) newValue;
            mVertical.setEnabled(!hideLabel);
        } else if (preference == mQsColumns) {
            final int qs_columns = Integer.parseInt(newValue.toString());
            mApplyChange.setEnabled(
                currentValue[0] != mQsRows.getValue() * 10 + qs_columns ||
                currentValue[1] != mQqsRows.getValue() * 10 + qs_columns
            );
        } else if (preference == mQsRows) {
            final int qs_rows = Integer.parseInt(newValue.toString());
            mQqsRows.setMax(qs_rows - 1);
            if (mQqsRows.getValue() > qs_rows - 1) {
                mQqsRows.setValue(qs_rows - 1);
            }
            mApplyChange.setEnabled(
                currentValue[0] != qs_rows * 10 + mQsColumns.getValue() ||
                currentValue[1] != mQqsRows.getValue() * 10 + mQsColumns.getValue()
            );
        } else if (preference == mQqsRows) {
            final int qqs_rows = Integer.parseInt(newValue.toString());
            mApplyChange.setEnabled(
                currentValue[0] != mQsRows.getValue() * 10 + mQsColumns.getValue() ||
                currentValue[1] != qqs_rows * 10 + mQsColumns.getValue()
            );
        }
        return true;
    }

    private void initPreference() {
        final int index_qs = Settings.System.getIntForUser(getContentResolver(),
                QS_LAYOUT_CUSTOM, 42, UserHandle.USER_SYSTEM);
        final int index_qqs = Settings.System.getIntForUser(getContentResolver(),
                QQS_LAYOUT_CUSTOM, 22, UserHandle.USER_SYSTEM);
        mQsColumns.setValue(index_qs % 10);
        mQsRows.setValue(index_qs / 10);
        mQqsRows.setValue(index_qqs / 10);
        mQqsRows.setMax(mQsRows.getValue() - 1);
        currentValue[0] = index_qs;
        currentValue[1] = index_qqs;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
