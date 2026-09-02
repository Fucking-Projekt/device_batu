package org.lineageos.settings.kcal;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import org.lineageos.settings.R;

public class KcalSettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG_KCAL = "kcal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                new KcalSettings(), TAG_KCAL).commit();
    }
}
