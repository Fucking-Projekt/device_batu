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

package org.lineageos.settings.kcal;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class KcalService extends Service implements KcalController {

    private static final String TAG = "KcalService";
    private static final long RESTORE_DELAY_MS = 800;

    private IActivityTaskManager mActivityTaskManager;
    private Handler mHandler;
    private PowerManager mPowerManager;
    private boolean mRestoreScheduled;

    @Override
    public void onCreate() {
        mHandler = new Handler(Looper.getMainLooper());
        mPowerManager = getSystemService(PowerManager.class);
        try {
            mActivityTaskManager = ActivityTaskManager.getService();
            mActivityTaskManager.registerTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            // Do nothing
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            if (mActivityTaskManager != null) {
                mActivityTaskManager.unregisterTaskStackListener(mTaskListener);
            }
        } catch (RemoteException e) {
            // Do nothing
        }
        mHandler.removeCallbacksAndMessages(null);
        mRestoreScheduled = false;
    }

    public static void startService(Context context) {
        context.startService(new Intent(context, KcalService.class));
    }

    private void scheduleRestore() {
        if (mRestoreScheduled) {
            return;
        }
        mRestoreScheduled = true;
        mHandler.postDelayed(() -> {
            mRestoreScheduled = false;
            restoreKcal();
        }, RESTORE_DELAY_MS);
    }

    private void restoreKcal() {
        if (!mPowerManager.isScreenOn()) {
            return;
        }

        String kcalRed = getString(R.string.config_kcalRedSysNode);
        String kcalGreen = getString(R.string.config_kcalGreenSysNode);
        String kcalBlue = getString(R.string.config_kcalBlueSysNode);
        String kcalSat = getString(R.string.config_kcalSatSysNode);
        String kcalVal = getString(R.string.config_kcalValSysNode);
        String kcalCont = getString(R.string.config_kcalContSysNode);
        String kcalHue = getString(R.string.config_kcalHueSysNode);

        FileUtils.setValue(kcalRed, Settings.Secure.getInt(getContentResolver(),
                PREF_RED, RED_DEFAULT));
        FileUtils.setValue(kcalGreen, Settings.Secure.getInt(getContentResolver(),
                PREF_GREEN, GREEN_DEFAULT));
        FileUtils.setValue(kcalBlue, Settings.Secure.getInt(getContentResolver(),
                PREF_BLUE, BLUE_DEFAULT));
        FileUtils.setValue(kcalSat, Settings.Secure.getInt(getContentResolver(),
                PREF_GRAYSCALE, 0) == 1 ? 0 :
                        Settings.Secure.getInt(getContentResolver(),
                                PREF_SATURATION, SATURATION_DEFAULT));
        FileUtils.setValue(kcalVal, Settings.Secure.getInt(getContentResolver(),
                PREF_VALUE, VALUE_DEFAULT));
        FileUtils.setValue(kcalCont, Settings.Secure.getInt(getContentResolver(),
                PREF_CONTRAST, CONTRAST_DEFAULT));
        FileUtils.setValue(kcalHue, Settings.Secure.getInt(getContentResolver(),
                PREF_HUE, HUE_DEFAULT));
    }

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            scheduleRestore();
        }
    };
}
