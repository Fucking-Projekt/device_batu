/*
 * Copyright (C) 2026 The LineageOS Project
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

package org.lineageos.settings.preferences;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class DT2WTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    private void updateTile() {
        final Tile tile = getQsTile();
        if (tile != null) {
            boolean enabled = DT2WSwitchPreference.readFromSysfs();
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean currentState = DT2WSwitchPreference.readFromSysfs();
        boolean newState = !currentState;
        DT2WSwitchPreference.writeToSysfs(newState);
        DT2WSwitchPreference.syncSettings(this, newState);
        updateTile();
    }
}
