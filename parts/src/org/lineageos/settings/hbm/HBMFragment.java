/*
 * Copyright (C) 2016 The OmniROM Project
 * Copyright (C) 2018-2021 crDroid Android Project
 * Copyright (C) 2019-2022 Evolution X Project
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

package org.lineageos.settings.hbm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class HBMFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_HBM_SWITCH = HBMUtils.KEY_HBM;
    public static final String KEY_AUTO_HBM_SWITCH = HBMUtils.KEY_AUTO_HBM;
    public static final String KEY_AUTO_HBM_THRESHOLD = HBMUtils.KEY_AUTO_HBM_THRESHOLD;
    public static final String KEY_HBM_DISABLE_TIME = HBMUtils.KEY_HBM_DISABLE_TIME;

    private TwoStatePreference mHBMModeSwitch;
    private TwoStatePreference mAutoHBMSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.hbm_settings, rootKey);

        // HBM
        mHBMModeSwitch = (TwoStatePreference) findPreference(KEY_HBM_SWITCH);
        if (mHBMModeSwitch != null) {
            mHBMModeSwitch.setOnPreferenceChangeListener(new HBMModeSwitch(getContext()));
        }

        // AutoHBM
        mAutoHBMSwitch = (TwoStatePreference) findPreference(KEY_AUTO_HBM_SWITCH);
        if (mAutoHBMSwitch != null) {
            mAutoHBMSwitch.setOnPreferenceChangeListener(this);
            mAutoHBMSwitch.setChecked(isAUTOHBMEnabled(getContext()));
        }
    }

    public static boolean isAUTOHBMEnabled(Context context) {
        if (context == null) return false;
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HBMUtils.KEY_AUTO_HBM, false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAutoHBMSwitch) {
            boolean enabled = (Boolean) newValue;
            SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            prefChange.putBoolean(HBMUtils.KEY_AUTO_HBM, enabled).apply();
            HBMUtils.enableService(getContext());
            return true;
        }
        return false;
    }
}
