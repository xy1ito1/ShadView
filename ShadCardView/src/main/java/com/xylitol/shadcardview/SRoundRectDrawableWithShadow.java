package com.xylitol.shadcardview;

import static com.xylitol.shadcardview.CornerVisibility.*;
import static com.xylitol.shadcardview.ShadowDirection.*;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.Arrays;

/**
 * declaration:
 * time:
 */
public class SRoundRectDrawableWithShadow extends Drawable {
    private int mInsetShadow = 0; // extra shadow to avoid gaps between card and shadow
    /*
     * This helper is set by CardView implementations.
     * <p>
     * Prior to API 17, canvas.drawRoundRect is expensive; which is why we need this interface
     * to draw efficient rounded rectangles before 17.
     * */
    private Paint mPaint;
    private Paint mCornerShadowPaint;
    private Paint mEdgeShadowPaint;
    private RectF mCardBounds;
    private Float mCornerRadius = 0f;
    private Path mCornerShadowPath = null;

    // actual value set by developer
    private Float mRawMaxShadowSize = 0f;

    // multiplied value to account for shadow offset
    private Float mShadowSize = 0f;

    // actual value set by developer
    private Float mRawShadowSize = 0f;

    private ColorStateList mBackground = null;

    private boolean mDirty = true;

    private int mShadowStartColor = 0;

    private int mShadowEndColor = 0;

    private boolean mAddPaddingForCorners = true;

    /**
     * If shadow size is set to a value above max shadow, we print a warning
     */
    private boolean mPrintedShadowClipWarning = false;
    private int mLightDirection = DIRECTION_TOP;
    private int mCornerVisibility = NONE;
    private SCardViewDelegate mCardDelegate;
    private Pair<Pair<Float, Float>, Pair<Float, Float>> mTranslatePos = null;

    public SRoundRectDrawableWithShadow(SCardViewDelegate cardViewDelegate, Resources resources, ColorStateList backgroundColor,
                                        Float radius, Float shadowSize, Float maxShadowSize,
                                        Integer direction, Integer cornerVisibility, Integer startColor, Integer endColor) {
        mShadowStartColor = startColor == -1 ? resources.getColor(R.color.sl_cardview_shadow_start_color) : startColor;
        mShadowEndColor = endColor == -1 ? resources.getColor(R.color.sl_cardview_shadow_end_color) : endColor;
        mInsetShadow = resources.getDimensionPixelSize(R.dimen.cardview_compat_inset_shadow);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        setBackground(backgroundColor);
        mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mCornerShadowPaint.setStyle(Paint.Style.FILL);
        mCornerRadius = (float) ((int) (radius + 0.5f));
        mCardBounds = new RectF();
        mEdgeShadowPaint = new Paint(mCornerShadowPaint);

        mEdgeShadowPaint.setAntiAlias(false);
        mLightDirection = direction;
        mCornerVisibility = cornerVisibility;
        mCardDelegate = cardViewDelegate;
        setShadowSize(shadowSize, maxShadowSize);

    }

    private void setBackground(ColorStateList color) {
        mBackground = (color == null ? ColorStateList.valueOf(Color.TRANSPARENT) : color);
        mPaint.setColor(mBackground.getColorForState(getState(), mBackground.getDefaultColor()));
    }

    /**
     * Casts the value to an even integer.
     */
    private int toEven(Float value) {
        int i = (int) (value + 0.5f);
        return i % 2 == 1 ? i - 1 : i;
    }

    public void setAddPaddingForCorners(Boolean addPaddingForCorners) {
        mAddPaddingForCorners = addPaddingForCorners;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        mCornerShadowPaint.setAlpha(alpha);
        mEdgeShadowPaint.setAlpha(alpha);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mDirty = true;
    }

    private void setShadowSize(Float shadowSize, Float maxShadowSize) {
        Float updateShadowSize = shadowSize;
        Float updateMaxShadowSize = maxShadowSize;
        if (updateShadowSize < 0f) {
            throw new IllegalArgumentException("Invalid shadow size " + updateShadowSize
                    + ". Must be >= 0");
        }
        if (updateMaxShadowSize < 0f) {
            throw new IllegalArgumentException("Invalid max shadow size " + updateMaxShadowSize
                    + ". Must be >= 0");
        }

        updateShadowSize = Float.valueOf(toEven(updateShadowSize));
        updateMaxShadowSize = Float.valueOf(toEven(updateMaxShadowSize));
        if (updateShadowSize > updateMaxShadowSize) {
            updateShadowSize = updateMaxShadowSize;
            if (!mPrintedShadowClipWarning) {
                mPrintedShadowClipWarning = true;
            }
        }
        if (mRawShadowSize == updateShadowSize && mRawMaxShadowSize == updateMaxShadowSize) {
            return;
        }
        mRawShadowSize = updateShadowSize;
        mRawMaxShadowSize = updateMaxShadowSize;
        mTranslatePos = calculateShadowDirection();
        mShadowSize = (updateShadowSize * SHADOW_MULTIPLIER + Float.valueOf(mInsetShadow) + .5f);
        mDirty = true;
        invalidateSelf();
    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
        int vOffset = (int) Math.ceil(calculateVerticalPadding(mRawMaxShadowSize, mCornerRadius,
                mAddPaddingForCorners));
        int hOffset = (int) Math.ceil(calculateHorizontalPadding(mRawMaxShadowSize, mCornerRadius,
                mAddPaddingForCorners));
        padding.set(hOffset, vOffset, hOffset, vOffset);


        return true;
    }

    // used to calculate content padding
    private static double COS_45 = Math.cos(Math.toRadians(45.0));
    public static RoundRectHelper sRoundRectHelper = null;
    public static float SHADOW_MULTIPLIER = 1.5f;

    private static double calculateVerticalPadding(Float maxShadowSize, Float cornerRadius,
                                                   Boolean addPaddingForCorners) {
        return addPaddingForCorners ?
                (float) (maxShadowSize * SHADOW_MULTIPLIER + (1 - COS_45) * cornerRadius) :
                maxShadowSize * SHADOW_MULTIPLIER;
    }

    private static double calculateHorizontalPadding(Float maxShadowSize, Float cornerRadius,
                                                     Boolean addPaddingForCorners) {
        return addPaddingForCorners ?
                (float) (maxShadowSize + (1 - COS_45) * cornerRadius) :
                maxShadowSize;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        int newColor = mBackground.getColorForState(state, mBackground.getDefaultColor());
        if (mPaint.getColor() == newColor) {
            return false;
        }
        mPaint.setColor(newColor);
        mDirty = true;
        invalidateSelf();
        return true;
    }

    @Override
    public boolean isStateful() {
        return mBackground != null && mBackground.isStateful() || super.isStateful();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setCornerRadius(Float radius) {
        Float updateRadius = radius;
        if (updateRadius < 0f) {
            throw new IllegalArgumentException("Invalid radius $updateRadius. Must be >= 0");
        }
        updateRadius = Float.valueOf(updateRadius + .5f);
        if (mCornerRadius == updateRadius) {
            return;
        }
        mCornerRadius = updateRadius;
        mDirty = true;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mDirty) {
            buildComponents(getBounds());
            mDirty = false;
        }

        if (mTranslatePos != null) {
            canvas.translate(mTranslatePos.first.first, mTranslatePos.first.second);
            drawShadow(canvas);
            canvas.translate(mTranslatePos.second.first, mTranslatePos.second.second);
            if (sRoundRectHelper != null) {
                sRoundRectHelper.drawRoundRect(canvas, mCardBounds, mCornerRadius, mCornerVisibility, mPaint);
            }
        }
    }

    /**
     * According to the position of light,calculate shadow's position of the card
     */
    private Pair<Pair<Float, Float>, Pair<Float, Float>> calculateShadowDirection() {
        float moveDistance = mRawShadowSize / 2;

        if (mLightDirection == DIRECTION_NONE) {
            Pair shadowPos = new Pair(0f, 0f);
            Pair rectPos = new Pair(0f, 0f);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_LEFT) {
            Pair shadowPos = new Pair(0f, 0f);
            Pair rectPos = new Pair(-moveDistance, 0f);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_RIGHT) {
            Pair shadowPos = new Pair(0f, 0f);
            Pair rectPos = new Pair(moveDistance, 0f);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_TOP) {

            Pair shadowPos = new Pair(0f, 0f);
            Pair rectPos = new Pair(0f, -moveDistance);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_BOTTOM) {

            Pair shadowPos = new Pair(0f, 0f);
            Pair rectPos = new Pair(0f, moveDistance);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_LT) {

            Pair shadowPos = new Pair(0f, moveDistance);
            Pair rectPos = new Pair(-moveDistance, -moveDistance);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_RT) {

            Pair shadowPos = new Pair(0f, moveDistance);
            Pair rectPos = new Pair(moveDistance, -moveDistance);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_LB) {

            Pair shadowPos = new Pair(0f, -moveDistance);
            Pair rectPos = new Pair(-moveDistance, moveDistance);
            return new Pair<>(shadowPos, rectPos);
        } else if (mLightDirection == DIRECTION_RB) {

            Pair shadowPos = new Pair(0f, -moveDistance);
            Pair rectPos = new Pair(moveDistance, moveDistance);
            return new Pair<>(shadowPos, rectPos);
        } else {
            throw new IllegalArgumentException("invalid light direction exception");
        }
    }

    private void drawShadow(Canvas canvas) {
        RectF visibility = calculateCornerVisibility(); //顺时针 - 可见性
        // LT
        int saved = canvas.save();
        drawLTCorner(canvas, visibility.left);
        canvas.restoreToCount(saved);
        // RB
        saved = canvas.save();
        drawRBCorner(canvas, visibility.right);
        canvas.restoreToCount(saved);
        // LB
        saved = canvas.save();
        drawLBCorner(canvas, visibility.bottom);
        canvas.restoreToCount(saved);
        // RT
        saved = canvas.save();
        drawRTCorner(canvas, visibility.top);
        canvas.restoreToCount(saved);
    }

    private RectF calculateCornerVisibility() {

        if (mCornerVisibility == NOLEFTCORNER) {
            return new RectF(0f, mCornerRadius, mCornerRadius, 0f);
        } else if (mCornerVisibility == NORIGHTCORNER) {
            return new RectF(mCornerRadius, 0f, 0f, mCornerRadius);
        } else if (mCornerVisibility == NOTOPCORNER) {
            return new RectF(0f, 0f, mCornerRadius, mCornerRadius);
        } else if (mCornerVisibility == NOBOTTOMCORNER) {
            return new RectF(mCornerRadius, mCornerRadius, 0f, 0f);
        } else if (mCornerVisibility == NOLT_RBCORNER) {
            return new RectF(0f, mCornerRadius, 0f, mCornerRadius);
        } else if (mCornerVisibility == NORT_LBCORNER) {
            return new RectF(mCornerRadius, 0f, mCornerRadius, 0f);
        } else {
            return new RectF(mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius);
        }
    }

    private void drawRTCorner(Canvas canvas, Float cornerRadius) {
        float edgeShadowTop = -cornerRadius - mShadowSize;
        float inset = cornerRadius + mInsetShadow + mRawShadowSize / 2;
        float right = mCardBounds.height() - 2 * inset;
        boolean drawVerticalEdges = right > 0;
        buildShadowCorners(cornerRadius);
        canvas.translate(mCardBounds.right - inset, mCardBounds.top + inset);
        canvas.rotate(90f);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawVerticalEdges) {
            if (mCornerVisibility == NOTOPCORNER || mCornerVisibility == NORT_LBCORNER) {
                right -= mCornerRadius;
            }
            if (mCornerVisibility == NOBOTTOMCORNER || mCornerVisibility == NOLT_RBCORNER) {
                right += mCornerRadius;
            }
            canvas.drawRect(0f, edgeShadowTop, right, -cornerRadius, mEdgeShadowPaint);
        }
    }

    private void drawRBCorner(Canvas canvas, Float cornerRadius) {
        float edgeShadowTop = -cornerRadius - mShadowSize;
        float inset = cornerRadius + mInsetShadow + mRawShadowSize / 2;
        boolean drawHorizontalEdges = mCardBounds.width() - 2 * inset > 0;

        buildShadowCorners(cornerRadius);
        canvas.translate(mCardBounds.right - inset, mCardBounds.bottom - inset);
        canvas.rotate(180f);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawHorizontalEdges) {
            float right = mCardBounds.width() - 2 * inset;
            if (mCornerVisibility == NOLEFTCORNER || mCornerVisibility == NORT_LBCORNER)
                right += mCornerRadius;
            if (mCornerVisibility == NORIGHTCORNER || mCornerVisibility == NOLT_RBCORNER)
                right -= mCornerRadius;
            canvas.drawRect(0f, edgeShadowTop, right, -cornerRadius, mEdgeShadowPaint);
        }
    }

    private void drawLBCorner(Canvas canvas, Float cornerRadius) {
        buildShadowCorners(cornerRadius);
        float edgeShadowTop = -cornerRadius - mShadowSize;
        float inset = cornerRadius + mInsetShadow + mRawShadowSize / 2;
        float right = mCardBounds.height() - 2 * inset;
        boolean drawVerticalEdges = right > 0;
        canvas.translate(mCardBounds.left + inset, mCardBounds.bottom - inset);
        canvas.rotate(270f);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawVerticalEdges) {
            if (mCornerVisibility == NOTOPCORNER || mCornerVisibility == NOLT_RBCORNER) {
                right += mCornerRadius;
            }
            if (mCornerVisibility == NOBOTTOMCORNER || mCornerVisibility == NORT_LBCORNER) {
                right -= mCornerRadius;
            }
            canvas.drawRect(0f, edgeShadowTop, right, -cornerRadius, mEdgeShadowPaint);
        }
    }

    private void drawLTCorner(Canvas canvas, Float cornerRadius) {
        float edgeShadowTop = -cornerRadius - mShadowSize;
        float inset = cornerRadius + mInsetShadow + mRawShadowSize / 2;
        boolean drawHorizontalEdges = mCardBounds.width() - 2 * inset > 0;
        buildShadowCorners(cornerRadius);
        canvas.translate(mCardBounds.left + inset, mCardBounds.top + inset);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawHorizontalEdges) {
            float right = mCardBounds.width() - 2 * inset;
            if (mCornerVisibility == NORIGHTCORNER || mCornerVisibility == NORT_LBCORNER)
                right += mCornerRadius;
            if (mCornerVisibility == NOLEFTCORNER || mCornerVisibility == NOLT_RBCORNER)
                right -= mCornerRadius;
            canvas.drawRect(0f, edgeShadowTop, right, -cornerRadius, mEdgeShadowPaint);
        }
    }

    private void buildShadowCorners(Float cornerRadius) {
        RectF innerBounds = new RectF(-cornerRadius, -cornerRadius, cornerRadius, cornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-mShadowSize, -mShadowSize);

        if (mCornerShadowPath == null) {
            mCornerShadowPath = new Path();
        } else {
            mCornerShadowPath.reset();
        }
        mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
        mCornerShadowPath.moveTo(-cornerRadius, 0f);
        mCornerShadowPath.rLineTo(-mShadowSize, 0f);
        // outer arc
        mCornerShadowPath.arcTo(outerBounds, 180f, 90f, false);
        // inner arc
        mCornerShadowPath.arcTo(innerBounds, 270f, -90f, false);
        mCornerShadowPath.close();
        float startRatio = cornerRadius / (cornerRadius + mShadowSize);
        mCornerShadowPaint.setShader(new RadialGradient(0f, 0f, cornerRadius + mShadowSize,
                new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                new float[]{0f, startRatio, 1f},
                Shader.TileMode.CLAMP));

        // we offset the content shadowSize/2 pixels up to make it more realistic.
        // this is why edge shadow shader has some extra space
        // When drawing bottom edge shadow, we use that extra space.
        mEdgeShadowPaint.setShader(new LinearGradient(0f, -cornerRadius + mShadowSize, 0f,
                -cornerRadius - mShadowSize,
                new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
        mEdgeShadowPaint.setAntiAlias(false);
    }

    private void buildComponents(Rect bounds) {
        // Card is offset SHADOW_MULTIPLIER * maxShadowSize to account for the shadow shift.
        // We could have different top-bottom offsets to avoid extra gap above but in that case
        // center aligning Views inside the CardView would be problematic.
        float verticalOffset = mRawMaxShadowSize * SHADOW_MULTIPLIER;
        mCardBounds.set(bounds.left + mRawMaxShadowSize, bounds.top + verticalOffset,
                bounds.right - mRawMaxShadowSize, bounds.bottom - verticalOffset);
        buildShadowCorners(mCornerRadius);
    }

    Float getCornerRadius() {
        return mCornerRadius;
    }

    void getMaxShadowAndCornerPadding(Rect into) {
        getPadding(into);
    }

    void setShadowSize(Float size) {
        setShadowSize(size, mRawMaxShadowSize);
    }

    void setMaxShadowSize(Float size) {
        setShadowSize(mRawShadowSize, size);
    }

    Float getShadowSize() {
        return mRawShadowSize;
    }

    Float getMaxShadowSize() {
        return mRawMaxShadowSize;
    }

    Float getMinWidth() {
        float content = 2 * Math.max(mRawMaxShadowSize, mCornerRadius + mInsetShadow + mRawMaxShadowSize / 2);
        return content + (mRawMaxShadowSize + mInsetShadow) * 2;
    }

    Float getMinHeight() {
        float content = 2 * Math.max(mRawMaxShadowSize, mCornerRadius + mInsetShadow
                + mRawMaxShadowSize * SHADOW_MULTIPLIER / 2);
        return content + (mRawMaxShadowSize * SHADOW_MULTIPLIER + mInsetShadow) * 2;
    }

    void setColor(ColorStateList color) {
        setBackground(color);
        invalidateSelf();
    }

    ColorStateList getColor() {
        return mBackground;
    }

    RectF getCardRectSize() {
        return mCardBounds;
    }

    Pair<Float, Float> getMoveDistance() {
        if (mTranslatePos != null) {
            float x = mTranslatePos.first.first + mTranslatePos.second.first;
            float y = mTranslatePos.first.second + mTranslatePos.second.second;
            return new Pair(x, y);
        } else {
            return null;
        }
    }

    void setShadowColor(int startColor, int endColor) {
        mShadowStartColor = startColor;
        mShadowEndColor = endColor;
        invalidateSelf();
    }

    void setColors(int backgroundColor, int shadowStartColor, int shadowEndColor) {
        mBackground = ColorStateList.valueOf(backgroundColor);
        mPaint.setColor(mBackground.getColorForState(getState(), mBackground.getDefaultColor()));
        mShadowStartColor = shadowStartColor;
        mShadowEndColor = shadowEndColor;
        invalidateSelf();
    }

    interface RoundRectHelper {
        void drawRoundRect(Canvas canvas, RectF bounds, Float cornerRadius, int cornerVisibility, Paint paint);
    }

}
