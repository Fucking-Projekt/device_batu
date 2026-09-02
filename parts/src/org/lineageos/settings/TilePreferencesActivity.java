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

package org.lineageos.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.lineageos.settings.dirac.DiracActivity;
import org.lineageos.settings.kamisstuff.KamisStuffActivity;
import org.lineageos.settings.kcal.KcalSettingsActivity;
import org.lineageos.settings.thermal.ThermalActivity;

public class TilePreferencesActivity extends Activity {

    private static final String TAG = "TilePreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ComponentName tileComponent = getIntent().getParcelableExtra(
                Intent.EXTRA_COMPONENT_NAME, ComponentName.class);

        Intent target;
        if (tileComponent != null) {
            String className = tileComponent.getClassName();
            Log.d(TAG, "Long-press from tile: " + className);
            if (className.contains("DiracTileService")) {
                target = new Intent(this, DiracActivity.class);
            } else if (className.contains("ThermalTileService")) {
                target = new Intent(this, ThermalActivity.class);
            } else if (className.contains("KcalTileService")) {
                target = new Intent(this, KcalSettingsActivity.class);
            } else {
                target = new Intent(this, KamisStuffActivity.class);
            }
        } else {
            target = new Intent(this, KamisStuffActivity.class);
        }

        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(target);
        finish();
    }
}
