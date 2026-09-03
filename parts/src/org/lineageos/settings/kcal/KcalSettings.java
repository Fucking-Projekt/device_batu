package org.lineageos.settings.kcal;

import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.preferences.SecureSettingSeekBarPreference;
import org.lineageos.settings.preferences.SecureSettingSwitchPreference;
import org.lineageos.settings.utils.FileUtils;

public class KcalSettings extends SettingsBasePreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, KcalController {

    private String mKcalRed;
    private String mKcalGreen;
    private String mKcalBlue;
    private String mKcalSat;
    private String mKcalVal;
    private String mKcalCont;
    private String mKcalHue;

    private SecureSettingSwitchPreference mSetOnBoot;
    private SecureSettingSeekBarPreference mRed;
    private SecureSettingSeekBarPreference mGreen;
    private SecureSettingSeekBarPreference mBlue;
    private SecureSettingSeekBarPreference mSaturation;
    private SecureSettingSeekBarPreference mValue;
    private SecureSettingSeekBarPreference mContrast;
    private SecureSettingSeekBarPreference mHue;
    private SecureSettingSwitchPreference mGrayscale;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.kcal_preferences, rootKey);

        mKcalRed = getString(R.string.config_kcalRedSysNode);
        mKcalGreen = getString(R.string.config_kcalGreenSysNode);
        mKcalBlue = getString(R.string.config_kcalBlueSysNode);
        mKcalSat = getString(R.string.config_kcalSatSysNode);
        mKcalVal = getString(R.string.config_kcalValSysNode);
        mKcalCont = getString(R.string.config_kcalContSysNode);
        mKcalHue = getString(R.string.config_kcalHueSysNode);

        mSetOnBoot = (SecureSettingSwitchPreference) findPreference(PREF_SETONBOOT);
        mSetOnBoot.setOnPreferenceChangeListener(this);

        mRed = (SecureSettingSeekBarPreference) findPreference(PREF_RED);
        mRed.setOnPreferenceChangeListener(this);

        mGreen = (SecureSettingSeekBarPreference) findPreference(PREF_GREEN);
        mGreen.setOnPreferenceChangeListener(this);

        mBlue = (SecureSettingSeekBarPreference) findPreference(PREF_BLUE);
        mBlue.setOnPreferenceChangeListener(this);

        mSaturation = (SecureSettingSeekBarPreference) findPreference(PREF_SATURATION);
        mSaturation.setOnPreferenceChangeListener(this);

        mValue = (SecureSettingSeekBarPreference) findPreference(PREF_VALUE);
        mValue.setOnPreferenceChangeListener(this);

        mContrast = (SecureSettingSeekBarPreference) findPreference(PREF_CONTRAST);
        mContrast.setOnPreferenceChangeListener(this);

        mHue = (SecureSettingSeekBarPreference) findPreference(PREF_HUE);
        mHue.setOnPreferenceChangeListener(this);

        mGrayscale = (SecureSettingSwitchPreference) findPreference(PREF_GRAYSCALE);
        mGrayscale.setOnPreferenceChangeListener(this);

        Preference mPresets = findPreference("kcal_presets");
        mPresets.setOnPreferenceClickListener(this);

        Preference mReset = findPreference("kcal_reset");
        mReset.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncFromSysfs();
    }

    private void syncFromSysfs() {
        int red = readSysfsInt(mKcalRed, PREF_RED, RED_DEFAULT);
        int green = readSysfsInt(mKcalGreen, PREF_GREEN, GREEN_DEFAULT);
        int blue = readSysfsInt(mKcalBlue, PREF_BLUE, BLUE_DEFAULT);
        int sat = readSysfsInt(mKcalSat, PREF_SATURATION, SATURATION_DEFAULT);
        int value = readSysfsInt(mKcalVal, PREF_VALUE, VALUE_DEFAULT);
        int contrast = readSysfsInt(mKcalCont, PREF_CONTRAST, CONTRAST_DEFAULT);
        int hue = readSysfsInt(mKcalHue, PREF_HUE, HUE_DEFAULT);
        boolean grayscale = sat == 0 && Settings.Secure.getInt(
                getContext().getContentResolver(), PREF_GRAYSCALE, 0) == 1;

        Settings.Secure.putInt(getContext().getContentResolver(), PREF_RED, red);
        Settings.Secure.putInt(getContext().getContentResolver(), PREF_GREEN, green);
        Settings.Secure.putInt(getContext().getContentResolver(), PREF_BLUE, blue);
        Settings.Secure.putInt(getContext().getContentResolver(), PREF_SATURATION, sat);
        Settings.Secure.putInt(getContext().getContentResolver(), PREF_VALUE, value);
        Settings.Secure.putInt(getContext().getContentResolver(), PREF_CONTRAST, contrast);
        Settings.Secure.putInt(getContext().getContentResolver(), PREF_HUE, hue);

        mRed.refresh(red);
        mGreen.refresh(green);
        mBlue.refresh(blue);
        mSaturation.refresh(sat);
        mValue.refresh(value);
        mContrast.refresh(contrast);
        mHue.refresh(hue);
        mGrayscale.setChecked(grayscale);
        mSaturation.setEnabled(!grayscale);
    }

    private int readSysfsInt(String sysfsPath, String prefKey, int defaultValue) {
        String line = FileUtils.readOneLine(sysfsPath);
        if (line != null) {
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                // Fall through to Settings.Secure
            }
        }
        return Settings.Secure.getInt(getContext().getContentResolver(), prefKey, defaultValue);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        if ("kcal_presets".equals(key)) {
            new PresetDialog().show(getParentFragmentManager(),
                    KcalSettings.class.getName(), this);
            return true;
        } else if ("kcal_reset".equals(key)) {
            applyValues(RED_DEFAULT + " " +
                    GREEN_DEFAULT + " " +
                    BLUE_DEFAULT + " " +
                    SATURATION_DEFAULT + " " +
                    VALUE_DEFAULT + " " +
                    CONTRAST_DEFAULT + " " +
                    HUE_DEFAULT);
            setmGrayscale(GRAYSCALE_DEFAULT);
            setmSetOnBoot(SETONBOOT_DEFAULT);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();

        switch (key) {
            case PREF_RED:
                FileUtils.setValue(mKcalRed, (int) value);
                break;

            case PREF_GREEN:
                FileUtils.setValue(mKcalGreen, (int) value);
                break;

            case PREF_BLUE:
                FileUtils.setValue(mKcalBlue, (int) value);
                break;

            case PREF_SATURATION:
                if (!(Settings.Secure.getInt(getContext().getContentResolver(), PREF_GRAYSCALE, 0) == 1)) {
                    FileUtils.setValue(mKcalSat, (int) value);
                }
                break;

            case PREF_VALUE:
                FileUtils.setValue(mKcalVal, (int) value);
                break;

            case PREF_CONTRAST:
                FileUtils.setValue(mKcalCont, (int) value);
                break;

            case PREF_HUE:
                FileUtils.setValue(mKcalHue, (int) value);
                break;

            case PREF_GRAYSCALE:
                setmGrayscale((boolean) value);
                break;

            default:
                break;
        }
        return true;
    }

    void applyValues(String preset) {
        String[] values = preset.split(" ");
        int red = Integer.parseInt(values[0]);
        int green = Integer.parseInt(values[1]);
        int blue = Integer.parseInt(values[2]);
        int sat = Integer.parseInt(values[3]);
        int value = Integer.parseInt(values[4]);
        int contrast = Integer.parseInt(values[5]);
        int hue = Integer.parseInt(values[6]);

        mRed.refresh(red);
        mGreen.refresh(green);
        mBlue.refresh(blue);
        mSaturation.refresh(sat);
        mValue.refresh(value);
        mContrast.refresh(contrast);
        mHue.refresh(hue);
    }

    void setmSetOnBoot(boolean checked) {
        mSetOnBoot.setChecked(checked);
    }

    void setmGrayscale(boolean checked) {
        mGrayscale.setChecked(checked);
        mSaturation.setEnabled(!checked);
        FileUtils.setValue(mKcalSat, checked ? 0 :
                Settings.Secure.getInt(getContext().getContentResolver(), PREF_SATURATION,
                        SATURATION_DEFAULT));
    }
}
