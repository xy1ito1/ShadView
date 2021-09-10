package com.xylitol.shadcardview;

import static com.xylitol.shadcardview.CornerVisibility.NOBOTTOMCORNER;
import static com.xylitol.shadcardview.CornerVisibility.NOLEFTCORNER;
import static com.xylitol.shadcardview.CornerVisibility.NOLT_RBCORNER;
import static com.xylitol.shadcardview.CornerVisibility.NORIGHTCORNER;
import static com.xylitol.shadcardview.CornerVisibility.NORT_LBCORNER;
import static com.xylitol.shadcardview.CornerVisibility.NOTOPCORNER;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * declaration:
 * time:
 */
public class SCardViewBaseImpl implements SCardViewImpl {

    private static RectF mCornerRect = new RectF();

    @Override
    public void initStatic() {
        // Draws a round rect using 7 draw operations. This is faster than using
        // canvas.drawRoundRect before JBMR1 because API 11-16 used alpha mask textures to draw
        // shapes.
        SRoundRectDrawableWithShadow.sRoundRectHelper = new SRoundRectDrawableWithShadow.RoundRectHelper() {
            @Override
            public void drawRoundRect(Canvas canvas, RectF bounds, Float cornerRadius, int cornerVisibility, Paint paint) {
                float twoRadius = cornerRadius * 2;
                float innerWidth = bounds.width() - twoRadius - 1f;
                float innerHeight = bounds.height() - twoRadius - 1f;
                if (cornerRadius >= 1f) {
                    // increment corner radius to account for half pixels.
                    float roundedCornerRadius = cornerRadius + .5f;
                    mCornerRect.set(-roundedCornerRadius, -roundedCornerRadius, roundedCornerRadius, roundedCornerRadius);
                    int saved = canvas.save();
                    canvas.translate(bounds.left + roundedCornerRadius, bounds.top + roundedCornerRadius);
                    if (cornerVisibility == NOLEFTCORNER || cornerVisibility == NOTOPCORNER || cornerVisibility == NOLT_RBCORNER)
                        canvas.drawRect(-roundedCornerRadius, -roundedCornerRadius, 0f, 0f, paint);
                    else
                        canvas.drawArc(mCornerRect, 180f, 90f, true, paint);
                    canvas.translate(innerWidth, 0f);
                    canvas.rotate(90f);
                    if (cornerVisibility == NORIGHTCORNER || cornerVisibility == NOTOPCORNER || cornerVisibility == NORT_LBCORNER)
                        canvas.drawRect(-roundedCornerRadius, -roundedCornerRadius, 0f, 0f, paint);
                    else
                        canvas.drawArc(mCornerRect, 180f, 90f, true, paint);
                    canvas.translate(innerHeight, 0f);
                    canvas.rotate(90f);
                    if (cornerVisibility == NORIGHTCORNER || cornerVisibility == NOBOTTOMCORNER || cornerVisibility == NOLT_RBCORNER)
                        canvas.drawRect(-roundedCornerRadius, -roundedCornerRadius, 0f, 0f, paint);
                    else
                        canvas.drawArc(mCornerRect, 180f, 90f, true, paint);
                    canvas.translate(innerWidth, 0f);
                    canvas.rotate(90f);
                    if (cornerVisibility == NOLEFTCORNER || cornerVisibility == NOBOTTOMCORNER || cornerVisibility == NORT_LBCORNER)
                        canvas.drawRect(-roundedCornerRadius, -roundedCornerRadius, 0f, 0f, paint);
                    else
                        canvas.drawArc(mCornerRect, 180f, 90f, true, paint);
                    canvas.restoreToCount(saved);

                    //draw top and bottom pieces
                    canvas.drawRect(bounds.left + roundedCornerRadius - 1f, bounds.top,
                            bounds.right - roundedCornerRadius + 1f,
                            bounds.top + roundedCornerRadius, paint);

                    canvas.drawRect(bounds.left + roundedCornerRadius - 1f,
                            bounds.bottom - roundedCornerRadius,
                            bounds.right - roundedCornerRadius + 1f, bounds.bottom, paint);
                }
                // center
                canvas.drawRect(bounds.left, bounds.top + cornerRadius,
                        bounds.right, bounds.bottom - cornerRadius, paint);
            }
        };
    }

    @Override
    public void initialize(SCardViewDelegate cardView, Context context, ColorStateList backgroundColor, Float radius, Float elevation, Float maxElevation, Integer direction, Integer cornerVisibility, Integer startColor, Integer endColor) {
        SRoundRectDrawableWithShadow background = createBackground(cardView, context, backgroundColor, radius,
                elevation, maxElevation, direction, cornerVisibility, startColor, endColor);
        background.setAddPaddingForCorners(cardView.getPreventCornerOverlap());
        cardView.setCardBackground(background);
        updatePadding(cardView);
    }

    private SRoundRectDrawableWithShadow createBackground(SCardViewDelegate cardViewDelegate, Context context,
                                                          ColorStateList backgroundColor, Float radius, Float elevation,
                                                          Float maxElevation, int direction, int cornerVisibility, int startColor,
                                                          int endColor) {
        return new SRoundRectDrawableWithShadow(cardViewDelegate, context.getResources(), backgroundColor, radius,
                elevation, maxElevation, direction, cornerVisibility, startColor, endColor);
    }

    @Override
    public void updatePadding(SCardViewDelegate cardView) {
        Rect shadowPadding = new Rect();

        getShadowBackground(cardView).getMaxShadowAndCornerPadding(shadowPadding);
        cardView.setMinWidthHeightInternal((int) Math.ceil(getMinWidth(cardView)),
                (int) Math.ceil(getMinHeight(cardView)));
        cardView.setShadowPadding(shadowPadding.left, shadowPadding.top,
                shadowPadding.right, shadowPadding.bottom);
    }

    @Override
    public void onCompatPaddingChanged(SCardViewDelegate cardView) {
        // NO OP
    }

    @Override
    public void onPreventCornerOverlapChanged(SCardViewDelegate cardView) {
        getShadowBackground(cardView).setAddPaddingForCorners(cardView.getPreventCornerOverlap());
        updatePadding(cardView);
    }

    @Override
    public void setBackgroundColor(SCardViewDelegate cardView, ColorStateList color) {
        getShadowBackground(cardView).setColor(color);
    }

    @Override
    public void setShadowColor(SCardViewDelegate cardView, int startColor, int endColor) {

        getShadowBackground(cardView).setShadowColor(startColor, endColor);
    }

    @Override
    public void setColors(SCardViewDelegate cardView, Integer backgroundColor, Integer shadowStartColor, Integer shadowEndColor) {

        getShadowBackground(cardView).setColors(backgroundColor, shadowStartColor, shadowEndColor);
    }

    @Override
    public ColorStateList getBackgroundColor(SCardViewDelegate cardView) {
        return getShadowBackground(cardView).getColor();
    }

    @Override
    public void setRadius(SCardViewDelegate cardView, Float radius) {

        getShadowBackground(cardView).setCornerRadius(radius);
        updatePadding(cardView);
    }

    @Override
    public Float getRadius(SCardViewDelegate cardView) {
        return getShadowBackground(cardView).getCornerRadius();
    }

    @Override
    public void setElevation(SCardViewDelegate cardView, Float elevation) {

        getShadowBackground(cardView).setShadowSize(elevation);
    }

    @Override
    public Float getElevation(SCardViewDelegate cardView) {
        return getShadowBackground(cardView).getShadowSize();
    }

    @Override
    public void setMaxElevation(SCardViewDelegate cardView, Float maxFloat) {

        getShadowBackground(cardView).setMaxShadowSize(maxFloat);
        updatePadding(cardView);
    }

    @Override
    public Float getMaxElevation(SCardViewDelegate cardView) {
        return getShadowBackground(cardView).getMaxShadowSize();
    }

    @Override
    public Float getMinWidth(SCardViewDelegate cardView) {
        return getShadowBackground(cardView).getMinWidth();
    }

    @Override
    public Float getMinHeight(SCardViewDelegate cardView) {
        return getShadowBackground(cardView).getMinHeight();
    }

    @Override
    public SRoundRectDrawableWithShadow getShadowBackground(SCardViewDelegate cardView) {
        return (SRoundRectDrawableWithShadow) cardView.getCardBackground();
    }
}
