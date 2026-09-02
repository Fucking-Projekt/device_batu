/*
 * Copyright (C) 2018,2020 The LineageOS Project
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

package org.lineageos.settings.dirac;

import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.settings.R;

public class DiracSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "DiracSettingsFragment";
    private static final String PREF_ENABLE = "dirac_enable";
    private static final String PREF_HEADSET = "dirac_headset_pref";
    private static final String PREF_HIFI = "dirac_hifi_pref";
    private static final String PREF_PRESET = "dirac_preset_pref";
    private static final String PREF_SCENE = "scenario_selection";

    private MainSwitchPreference mSwitchBar;
    private SwitchPreference mHifi;
    private DiracUtils mDiracUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dirac_settings, rootKey);

        try {
            mDiracUtils = DiracUtils.getInstance(getActivity());
        } catch (Exception e) {
            Log.d(TAG, "Dirac is not present in system");
        }

        boolean enhancerEnabled = mDiracUtils != null && mDiracUtils.isDiracEnabled();
        mSwitchBar = (MainSwitchPreference) findPreference(PREF_ENABLE);
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.setChecked(enhancerEnabled);

        ListPreference headsetType = (ListPreference) findPreference(PREF_HEADSET);
        headsetType.setOnPreferenceChangeListener(this);
        headsetType.setEnabled(enhancerEnabled);

        ListPreference preset = (ListPreference) findPreference(PREF_PRESET);
        preset.setOnPreferenceChangeListener(this);
        preset.setEnabled(enhancerEnabled);

        mHifi = (SwitchPreference) findPreference(PREF_HIFI);
        mHifi.setOnPreferenceChangeListener(this);

        boolean hifiEnable = mDiracUtils != null && mDiracUtils.getHifiMode();
        headsetType.setEnabled(!hifiEnable && enhancerEnabled);
        preset.setEnabled(!hifiEnable && enhancerEnabled);

        ListPreference scenes = (ListPreference) findPreference(PREF_SCENE);
        scenes.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncFromHal();
    }

    private void syncFromHal() {
        if (mDiracUtils == null) return;

        boolean enhancerEnabled = mDiracUtils.isDiracEnabled();
        mSwitchBar.setChecked(enhancerEnabled);

        boolean hifiEnable = mDiracUtils.getHifiMode();
        mHifi.setChecked(hifiEnable);

        ListPreference headsetType = (ListPreference) findPreference(PREF_HEADSET);
        ListPreference preset = (ListPreference) findPreference(PREF_PRESET);
        headsetType.setEnabled(!hifiEnable && enhancerEnabled);
        preset.setEnabled(!hifiEnable && enhancerEnabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSwitchBar.setChecked(isChecked);
        mDiracUtils.setEnabled(isChecked);

        if (!isChecked) {
            mHifi.setChecked(false);
            mDiracUtils.setHifiMode(0);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mDiracUtils == null) return false;
        switch (preference.getKey()) {
            case PREF_HIFI:
                mDiracUtils.setHifiMode((Boolean) newValue ? 1 : 0);
                return true;
            case PREF_HEADSET:
                mDiracUtils.setHeadsetType(Integer.parseInt(newValue.toString()));
                return true;
            case PREF_PRESET:
                mDiracUtils.setLevel((String) newValue);
                return true;
            case PREF_SCENE:
                mDiracUtils.setScenario(Integer.parseInt(newValue.toString()));
                return true;
            default:
                return false;
        }
    }
}
