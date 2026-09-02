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

package org.lineageos.settings.thermal;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.settings.R;

public class ThermalTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_thermal));
            tile.setLabel(getString(R.string.thermal_title));
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent thermalProfiles = new Intent(this, ThermalActivity.class);
        thermalProfiles.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(thermalProfiles);
    }
}
