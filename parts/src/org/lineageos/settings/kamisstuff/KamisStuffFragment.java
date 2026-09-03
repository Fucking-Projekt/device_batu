/*
 * Copyright (C) 2025 KamiKaonashi
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

package org.lineageos.settings.kamisstuff;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.fps.FPSInfoService;
import org.lineageos.settings.kernelmanager.KernelManagerActivity;
import org.lineageos.settings.kcal.KcalSettingsActivity;
import org.lineageos.settings.preferences.SysfsSwitchPreference;
import org.lineageos.settings.speaker.ClearSpeakerActivity;
import org.lineageos.settings.refreshrate.RefreshActivity;
import org.lineageos.settings.resolution.ResolutionActivity;
import org.lineageos.settings.resolution.SystemResolutionActivity;
import org.lineageos.settings.dirac.DiracActivity;
import org.lineageos.settings.thermal.ThermalActivity;
import org.lineageos.settings.useless.UselessActivity;

public class KamisStuffFragment extends SettingsBasePreferenceFragment {

    private static final String KEY_USELESS = "useless";
    private static final String KEY_KERNEL_MANAGER = "kernel_manager";
    private static final String KEY_THERMAL = "thermal";
    private static final String KEY_CLEAR_SPEAKER = "clear_speaker";
    private static final String KEY_REFRESH_RATE = "refresh_rate";
    private static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_DIRAC = "dirac";
    private static final String KEY_KCAL = "kcal";
    private static final String KEY_SYSTEM_RESOLUTION = "system_resolution";
    private static final String KEY_FPS_INFO = "fps_info";
    private static final String KEY_HIGH_TOUCH_POLLING = "high_touch_polling";
    private static final String KEY_BYPASS_CHARGE = "bypass_charge";

    private boolean isFPSInfoServiceRunning() {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FPSInfoService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        SysfsSwitchPreference bypassChargePref = findPreference(KEY_BYPASS_CHARGE);
        if (bypassChargePref != null && bypassChargePref.isSupported()) {
            bypassChargePref.readFromSysfs();
        }

        SysfsSwitchPreference highTouchPref = findPreference(KEY_HIGH_TOUCH_POLLING);
        if (highTouchPref != null && highTouchPref.isSupported()) {
            highTouchPref.readFromSysfs();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.kamis_stuff_settings, rootKey);

        // Display
        Preference refreshRatePref = findPreference(KEY_REFRESH_RATE);
        if (refreshRatePref != null) {
            refreshRatePref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), RefreshActivity.class));
                return true;
            });
        }

        Preference resolutionPref = findPreference(KEY_RESOLUTION);
        if (resolutionPref != null) {
            resolutionPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), ResolutionActivity.class));
                return true;
            });
        }

        Preference kcalPref = findPreference(KEY_KCAL);
        if (kcalPref != null) {
            kcalPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), KcalSettingsActivity.class));
                return true;
            });
        }

        Preference systemResolutionPref = findPreference(KEY_SYSTEM_RESOLUTION);
        if (systemResolutionPref != null) {
            systemResolutionPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), SystemResolutionActivity.class));
                return true;
            });
        }

        SwitchPreference fpsInfoPref = findPreference(KEY_FPS_INFO);
        if (fpsInfoPref != null) {
            fpsInfoPref.setChecked(isFPSInfoServiceRunning());
            fpsInfoPref.setOnPreferenceChangeListener((preference, value) -> {
                boolean enabled = (Boolean) value;
                Intent fpsIntent = new Intent(getContext(), FPSInfoService.class);
                if (enabled) {
                    getContext().startService(fpsIntent);
                } else {
                    getContext().stopService(fpsIntent);
                }
                return true;
            });
        }

        // SysfsSwitchPreference handles high touch polling automatically via XML

        // Audio
        Preference diracPref = findPreference(KEY_DIRAC);
        if (diracPref != null) {
            diracPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), DiracActivity.class));
                return true;
            });
        }

        Preference clearSpeakerPref = findPreference(KEY_CLEAR_SPEAKER);
        if (clearSpeakerPref != null) {
            clearSpeakerPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), ClearSpeakerActivity.class));
                return true;
            });
        }

        // System
        Preference thermalPref = findPreference(KEY_THERMAL);
        if (thermalPref != null) {
            thermalPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), ThermalActivity.class));
                return true;
            });
        }

        SysfsSwitchPreference bypassChargePref = findPreference(KEY_BYPASS_CHARGE);
        if (bypassChargePref != null && bypassChargePref.isSupported()) {
            bypassChargePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fastcharge_bypass_title)
                        .setMessage(R.string.fastcharge_bypass_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            bypassChargePref.writeToSysfs(true);
                            bypassChargePref.setChecked(true);
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            bypassChargePref.setChecked(false);
                        })
                        .show();
                    return false;
                } else {
                    return true;
                }
            });
        }

        Preference kernelManagerPref = findPreference(KEY_KERNEL_MANAGER);
        if (kernelManagerPref != null) {
            kernelManagerPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), KernelManagerActivity.class));
                return true;
            });
        }

        Preference uselessPref = findPreference(KEY_USELESS);
        if (uselessPref != null) {
            uselessPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), UselessActivity.class));
                return true;
            });
        }
    }
}
