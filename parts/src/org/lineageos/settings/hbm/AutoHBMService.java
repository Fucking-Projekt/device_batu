/*
 * Copyright (C) 2016 The OmniROM Project
 * Copyright (C) 2022 The Evolution X Project
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

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.display.DcDimmingTileService;
import org.lineageos.settings.utils.FileUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoHBMService extends Service {

    private static boolean mAutoHBMActive = false;
    private ExecutorService mExecutorService;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private SharedPreferences mSharedPrefs;

    public void activateLightSensorRead() {
        submit(() -> {
            mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager != null) {
                mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                if (mLightSensor != null) {
                    mSensorManager.registerListener(mSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        });
    }

    public void deactivateLightSensorRead() {
        submit(() -> {
            if (mSensorManager != null) {
                mSensorManager.unregisterListener(mSensorEventListener);
            }
            mAutoHBMActive = false;
            enableHBM(false);
        });
    }

    private void enableHBM(boolean enable) {
        if (enable) {
            FileUtils.writeLine(HBMUtils.HBM_NODE, "1");
            FileUtils.writeLine(HBMUtils.BACKLIGHT_NODE, "2047");
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            FileUtils.writeLine(HBMUtils.HBM_NODE, "0");
        }
    }

    private boolean isCurrentlyEnabled() {
        return FileUtils.getFileValueAsBoolean(HBMUtils.HBM_NODE, false);
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            KeyguardManager km =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean keyguardShowing = km != null && km.inKeyguardRestrictedInputMode();
            float luxThreshold = Float.parseFloat(mSharedPrefs.getString(HBMUtils.KEY_AUTO_HBM_THRESHOLD, "20000"));
            long timeToDisableHBM = Long.parseLong(mSharedPrefs.getString(HBMUtils.KEY_HBM_DISABLE_TIME, "1"));
            boolean dcDimmingEnabled = mSharedPrefs.getBoolean(DcDimmingTileService.DC_DIMMING_ENABLE_KEY, false);

            if (lux > luxThreshold) {
                if ((!mAutoHBMActive || !isCurrentlyEnabled()) && !keyguardShowing && !dcDimmingEnabled) {
                    mAutoHBMActive = true;
                    enableHBM(true);
                }
            }
            if (lux < luxThreshold) {
                if (mAutoHBMActive) {
                    mExecutorService.submit(() -> {
                        try {
                            Thread.sleep(timeToDisableHBM * 1000);
                        } catch (InterruptedException ignored) {
                        }
                        if (lux < luxThreshold) {
                            mAutoHBMActive = false;
                            enableHBM(false);
                        }
                    });
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                activateLightSensorRead();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                deactivateLightSensorRead();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutorService = Executors.newSingleThreadExecutor();
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isInteractive()) {
            activateLightSensorRead();
        }
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isInteractive()) {
            deactivateLightSensorRead();
        }
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
