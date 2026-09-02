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
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class SysfsSwitchPreference extends SwitchPreference {

    private String mSysfsPath;
    private boolean mInvertValue;

    public SysfsSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SysfsSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SysfsSwitchPreference(Context context) {
        super(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        setPersistent(false);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.SysfsSwitchPreference);
            try {
                mSysfsPath = a.getString(R.styleable.SysfsSwitchPreference_sysfsPath);
                mInvertValue = a.getBoolean(
                        R.styleable.SysfsSwitchPreference_invertValue, false);
            } finally {
                a.recycle();
            }
        }

        if (mSysfsPath == null || !FileUtils.isFileReadable(mSysfsPath)) {
            setEnabled(false);
            setSummary(R.string.sysfs_node_unavailable);
        }
    }

    public String getSysfsPath() {
        return mSysfsPath;

    }

    public boolean isSupported() {
        return mSysfsPath != null && FileUtils.isFileReadable(mSysfsPath);
    }

    public boolean readFromSysfs() {
        if (mSysfsPath == null) return false;
        String value = FileUtils.readOneLine(mSysfsPath);
        if (value == null) return false;
        boolean on = value.trim().equals(mInvertValue ? "0" : "1");
        setChecked(on);
        return true;
    }

    public void writeToSysfs(boolean value) {
        if (mSysfsPath == null || !FileUtils.isFileWritable(mSysfsPath)) return;
        String writeValue = (mInvertValue ? !value : value) ? "1" : "0";
        FileUtils.writeLine(mSysfsPath, writeValue);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        readFromSysfs();
    }

    @Override
    protected void onClick() {
        boolean newValue = !isChecked();
        if (callChangeListener(newValue)) {
            writeToSysfs(newValue);
            setChecked(newValue);
        }
    }
}
