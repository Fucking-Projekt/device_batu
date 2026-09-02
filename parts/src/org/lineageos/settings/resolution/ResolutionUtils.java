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

package org.lineageos.settings.resolution;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.RemoteException;
import android.os.UserHandle;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import androidx.preference.PreferenceManager;

public final class ResolutionUtils {

    private static final String TAG = "ResolutionUtils";

    // Per-app list buckets
    private static final String RESOLUTION_CONTROL = "resolutioncontrol";
    private static final String RESOLUTION_480P = "resolution.480p";
    private static final String RESOLUTION_540P = "resolution.540p";
    private static final String RESOLUTION_720P = "resolution.720p";
    private static final String RESOLUTION_1080P = "resolution.1080p";
    private static final String RESOLUTION_1_5K = "resolution.1.5k";
    private static final int BUCKET_COUNT = 5;

    // System-wide baseline state
    private static final String RESOLUTION_GLOBAL_STATE = "resolution.global_state";

    // States
    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_480P = 1;
    protected static final int STATE_540P = 2;
    protected static final int STATE_720P = 3;
    protected static final int STATE_1080P = 4;
    protected static final int STATE_1_5K = 5;

    // System resolution spinner: Default, 480p, 540p, 720p, 1.5K (no 1080p — same as Default)
    protected static final int[] SYSTEM_STATE_MAP = {
            STATE_DEFAULT,
            STATE_480P,
            STATE_540P,
            STATE_720P,
            STATE_1_5K
    };

    protected boolean isAppInList = false;

    // State tracking to prevent aggressive resets
    private int mCurrentState = STATE_DEFAULT;
    private int mSavedUserDensity = -1;

    private static class ResolutionConfig {
        int width;
        int height;
        int density; // integer DPI

        ResolutionConfig(int w, int h, int d) {
            width = w;
            height = h;
            density = d;
        }
    }

    private static ResolutionConfig[] RESOLUTION_CONFIGS = new ResolutionConfig[6];

    private final SharedPreferences mSharedPrefs;
    private final Context mContext;

    // Native baselines from physical mode and initial density
    private int mStockWidth;
    private int mStockHeight;
    private int mInitialDensity;

    protected ResolutionUtils(Context context) {
        mContext = context.getApplicationContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        initializeStockBaselines();
        calculateResolutionConfigs();
        mCurrentState = getCurrentActualState();
    }

    public static void startService(Context context) {
        new ResolutionUtils(context).applyBaselineFromGlobal();
        context.startServiceAsUser(new Intent(context, ResolutionService.class), UserHandle.CURRENT);
    }

    private void initializeStockBaselines() {
        DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        Display d = dm.getDisplay(Display.DEFAULT_DISPLAY);
        Display.Mode mode = d.getMode();
        mStockWidth = mode.getPhysicalWidth();
        mStockHeight = mode.getPhysicalHeight();

        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            mInitialDensity = wm.getInitialDisplayDensity(Display.DEFAULT_DISPLAY);
        } catch (RemoteException e) {
            mInitialDensity = 440;
        }
    }

    private void calculateResolutionConfigs() {
        // Scale by target WIDTH; keep native aspect ratio and scale DPI proportionally
        RESOLUTION_CONFIGS[STATE_DEFAULT] =
                new ResolutionConfig(mStockWidth, mStockHeight, mInitialDensity);

        int w480 = 480;
        float s480 = (float) w480 / (float) mStockWidth;
        int h480 = Math.max(1, Math.round(mStockHeight * s480));
        int d480 = Math.max(120, Math.round(mInitialDensity * s480));
        RESOLUTION_CONFIGS[STATE_480P] = new ResolutionConfig(w480, h480, d480);

        int w540 = 540;
        float s540 = (float) w540 / (float) mStockWidth;
        int h540 = Math.max(1, Math.round(mStockHeight * s540));
        int d540 = Math.max(120, Math.round(mInitialDensity * s540));
        RESOLUTION_CONFIGS[STATE_540P] = new ResolutionConfig(w540, h540, d540);

        int w720 = 720;
        float s720 = (float) w720 / (float) mStockWidth;
        int h720 = Math.max(1, Math.round(mStockHeight * s720));
        int d720 = Math.max(120, Math.round(mInitialDensity * s720));
        RESOLUTION_CONFIGS[STATE_720P] = new ResolutionConfig(w720, h720, d720);

        // 1080p = native resolution (force regardless of global)
        RESOLUTION_CONFIGS[STATE_1080P] =
                new ResolutionConfig(mStockWidth, mStockHeight, mInitialDensity);

        // 1.5K = 1215x2700@495dpi (1.125x native)
        int w1_5k = 1215;
        float s1_5k = (float) w1_5k / (float) mStockWidth;
        int h1_5k = Math.max(1, Math.round(mStockHeight * s1_5k));
        int d1_5k = Math.max(120, Math.round(mInitialDensity * s1_5k));
        RESOLUTION_CONFIGS[STATE_1_5K] = new ResolutionConfig(w1_5k, h1_5k, d1_5k);
    }

    // ----- System-wide baseline -----

    public int getGlobalState() {
        return mSharedPrefs.getInt(RESOLUTION_GLOBAL_STATE, STATE_DEFAULT);
    }

    public void setGlobalState(int state) {
        if (state < STATE_DEFAULT || state > STATE_1_5K) state = STATE_DEFAULT;
        mSharedPrefs.edit().putInt(RESOLUTION_GLOBAL_STATE, state).apply();
        applyBaselineFromGlobal();
    }

    public void applyBaselineFromGlobal() {
        int s = getGlobalState();
        ResolutionConfig cfg = RESOLUTION_CONFIGS[s];
        applyResolution(cfg, s);
    }

    public void restoreDefaultResolution() {
        applyBaselineFromGlobal();
    }

    // ----- Per-app list management -----

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(RESOLUTION_CONTROL, profiles).apply();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(RESOLUTION_CONTROL, null);
        if (value == null || value.isEmpty()) {
            value = String.join(";", RESOLUTION_480P, RESOLUTION_540P,
                    RESOLUTION_720P, RESOLUTION_1080P, RESOLUTION_1_5K);
            writeValue(value);
        }
        String[] modes = value.split(";");
        if (modes.length < BUCKET_COUNT) {
            String[] defaults = {
                    RESOLUTION_480P, RESOLUTION_540P, RESOLUTION_720P,
                    RESOLUTION_1080P, RESOLUTION_1_5K
            };
            String[] fixed = new String[BUCKET_COUNT];
            for (int i = 0; i < BUCKET_COUNT; i++) {
                fixed[i] = modes.length > i ? modes[i] : defaults[i];
            }
            value = String.join(";", fixed);
            writeValue(value);
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(";");

        if (mode >= STATE_480P && mode < BUCKET_COUNT) {
            modes[mode - 1] = modes[mode - 1] + packageName + ",";
        }
        writeValue(String.join(";", modes));
    }

    protected int getStateForPackage(String packageName) {
        String[] modes = getValue().split(";");
        for (int i = 0; i < Math.min(modes.length, BUCKET_COUNT); i++) {
            if (modes[i].contains(packageName + ",")) {
                return i + 1; // bucket 0 → STATE_480P(1), bucket 1 → STATE_540P(2), etc.
            }
        }
        return STATE_DEFAULT;
    }

    protected void setResolution(String packageName) {
        String[] modes = getValue().split(";");
        int globalState = getGlobalState();
        int newState = globalState;

        isAppInList = false;
        for (int i = 0; i < Math.min(modes.length, BUCKET_COUNT); i++) {
            if (modes[i].contains(packageName + ",")) {
                newState = i + 1; // bucket index → state
                isAppInList = true;
                break;
            }
        }

        ResolutionConfig cfg = RESOLUTION_CONFIGS[newState];
        applyResolution(cfg, newState);
    }

    // ----- Low-level application -----

    private void applyResolution(ResolutionConfig cfg, int newState) {
        if (newState == mCurrentState) {
            return;
        }

        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            if (newState != STATE_DEFAULT) {
                if (mCurrentState == STATE_DEFAULT) {
                    mSavedUserDensity = wm.getBaseDisplayDensity(Display.DEFAULT_DISPLAY);
                }
                wm.setForcedDisplaySize(Display.DEFAULT_DISPLAY, cfg.width, cfg.height);
                wm.setForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, cfg.density, UserHandle.USER_CURRENT);
            } else {
                wm.clearForcedDisplaySize(Display.DEFAULT_DISPLAY);
                wm.clearForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, UserHandle.USER_CURRENT);
                mSavedUserDensity = -1;
            }
            mCurrentState = newState;
        } catch (RemoteException e) {
            // Swallow to avoid crashes
        }
    }

    public int getCurrentActualState() {
        DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
        Point realSize = new Point();
        display.getRealSize(realSize);

        int currentWidth = Math.min(realSize.x, realSize.y);
        for (int i = 0; i < RESOLUTION_CONFIGS.length; i++) {
            if (RESOLUTION_CONFIGS[i] != null && RESOLUTION_CONFIGS[i].width == currentWidth) {
                return i;
            }
        }
        return STATE_DEFAULT;
    }

    public String getResolutionString(int state) {
        ResolutionConfig c = (state >= 0 && state < RESOLUTION_CONFIGS.length) ? RESOLUTION_CONFIGS[state] : null;
        return c != null ? (c.width + "x" + c.height) : (mStockWidth + "x" + mStockHeight);
    }
}
