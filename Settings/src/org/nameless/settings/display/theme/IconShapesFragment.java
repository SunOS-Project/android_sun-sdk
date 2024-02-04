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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settingslib.Utils;

import java.util.Collections;
import java.util.List;

public class IconShapesFragment extends SettingsPreferenceFragment {

    private static final String CATEGORY = ThemeUtils.ICON_SHAPE_KEY;

    private List<String> mPkgs;
    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_customization_icon_shape_title);

        mThemeUtils = new ThemeUtils(getActivity());
        mPkgs = mThemeUtils.getOverlayPackagesForCategory(CATEGORY, "android");
        Collections.sort(mPkgs);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_view, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
            return new CustomViewHolder(v);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            final String pkg = mPkgs.get(position);

            holder.image.setBackgroundDrawable(mThemeUtils.createShapeDrawable(pkg));

            final String currentPackageName = mThemeUtils.getOverlayInfos(CATEGORY, "android").stream()
                    .filter(info -> info.isEnabled())
                    .map(info -> info.packageName)
                    .findFirst()
                    .orElse("android");

            holder.name.setText("android".equals(pkg) ? "Default" : getLabel(holder.name.getContext(), pkg));

            final boolean isDefault = "android".equals(currentPackageName) && "android".equals(pkg);
            final int color = ColorUtils.setAlphaComponent(
                    Utils.getColorAttrDefaultColor(getContext(), android.R.attr.colorAccent),
                    pkg.equals(currentPackageName) || isDefault ? 170 : 75);
            holder.image.setBackgroundTintList(ColorStateList.valueOf(color));

            holder.itemView.findViewById(R.id.option_tile).setBackgroundDrawable(null);
            holder.itemView.setOnClickListener(v -> {
                enableOverlays(position);
            });
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }

        final class CustomViewHolder extends RecyclerView.ViewHolder {

            TextView name;
            ImageView image;

            CustomViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.option_label);
                image = (ImageView) itemView.findViewById(R.id.option_thumbnail);
            }
        }
    }

    private String getLabel(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
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
