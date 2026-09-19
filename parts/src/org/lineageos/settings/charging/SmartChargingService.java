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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

public class SmartChargingService extends Service {

    private static final String TAG = "SmartChargingService";

    public static final String ACTION_RE_EVALUATE =
            "org.lineageos.settings.charging.ACTION_RE_EVALUATE";

    private ChargingUtils mChargingUtils;
    private boolean mReceiverRegistered = false;
    private long mSuspendedTimestamp = 0;

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

                if (scale > 0 && level >= 0) {
                    int batteryPct = (level * 100) / scale;
                    handleBatteryUpdate(batteryPct, status, plugged != 0);
                }
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                handlePowerStateChanged(true);
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                handlePowerStateChanged(false);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mChargingUtils = ChargingUtils.getInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mBatteryReceiver, filter);
        mReceiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_RE_EVALUATE.equals(intent.getAction())) {
            reEvaluate();
        }
        return START_STICKY;
    }

    private void reEvaluate() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            if (scale > 0 && level >= 0) {
                handleBatteryUpdate((level * 100) / scale, -1, plugged != 0);
            }
        }
    }

    private void handlePowerStateChanged(boolean connected) {
        if (!connected) {
            // Restore charging when unplugged so the device will charge next time plugged in
            mChargingUtils.setChargingEnabled(true);
            mSuspendedTimestamp = 0;
        }
    }

    private void handleBatteryUpdate(int batteryPct, int status, boolean isPlugged) {
        if (!isPlugged) {
            mSuspendedTimestamp = 0;
            return;
        }

        if (!mChargingUtils.isEnabled()) {
            mChargingUtils.setChargingEnabled(true);
            return;
        }

        // Bypass charging mode overrides limit
        if (mChargingUtils.isBypassChargingEnabled()) {
            mChargingUtils.setChargingEnabled(false);
            return;
        }

        int limit = mChargingUtils.getChargingLimit();
        int resetLimit = mChargingUtils.getResetLimit();
        int resumeLevel = Math.max(0, limit - resetLimit);

        if (batteryPct >= limit) {
            mChargingUtils.setChargingEnabled(false);
            if (mSuspendedTimestamp == 0) {
                mSuspendedTimestamp = System.currentTimeMillis();
            }
        } else if (batteryPct <= resumeLevel) {
            mChargingUtils.setChargingEnabled(true);
            mSuspendedTimestamp = 0;
        } else {
            // Check time-based resume
            int resetTimeMinutes = mChargingUtils.getResetTime();
            if (resetTimeMinutes > 0 && mSuspendedTimestamp > 0) {
                long elapsedMinutes = (System.currentTimeMillis() - mSuspendedTimestamp) / 60000;
                if (elapsedMinutes >= resetTimeMinutes) {
                    mChargingUtils.setChargingEnabled(true);
                    mSuspendedTimestamp = 0;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiverRegistered) {
            unregisterReceiver(mBatteryReceiver);
            mReceiverRegistered = false;
        }
        // Safety: enable charging back when service is destroyed
        if (mChargingUtils != null) {
            mChargingUtils.setChargingEnabled(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
