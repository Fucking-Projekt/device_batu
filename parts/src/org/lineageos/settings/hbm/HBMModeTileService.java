/*
 * Copyright (C) 2018 The OmniROM Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.display.DcDimmingTileService;
import org.lineageos.settings.utils.FileUtils;

public class HBMModeTileService extends TileService {

    private BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                sharedPrefs.edit().putBoolean(HBMUtils.KEY_HBM, false).apply();
                updateUI(false);
            }
        }
    };

    private void updateUI(boolean enabled) {
        final Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenStateReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenStateReceiver);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateUI(sharedPrefs.getBoolean(HBMUtils.KEY_HBM, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean dcDimmingEnabled = sharedPrefs.getBoolean(DcDimmingTileService.DC_DIMMING_ENABLE_KEY, false);
        if (dcDimmingEnabled) {
            return;
        }
        final boolean enabled = !(sharedPrefs.getBoolean(HBMUtils.KEY_HBM, false));
        FileUtils.writeLine(HBMUtils.HBM_NODE, enabled ? "1" : "0");
        if (enabled) {
            FileUtils.writeLine(HBMUtils.BACKLIGHT_NODE, "2047");
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        }
        sharedPrefs.edit().putBoolean(HBMUtils.KEY_HBM, enabled).apply();
        updateUI(enabled);
    }
}
