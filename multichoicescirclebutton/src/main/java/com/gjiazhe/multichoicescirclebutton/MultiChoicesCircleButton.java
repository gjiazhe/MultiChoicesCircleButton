package com.gjiazhe.multichoicescirclebutton;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gjz on 08/12/2016.
 */

public class MultiChoicesCircleButton extends View {
    private byte mState = STATE_COLLAPSED;
    private static final byte STATE_COLLAPSED = 1;
    private static final byte STATE_EXPANDED = 2;
    private static final byte STATE_COLLAPSING = 3;
    private static final byte STATE_EXPANDING = 4;

    private boolean mParallaxEnabled;

    private boolean mHidden;

    private Drawable mIcon;

    private float mCollapseRadius;
    private float mExpandRadius;
    private float mCircleCentreX;
    private float mCircleCentreY;

    private float mItemRadius;
    private float mItemDistanceToCentre; // The distance from items' centre to button's centre
    private int mItemBackgroundColor;

    private float mCurrentExpandProgress = 0f;
    private float mFromExpandProgress;
    private Animation expandAnimation;
    private Animation collapseAnimation;
    private int mDuration;

    private String mText;
    private float mTextSize;
    private int mTextColor;
    private int mButtonColor;

    private boolean mShowBackgroundShadowEnable;
    private int mBackgroundShadowColor;
    private ArgbEvaluator backgroundEvaluator;

    private Paint mPaint;
    private Camera mCamera;
    private Matrix mMatrix;

    private List<Item> mItems = new ArrayList<>();
    private int mHoverItemIndex = -1;

    private OnSelectedItemListener mSelectedItemListener;
    private OnHoverItemListener mOnHoverItemListener;

    public MultiChoicesCircleButton(Context context) {
        this(context, null);
    }

    public MultiChoicesCircleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiChoicesCircleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiChoicesCircleButton);
        mIcon = typedArray.getDrawable(R.styleable.MultiChoicesCircleButton_mccb_icon);
        mParallaxEnabled = typedArray.getBoolean(R.styleable.MultiChoicesCircleButton_mccb_enableParallax, true);
        mCollapseRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_collapseRadius, dp2px(40));
        mExpandRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_expandRadius, dp2px(120));
        mText = typedArray.getString(R.styleable.MultiChoicesCircleButton_mccb_text);
        mTextSize = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_textSize, sp2px(35));
        mTextColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_textColor, Color.WHITE);
        mButtonColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_buttonColor, Color.parseColor("#FC516A"));
        mDuration = typedArray.getInt(R.styleable.MultiChoicesCircleButton_mccb_duration, 200);
        mItemRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_itemRadius, dp2px(20));
        mItemDistanceToCentre = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_itemDistanceToCentre, mExpandRadius * 2 / 3);
        mItemBackgroundColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_itemBackgroundColor, Color.WHITE);
        mShowBackgroundShadowEnable = typedArray.getBoolean(R.styleable.MultiChoicesCircleButton_mccb_showBackgroundShadow, true);
        mBackgroundShadowColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_backgroundShadowColor, Color.parseColor("#bb757575"));
        typedArray.recycle();

        initPaint();
        initAnimation();
        if (mParallaxEnabled) {
            mCamera = new Camera();
            mMatrix = new Matrix();
        }
        if (mShowBackgroundShadowEnable) {
            backgroundEvaluator = new ArgbEvaluator();
        }
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initAnimation() {
        expandAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mCurrentExpandProgress = mFromExpandProgress + (1 - mFromExpandProgress) * interpolatedTime;
                if (mCurrentExpandProgress >= 1f) {
                    mCurrentExpandProgress = 1f;
                    mState = STATE_EXPANDED;
                }
                invalidate();
                updateBackgroundColor();
            }
        };

        collapseAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mCurrentExpandProgress = mFromExpandProgress  * (1 - interpolatedTime);
                if (mCurrentExpandProgress <= 0f) {
                    mCurrentExpandProgress = 0f;
                    mState = STATE_COLLAPSED;
                }
                invalidate();
                updateBackgroundColor();
            }
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        mCircleCentreX = viewWidth / 2;
        mCircleCentreY = viewHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventY = event.getY();
        float eventX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (actionDownInCircle(eventX, eventY)) {
                    if (mParallaxEnabled) {
                        calculateRotateMatrix(eventX, eventY);
                    }
                    clearAnimation();
                    mFromExpandProgress = mCurrentExpandProgress;
                    mState = STATE_EXPANDING;
                    startExpandAnimation();
                    return true;
                } else {
                    return false;
                }

            case MotionEvent.ACTION_MOVE:
                if (mState == STATE_EXPANDED && actionDownInCircle(eventX, eventY)) {
                    mHoverItemIndex = getSelectedItemIndex(eventX, eventY);
                    if (mHoverItemIndex != -1 && mOnHoverItemListener != null) {
                        mOnHoverItemListener.onHovered(mItems.get(mHoverItemIndex), mHoverItemIndex);
                    }
                }
                if (mParallaxEnabled) {
                    calculateRotateMatrix(eventX, eventY);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mHoverItemIndex != -1 && mState == STATE_EXPANDED) {
                    if (mSelectedItemListener != null) {
                        mSelectedItemListener.onSelected(mItems.get(mHoverItemIndex), mHoverItemIndex);
                    }
                    mHoverItemIndex = -1;
                }
                clearAnimation();
                mFromExpandProgress = mCurrentExpandProgress;
                mState = STATE_COLLAPSING;
                startCollapseAnimation();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean actionDownInCircle(float x, float y) {
        final float currentRadius = (mExpandRadius - mCollapseRadius) * mCurrentExpandProgress + mCollapseRadius;
        double distance = Math.pow(x - mCircleCentreX, 2) + Math.pow(y - mCircleCentreY, 2);
        return distance <= currentRadius * currentRadius;
    }

    private int getSelectedItemIndex(float x, float y) {
        if (!mItems.isEmpty()) {
            for (int i = 0; i < mItems.size(); i++) {
                Item item = mItems.get(i);
                float offsetX = (float) (mItemDistanceToCentre * Math.cos(Math.PI * item.angle / 180));
                float offsetY = (float) (mItemDistanceToCentre * Math.sin(Math.PI * item.angle / 180));
                float itemCentreX = mCircleCentreX - offsetX * mCurrentExpandProgress;
                float itemCentreY = mCircleCentreY - offsetY * mCurrentExpandProgress;
                double distance = Math.pow(x - itemCentreX, 2) + Math.pow(y - itemCentreY, 2);
                if (distance < mItemRadius * mItemRadius) {
                    return i;
                }
            }
        }
        return  -1;
    }

    private void calculateRotateMatrix(float eventX, float eventY) {
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        final int size = Math.max(width, height);

        final float offsetY = mCircleCentreY - eventY;
        final float offsetX = mCircleCentreX - eventX;
        final float rotateX = offsetY / size * 45;
        final float rotateY = -offsetX / size * 45;
        mCamera.save();
        mCamera.rotateX(rotateX);
        mCamera.rotateY(rotateY);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        mMatrix.preTranslate(-mCircleCentreX, -mCircleCentreY);
        mMatrix.postTranslate(mCircleCentreX, mCircleCentreY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Rotate for parallax effect
        if (mParallaxEnabled && (mState == STATE_EXPANDING || mState == STATE_EXPANDED)) {
            canvas.concat(mMatrix);
        }

        // Draw circle
        mPaint.setAlpha(255);
        mPaint.setColor(mButtonColor);
        final float radius = (mExpandRadius - mCollapseRadius) * mCurrentExpandProgress + mCollapseRadius;
        canvas.drawCircle(mCircleCentreX, mCircleCentreY, radius, mPaint);

        if (mState == STATE_COLLAPSED) {
            // Draw icon
            if (mIcon != null) {
                float iconRadius = mCollapseRadius / 3;
                int left = (int) (mCircleCentreX - iconRadius);
                int top = (int) (mCircleCentreY - iconRadius * 2);
                int right = (int) (mCircleCentreX + iconRadius);
                int bottom = (int) (mCircleCentreY);
                mIcon.setBounds(left, top, right, bottom);
                mIcon.draw(canvas);
            }
            return;
        }

        // Draw text
        String text = mHoverItemIndex == -1 ? mText : mItems.get(mHoverItemIndex).text;
        if (text != null && text.length() != 0) {
            mPaint.setTextSize(mTextSize * mCurrentExpandProgress);
            mPaint.setColor(mTextColor);
            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            final float textHeight = fontMetrics.bottom - fontMetrics.top;
            final float baseLineY = mCircleCentreY - radius - textHeight / 2
                    - (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.ascent;
            canvas.drawText(text, mCircleCentreX, baseLineY, mPaint);
        }

        // Draw items
        if (!mItems.isEmpty()) {
            mPaint.setColor(mItemBackgroundColor);
            for (int i = 0; i < mItems.size(); i++) {
                Item item = mItems.get(i);

                mPaint.setAlpha(mHoverItemIndex == i ? 255 : 255*7/10);

                float offsetX = (float) (mItemDistanceToCentre * Math.cos(Math.PI * item.angle / 180));
                float offsetY = (float) (mItemDistanceToCentre * Math.sin(Math.PI * item.angle / 180));
                float itemCentreX = mCircleCentreX - offsetX * mCurrentExpandProgress;
                float itemCentreY = mCircleCentreY - offsetY * mCurrentExpandProgress;
                canvas.drawCircle(itemCentreX, itemCentreY, mItemRadius * mCurrentExpandProgress, mPaint);

                if (item.icon != null) {
                    float iconRadius = mItemRadius * 2 / 3 * mCurrentExpandProgress;
                    int left = (int) (itemCentreX - iconRadius);
                    int top = (int) (itemCentreY - iconRadius);
                    int right = (int) (itemCentreX + iconRadius);
                    int bottom = (int) (itemCentreY + iconRadius);
                    item.icon.setBounds(left, top, right, bottom);
                    item.icon.draw(canvas);
                }
            }
        }
    }

    private void updateBackgroundColor() {
        if (mShowBackgroundShadowEnable) {
            int color = (Integer) backgroundEvaluator.evaluate(mCurrentExpandProgress, Color.TRANSPARENT, mBackgroundShadowColor);
            setBackgroundColor(color);
        }
    }

    private void startExpandAnimation() {
        expandAnimation.setDuration(mDuration);
        startAnimation(expandAnimation);
    }

    private void startCollapseAnimation() {
        collapseAnimation.setDuration(mDuration);
        startAnimation(collapseAnimation);
    }

    private static float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    private static float sp2px(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    public void hide() {
        hide(true);
    }

    public void hide(boolean withAnimation) {
        if (mHidden) {
            return;
        }

        mHidden = true;
        if (mState == STATE_EXPANDED) {
            mState = STATE_COLLAPSED;
            mCurrentExpandProgress = 0;
            mHoverItemIndex = -1;
            invalidate();
        }
        ViewCompat.animate(this)
                .translationY(mCollapseRadius)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setDuration(withAnimation ? 200 : 0)
                .start();
    }

    public void show() {
        show(true);
    }

    public void show(boolean withAnimation) {
        if (!mHidden) {
            return;
        }
        mHidden = false;
        ViewCompat.animate(this)
                .translationY(0)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setDuration(withAnimation ? 200 : 0)
                .start();
    }

    public void setButtonItems(List<Item> items) {
        mHoverItemIndex = -1;
        mItems.clear();
        mItems.addAll(items);
        invalidate();
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
        invalidate();
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public float getCollapseRadius() {
        return mCollapseRadius;
    }

    public void setCollapseRadius(float collapseRadius) {
        this.mCollapseRadius = collapseRadius;
        invalidate();
    }

    public float getExpandRadius() {
        return mExpandRadius;
    }

    public void setExpandRadius(float expandRadius) {
        this.mExpandRadius = expandRadius;
        invalidate();
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
        invalidate();
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        this.mTextSize = textSize;
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        invalidate();
    }

    public int getButtonColor() {
        return mButtonColor;
    }

    public void setButtonColor(int buttonColor) {
        this.mButtonColor = buttonColor;
        invalidate();
    }

    public boolean isParallaxEnabled() {
        return mParallaxEnabled;
    }

    public void setParallaxEnabled(boolean enabled) {
        this.mParallaxEnabled = enabled;
        if (enabled) {
            mCamera = new Camera();
            mMatrix = new Matrix();
        } else {
            mCamera = null;
            mMatrix = null;
        }
    }

    public void setShowBackgroundShadowEnable(boolean show) {
        mShowBackgroundShadowEnable = show;
        if (show) {
            backgroundEvaluator = new ArgbEvaluator();
        } else {
            backgroundEvaluator = null;
        }
    }

    public void setItemRadius(float radius) {
        mItemRadius = radius;
        invalidate();
    }

    public float getItemRadius() {
        return mItemRadius;
    }

    public void setItemDistanceToCentre(float distance) {
        mItemDistanceToCentre = distance;
        invalidate();
    }

    public float getItemDistanceToCentre() {
        return mItemDistanceToCentre;
    }

    public void setItemBackgroundColor(int color) {
        mItemBackgroundColor = color;
        invalidate();
    }

    public int getItemBackgroundColor() {
        return mItemBackgroundColor;
    }

    public boolean isShowBackgroundShadowEnable() {
        return mShowBackgroundShadowEnable;
    }

    public void setBackgroundShadowColor(int color) {
        mBackgroundShadowColor = color;
        invalidate();
    }

    public int getBackgroundShadowColor() {
        return mBackgroundShadowColor;
    }

    public OnSelectedItemListener getOnSelectedItemListener() {
        return mSelectedItemListener;
    }

    public void setOnSelectedItemListener(OnSelectedItemListener mListener) {
        this.mSelectedItemListener = mListener;
    }

    public OnHoverItemListener getOnHoverItemListener() {
        return mOnHoverItemListener;
    }

    public void setOnHoverItemListener(OnHoverItemListener onHoverItemListener) {
        this.mOnHoverItemListener = onHoverItemListener;
    }

    public static class Item {
        private String text;
        private Drawable icon;
        private int angle;

        public Item(String text, Drawable icon , int angle) {
            this.text = text;
            this.icon = icon;
            this.angle = angle;
        }

        public String getText() {
            return text;
        }

        public Drawable getIcon() {
            return icon;
        }

        public int getAngle() {
            return angle;
        }
    }

    public interface OnSelectedItemListener {
        void onSelected(Item item, int index);
    }

    public interface OnHoverItemListener {
        void onHovered(Item item, int index);
    }
}
