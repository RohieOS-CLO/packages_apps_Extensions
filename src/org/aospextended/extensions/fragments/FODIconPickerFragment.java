/*
 * Copyright (C) 2021 AospExtended ROM Project
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

package org.aospextended.extensions.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import androidx.preference.PreferenceViewHolder;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import android.net.Uri;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import com.bumptech.glide.Glide;

import com.android.internal.util.aospextended.clock.ClockFace;
import com.android.internal.util.aospextended.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONException;

public class FODIconPickerFragment extends SettingsPreferenceFragment {

    private RecyclerView mRecyclerView;

    private int[] mIcons = {
        R.drawable.fod_icon_default,
        R.drawable.fod_icon_aex,
        R.drawable.fod_icon_default_1,
        R.drawable.fod_icon_default_2,
        R.drawable.fod_icon_default_3,
        R.drawable.fod_icon_default_4,
        R.drawable.fod_icon_default_5,
        R.drawable.fod_icon_arc_reactor,
        R.drawable.fod_icon_cpt_america_flat,
        R.drawable.fod_icon_cpt_america_flat_gray,
        R.drawable.fod_icon_dragon_black_flat,
        R.drawable.fod_icon_glow_circle,
        R.drawable.fod_icon_neon_arc,
        R.drawable.fod_icon_neon_arc_gray,
        R.drawable.fod_icon_neon_circle_pink,
        R.drawable.fod_icon_neon_triangle,
        R.drawable.fod_icon_paint_splash_circle,
        R.drawable.fod_icon_rainbow_horn,
        R.drawable.fod_icon_shooky,
        R.drawable.fod_icon_spiral_blue,
        R.drawable.fod_icon_sun_metro,
        R.drawable.fod_icon_scratch_red_blue,
        R.drawable.fod_icon_scratch_pink_blue,
        R.drawable.fod_icon_fire_ice_ouroboros,
        R.drawable.fod_icon_transparent
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.fod_icon_picker_title);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.item_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.layout);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        FODIconAdapter mFODIconAdapter = new FODIconAdapter(getActivity());
        mRecyclerView.setAdapter(mFODIconAdapter);

        return view;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EXTENSIONS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public class FODIconAdapter extends RecyclerView.Adapter<FODIconAdapter.FODIconViewHolder> {
        Context context;
        int mSelectedIcon = -1;
        int mAppliedIcon;

        public FODIconAdapter(Context context) {
            this.context = context;
        }

        @Override
        public FODIconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
            FODIconViewHolder vh = new FODIconViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(FODIconViewHolder holder, final int position) {
            int iconRes = mIcons[position];

            Glide.with(holder.image.getContext())
                    .load("")
                    .placeholder(holder.image.getContext().getDrawable(mIcons[position]))
                    .into(holder.image);

            holder.image.setPadding(20,20,20,20);

            holder.name.setVisibility(View.GONE);

            if (position == Settings.System.getInt(context.getContentResolver(),
                Settings.System.FOD_ICON, 0)) {
                mAppliedIcon = iconRes;
                if (mSelectedIcon == -1) {
                    mSelectedIcon = iconRes;
                }
            }
            holder.itemView.setActivated(iconRes == mSelectedIcon);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateActivatedStatus(mSelectedIcon, false);
                    updateActivatedStatus(iconRes, true);
                    mSelectedIcon = iconRes;
                    Settings.System.putInt(getActivity().getContentResolver(),
                            Settings.System.FOD_ICON, position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mIcons.length;
        }

        public class FODIconViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image;
            public FODIconViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.option_label);
                image = (ImageView) itemView.findViewById(R.id.option_thumbnail);
            }
        }

        private void updateActivatedStatus(int icon, boolean isActivated) {
            List<Integer> list = new ArrayList<>(mIcons.length);
            for (int i : mIcons) {
                list.add(i);
            }
            int index = list.indexOf(icon);
            if (index < 0) {
                return;
            }
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && holder.itemView != null) {
                holder.itemView.setActivated(isActivated);
            }
        }
    }
}
