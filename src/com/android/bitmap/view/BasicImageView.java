package com.android.bitmap.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.bitmap.drawable.BasicBitmapDrawable;

/**
 * A helpful ImageView replacement that can generally be used in lieu of ImageView.
 * BasicImageView has logic to unbind its BasicBitmapDrawable when it is detached from the window.
 */
public class BasicImageView extends ImageView {
    private BasicBitmapDrawable mDrawable;

    public BasicImageView(final Context context) {
        this(context, null);
    }

    public BasicImageView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BasicImageView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Set the given BasicBitmapDrawable as the source for this BasicImageView.
     * @param drawable The source drawable.
     */
    public void setDrawable(BasicBitmapDrawable drawable) {
        super.setImageDrawable(drawable);
        mDrawable = drawable;
    }

    public BasicBitmapDrawable getDrawable() {
        return mDrawable;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mDrawable.unbind();
    }

    @Override
    public void setImageDrawable(final Drawable drawable) {
        throw new UnsupportedOperationException(
                "BasicImageView is only compatible with BasicBitmapDrawable. Use setDrawable() "
                        + "instead.");
    }

    @Override
    public void setImageResource(final int resId) {
        throw new UnsupportedOperationException(
                "BasicImageView is only compatible with BasicBitmapDrawable. Use setDrawable() "
                        + "instead.");
    }

    @Override
    public void setImageURI(final Uri uri) {
        throw new UnsupportedOperationException(
                "BasicImageView is only compatible with BasicBitmapDrawable. Use setDrawable() "
                        + "instead.");
    }

    @Override
    public void setImageBitmap(final Bitmap bm) {
        throw new UnsupportedOperationException(
                "BasicImageView is only compatible with BasicBitmapDrawable. Use setDrawable() "
                        + "instead.");
    }
}
