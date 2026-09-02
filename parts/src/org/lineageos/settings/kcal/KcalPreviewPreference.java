package org.lineageos.settings.kcal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import org.lineageos.settings.R;

public class KcalPreviewPreference extends Preference {

    private ViewPager2 mKcalPreview;

    public KcalPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.kcal_preview_layout);
    }

    public KcalPreviewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.kcal_preview_layout);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mKcalPreview = (ViewPager2) holder.findViewById(R.id.kcal_preview);
        if (mKcalPreview != null && mKcalPreview.getAdapter() == null) {
            mKcalPreview.setAdapter(new ViewPagerAdapter());
            mKcalPreview.setPageTransformer(new FadeOutTransformation());
        }
    }

    public ViewPager2 getViewPager() {
        return mKcalPreview;
    }
}
