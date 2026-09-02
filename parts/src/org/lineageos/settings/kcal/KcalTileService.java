package org.lineageos.settings.kcal;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.settings.R;

public class KcalTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_kcal));
            tile.setLabel(getString(R.string.advanced_settings_kcal));
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
        Intent intent = new Intent(this, KcalSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }
}
