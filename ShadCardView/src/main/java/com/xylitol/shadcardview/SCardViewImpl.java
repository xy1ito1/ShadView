package com.xylitol.shadcardview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;

/**
 * declaration;
 * time;
 */
public interface SCardViewImpl {

    void initialize(SCardViewDelegate cardView, Context context, ColorStateList backgroundColor,
                    Float radius, Float elevation, Float maxElevation, Integer direction, Integer cornerVisibility, Integer startColor, Integer endColor);

    void setRadius(SCardViewDelegate cardView, Float radius);

    Float getRadius(SCardViewDelegate cardView);

    void setElevation(SCardViewDelegate cardView, Float elevation);

    Float getElevation(SCardViewDelegate cardView);

    void initStatic();

    void setMaxElevation(SCardViewDelegate cardView, Float maxFloat);

    Float getMaxElevation(SCardViewDelegate cardView);

    Float getMinWidth(SCardViewDelegate cardView);

    Float getMinHeight(SCardViewDelegate cardView);

    void updatePadding(SCardViewDelegate cardView);

    void onCompatPaddingChanged(SCardViewDelegate cardView);

    void onPreventCornerOverlapChanged(SCardViewDelegate cardView);

    void setBackgroundColor(SCardViewDelegate cardView, ColorStateList color);

    void setShadowColor(SCardViewDelegate cardView, @ColorInt int startColor, @ColorInt int endColor);

    ColorStateList getBackgroundColor(SCardViewDelegate cardView);

    Drawable getShadowBackground(SCardViewDelegate cardView);

    void setColors(SCardViewDelegate cardView, Integer backgroundColor, Integer shadowStartColor, Integer shadowEndColor);

}
