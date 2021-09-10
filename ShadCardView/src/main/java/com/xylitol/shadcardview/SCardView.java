package com.xylitol.shadcardview;

import static com.xylitol.shadcardview.CornerVisibility.*;
import static com.xylitol.shadcardview.ShadowDirection.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * declaration:
 * time:
 */
public class SCardView extends FrameLayout {
    public SCardView(@NonNull Context context) {
        this(context, null);
    }

    public SCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @SuppressLint("ResourceType")
    private int[] COLOR_BACKGROUND_ATTR = getResources().getIntArray(android.R.attr.colorBackground);
    private int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;
    private SCardViewImpl IMPL;

    private boolean mCompatPadding = false;


    private boolean mPreventCornerOverlap = false;

    /**
     * 是否使用边角区域放置内容
     */
    private boolean mUseCornerArea = false;

    /**
     * CardView requires to have a particular minimum size to draw shadows before API 21. If
     * developer also sets min width/height, they might be overridden.
     * <p>
     * CardView works around this issue by recording user given parameters and using an internal
     * method to set them.
     */
    int mUserSetMinWidth = 0;

    int mUserSetMinHeight = 0;
    Rect mContentPadding = new Rect();
    Rect mShadowBounds = new Rect();

    private SCardViewDelegate mCardViewDelegate = new SCardViewDelegate() {
        private Drawable mCardBackground = null;

        @Override
        public void setCardBackground(Drawable drawable) {
            mCardBackground = drawable;
            setBackgroundDrawable(drawable);
        }

        @Override
        public Drawable getCardBackground() {
            return mCardBackground;
        }

        @Override
        public boolean getUseCompatPadding() {
            return SCardView.this.getUseCompatPadding();
        }

        @Override
        public boolean getPreventCornerOverlap() {
            return SCardView.this.getPreventCornerOverlap();
        }

        @Override
        public void setShadowPadding(int left, int top, int right, int bottom) {
            mShadowBounds.set(left, top, right, bottom);
            SCardView.this.setPadding(left + mContentPadding.left, top + mContentPadding.top,
                    right + mContentPadding.right, bottom + mContentPadding.bottom);
        }

        @Override
        public void setMinWidthHeightInternal(int width, int height) {
            if (width > mUserSetMinWidth) {
                SCardView.this.setMinimumWidth(width);
            }
            if (height > mUserSetMinHeight) {
                SCardView.this.setMinimumHeight(height);
            }
        }

        @Override
        public View getCardView() {
            return SCardView.this;
        }
    };

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SCardView, defStyleAttr,
                R.style.CardView);

        ColorStateList backgroundColor = null;
        if (a.hasValue(R.styleable.SCardView_cardBackgroundColor)) {
            backgroundColor = a.getColorStateList(R.styleable.SCardView_cardBackgroundColor);
        } else {
            // There isn't one set, so we'll compute one based on the theme
            TypedArray aa = getContext().obtainStyledAttributes(COLOR_BACKGROUND_ATTR);
            int themeColorBackground = aa.getColor(0, 0);
            aa.recycle();

            // If the theme colorBackground is light, use our own light color, otherwise dark
            float[] hsv = new float[3];
            Color.colorToHSV(themeColorBackground, hsv);
            if (hsv[2] > 0.5f) {
                backgroundColor = ColorStateList.valueOf(getResources().getColor(R.color.sl_cardview_light_background));
            } else {
                backgroundColor = ColorStateList.valueOf(getResources().getColor(R.color.sl_cardview_dark_background));
            }
        }
        float radius = a.getDimension(R.styleable.SCardView_cardCornerRadius, 0f);
        float elevation = a.getDimension(R.styleable.SCardView_cardElevation, 0f);
        float maxElevation = a.getDimension(R.styleable.SCardView_cardMaxElevation, 0f);
        mCompatPadding = a.getBoolean(R.styleable.SCardView_cardUseCompatPadding, false);
        mPreventCornerOverlap = a.getBoolean(R.styleable.SCardView_cardPreventCornerOverlap, true);
        mUseCornerArea = a.getBoolean(R.styleable.SCardView_cardUseCornerArea, false);
        int defaultPadding = a.getDimensionPixelSize(R.styleable.SCardView_contentPadding, 0);
        mContentPadding.left = a.getDimensionPixelSize(R.styleable.SCardView_contentPaddingLeft,
                defaultPadding);
        mContentPadding.top = a.getDimensionPixelSize(R.styleable.SCardView_contentPaddingTop,
                defaultPadding);
        mContentPadding.right = a.getDimensionPixelSize(R.styleable.SCardView_contentPaddingRight,
                defaultPadding);
        mContentPadding.bottom = a.getDimensionPixelSize(R.styleable.SCardView_contentPaddingBottom,
                defaultPadding);
        if (elevation > maxElevation) {
            maxElevation = elevation;
        }
        int direction = a.getInt(R.styleable.SCardView_cardLightDirection, DIRECTION_TOP);
        int cardCornerVisibility = a.getInt(R.styleable.SCardView_cardCornerVisibility, NONE);
        int shadowStartColor = a.getColor(R.styleable.SCardView_cardShadowStartColor, -1);
        int shadowEndColor = a.getColor(R.styleable.SCardView_cardShadowEndColor, -1);
        mUserSetMinWidth = a.getDimensionPixelSize(R.styleable.SCardView_android_minWidth, 0);
        mUserSetMinHeight = a.getDimensionPixelSize(R.styleable.SCardView_android_minHeight, 0);
        a.recycle();
        if (Build.VERSION.SDK_INT >= 17) {
            if (cardCornerVisibility == NONE)
                IMPL = new SCardViewApi17Impl();
            else
                IMPL = new SCardViewBaseImpl();
        } else {
            IMPL = new SCardViewBaseImpl();
        }

        IMPL.initStatic();

        IMPL.initialize(mCardViewDelegate, context, backgroundColor, radius,
                elevation, maxElevation, direction, cardCornerVisibility, shadowStartColor, shadowEndColor);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
    }

    /**
     * Returns whether CardView will add inner padding on platforms Lollipop and after.
     *
     * @return `true` if CardView adds inner padding on platforms Lollipop and after to
     * have same dimensions with platforms before Lollipop.
     */
    public Boolean getUseCompatPadding() {
        return mCompatPadding;
    }

    /**
     * CardView adds additional padding to draw shadows on platforms before Lollipop.
     * <p>
     * <p>
     * This may cause Cards to have different sizes between Lollipop and before Lollipop. If you
     * need to align CardView with other Views, you may need api version specific dimension
     * resources to account for the changes.
     * As an alternative, you can set this flag to `true` and CardView will add the same
     * padding values on platforms Lollipop and after.
     * <p>
     * <p>
     * Since setting this flag to true adds unnecessary gaps in the UI, default value is
     * `false`.
     *
     * @param useCompatPadding `true>` if CardView should add padding for the shadows on
     *                         platforms Lollipop and above.
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardUseCompatPadding
     */
    public void setUseCompatPadding(Boolean useCompatPadding) {
        if (mCompatPadding != useCompatPadding) {
            mCompatPadding = useCompatPadding;
            IMPL.onCompatPaddingChanged(mCardViewDelegate);
        }
    }

    /**
     * Sets the padding between the Card's edges and the children of CardView.
     * <p>
     * <p>
     * Depending on platform version or [.getUseCompatPadding] settings, CardView may
     * update these values before calling [android.view.View.setPadding].
     *
     * @param left   The left padding in pixels
     * @param top    The top padding in pixels
     * @param right  The right padding in pixels
     * @param bottom The bottom padding in pixels
     * @attr ref android.support.v7.cardview.R.styleable#CardView_contentPadding
     * @attr ref android.support.v7.cardview.R.styleable#CardView_contentPaddingLeft
     * @attr ref android.support.v7.cardview.R.styleable#CardView_contentPaddingTop
     * @attr ref android.support.v7.cardview.R.styleable#CardView_contentPaddingRight
     * @attr ref android.support.v7.cardview.R.styleable#CardView_contentPaddingBottom
     */
    public void setContentPadding(int left, int top, int right, int bottom) {
        mContentPadding.set(left, top, right, bottom);
        IMPL.updatePadding(mCardViewDelegate);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int updateWidthMeasureSpec = widthMeasureSpec;
        int updateHeightMeasureSpec = heightMeasureSpec;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            int minWidth = (int) Math.ceil(IMPL.getMinWidth(mCardViewDelegate));
            int width = MeasureSpec.getSize(widthMeasureSpec);
            updateWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(minWidth, width), widthMode);
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            // Do nothing
        }

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            int minHeight = (int) Math.ceil(IMPL.getMinHeight(mCardViewDelegate));
            int height = MeasureSpec.getSize(heightMeasureSpec);
            updateWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(minHeight, height), widthMode);
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            // Do nothing
        }

        super.onMeasure(updateWidthMeasureSpec, updateHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren(left, top, right, bottom, false /* no force left gravity */);
    }

    private void layoutChildren(int left, int top, int right, int bottom, Boolean forceLeftGravity) {
        int count = getChildCount();
        SRoundRectDrawableWithShadow bg = (SRoundRectDrawableWithShadow) IMPL.getShadowBackground(mCardViewDelegate);
        RectF rectF = bg.getCardRectSize();
        Pair<Float, Float> movePair = bg.getMoveDistance();
        Float cornerRadius = bg.getCornerRadius();
        double iex = (cornerRadius - (Math.sqrt(2.0) * cornerRadius) / 2 + 0.5f);
        int parentLeft;
        int parentRight;
        int parentTop;
        int parentBottom;
        if (movePair != null) {
            float verticalMove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? movePair.second : 0f;
            float horizontalMove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? movePair.first : 0f;
            parentLeft = (int) horizontalMove;
            parentRight = (int) (right - left + horizontalMove);
            parentTop = (int) verticalMove;
            parentBottom = (int) (bottom - top + verticalMove);
            //控制边角区域是否显示内容
            if (!mUseCornerArea) {
                parentLeft += iex;
                parentTop += iex;
                parentRight -= iex;
                parentBottom -= iex;
            }
            //内容显示区域修正，防止内容显示不全
            if (parentLeft < getPaddingLeft())
                parentLeft = getPaddingLeft();
            if (parentRight > (right - left - getPaddingRight()))
                parentRight = right - left - getPaddingRight();
            if (parentTop < getPaddingTop())
                parentTop = getPaddingTop();
            if (parentBottom > (bottom - top - getPaddingBottom()))
                parentBottom = bottom - top - getPaddingBottom();
        } else {
            parentLeft = getPaddingLeft();
            parentRight = right - left - getPaddingRight();
            parentTop = getPaddingTop();
            parentBottom = bottom - top - getPaddingBottom();
        }

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() != View.GONE) {
                FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }
                int layoutDirection = getLayoutDirection(); //Please ignore this warning , this code work well under the Android 17
                int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                int horizontalGravity = absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
                int childLeft;
                switch (horizontalGravity) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin;
                        } else {
                            childLeft = parentLeft + lp.leftMargin;
                        }
                        break;
                    case Gravity.LEFT:
                        childLeft = parentLeft + lp.leftMargin;
                        break;
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                        break;
                }
                int childTop;
                switch (verticalGravity) {
                    case Gravity.CENTER_VERTICAL:
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                                lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.TOP:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                        break;
                }
                int childRight = childLeft + width;
                int childBottom = childTop + height;
                //根据边角控制 修正 child 显示大小
                if (!mUseCornerArea) {
                    if (childRight > parentRight)
                        childRight = parentRight;
                    if (childBottom > parentBottom)
                        childBottom = parentBottom;
                }
                child.layout(childLeft, childTop, childRight, childBottom);
            }
        }
    }

    @Override
    public void setMinimumWidth(int minWidth) {
        mUserSetMinWidth = minWidth;
        super.setMinimumWidth(minWidth);
    }

    @Override
    public void setMinimumHeight(int minHeight) {
        mUserSetMinHeight = minHeight;
        super.setMinimumHeight(minHeight);
    }

    /**
     * Updates the background color of the CardView
     *
     * @param color The new color to set for the card background
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardBackgroundColor
     */
    public void setCardBackgroundColor(@ColorInt int color) {
        IMPL.setBackgroundColor(mCardViewDelegate, ColorStateList.valueOf(color));
    }

    /**
     * Updates the background ColorStateList of the CardView
     *
     * @param color The new ColorStateList to set for the card background
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardBackgroundColor
     */
    public void setCardBackgroundColor(ColorStateList color) {
        IMPL.setBackgroundColor(mCardViewDelegate, color);
    }

    /**
     * Updates the shadow color of the CardView
     *
     * @param startColor The new startColor to set for the card shadow
     * @param endColor   The new endColor to set for the card shadow
     */
    public void setCardShadowColor(@ColorInt int startColor, @ColorInt int endColor) {
        IMPL.setShadowColor(mCardViewDelegate, startColor, endColor);
    }

    /**
     * update the both of background color and shadow color of the card view
     */
    public void setColors(@ColorInt int backgroundColor, @ColorInt int shadowStartColor, @ColorInt int shadowEndColor) {
        IMPL.setColors(mCardViewDelegate, backgroundColor, shadowStartColor, shadowEndColor);
    }

    /**
     * Returns the background color state list of the CardView.
     *
     * @return The background color state list of the CardView.
     */
    public ColorStateList getCardBackgroundColor() {
        return IMPL.getBackgroundColor(mCardViewDelegate);
    }

    /**
     * Returns the inner padding after the Card's left edge
     *
     * @return the inner padding after the Card's left edge
     */
    public int getContentPaddingLeft() {
        return mContentPadding.left;
    }

    /**
     * Returns the inner padding before the Card's right edge
     *
     * @return the inner padding before the Card's right edge
     */
    public int getContentPaddingRight() {
        return mContentPadding.right;
    }

    /**
     * Returns the inner padding after the Card's top edge
     *
     * @return the inner padding after the Card's top edge
     */
    public int getContentPaddingTop() {
        return mContentPadding.top;
    }

    /**
     * Returns the inner padding before the Card's bottom edge
     *
     * @return the inner padding before the Card's bottom edge
     */
    public int getContentPaddingBottom() {
        return mContentPadding.bottom;
    }

    /**
     * Updates the corner radius of the CardView.
     *
     * @param radius The radius in pixels of the corners of the rectangle shape
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardCornerRadius
     * @see .setRadius
     */
    public void setRadius(Float radius) {
        IMPL.setRadius(mCardViewDelegate, radius);
    }

    /**
     * Returns the corner radius of the CardView.
     *
     * @return Corner radius of the CardView
     * @see .getRadius
     */
    public Float getRadius() {
        return IMPL.getRadius(mCardViewDelegate);
    }

    /**
     * Updates the backward compatible elevation of the CardView.
     *
     * @param elevation The backward compatible elevation in pixels.
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardElevation
     * @see .getCardElevation
     * @see .setMaxCardElevation
     */
    public void setCardElevation(Float elevation) {
        IMPL.setElevation(mCardViewDelegate, elevation);
    }

    /**
     * Returns the backward compatible elevation of the CardView.
     *
     * @return Elevation of the CardView
     * @see .setCardElevation
     * @see .getMaxCardElevation
     */
    public Float getCardElevation() {
        return IMPL.getElevation(mCardViewDelegate);
    }

    /**
     * Updates the backward compatible maximum elevation of the CardView.
     * <p>
     * <p>
     * Calling this method has no effect if device OS version is Lollipop or newer and
     * [.getUseCompatPadding] is `false`.
     *
     * @param maxElevation The backward compatible maximum elevation in pixels.
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardMaxElevation
     * @see .setCardElevation
     * @see .getMaxCardElevation
     */
    public void setMaxCardElevation(Float maxElevation) {
        IMPL.setMaxElevation(mCardViewDelegate, maxElevation);
    }

    /**
     * Returns the backward compatible maximum elevation of the CardView.
     *
     * @return Maximum elevation of the CardView
     * @see .setMaxCardElevation
     * @see .getCardElevation
     */
    public Float getMaxCardElevation() {
        return IMPL.getMaxElevation(mCardViewDelegate);
    }

    /**
     * Returns whether CardView should add extra padding to content to avoid overlaps with rounded
     * corners on pre-Lollipop platforms.
     *
     * @return True if CardView prevents overlaps with rounded corners on platforms before Lollipop.
     * Default value is `true`.
     */
    public Boolean getPreventCornerOverlap() {
        return mPreventCornerOverlap;
    }

    /**
     * On pre-Lollipop platforms, CardView does not clip the bounds of the Card for the rounded
     * corners. Instead, it adds padding to content so that it won't overlap with the rounded
     * corners. You can disable this behavior by setting this field to `false`.
     * <p>
     * <p>
     * Setting this value on Lollipop and above does not have any effect unless you have enabled
     * compatibility padding.
     *
     * @param preventCornerOverlap Whether CardView should add extra padding to content to avoid
     *                             overlaps with the CardView corners.
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardPreventCornerOverlap
     * @see .setUseCompatPadding
     */
    public void setPreventCornerOverlap(Boolean preventCornerOverlap) {
        if (preventCornerOverlap != mPreventCornerOverlap) {
            mPreventCornerOverlap = preventCornerOverlap;
            IMPL.onPreventCornerOverlapChanged(mCardViewDelegate);
        }
    }
}
