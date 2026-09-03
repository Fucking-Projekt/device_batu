/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;
import org.lineageos.settings.dirac.DiracUtils;
import org.lineageos.settings.fps.FPSInfoService;
import org.lineageos.settings.kcal.KcalController;
import org.lineageos.settings.kcal.KcalService;
import org.lineageos.settings.refreshrate.RefreshUtils;
import org.lineageos.settings.resolution.ResolutionUtils;
import org.lineageos.settings.thermal.ThermalUtils;
import org.lineageos.settings.utils.FileUtils;

public class BootCompletedReceiver extends BroadcastReceiver implements KcalController {
    private static final boolean DEBUG = false;
    private static final String TAG = "XiaomiParts";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG)
            Log.d(TAG, "Received boot completed intent");

        // KCAL
        if (Settings.Secure.getInt(context.getContentResolver(), PREF_SETONBOOT, 0) == 1) {
            String kcalRed = context.getString(R.string.config_kcalRedSysNode);
            String kcalGreen = context.getString(R.string.config_kcalGreenSysNode);
            String kcalBlue = context.getString(R.string.config_kcalBlueSysNode);
            String kcalSat = context.getString(R.string.config_kcalSatSysNode);
            String kcalVal = context.getString(R.string.config_kcalValSysNode);
            String kcalCont = context.getString(R.string.config_kcalContSysNode);
            String kcalHue = context.getString(R.string.config_kcalHueSysNode);

            FileUtils.setValue(kcalRed, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_RED, RED_DEFAULT));
            FileUtils.setValue(kcalGreen, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_GREEN, GREEN_DEFAULT));
            FileUtils.setValue(kcalBlue, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_BLUE, BLUE_DEFAULT));
            FileUtils.setValue(kcalSat, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_GRAYSCALE, 0) == 1 ? 0 :
                    Settings.Secure.getInt(context.getContentResolver(),
                            PREF_SATURATION, SATURATION_DEFAULT));
            FileUtils.setValue(kcalVal, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_VALUE, VALUE_DEFAULT));
            FileUtils.setValue(kcalCont, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_CONTRAST, CONTRAST_DEFAULT));
            FileUtils.setValue(kcalHue, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_HUE, HUE_DEFAULT));
        }

        // KCAL watchdog — restores KCAL on display mode switch (resolution change, etc.)
        // Screen on/off restore handled by kernel
        KcalService.startService(context);

        // Dirac
        try {
            DiracUtils.getInstance(context);
        } catch (Exception e) {
            Log.d(TAG, "Dirac is not present in system");
        }

        // Thermal Profiles
        ThermalUtils.startService(context);

        // Per-App-RR
        RefreshUtils.startService(context);

        // Per-App-Resolution
        ResolutionUtils.startService(context);

        // FPS Info
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("fps_info", false)) {
            context.startService(new Intent(context, FPSInfoService.class));
        }
    }
}
