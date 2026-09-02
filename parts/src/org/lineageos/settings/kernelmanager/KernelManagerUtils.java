/*
 * Copyright (C) 2025 KamiKaonashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.kernelmanager;

import org.lineageos.settings.utils.FileUtils;

public class KernelManagerUtils {

    public static final int EFFICIENCY_CLUSTER = 0;
    public static final int PERFORMANCE_CLUSTER = 6;

    private static final int[] POLICIES = {EFFICIENCY_CLUSTER, PERFORMANCE_CLUSTER};
    private static final String DEFAULT_GOVERNOR = "schedutil";

    private static final String CPU_BASE_PATH = "/sys/devices/system/cpu/cpufreq/policy";
    private static final String SCALING_GOVERNOR = "/scaling_governor";
    private static final String SCALING_MIN_FREQ = "/scaling_min_freq";
    private static final String SCALING_MAX_FREQ = "/scaling_max_freq";
    private static final String SCALING_AVAILABLE_GOVERNORS = "/scaling_available_governors";
    private static final String SCALING_AVAILABLE_FREQUENCIES = "/scaling_available_frequencies";

    public String[] getAvailableGovernors() {
        String governors = FileUtils.readOneLine(
                CPU_BASE_PATH + EFFICIENCY_CLUSTER + SCALING_AVAILABLE_GOVERNORS);
        if (governors != null) {
            return governors.trim().split("\\s+");
        }
        return new String[]{"schedutil", "performance", "powersave", "ondemand", "conservative"};
    }

    public String[] getAvailableFrequencies(int cluster) {
        String frequencies = FileUtils.readOneLine(
                CPU_BASE_PATH + cluster + SCALING_AVAILABLE_FREQUENCIES);
        if (frequencies != null) {
            return frequencies.trim().split("\\s+");
        }
        return null;
    }

    public String getCurrentGovernor(int cluster) {
        String governor = FileUtils.readOneLine(CPU_BASE_PATH + cluster + SCALING_GOVERNOR);
        return governor != null ? governor.trim() : DEFAULT_GOVERNOR;
    }

    public String getCurrentMinFrequency(int cluster) {
        String freq = FileUtils.readOneLine(CPU_BASE_PATH + cluster + SCALING_MIN_FREQ);
        if (freq != null) {
            return freq.trim();
        }
        String[] frequencies = getAvailableFrequencies(cluster);
        if (frequencies != null && frequencies.length > 0) {
            return frequencies[0];
        }
        return "0";
    }

    public String getCurrentMaxFrequency(int cluster) {
        String freq = FileUtils.readOneLine(CPU_BASE_PATH + cluster + SCALING_MAX_FREQ);
        if (freq != null) {
            return freq.trim();
        }
        String[] frequencies = getAvailableFrequencies(cluster);
        if (frequencies != null && frequencies.length > 0) {
            return frequencies[frequencies.length - 1];
        }
        return "0";
    }

    public void setGovernor(String governor) {
        for (int cluster : POLICIES) {
            FileUtils.writeLine(CPU_BASE_PATH + cluster + SCALING_GOVERNOR, governor);
        }
    }

    public void setFrequencyRange(int cluster, String minFreq, String maxFreq) {
        FileUtils.writeLine(CPU_BASE_PATH + cluster + SCALING_MIN_FREQ, minFreq);
        FileUtils.writeLine(CPU_BASE_PATH + cluster + SCALING_MAX_FREQ, maxFreq);
    }

    public void setEfficiencyClusterFrequency(String minFreq, String maxFreq) {
        setFrequencyRange(EFFICIENCY_CLUSTER, minFreq, maxFreq);
    }

    public void setPerformanceClusterFrequency(String minFreq, String maxFreq) {
        setFrequencyRange(PERFORMANCE_CLUSTER, minFreq, maxFreq);
    }

    public void resetToDefaults() {
        setGovernor(DEFAULT_GOVERNOR);
        for (int cluster : POLICIES) {
            String[] frequencies = getAvailableFrequencies(cluster);
            if (frequencies != null && frequencies.length > 0) {
                setFrequencyRange(cluster, frequencies[0], frequencies[frequencies.length - 1]);
            }
        }
    }
}
