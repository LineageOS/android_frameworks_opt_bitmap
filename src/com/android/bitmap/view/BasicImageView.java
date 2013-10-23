/*
 * Copyright (C) 2013 The Android Open Source Project
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
