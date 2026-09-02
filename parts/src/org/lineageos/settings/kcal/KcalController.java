package org.lineageos.settings.kcal;

public interface KcalController {

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
}
