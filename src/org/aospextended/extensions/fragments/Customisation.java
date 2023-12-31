/*
 * Copyright (C) 2018 AospExtended ROM Project
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

import static android.os.UserHandle.USER_SYSTEM;
import static android.os.UserHandle.USER_CURRENT;

import android.app.ActivityManagerNative;
import android.app.UiModeManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.view.IWindowManager;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Locale;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Utils;

import com.android.internal.util.aospextended.AEXUtils;
import com.android.internal.util.aospextended.ThemeUtils;
import com.android.internal.util.aospextended.fod.FodUtils;

import org.aospextended.extensions.preference.FontListPreference;
import org.aospextended.extensions.preference.SystemSettingListPreference;
import org.aospextended.extensions.preference.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

@SearchIndexable
public class Customisation extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "Customisation";

    private static final String SYSTEM_THEME_STYLE = "android.theme.customization.theme_style";
    private static final String SYSTEM_FONT_STYLE = "android.theme.customization.font";

    private static final String FOD_FOOTER = "fod_footer";

    private static final String SWITCH_STYLE = "switch_style";

    private SystemSettingListPreference mSwitchStyle;

    private FontListPreference mFontPreference;

    private Context mContext;
    private ThemeUtils mThemeUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customisation);

        mContext = getActivity();
        mThemeUtils = new ThemeUtils(mContext);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen screen = getPreferenceScreen();

        mFontPreference = (FontListPreference) screen.findPreference(SYSTEM_FONT_STYLE);
        mFontPreference.setOnPreferenceChangeListener(this);
        updateState((ListPreference) mFontPreference);

        mSwitchStyle = (SystemSettingListPreference) screen.findPreference(SWITCH_STYLE);
        int switchStyle = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SWITCH_STYLE, 0, UserHandle.USER_CURRENT);
        int valueIndex = mSwitchStyle.findIndexOfValue(String.valueOf(switchStyle));
        mSwitchStyle.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        mSwitchStyle.setSummary(mSwitchStyle.getEntry());
        mSwitchStyle.setOnPreferenceChangeListener(this);

        SystemSettingSwitchPreference mFodAnim = (SystemSettingSwitchPreference) findPreference("fod_recognizing_animation");
        Preference mFodAnimList = (Preference) findPreference("fod_recognizing_animation_preview");

        boolean mFodAnimPkgInstalled = AEXUtils.isPackageInstalled(getContext(),getResources()
                .getString(com.android.internal.R.string.config_fodAnimationPackage));
        PreferenceCategory fod = (PreferenceCategory) screen.findPreference("fod_category");
        if (!mFodAnimPkgInstalled) {
            fod.removePreference(mFodAnim);
            fod.removePreference(mFodAnimList);
        }
        if (!FodUtils.hasFodSupport(mContext)) {
            screen.removePreference(fod);
        } else {
            findPreference(FOD_FOOTER).setTitle(R.string.fod_pressed_color_footer);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EXTENSIONS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState((ListPreference) mFontPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFontPreference) {
            mThemeUtils.setOverlayEnabled(SYSTEM_FONT_STYLE, (String) newValue);
            return true;
        } else if (preference == mSwitchStyle) {
            int switchStyleValue = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SWITCH_STYLE, switchStyleValue, UserHandle.USER_CURRENT);
            mSwitchStyle.setSummary(mSwitchStyle.getEntries()[switchStyleValue]);
            return true;
        }
        return false;
    }

    public void updateState(ListPreference preference) {
        String currentPackageName = mThemeUtils.getOverlayInfos(preference.getKey()).stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse("Default");

        List<String> pkgs = mThemeUtils.getOverlayPackagesForCategory(preference.getKey());
        List<String> labels = mThemeUtils.getLabels(preference.getKey());


        preference.setEntries(labels.toArray(new String[labels.size()]));
        preference.setEntryValues(pkgs.toArray(new String[pkgs.size()]));
        preference.setValue("Default".equals(currentPackageName) ? pkgs.get(0) : currentPackageName);
        preference.setSummary("Default".equals(currentPackageName) ? "Default" : labels.get(pkgs.indexOf(currentPackageName)));
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.customisation;
                result.add(sir);
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
    };
}
