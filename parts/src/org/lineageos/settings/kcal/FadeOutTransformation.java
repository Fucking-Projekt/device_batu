package org.lineageos.settings.kcal;

import androidx.viewpager2.widget.ViewPager2;
import android.view.View;

public class FadeOutTransformation implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        page.setTranslationX(-position*page.getWidth());
        page.setAlpha(1-Math.abs(position));
    }
}
