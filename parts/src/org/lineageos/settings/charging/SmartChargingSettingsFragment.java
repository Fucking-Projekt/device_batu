/*
 * Copyright (C) 2026 The LineageOS Project
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

package org.lineageos.settings.charging;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SeekBarPreference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class SmartChargingSettingsFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private ChargingUtils mChargingUtils;

    private MainSwitchPreference mMainSwitchPreference;
    private SeekBarPreference mLimitPreference;
    private TwoStatePreference mBypassPreference;
    private ListPreference mResetLimitPreference;
    private ListPreference mResetTimePreference;
    private PreferenceCategory mSettingsCategory;
    private PreferenceCategory mAdvancedCategory;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.smart_charging_settings, rootKey);

        mChargingUtils = ChargingUtils.getInstance(getContext());

        mMainSwitchPreference = findPreference(ChargingUtils.KEY_CHARGING_CONTROL_ENABLED);
        mLimitPreference = findPreference(ChargingUtils.KEY_CHARGING_CONTROL_LIMIT);
        mBypassPreference = findPreference(ChargingUtils.KEY_CHARGING_CONTROL_BYPASS);
        mResetLimitPreference = findPreference(ChargingUtils.KEY_CHARGING_CONTROL_RESET_LIMIT);
        mResetTimePreference = findPreference(ChargingUtils.KEY_CHARGING_CONTROL_RESET_TIME);

        mSettingsCategory = findPreference("charging_control_category_settings");
        mAdvancedCategory = findPreference("charging_control_category_advanced");

        boolean enabled = mChargingUtils.isEnabled();
        if (mMainSwitchPreference != null) {
            mMainSwitchPreference.setChecked(enabled);
            mMainSwitchPreference.addOnSwitchChangeListener(this);
        }

        if (mLimitPreference != null) {
            mLimitPreference.setValue(mChargingUtils.getChargingLimit());
            mLimitPreference.setOnPreferenceChangeListener(this);
        }

        if (mBypassPreference != null) {
            mBypassPreference.setChecked(mChargingUtils.isBypassChargingEnabled());
            mBypassPreference.setOnPreferenceChangeListener(this);
        }

        if (mResetLimitPreference != null) {
            mResetLimitPreference.setValue(String.valueOf(mChargingUtils.getResetLimit()));
            mResetLimitPreference.setOnPreferenceChangeListener(this);
        }

        if (mResetTimePreference != null) {
            mResetTimePreference.setValue(String.valueOf(mChargingUtils.getResetTime()));
            mResetTimePreference.setOnPreferenceChangeListener(this);
        }

        updatePreferencesEnabled(enabled);
    }

    private void updatePreferencesEnabled(boolean enabled) {
        if (mSettingsCategory != null) {
            mSettingsCategory.setEnabled(enabled);
        }
        if (mAdvancedCategory != null) {
            mAdvancedCategory.setEnabled(enabled);
        }
        if (mLimitPreference != null) {
            mLimitPreference.setEnabled(enabled);
        }
        if (mBypassPreference != null) {
            mBypassPreference.setEnabled(enabled);
        }
        if (mResetLimitPreference != null) {
            mResetLimitPreference.setEnabled(enabled);
        }
        if (mResetTimePreference != null) {
            mResetTimePreference.setEnabled(enabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mChargingUtils.setEnabled(isChecked);
        updatePreferencesEnabled(isChecked);
        ChargingUtils.checkService(getContext());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLimitPreference) {
            int value = (Integer) newValue;
            mChargingUtils.setChargingLimit(value);
            notifyService();
            return true;
        } else if (preference == mBypassPreference) {
            boolean value = (Boolean) newValue;
            mChargingUtils.setBypassChargingEnabled(value);
            notifyService();
            return true;
        } else if (preference == mResetLimitPreference) {
            int value = Integer.parseInt((String) newValue);
            mChargingUtils.setResetLimit(value);
            notifyService();
            return true;
        } else if (preference == mResetTimePreference) {
            int value = Integer.parseInt((String) newValue);
            mChargingUtils.setResetTime(value);
            notifyService();
            return true;
        }
        return false;
    }

    private void notifyService() {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), SmartChargingService.class);
        intent.setAction(SmartChargingService.ACTION_RE_EVALUATE);
        getContext().startService(intent);
    }
}
