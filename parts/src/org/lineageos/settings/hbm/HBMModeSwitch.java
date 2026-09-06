/*
 * Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.lineageos.settings.hbm;

import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.display.DcDimmingTileService;
import org.lineageos.settings.utils.FileUtils;

public class HBMModeSwitch implements OnPreferenceChangeListener {
    private Context mContext;

    public HBMModeSwitch(Context context) {
        mContext = context;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean enabled = (Boolean) newValue;
        boolean dcDimmingEnabled = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(DcDimmingTileService.DC_DIMMING_ENABLE_KEY, false);
        if (dcDimmingEnabled) {
            return false;
        }
        if (!FileUtils.writeLine(HBMUtils.HBM_NODE, enabled ? "1" : "0")) {
            return false;
        }
        if (enabled) {
            FileUtils.writeLine(HBMUtils.BACKLIGHT_NODE, "2047");
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        }
        return true;
    }
}
