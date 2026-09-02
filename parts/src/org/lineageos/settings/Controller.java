package org.lineageos.settings;

public interface Controller {

    // Kcal
    String PREF_SETONBOOT = "set_on_boot";
    String PREF_RED = "color_red";
    String PREF_GREEN = "color_green";
    String PREF_BLUE = "color_blue";
    String PREF_SATURATION = "saturation";
    String PREF_VALUE = "value";
    String PREF_CONTRAST = "contrast";
    String PREF_HUE = "hue";
    String PREF_GRAYSCALE = "grayscale";

    boolean SETONBOOT_DEFAULT = false;
    int RED_DEFAULT = 256;
    int GREEN_DEFAULT = 256;
    int BLUE_DEFAULT = 256;
    int SATURATION_DEFAULT = 255;
    int VALUE_DEFAULT = 255;
    int CONTRAST_DEFAULT = 255;
    int HUE_DEFAULT = 0;
    boolean GRAYSCALE_DEFAULT = false;

    String KCAL_CONT = "/sys/module/msm_drm/parameters/kcal_cont";
    String KCAL_HUE = "/sys/module/msm_drm/parameters/kcal_hue";
    String KCAL_RED = "/sys/module/msm_drm/parameters/kcal_red";
    String KCAL_GREEN = "/sys/module/msm_drm/parameters/kcal_green";
    String KCAL_BLUE = "/sys/module/msm_drm/parameters/kcal_blue";
    String KCAL_SAT = "/sys/module/msm_drm/parameters/kcal_sat";
    String KCAL_VAL = "/sys/module/msm_drm/parameters/kcal_val";
}
