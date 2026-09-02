/*
 * Copyright (C) 2019-2024 crDroid Android Project
 * Copyright (C) 2025 LineageOS
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

package org.lineageos.settings.fps;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.lineageos.settings.R;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FPSInfoService extends Service {

    private static final String TAG = "FPSInfoService";
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final int BACKGROUND_ALPHA = 120;

    // Burn-in protection — zigzag pattern matching SystemUI BurnInHelper
    private static final long BURN_IN_UPDATE_INTERVAL_MS = 1000;
    private static final int BURN_IN_AMPLITUDE_X = 6;   // pixels
    private static final int BURN_IN_AMPLITUDE_Y = 14;  // pixels
    private static final float BURN_IN_PERIOD_X = 43f;   // minutes
    private static final float BURN_IN_PERIOD_Y = 271f;  // minutes
    private static final long BURN_IN_RAMP_UP_MS = 240_000L; // 4 minutes

    private WindowManager mWindowManager;
    private TextView mFpsView;
    private Configuration mConfiguration;
    private Handler mMainHandler;
    private Runnable mFpsRunnable;
    private Runnable mBurnInRunnable;
    private RandomAccessFile mFpsNode;
    private boolean mRunning = false;
    private long mBurnInStartTimeMs;

    private WindowManager.LayoutParams mLayoutParams;

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                startReading();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                stopReading();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        final String fpsSysfsNode = getString(R.string.config_fpsInfoSysNode);

        mMainHandler = new Handler(Looper.getMainLooper());
        mWindowManager = getSystemService(WindowManager.class);
        mConfiguration = getResources().getConfiguration();

        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mLayoutParams.y = getTopInset();

        File file = new File(fpsSysfsNode);
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "FPS sysfs node not available: " + fpsSysfsNode);
            stopSelf();
            return;
        }
        try {
            mFpsNode = new RandomAccessFile(fpsSysfsNode, "r");
        } catch (IOException e) {
            Log.e(TAG, "Failed to open FPS node: " + e.getMessage());
            stopSelf();
            return;
        }

        int padding = getResources().getDimensionPixelSize(R.dimen.fps_info_text_padding);
        mFpsView = new TextView(this);
        mFpsView.setBackgroundColor(ColorUtils.setAlphaComponent(Color.BLACK, BACKGROUND_ALPHA));
        mFpsView.setTextColor(Color.WHITE);
        mFpsView.setPadding(padding, padding, padding, padding);

        mFpsRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mRunning) return;
                int fps = measureFps();
                if (mFpsView != null) {
                    mFpsView.setText(getString(R.string.fps_text_placeholder, fps));
                    mFpsView.setVisibility(View.VISIBLE);
                }
                mMainHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        mBurnInRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mRunning || mFpsView == null) return;
                updateBurnInOffset();
                mMainHandler.postDelayed(this, BURN_IN_UPDATE_INTERVAL_MS);
            }
        };

        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int startId, int flags) {
        startReading();
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mConfiguration.orientation != newConfig.orientation) {
            mLayoutParams.y = getTopInset();
            if (mFpsView != null && mFpsView.getParent() != null) {
                mWindowManager.updateViewLayout(mFpsView, mLayoutParams);
            }
        }
        mConfiguration = newConfig;
    }

    @Override
    public void onDestroy() {
        stopReading();
        try {
            unregisterReceiver(mScreenStateReceiver);
        } catch (IllegalArgumentException e) {
            // Already unregistered
        }
        if (mFpsNode != null) {
            try {
                mFpsNode.close();
            } catch (IOException e) {
                // Ignore
            }
            mFpsNode = null;
        }
        removeFpsViewIfNeeded();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getTopInset() {
        return mWindowManager.getCurrentWindowMetrics()
                .getWindowInsets()
                .getInsets(WindowInsets.Type.statusBars())
                .top;
    }

    private void startReading() {
        stopReading();
        mRunning = true;
        mBurnInStartTimeMs = System.currentTimeMillis();
        addView();
        mMainHandler.post(mFpsRunnable);
        mMainHandler.post(mBurnInRunnable);
    }

    private void stopReading() {
        mRunning = false;
        mMainHandler.removeCallbacks(mFpsRunnable);
        mMainHandler.removeCallbacks(mBurnInRunnable);
        // Reset translation so view doesn't stay offset
        if (mFpsView != null) {
            mFpsView.setTranslationX(0f);
            mFpsView.setTranslationY(0f);
        }
        removeFpsViewIfNeeded();
    }

    private void addView() {
        if (mFpsView != null && mFpsView.getParent() == null) {
            mWindowManager.addView(mFpsView, mLayoutParams);
        }
    }

    private void removeFpsViewIfNeeded() {
        if (mFpsView != null && mFpsView.getParent() != null) {
            mWindowManager.removeViewImmediate(mFpsView);
        }
    }

    // ----- Burn-in protection -----

    private void updateBurnInOffset() {
        long elapsed = System.currentTimeMillis() - mBurnInStartTimeMs;
        float rampFraction = Math.min(1f, (float) elapsed / (float) BURN_IN_RAMP_UP_MS);

        int ampX = Math.round(BURN_IN_AMPLITUDE_X * rampFraction);
        int ampY = Math.round(BURN_IN_AMPLITUDE_Y * rampFraction);

        int offsetX = getBurnInOffset(ampX, true) - ampX / 2;
        int offsetY = getBurnInOffset(ampY, false) - ampY / 2;

        mFpsView.setTranslationX(offsetX);
        mFpsView.setTranslationY(offsetY);
    }

    /**
     * Zigzag (triangle wave) based on wall-clock time.
     * Matches SystemUI BurnInHelper algorithm: periods are coprime so (x,y) rarely repeats.
     * Returns value in [0, amplitude].
     */
    private static int getBurnInOffset(int amplitude, boolean xAxis) {
        if (amplitude == 0) return 0;
        float minutes = System.currentTimeMillis() / 60_000f;
        float period = xAxis ? BURN_IN_PERIOD_X : BURN_IN_PERIOD_Y;
        float xprime = (minutes % period) / (period / 2f);
        float interp = (xprime <= 1f) ? xprime : 2f - xprime;
        return Math.round(interp * amplitude);
    }

    // ----- FPS measurement -----

    private int measureFps() {
        if (mFpsNode == null) return -1;
        String measuredFps;
        try {
            mFpsNode.seek(0L);
            measuredFps = mFpsNode.readLine();
        } catch (IOException e) {
            Log.e(TAG, "IOException accessing FPS node: " + e.getMessage());
            return -1;
        }
        if (measuredFps == null) return -1;
        String fpsStr = measuredFps.trim();
        if (fpsStr.contains(": ")) {
            String[] parts = fpsStr.split("\\s+");
            fpsStr = parts.length > 1 ? parts[1] : fpsStr;
        }
        try {
            return Math.round(Float.parseFloat(fpsStr));
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException parsing FPS: " + e.getMessage());
        }
        return -1;
    }
}
