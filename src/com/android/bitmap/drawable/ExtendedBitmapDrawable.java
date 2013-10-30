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

package com.android.bitmap.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.android.bitmap.BitmapCache;
import com.android.bitmap.DecodeAggregator;
import com.android.bitmap.DecodeTask;
import com.android.bitmap.R;
import com.android.bitmap.RequestKey;
import com.android.bitmap.RequestKey.FileDescriptorFactory;
import com.android.bitmap.ReusableBitmap;
import com.android.bitmap.util.Trace;

/**
 * This class encapsulates all functionality needed to display a single image bitmap,
 * including request creation/cancelling, data unbinding and re-binding, and fancy animations
 * to draw upon state changes.
 * <p>
 * The actual bitmap decode work is handled by {@link DecodeTask}.
 * TODO: have this class extend from BasicBitmapDrawable
 */
public class ExtendedBitmapDrawable extends BasicBitmapDrawable implements
    Runnable, Parallaxable, DecodeAggregator.Callback {

    // Ordered display.
    private DecodeAggregator mDecodeAggregator;
    
    // Parallax.
    private float mParallaxFraction = 0.5f;
    private float mParallaxSpeedMultiplier;
    private static final float DECODE_VERTICAL_CENTER = 1f / 3;

    // Placeholder and progress.
    private static final int LOAD_STATE_UNINITIALIZED = 0;
    private static final int LOAD_STATE_NOT_YET_LOADED = 1;
    private static final int LOAD_STATE_LOADING = 2;
    private static final int LOAD_STATE_LOADED = 3;
    private static final int LOAD_STATE_FAILED = 4;
    private int mLoadState = LOAD_STATE_UNINITIALIZED;
    private Placeholder mPlaceholder;
    private Progress mProgress;
    private int mProgressDelayMs;
    private final Handler mHandler = new Handler();

    public static final boolean DEBUG = false;
    public static final String TAG = ExtendedBitmapDrawable.class.getSimpleName();

    public ExtendedBitmapDrawable(final Resources res, final BitmapCache cache,
            final boolean limitDensity, final DecodeAggregator decodeAggregator,
            final Drawable placeholder, final Drawable progress) {
        super(res, cache, limitDensity);

        // Ordered display.
        this.mDecodeAggregator = decodeAggregator;

        // Placeholder and progress.
        final int fadeOutDurationMs = res.getInteger(R.integer.bitmap_fade_animation_duration);
        final int tileColor = res.getColor(R.color.bitmap_placeholder_background_color);
        mProgressDelayMs = res.getInteger(R.integer.bitmap_progress_animation_delay);

        int placeholderSize = res.getDimensionPixelSize(R.dimen.placeholder_size);
        mPlaceholder = new Placeholder(placeholder.getConstantState().newDrawable(res), res,
                placeholderSize, placeholderSize, fadeOutDurationMs, tileColor);
        mPlaceholder.setCallback(this);

        int progressBarSize = res.getDimensionPixelSize(R.dimen.progress_bar_size);
        mProgress = new Progress(progress.getConstantState().newDrawable(res), res,
                progressBarSize, progressBarSize, fadeOutDurationMs, tileColor);
        mProgress.setCallback(this);
    }

    @Override
    public void setParallaxFraction(float fraction) {
        mParallaxFraction = fraction;
        invalidateSelf();
    }

    public void setParallaxSpeedMultiplier(final float parallaxSpeedMultiplier) {
        mParallaxSpeedMultiplier = parallaxSpeedMultiplier;
        invalidateSelf();
    }

    /**
     * This sets the drawable to the failed state, which remove all animations from the placeholder.
     * This is different from unbinding to the uninitialized state, where we expect animations.
     */
    public void showStaticPlaceholder() {
        setLoadState(LOAD_STATE_FAILED);
    }

    @Override
    protected void setImage(final RequestKey key) {
        if (mCurrKey != null && mCurrKey.equals(key)) {
            return;
        }

        if (mCurrKey != null && mDecodeAggregator != null) {
            mDecodeAggregator.forget(mCurrKey);
        }

        mHandler.removeCallbacks(this);
        // start from a clean slate on every bind
        // this allows the initial transition to be specially instantaneous, so e.g. a cache hit
        // doesn't unnecessarily trigger a fade-in
        setLoadState(LOAD_STATE_UNINITIALIZED);
        if (key == null) {
            setLoadState(LOAD_STATE_FAILED);
        }

        super.setImage(key);
    }

    @Override
    protected void setBitmap(ReusableBitmap bmp) {
        setLoadState((bmp != null) ? LOAD_STATE_LOADED : LOAD_STATE_FAILED);

        super.setBitmap(bmp);
    }

    @Override
    protected void decode(final FileDescriptorFactory factory) {
        boolean executeStateChange = shouldExecuteStateChange();
        if (executeStateChange) {
            setLoadState(LOAD_STATE_NOT_YET_LOADED);
        }

        super.decode(factory);
    }

    protected boolean shouldExecuteStateChange() {
        // TODO: AttachmentDrawable should override this method to match prev and curr request keys.
        return /* opts.stateChanges */ true;
    }

    @Override
    public float getDrawVerticalCenter() {
        return mParallaxFraction;
    }

    @Override
    protected float getDrawVerticalOffsetMultiplier() {
        return mParallaxSpeedMultiplier;
    }

    @Override
    protected float getDecodeVerticalCenter() {
        return DECODE_VERTICAL_CENTER;
    }

    @Override
    public void draw(final Canvas canvas) {
        final Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }

        super.draw(canvas);

        // Draw the two possible overlay layers in reverse-priority order.
        // (each layer will no-op the draw when appropriate)
        // This ordering means cross-fade transitions are just fade-outs of each layer.
        mProgress.draw(canvas);
        mPlaceholder.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        final int old = mPaint.getAlpha();
        super.setAlpha(alpha);
        mPlaceholder.setAlpha(alpha);
        mProgress.setAlpha(alpha);
        if (alpha != old) {
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        super.setColorFilter(cf);
        mPlaceholder.setColorFilter(cf);
        mProgress.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        mPlaceholder.setBounds(bounds);
        mProgress.setBounds(bounds);
    }

    @Override
    public void onDecodeBegin(final RequestKey key) {
        if (mDecodeAggregator != null) {
            mDecodeAggregator.expect(key, this);
        } else {
            onBecomeFirstExpected(key);
        }
        super.onDecodeBegin(key);
    }

    @Override
    public void onBecomeFirstExpected(final RequestKey key) {
        if (!key.equals(mCurrKey)) {
            return;
        }
        // normally, we'd transition to the LOADING state now, but we want to delay that a bit
        // to minimize excess occurrences of the rotating spinner
        mHandler.postDelayed(this, mProgressDelayMs);
    }

    @Override
    public void run() {
        if (mLoadState == LOAD_STATE_NOT_YET_LOADED) {
            setLoadState(LOAD_STATE_LOADING);
        }
    }

    @Override
    public void onDecodeComplete(final RequestKey key, final ReusableBitmap result) {
        if (mDecodeAggregator != null) {
            mDecodeAggregator.execute(key, new Runnable() {
                @Override
                public void run() {
                    ExtendedBitmapDrawable.super.onDecodeComplete(key, result);
                }

                @Override
                public String toString() {
                    return "DONE";
                }
            });
        } else {
            super.onDecodeComplete(key, result);
        }
    }

    @Override
    public void onDecodeCancel(final RequestKey key) {
        if (mDecodeAggregator != null) {
            mDecodeAggregator.forget(key);
        }
        super.onDecodeCancel(key);
    }

    /**
     * Each attachment gets its own placeholder and progress indicator, to be shown, hidden,
     * and animated based on Drawable#setVisible() changes, which are in turn driven by
     * setLoadState().
     */
    private void setLoadState(int loadState) {
        if (DEBUG) {
            Log.v(TAG, String.format("IN setLoadState. old=%s new=%s key=%s this=%s",
                    mLoadState, loadState, mCurrKey, this));
        }
        if (mLoadState == loadState) {
            if (DEBUG) {
                Log.v(TAG, "OUT no-op setLoadState");
            }
            return;
        }

        Trace.beginSection("set load state");
        switch (loadState) {
            // This state differs from LOADED in that the subsequent state transition away from
            // UNINITIALIZED will not have a fancy transition. This allows list item binds to
            // cached data to take immediate effect without unnecessary whizzery.
            case LOAD_STATE_UNINITIALIZED:
                mPlaceholder.reset();
                mProgress.reset();
                break;
            case LOAD_STATE_NOT_YET_LOADED:
                mPlaceholder.setPulseEnabled(true);
                mPlaceholder.setVisible(true);
                mProgress.setVisible(false);
                break;
            case LOAD_STATE_LOADING:
                mPlaceholder.setVisible(false);
                mProgress.setVisible(true);
                break;
            case LOAD_STATE_LOADED:
                mPlaceholder.setVisible(false);
                mProgress.setVisible(false);
                break;
            case LOAD_STATE_FAILED:
                mPlaceholder.setPulseEnabled(false);
                mPlaceholder.setVisible(true);
                mProgress.setVisible(false);
                break;
        }
        Trace.endSection();

        mLoadState = loadState;
        boolean placeholderVisible = mPlaceholder != null && mPlaceholder.isVisible();
        boolean progressVisible = mProgress != null && mProgress.isVisible();

        if (DEBUG) {
            Log.v(TAG, String.format("OUT stateful setLoadState. new=%s placeholder=%s progress=%s",
                    loadState, placeholderVisible, progressVisible));
        }
    }

    private static class Placeholder extends TileDrawable {

        private final ValueAnimator mPulseAnimator;
        private boolean mPulseEnabled = true;
        private float mPulseAlphaFraction = 1f;

        public Placeholder(Drawable placeholder, Resources res,
                int placeholderWidth, int placeholderHeight, int fadeOutDurationMs,
                int tileColor) {
            super(placeholder, placeholderWidth, placeholderHeight, tileColor, fadeOutDurationMs);
            mPulseAnimator = ValueAnimator.ofInt(55, 255)
                    .setDuration(res.getInteger(R.integer.bitmap_placeholder_animation_duration));
            mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            mPulseAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mPulseAlphaFraction = ((Integer) animation.getAnimatedValue()) / 255f;
                    setInnerAlpha(getCurrentAlpha());
                }
            });
            mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    stopPulsing();
                }
            });
        }

        @Override
        public void setInnerAlpha(final int alpha) {
            super.setInnerAlpha((int) (alpha * mPulseAlphaFraction));
        }

        public void setPulseEnabled(boolean enabled) {
            mPulseEnabled = enabled;
            if (!mPulseEnabled) {
                stopPulsing();
            }
        }

        private void stopPulsing() {
            if (mPulseAnimator != null) {
                mPulseAnimator.cancel();
                mPulseAlphaFraction = 1f;
                setInnerAlpha(getCurrentAlpha());
            }
        }

        @Override
        public boolean setVisible(boolean visible) {
            final boolean changed = super.setVisible(visible);
            if (changed) {
                if (isVisible()) {
                    // start
                    if (mPulseAnimator != null && mPulseEnabled) {
                        mPulseAnimator.start();
                    }
                } else {
                    // can't cancel the pulsing yet-- wait for the fade-out animation to end
                    // one exception: if alpha is already zero, there is no fade-out, so stop now
                    if (getCurrentAlpha() == 0) {
                        stopPulsing();
                    }
                }
            }
            return changed;
        }

    }

    private static class Progress extends TileDrawable {

        private final ValueAnimator mRotateAnimator;

        public Progress(Drawable progress, Resources res,
                int progressBarWidth, int progressBarHeight, int fadeOutDurationMs,
                int tileColor) {
            super(progress, progressBarWidth, progressBarHeight, tileColor, fadeOutDurationMs);

            mRotateAnimator = ValueAnimator.ofInt(0, 10000)
                    .setDuration(res.getInteger(R.integer.bitmap_progress_animation_duration));
            mRotateAnimator.setInterpolator(new LinearInterpolator());
            mRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mRotateAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setLevel((Integer) animation.getAnimatedValue());
                }
            });
            mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mRotateAnimator != null) {
                        mRotateAnimator.cancel();
                    }
                }
            });
        }

        @Override
        public boolean setVisible(boolean visible) {
            final boolean changed = super.setVisible(visible);
            if (changed) {
                if (isVisible()) {
                    if (mRotateAnimator != null) {
                        mRotateAnimator.start();
                    }
                } else {
                    // can't cancel the rotate yet-- wait for the fade-out animation to end
                    // one exception: if alpha is already zero, there is no fade-out, so stop now
                    if (getCurrentAlpha() == 0 && mRotateAnimator != null) {
                        mRotateAnimator.cancel();
                    }
                }
            }
            return changed;
        }

    }
}
