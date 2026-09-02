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
    private static final String FPS_SYSFS_NODE =
            "/sys/devices/platform/soc/5e00000.qcom,mdss_mdp/drm/card0/sde-crtc-0/measured_fps";
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final int BACKGROUND_ALPHA = 120;

    private WindowManager mWindowManager;
    private TextView mFpsView;
    private Configuration mConfiguration;
    private Handler mMainHandler;
    private Runnable mFpsRunnable;
    private RandomAccessFile mFpsNode;
    private boolean mRunning = false;

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

        File file = new File(FPS_SYSFS_NODE);
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "FPS sysfs node not available: " + FPS_SYSFS_NODE);
            stopSelf();
            return;
        }
        try {
            mFpsNode = new RandomAccessFile(FPS_SYSFS_NODE, "r");
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
                }
                mMainHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
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
        removeView();
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
        if (mRunning) return;
        mRunning = true;
        addView();
        mMainHandler.post(mFpsRunnable);
    }

    private void stopReading() {
        mRunning = false;
        mMainHandler.removeCallbacks(mFpsRunnable);
        removeView();
    }

    private void addView() {
        if (mFpsView != null && mFpsView.getParent() == null) {
            mWindowManager.addView(mFpsView, mLayoutParams);
        }
    }

    private void removeView() {
        if (mFpsView != null && mFpsView.getParent() != null) {
            mWindowManager.removeViewImmediate(mFpsView);
        }
    }

    private int measureFps() {
        if (mFpsNode == null) return -1;
        try {
            mFpsNode.seek(0L);
            String measuredFps = mFpsNode.readLine();
            if (measuredFps == null) return -1;
            String trimmed = measuredFps.trim();
            // Handle "X: Y" format (some kernels output "crtc: fps_value")
            String fpsStr = trimmed.contains(": ")
                    ? trimmed.split("\\s+")[1] : trimmed;
            return Math.round(Float.parseFloat(fpsStr));
        } catch (IOException e) {
            Log.e(TAG, "IOException reading FPS node: " + e.getMessage());
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException parsing FPS: " + e.getMessage());
        }
        return -1;
    }
}
