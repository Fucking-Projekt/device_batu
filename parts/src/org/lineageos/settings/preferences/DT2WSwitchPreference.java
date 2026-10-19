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

package org.lineageos.settings.preferences;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DT2WSwitchPreference extends SwitchPreference {

    private static final String TAG = "DT2WSwitchPreference";
    public static final String FTS_GESTURE_NODE =
            "/sys/devices/platform/soc/4c88000.i2c/i2c-2/2-0038/fts_gesture_mode";
    public static final String SETTING_OPLUS_GESTURE =
            "oplus_customize_gesture_double_touch";

    public DT2WSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public DT2WSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DT2WSwitchPreference(Context context) {
        super(context, null);
        init();
    }

    private void init() {
        setPersistent(false);
        if (!isSupported()) {
            setEnabled(false);
            setSummary(R.string.sysfs_node_unavailable);
        }
    }

    public static boolean isSupported() {
        return FileUtils.fileExists(FTS_GESTURE_NODE);
    }

    public static boolean readFromSysfs() {
        File file = new File(FTS_GESTURE_NODE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.contains("Gesture Mode:On") || line.endsWith(":On") || line.equals("1")) {
                        return true;
                    } else if (line.contains("Gesture Mode:Off") || line.endsWith(":Off") || line.equals("0")) {
                        return false;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read from " + FTS_GESTURE_NODE, e);
            }
        }
        return false;
    }

    public static void writeToSysfs(boolean enabled) {
        String value = enabled ? "1" : "0";
        if (FileUtils.fileExists(FTS_GESTURE_NODE)) {
            FileUtils.setValue(FTS_GESTURE_NODE, value);
        }
    }

    public static void syncSettings(Context context, boolean enabled) {
        int intValue = enabled ? 1 : 0;
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_WAKE, intValue);
        Settings.Secure.putInt(context.getContentResolver(),
                SETTING_OPLUS_GESTURE, intValue);
    }

    public void updateState() {
        int value = Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_WAKE, -1);
        if (value == -1) {
            value = Settings.Secure.getInt(getContext().getContentResolver(),
                    SETTING_OPLUS_GESTURE, -1);
        }

        boolean enabled;
        if (value != -1) {
            enabled = (value == 1);
            // Ensure sysfs matches secure setting
            if (readFromSysfs() != enabled) {
                writeToSysfs(enabled);
            }
        } else {
            // Read from sysfs node ("Gesture Mode:On" / "Gesture Mode:Off")
            enabled = readFromSysfs();
            syncSettings(getContext(), enabled);
        }

        setChecked(enabled);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        updateState();
    }

    @Override
    protected void onClick() {
        boolean newValue = !isChecked();
        if (callChangeListener(newValue)) {
            setChecked(newValue);
            writeToSysfs(newValue);
            syncSettings(getContext(), newValue);
        }
    }
}
