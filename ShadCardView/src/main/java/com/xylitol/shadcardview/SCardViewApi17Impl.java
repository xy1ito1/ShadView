package com.xylitol.shadcardview;

/**
 * declaration:
 * time:
 */
public class SCardViewApi17Impl extends SCardViewBaseImpl {
    @Override
    public void initStatic() {
        SRoundRectDrawableWithShadow.sRoundRectHelper = (canvas, bounds, cornerRadius, cornerVisibility, paint) -> canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }
}
