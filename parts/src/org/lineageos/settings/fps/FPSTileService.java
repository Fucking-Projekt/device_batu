/*
 * Copyright (C) 2020 DerpFest ROM
 * Copyright (C) 2025 LineageOS
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

package org.lineageos.settings.fps;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.settings.R;

public class FPSTileService extends TileService {

    private boolean isShowing = false;

    @Override
    public void onStartListening() {
        super.onStartListening();
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        isShowing = false;
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FPSInfoService.class.getName().equals(service.service.getClassName())) {
                isShowing = true;
                break;
            }
        }
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_fps_tile));
            tile.setLabel(getString(R.string.fps_info_title));
            tile.setState(isShowing ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        Intent fpsinfo = new Intent(this, FPSInfoService.class);
        if (!isShowing) {
            startService(fpsinfo);
        } else {
            stopService(fpsinfo);
        }
        isShowing = !isShowing;
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(isShowing ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}
