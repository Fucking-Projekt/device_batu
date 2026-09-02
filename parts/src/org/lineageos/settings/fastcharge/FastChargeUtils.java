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

package org.lineageos.settings.fastcharge;

import android.content.Context;
import android.util.Log;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class FastChargeUtils {

    private static final String TAG = "FastChargeUtils";
    private final String mBypassChargeNode;

    public FastChargeUtils(Context context) {
        mBypassChargeNode = context.getString(R.string.config_bypassChargeSysNode);
    }

    public boolean isBypassChargeEnabled() {
        try {
            String value = FileUtils.readOneLine(mBypassChargeNode);
            return value != null && value.equals("0");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read bypass charge status", e);
            return false;
        }
    }

    public void enableBypassCharge(boolean enable) {
        try {
            FileUtils.writeLine(mBypassChargeNode, enable ? "0" : "1");
        } catch (Exception e) {
            Log.e(TAG, "Failed to write bypass charge status", e);
        }
    }

    public boolean isBypassChargeSupported() {
        return mBypassChargeNode != null && FileUtils.isFileReadable(mBypassChargeNode);
    }
}
