package org.lineageos.settings.kcal;

import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.preferences.SecureSettingSeekBarPreference;
import org.lineageos.settings.preferences.SecureSettingSwitchPreference;
import org.lineageos.settings.Controller;

public class KcalSettings extends SettingsBasePreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, Controller {

    private final FileUtils mFileUtils = new FileUtils();

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

        mSetOnBoot = (SecureSettingSwitchPreference) findPreference(PREF_SETONBOOT);
        mSetOnBoot.setOnPreferenceChangeListener(this);

        mRed = (SecureSettingSeekBarPreference) findPreference(PREF_RED);
        mRed.setOnPreferenceChangeListener(this);

        mGreen = (SecureSettingSeekBarPreference) findPreference(PREF_GREEN);
        mGreen.setOnPreferenceChangeListener(this);

        mBlue = (SecureSettingSeekBarPreference) findPreference(PREF_BLUE);
        mBlue.setOnPreferenceChangeListener(this);

        mSaturation = (SecureSettingSeekBarPreference) findPreference(PREF_SATURATION);
        mSaturation.setEnabled((Settings.Secure.getInt(getContext().getContentResolver(),
                PREF_GRAYSCALE, 0) == 0));
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
                mFileUtils.setValue(KCAL_RED, (int) value);
                break;

            case PREF_GREEN:
                mFileUtils.setValue(KCAL_GREEN, (int) value);
                break;

            case PREF_BLUE:
                mFileUtils.setValue(KCAL_BLUE, (int) value);
                break;

            case PREF_SATURATION:
                if (!(Settings.Secure.getInt(getContext().getContentResolver(), PREF_GRAYSCALE, 0) == 1)) {
                    mFileUtils.setValue(KCAL_SAT, (int) value);
                }
                break;

            case PREF_VALUE:
                mFileUtils.setValue(KCAL_VAL, (int) value);
                break;

            case PREF_CONTRAST:
                mFileUtils.setValue(KCAL_CONT, (int) value);
                break;

            case PREF_HUE:
                mFileUtils.setValue(KCAL_HUE, (int) value);
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
        mFileUtils.setValue(KCAL_SAT, checked ? 0 :
                Settings.Secure.getInt(getContext().getContentResolver(), PREF_SATURATION,
                        SATURATION_DEFAULT));
    }
}
