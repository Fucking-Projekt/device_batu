/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.settings.display;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.hbm.HBMUtils;
import org.lineageos.settings.utils.FileUtils;

import java.io.File;

public class DcDimmingSettingsFragment extends SettingsBasePreferenceFragment implements
        OnPreferenceChangeListener {

    private TwoStatePreference mDcDimmingPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dcdimming_settings, rootKey);
        mDcDimmingPreference = findPreference(DcDimmingUtils.DC_DIMMING_ENABLE_KEY);
        if (FileUtils.fileExists(DcDimmingUtils.DC_DIMMING_NODE)) {
            mDcDimmingPreference.setEnabled(true);
            mDcDimmingPreference.setOnPreferenceChangeListener(this);
        } else {
            mDcDimmingPreference.setSummary(R.string.dc_dimming_enable_summary_not_supported);
            mDcDimmingPreference.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (DcDimmingUtils.DC_DIMMING_ENABLE_KEY.equals(preference.getKey())) {
            boolean enabled = (boolean) newValue;
            FileUtils.writeLine(DcDimmingUtils.DC_DIMMING_NODE, enabled ? "1" : "0");
            if (enabled) {
                disableHBM();
            }
            updateHBMPreference(!enabled);
        }
        return true;
    }

    private void disableHBM() {
        FileUtils.writeLine(DcDimmingUtils.HBM_NODE, "0");
        File hbmFile = new File(DcDimmingUtils.HBM_NODE);
        hbmFile.setReadOnly();
        updateHBMUI(false);
    }

    private void updateHBMUI(boolean enabled) {
        Intent intent = new Intent("org.lineageos.settings.hbm.UPDATE_TILE");
        intent.putExtra("enabled", enabled);
        if (getActivity() != null) {
            getActivity().sendBroadcast(intent);
        }
        if (getContext() != null) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            sharedPrefs.edit().putBoolean(HBMUtils.KEY_HBM, enabled).apply();
        }
        updateHBMPreference(enabled);
    }

    private void updateHBMPreference(boolean enabled) {
        if (mDcDimmingPreference != null) {
            mDcDimmingPreference.setChecked(enabled);
        }
    }
}
