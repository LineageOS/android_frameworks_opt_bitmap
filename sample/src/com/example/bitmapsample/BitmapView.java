package com.example.bitmapsample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.bitmap.BitmapCache;
import com.android.bitmap.drawable.BasicBitmapDrawable;

public class BitmapView extends View {
    private BasicBitmapDrawable mBitmapDrawable;
    private float mDensity;

    public BitmapView(Context c) {
        this(c, null);
    }

    public BitmapView(Context c, AttributeSet attrs) {
        super(c, attrs);
        mDensity = getResources().getDisplayMetrics().density;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return (int) (100 * mDensity);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        mBitmapDrawable.setDecodeDimensions(w, h);
    }

    public void setImage(String uriString) {
        if (mBitmapDrawable != null) {
            mBitmapDrawable.bind(new BitmapRequestKeyImpl(uriString));
        }
    }

    public void initialize(BitmapCache cache) {
        mBitmapDrawable = new BasicBitmapDrawable(getResources(), cache);
        setBackground(mBitmapDrawable);
    }

}