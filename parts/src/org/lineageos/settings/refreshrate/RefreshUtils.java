/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;

import android.provider.Settings;
import android.view.OrientationEventListener;
import android.content.res.Configuration;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public final class RefreshUtils {

    private static final String REFRESH_CONTROL = "refresh_control";

    private float defaultMaxRate;
    private float defaultMinRate;
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String KEY_MIUI_REFRESH_RATE = "miui_refresh_rate";
    private static final String KEY_USER_REFRESH_RATE = "user_refresh_rate";
    private static final String KEY_OPLUS_REFRESH_RATE = "oplus_customize_screen_refresh_rate";

    private Context mContext;
    protected boolean isAppInList = false;

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_STANDARD = 1;
    protected static final int STATE_EXTREME = 2;
    protected static final int STATE_LAND = 3;

    private static final float REFRESH_STATE_DEFAULT = 120f;
    private static final float REFRESH_STATE_STANDARD = 60f;
    private static final float REFRESH_STATE_EXTREME = 120f;
    private static final float REFRESH_STATE_LAND = 60f;

    private static final String REFRESH_STANDARD = "refresh.standard=";
    private static final String REFRESH_EXTREME = "refresh.extreme=";
    private static final String REFRESH_LAND = "refresh.land=";

    private SharedPreferences mSharedPrefs;

    private OrientationEventListener orientationListener;
    private boolean isLandscape = false;

    protected RefreshUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context.getApplicationContext();
        getOldRate();
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, RefreshService.class),
                UserHandle.CURRENT);
    }

    protected void cleanup() {
        if (orientationListener != null) {
            orientationListener.disable();
            orientationListener = null;
        }
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply();
    }

    protected void getOldRate() {
        defaultMaxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        defaultMinRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        if (defaultMaxRate == 0.0f) {
            int oplusRate = Settings.Secure.getInt(mContext.getContentResolver(), KEY_OPLUS_REFRESH_RATE, 0);
            if (oplusRate == 2) {
                defaultMaxRate = REFRESH_STATE_STANDARD;
            } else if (oplusRate == 3) {
                defaultMaxRate = REFRESH_STATE_EXTREME;
            } else {
                int miuiRate = Settings.Secure.getInt(mContext.getContentResolver(), KEY_MIUI_REFRESH_RATE, 0);
                if (miuiRate > 0) {
                    defaultMaxRate = (float) miuiRate;
                } else {
                    defaultMaxRate = REFRESH_STATE_DEFAULT;
                }
            }
        }
        if (defaultMinRate == 0.0f) {
            int userRate = Settings.Secure.getInt(mContext.getContentResolver(), KEY_USER_REFRESH_RATE, 0);
            if (userRate > 0) {
                defaultMinRate = (float) userRate;
            } else {
                defaultMinRate = REFRESH_STATE_DEFAULT;
            }
        }
    }

    private float getUserMaxRefreshRate() {
        return Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }

    private float getUserMinRefreshRate() {
        return Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }

    private void initializeOrientationListener(String packageName) {
        if (orientationListener != null) {
            orientationListener.disable();
        }

        orientationListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }

                int currentOrientation = mContext.getResources().getConfiguration().orientation;
                boolean newIsLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
                if (newIsLandscape != isLandscape) {
                    isLandscape = newIsLandscape;
                    adjustRefreshRateForOrientation(packageName);
                }
            }
        };

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        } else {
            orientationListener.disable();
        }
    }

    private void adjustRefreshRateForOrientation(String packageName) {
        float minRate = defaultMinRate;
        float maxRate = (isLandscape && isAppInList) ? REFRESH_STATE_LAND : REFRESH_STATE_EXTREME;
        setRefreshRate(minRate, maxRate);
    }

    protected void checkOrientationAndSetRate(String packageName) {
        int currentOrientation = mContext.getResources().getConfiguration().orientation;
        isLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
        adjustRefreshRateForOrientation(packageName);
    }

    private void setLandscapeModeRefreshRate() {
        setRefreshRate(REFRESH_STATE_LAND, REFRESH_STATE_LAND);
    }

    private void setPortraitModeRefreshRate() {
        setRefreshRate(REFRESH_STATE_EXTREME, REFRESH_STATE_EXTREME);
    }

    private int getOplusRefreshRateValue(float rate) {
        return (rate <= REFRESH_STATE_STANDARD) ? 2 : 3;
    }

    private void setRefreshRate(float minRate, float maxRate) {
        if (minRate > maxRate) {
            minRate = maxRate;
        }
        Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxRate);
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_USER_REFRESH_RATE, (int) maxRate);
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MIUI_REFRESH_RATE, (int) maxRate);
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_OPLUS_REFRESH_RATE, getOplusRefreshRateValue(maxRate));
    }

    private String getValue() {
        String value = mSharedPrefs.getString(REFRESH_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = REFRESH_STANDARD + ":" + REFRESH_EXTREME + ":" + REFRESH_LAND;
            writeValue(value);
        }

        String[] modes = value.split(":", -1);
        if (modes.length < 3) {
            modes = new String[] {
                modes.length > 0 ? modes[0] : REFRESH_STANDARD,
                modes.length > 1 ? modes[1] : REFRESH_EXTREME,
                modes.length > 2 ? modes[2] : REFRESH_LAND
            };
            value = String.join(":", modes);
            writeValue(value);
        }
        return value;
    }

    private List<String> parsePackages(String section) {
        List<String> list = new ArrayList<>();
        if (section == null || section.isEmpty()) {
            return list;
        }
        int eqIndex = section.indexOf('=');
        String content = (eqIndex >= 0) ? section.substring(eqIndex + 1) : section;
        for (String pkg : content.split(",")) {
            String trimmed = pkg.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    protected void writePackage(String packageName, int mode) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        String value = getValue();
        String[] modes = value.split(":", -1);
        List<String> standardList = (modes.length > 0) ? parsePackages(modes[0]) : new ArrayList<>();
        List<String> extremeList = (modes.length > 1) ? parsePackages(modes[1]) : new ArrayList<>();
        List<String> landList = (modes.length > 2) ? parsePackages(modes[2]) : new ArrayList<>();

        standardList.remove(packageName);
        extremeList.remove(packageName);
        landList.remove(packageName);

        switch (mode) {
            case STATE_STANDARD:
                standardList.add(packageName);
                break;
            case STATE_EXTREME:
                extremeList.add(packageName);
                break;
            case STATE_LAND:
                landList.add(packageName);
                break;
            case STATE_DEFAULT:
            default:
                break;
        }

        String newStandard = REFRESH_STANDARD + (standardList.isEmpty() ? "" : String.join(",", standardList) + ",");
        String newExtreme = REFRESH_EXTREME + (extremeList.isEmpty() ? "" : String.join(",", extremeList) + ",");
        String newLand = REFRESH_LAND + (landList.isEmpty() ? "" : String.join(",", landList) + ",");

        String finalString = newStandard + ":" + newExtreme + ":" + newLand;
        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return STATE_DEFAULT;
        }
        String value = getValue();
        String[] modes = value.split(":", -1);
        if (modes.length > 0 && parsePackages(modes[0]).contains(packageName)) {
            return STATE_STANDARD;
        }
        if (modes.length > 1 && parsePackages(modes[1]).contains(packageName)) {
            return STATE_EXTREME;
        }
        if (modes.length > 2 && parsePackages(modes[2]).contains(packageName)) {
            return STATE_LAND;
        }
        return STATE_DEFAULT;
    }

    protected void setRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);
        float maxRate = defaultMaxRate;
        float minRate = defaultMinRate;
        isAppInList = false;

        if (state == STATE_STANDARD) {
            cleanup();
            maxRate = REFRESH_STATE_STANDARD;
            isAppInList = true;
        } else if (state == STATE_EXTREME) {
            cleanup();
            maxRate = REFRESH_STATE_EXTREME;
            isAppInList = true;
        } else if (state == STATE_LAND) {
            isAppInList = true;
            initializeOrientationListener(packageName);
            checkOrientationAndSetRate(packageName);
            return;
        } else {
            // STATE_DEFAULT: Not in list
            cleanup();
        }

        setRefreshRate(minRate, maxRate);
    }
}
