/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.settings.display;

public final class DcDimmingUtils {
    public static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    public static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dimlayer_exposure";
    public static final String HBM_NODE = "/sys/devices/platform/soc/5e00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm";

    private DcDimmingUtils() {
        // Utility class
    }
}
