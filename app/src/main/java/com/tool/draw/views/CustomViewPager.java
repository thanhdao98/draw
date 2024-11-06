package com.tool.draw.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.tool.draw.R;

public class CustomViewPager extends FrameLayout {
    private boolean isSwipeEnabled = true;
    private ViewPager2 viewPager;

    public CustomViewPager(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CustomViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.custom_view_pager, this, true);
        viewPager = new ViewPager2(context);
        addView(viewPager, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        viewPager.setUserInputEnabled(isSwipeEnabled);
    }

    public void setSwipeEnabled(boolean enabled) {
        this.isSwipeEnabled = enabled;
        viewPager.setUserInputEnabled(enabled);
    }

    public void setAdapter(ViewPagerAdapter adapter) {
        viewPager.setAdapter(adapter);
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }

    public void addOnPageChangeListener(ViewPager2.OnPageChangeCallback onPageChangeCallback) {
        viewPager.registerOnPageChangeCallback(onPageChangeCallback);
    }
}

