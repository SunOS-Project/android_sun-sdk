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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Collections;
import java.util.List;

public class FontsPickerFragment extends SettingsPreferenceFragment {

    private static final String CATEGORY = ThemeUtils.FONT_KEY;

    private List<String> mPkgs;
    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_customization_font_title);

        mThemeUtils = new ThemeUtils(getActivity());
        mPkgs = mThemeUtils.getOverlayPackagesForCategory(CATEGORY, "android");
        Collections.sort(mPkgs);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_view, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        mRecyclerView.setAdapter(new Adapter());
        return view;
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {

        private String mSelectedPkg;
        private String mAppliedPkg;

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fonts_option, parent, false);
            return new CustomViewHolder(v);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            final String pkg = mPkgs.get(position);
            final String label = getLabel(holder.itemView.getContext(), pkg);

            final String currentPackageName = mThemeUtils.getOverlayInfos(CATEGORY, "android").stream()
                    .filter(info -> info.isEnabled())
                    .map(info -> info.packageName)
                    .findFirst()
                    .orElse("android");

            holder.title.setText("android".equals(pkg) ? "Default" : label);
            holder.title.setTextSize(20);
            holder.title.setTypeface(getTypeface(holder.title.getContext(), pkg));
            holder.name.setVisibility(View.GONE);

            if (currentPackageName.equals(pkg)) {
                mAppliedPkg = pkg;
                if (mSelectedPkg == null) {
                    mSelectedPkg = pkg;
                }
            }

            holder.itemView.setActivated(pkg == mSelectedPkg);
            holder.itemView.setOnClickListener(v -> {
                updateActivatedStatus(mSelectedPkg, false);
                updateActivatedStatus(pkg, true);
                mSelectedPkg = pkg;
                enableOverlays(position);
            });
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }

        final class CustomViewHolder extends RecyclerView.ViewHolder {

            TextView name;
            TextView title;

            CustomViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.option_title);
                name = (TextView) itemView.findViewById(R.id.option_label);
            }
        }

        private void updateActivatedStatus(String pkg, boolean isActivated) {
            final int index = mPkgs.indexOf(pkg);
            if (index < 0) {
                return;
            }
            final RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && holder.itemView != null) {
                holder.itemView.setActivated(isActivated);
            }
        }
    }

    private Typeface getTypeface(Context context, String pkg) {
        try {
            final PackageManager pm = context.getPackageManager();
            final Resources res = pkg.equals("android") ? Resources.getSystem()
                    : pm.getResourcesForApplication(pkg);
            return Typeface.create(res.getString(
                    res.getIdentifier("config_bodyFontFamily",
                    "string", pkg)), Typeface.NORMAL);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getLabel(Context context, String pkg) {
        try {
            final PackageManager pm = context.getPackageManager();
            return pm.getApplicationInfo(pkg, 0)
                    .loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pkg;
    }

    private void enableOverlays(int position) {
        mThemeUtils.setOverlayEnabled(CATEGORY, mPkgs.get(position));
    }
}
