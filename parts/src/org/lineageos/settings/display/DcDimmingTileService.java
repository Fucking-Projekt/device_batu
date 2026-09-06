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

package org.lineageos.settings.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.hbm.HBMUtils;
import org.lineageos.settings.utils.FileUtils;

import java.io.File;

public class DcDimmingTileService extends TileService {

    public static final String DC_DIMMING_ENABLE_KEY = DcDimmingUtils.DC_DIMMING_ENABLE_KEY;

    private BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                Editor editor = sharedPrefs.edit();
                editor.putBoolean(DC_DIMMING_ENABLE_KEY, false);
                editor.apply();
                updateUI(false);
                disableHBM();
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

    private void disableHBM() {
        FileUtils.writeLine(DcDimmingUtils.HBM_NODE, "0");
        File hbmFile = new File(DcDimmingUtils.HBM_NODE);
        hbmFile.setReadOnly();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(HBMUtils.KEY_HBM, false);
        editor.apply();
        updateHBMUI(false);
    }

    private void updateHBMUI(boolean enabled) {
        Intent intent = new Intent("org.lineageos.settings.hbm.UPDATE_TILE");
        intent.putExtra("enabled", enabled);
        sendBroadcast(intent);
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
        updateUI(sharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !(sharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false));
        FileUtils.writeLine(DcDimmingUtils.DC_DIMMING_NODE, enabled ? "1" : "0");
        File hbmFile = new File(DcDimmingUtils.HBM_NODE);
        if (enabled) {
            disableHBM();
        } else {
            hbmFile.setWritable(true);
        }
        sharedPrefs.edit().putBoolean(DC_DIMMING_ENABLE_KEY, enabled).apply();
        updateUI(enabled);
    }
}
