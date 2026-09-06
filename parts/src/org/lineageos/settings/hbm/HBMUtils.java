/*
 * Copyright (C) 2016-2022 The OmniROM Project
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

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public final class HBMUtils {
    public static final String HBM_NODE = "/sys/devices/platform/soc/5e00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm";
    public static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";

    public static final String KEY_HBM = "hbm";
    public static final String KEY_AUTO_HBM = "auto_hbm";
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_HBM_DISABLE_TIME = "hbm_disable_time";

    private static boolean sServiceEnabled = false;

    private HBMUtils() {
        // Utility class
    }

    public static void startService(Context context) {
        if (context == null) return;
        context.startServiceAsUser(new Intent(context, AutoHBMService.class), UserHandle.CURRENT);
        sServiceEnabled = true;
    }

    public static void stopService(Context context) {
        if (context == null) return;
        sServiceEnabled = false;
        context.stopServiceAsUser(new Intent(context, AutoHBMService.class), UserHandle.CURRENT);
    }

    public static void enableService(Context context) {
        if (context == null) return;
        if (HBMFragment.isAUTOHBMEnabled(context) && !sServiceEnabled) {
            startService(context);
        } else if (!HBMFragment.isAUTOHBMEnabled(context) && sServiceEnabled) {
            stopService(context);
        }
    }
}
