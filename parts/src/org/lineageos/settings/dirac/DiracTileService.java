package org.lineageos.settings.dirac;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.settings.R;

public class DiracTileService extends TileService {

    private DiracUtils mDiracUtils;

    @Override
    public void onStartListening() {
        mDiracUtils = DiracUtils.getInstance(getApplicationContext());

        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_dirac));
            tile.setLabel(getString(R.string.dirac_title));
            if (mDiracUtils.isDiracEnabled()) {
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                tile.setState(Tile.STATE_INACTIVE);
            }
            tile.updateTile();
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        if (mDiracUtils == null) return;
        Tile tile = getQsTile();
        if (tile != null) {
            boolean enabled = mDiracUtils.isDiracEnabled();
            mDiracUtils.setEnabled(!enabled);
            tile.setState(!enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}