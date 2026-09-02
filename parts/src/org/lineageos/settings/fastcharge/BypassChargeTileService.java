/*
 * Copyright (C) 2025 KamiKaonashi
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

package org.lineageos.settings.fastcharge;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.settings.R;

public class BypassChargeTileService extends TileService {

    private FastChargeUtils mFastChargeUtils;

    @Override
    public void onStartListening() {
        mFastChargeUtils = new FastChargeUtils(getApplicationContext());

        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_bypass_charge));
            tile.setLabel(getString(R.string.fastcharge_bypass_title));
            if (mFastChargeUtils.isBypassChargeSupported()) {
                tile.setState(mFastChargeUtils.isBypassChargeEnabled()
                        ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            } else {
                tile.setState(Tile.STATE_UNAVAILABLE);
            }
            tile.updateTile();
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        if (mFastChargeUtils == null || !mFastChargeUtils.isBypassChargeSupported()) {
            return;
        }
        Tile tile = getQsTile();
        if (tile != null) {
            boolean enabled = mFastChargeUtils.isBypassChargeEnabled();
            mFastChargeUtils.enableBypassCharge(!enabled);
            tile.setState(!enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}
