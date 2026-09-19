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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import vendor.lineage.health.ChargingControlSupportedMode;
import vendor.lineage.health.ChargingLimitInfo;
import vendor.lineage.health.IChargingControl;

public class ChargingUtils {

    private static final String TAG = "ChargingUtils";
    private static final String SERVICE_NAME = "vendor.lineage.health.IChargingControl/default";

    public static final String KEY_CHARGING_CONTROL_ENABLED = "charging_control_enabled";
    public static final String KEY_CHARGING_CONTROL_MODE = "charging_control_mode";
    public static final String KEY_CHARGING_CONTROL_LIMIT = "charging_control_limit";
    public static final String KEY_CHARGING_CONTROL_BYPASS = "charging_control_bypass";
    public static final String KEY_CHARGING_CONTROL_RESET_LIMIT = "charging_control_reset_limit";
    public static final String KEY_CHARGING_CONTROL_RESET_TIME = "charging_control_reset_time";

    public static final String MODE_AUTO = "auto";
    public static final String MODE_LIMIT = "limit";

    private static ChargingUtils sInstance;

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;
    private IChargingControl mService;
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "IChargingControl service died");
            mService = null;
        }
    };

    private ChargingUtils(Context context) {
        mContext = context.getApplicationContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public static synchronized ChargingUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ChargingUtils(context);
        }
        return sInstance;
    }

    public Context getContext() {
        return mContext;
    }

    private synchronized IChargingControl getService() {
        if (mService != null && mService.asBinder().isBinderAlive()) {
            return mService;
        }

        IBinder binder = ServiceManager.getService(SERVICE_NAME);
        if (binder == null) {
            binder = ServiceManager.waitForDeclaredService(SERVICE_NAME);
        }

        if (binder != null) {
            mService = IChargingControl.Stub.asInterface(binder);
            try {
                binder.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to link death recipient", e);
            }
        } else {
            Log.e(TAG, "Failed to find " + SERVICE_NAME);
        }

        return mService;
    }

    public boolean isSupported() {
        return getService() != null;
    }

    public boolean setChargingEnabled(boolean enable) {
        IChargingControl service = getService();
        if (service == null) {
            Log.e(TAG, "setChargingEnabled failed: service is null");
            return false;
        }

        try {
            service.setChargingEnabled(enable);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set charging enabled", e);
            return false;
        }
    }

    public boolean getChargingEnabled() {
        IChargingControl service = getService();
        if (service == null) {
            return true;
        }

        try {
            return service.getChargingEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get charging enabled", e);
            return true;
        }
    }

    public int getSupportedMode() {
        IChargingControl service = getService();
        if (service == null) {
            return 0;
        }

        try {
            return service.getSupportedMode();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get supported mode", e);
            return 0;
        }
    }

    public boolean isLimitModeSupported() {
        return (getSupportedMode() & ChargingControlSupportedMode.LIMIT) != 0;
    }

    public boolean isBypassModeSupported() {
        return (getSupportedMode() & ChargingControlSupportedMode.BYPASS) != 0;
    }

    public boolean isEnabled() {
        return mSharedPrefs.getBoolean(KEY_CHARGING_CONTROL_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        mSharedPrefs.edit().putBoolean(KEY_CHARGING_CONTROL_ENABLED, enabled).apply();
    }

    public String getMode() {
        return mSharedPrefs.getString(KEY_CHARGING_CONTROL_MODE, MODE_LIMIT);
    }

    public void setMode(String mode) {
        mSharedPrefs.edit().putString(KEY_CHARGING_CONTROL_MODE, mode).apply();
    }

    public int getChargingLimit() {
        return mSharedPrefs.getInt(KEY_CHARGING_CONTROL_LIMIT, 80);
    }

    public void setChargingLimit(int limit) {
        mSharedPrefs.edit().putInt(KEY_CHARGING_CONTROL_LIMIT, limit).apply();
    }

    public boolean isBypassChargingEnabled() {
        return mSharedPrefs.getBoolean(KEY_CHARGING_CONTROL_BYPASS, false);
    }

    public void setBypassChargingEnabled(boolean enabled) {
        mSharedPrefs.edit().putBoolean(KEY_CHARGING_CONTROL_BYPASS, enabled).apply();
    }

    public int getResetLimit() {
        return Integer.parseInt(mSharedPrefs.getString(KEY_CHARGING_CONTROL_RESET_LIMIT, "5"));
    }

    public void setResetLimit(int limit) {
        mSharedPrefs.edit().putString(KEY_CHARGING_CONTROL_RESET_LIMIT, String.valueOf(limit)).apply();
    }

    public int getResetTime() {
        return Integer.parseInt(mSharedPrefs.getString(KEY_CHARGING_CONTROL_RESET_TIME, "120"));
    }

    public void setResetTime(int minutes) {
        mSharedPrefs.edit().putString(KEY_CHARGING_CONTROL_RESET_TIME, String.valueOf(minutes)).apply();
    }

    public static void checkService(Context context) {
        ChargingUtils utils = getInstance(context);
        Intent intent = new Intent(context, SmartChargingService.class);
        if (utils.isEnabled()) {
            context.startService(intent);
        } else {
            context.stopService(intent);
            utils.setChargingEnabled(true);
        }
    }
}
