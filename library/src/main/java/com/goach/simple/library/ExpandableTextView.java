/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright 2014 Manabu Shimobe
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

package com.goach.simple.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.goach.simple.library.utils.DeviceUtil;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;


/**
 * @author gzc
 * github原始地址
 * https://github.com/Manabu-GT/ExpandableTextView
 *
 * modify author  Goach.zhong
 * 优化的一些地方：
 *  1、mCollapsedHeight 为0问题，因为收起和展开是两个ExpandableTextView,所以就始终为0
 *  2、偶尔出现收起位置不对问题，这个是动画未执行end方法，修改动画即可
 *  3、在RecyclerView使用的时候，展开和收起状态未保存问题
 *  4、TextView不支持<img>标签，解决展示图片问题。
 *  5、展开和收缩实现折叠效果，再文字满一行才换行，不满一行，展开和文字在同一行
 */
public class ExpandableTextView extends LinearLayout implements View.OnClickListener {
    private static final String TAG = ExpandableTextView.class.getSimpleName();

    private static final int EXPAND_INDICATOR_IMAGE_BUTTON = 0;

    private static final int EXPAND_INDICATOR_TEXT_VIEW = 1;

    private static final int DEFAULT_TOGGLE_TYPE = EXPAND_INDICATOR_TEXT_VIEW;

    /* The default number of lines */
    private static final int MAX_COLLAPSED_LINES = 2;

    /* The default animation duration */
    private static final int DEFAULT_ANIM_DURATION = 200;

    /* The default alpha value when the animation starts */
    private static final float DEFAULT_ANIM_ALPHA_START = 0.7f;

    protected RichTextView mTv;

    protected View mToggleView; // View to expand/collapse

    private boolean mRelayout;

    private boolean mCollapsed = true; // Show short version as default.

    private int mCollapsedWidth;

    private int mTextHeightWithMaxLines;

    private int mMaxCollapsedLines;

    private int mMarginBetweenTxtAndBottom;

    private ExpandIndicatorController mExpandIndicatorController;

    private int mAnimationDuration;

    private float mAnimAlphaStart;

    private boolean mAnimating;

    @IdRes
    private int mExpandableTextId = R.id.expandable_text;

    @IdRes
    private int mExpandCollapseToggleId = R.id.expandable_state_text;

    private boolean mExpandToggleOnTextClick;

    /* Listener for callback */
    private OnExpandStateChangeListener mListener;

    /* For saving collapsed status when used in ListView */
    private SparseBooleanArray mCollapsedStatus;
    private int mPosition;
    private boolean isOverlap;

    public ExpandableTextView(Context context) {
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    @Override
    public void setOrientation(int orientation){
        if(LinearLayout.HORIZONTAL == orientation){
            throw new IllegalArgumentException("ExpandableTextView only supports Vertical Orientation.");
        }
        super.setOrientation(orientation);
    }

    @Override
    public void onClick(View view) {
        if (mToggleView.getVisibility() != View.VISIBLE) {
            return;
        }
        mCollapsed = !mCollapsed;
        if(isOverlap){
            setOverlap();
        }
        mExpandIndicatorController.changeState(mCollapsed);
        if (mCollapsedStatus != null) {
            mCollapsedStatus.put(mPosition, mCollapsed);
        }
        // mark that the animation is in progress
        mAnimating = true;
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", 1f, 1f);
        animator.setDuration(mAnimationDuration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int mStartHeight = getHeight();
                int mEndHeight;
                if (mCollapsed) {
                    mEndHeight = getTextViewLineTop(mTv,mMaxCollapsedLines+2);
                } else {
                    mEndHeight = getHeight() + mTextHeightWithMaxLines - mTv.getHeight();
                }
                final int newHeight = (int)((mEndHeight - mStartHeight) * animation.getAnimatedFraction() + mStartHeight);
                mTv.setMaxHeight(newHeight - mMarginBetweenTxtAndBottom);
                getLayoutParams().height = newHeight;
                requestLayout();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // clear animation here to avoid repeated applyTransformation() calls
                clearAnimation();
                // clear the animation flag
                mAnimating = false;
                // notify the listener
                if (mListener != null) {
                    mListener.onExpandStateChanged(mTv, !mCollapsed);
                }

            }
        });
        animator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // while an animation is in progress, intercept all the touch events to children to
        // prevent extra clicks during the animation
        return mAnimating;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        findViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If no change, measure and return
        if (!mRelayout || getVisibility() == View.GONE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        mRelayout = false;

        // Setup with optimistic case
        // i.e. Everything fits. No button needed
        mToggleView.setVisibility(View.GONE);
        mTv.setMaxLines(Integer.MAX_VALUE);

        // Measure
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // If the text fits in collapsed mode, we are done.
        if (mTv.getLineCount() <= mMaxCollapsedLines) {
            return;
        }

        // Saves the text height w/ max lines
        mTextHeightWithMaxLines = getRealTextViewHeight(mTv);

        // Doesn't fit in collapsed mode. Collapse text view as needed. Show
        // button.
        if (mCollapsed) {
            mTv.setMaxLines(mMaxCollapsedLines);
        }
        mToggleView.setVisibility(View.VISIBLE);

        // Re-measure with new setup
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mCollapsed) {
            // Gets the margin between the TextView's bottom and the ViewGroup's bottom
            mTv.post(new Runnable() {
                @Override
                public void run() {
                    mMarginBetweenTxtAndBottom = getHeight() - mTv.getHeight();
                    mCollapsedWidth = mTv.getMeasuredWidth();
                }
            });
        }
    }

    public void setOnExpandStateChangeListener(@Nullable OnExpandStateChangeListener listener) {
        mListener = listener;
    }

    public void setText(@Nullable CharSequence text) {
        mRelayout = true;
        if(TextUtils.isEmpty(text) || !text.toString().contains("<img")){
            mTv.setText(TextUtils.isEmpty(text)?"": Html.fromHtml(text.toString()));
        }else{
            mTv.setHtml(text.toString());
        }
        setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        clearAnimation();
        getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if(isOverlap){
            setOverlap();
        }
        requestLayout();
    }

    public void setText(@Nullable CharSequence text, @NonNull SparseBooleanArray collapsedStatus, int position) {
        mCollapsedStatus = collapsedStatus;
        mPosition = position;
        boolean isCollapsed = collapsedStatus.get(position, true);
        clearAnimation();
        mCollapsed = isCollapsed;
        mExpandIndicatorController.changeState(mCollapsed);
        setText(text);
    }

    @Nullable
    public CharSequence getText() {
        if (mTv == null) {
            return "";
        }
        return mTv.getText();
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ExpandableTextView);
        mMaxCollapsedLines = typedArray.getInt(R.styleable.ExpandableTextView_maxCollapsedLines, MAX_COLLAPSED_LINES);
        mAnimationDuration = typedArray.getInt(R.styleable.ExpandableTextView_animDuration, DEFAULT_ANIM_DURATION);
        mAnimAlphaStart = typedArray.getFloat(R.styleable.ExpandableTextView_animAlphaStart, DEFAULT_ANIM_ALPHA_START);
        mExpandableTextId = typedArray.getResourceId(R.styleable.ExpandableTextView_expandableTextId, R.id.expandable_text);
        mExpandCollapseToggleId = typedArray.getResourceId(R.styleable.ExpandableTextView_expandCollapseToggleId, R.id.expandable_state_text);
        mExpandToggleOnTextClick = typedArray.getBoolean(R.styleable.ExpandableTextView_expandToggleOnTextClick, true);
        isOverlap = typedArray.getBoolean(R.styleable.ExpandableTextView_isOverlap, false);
        mExpandIndicatorController = setupExpandToggleController(getContext(), typedArray);

        typedArray.recycle();
        // enforces vertical orientation
        setOrientation(LinearLayout.VERTICAL);

        // default visibility is gone
        setVisibility(GONE);
    }

    private void findViews() {
        mTv = (RichTextView) findViewById(mExpandableTextId);
        if (mExpandToggleOnTextClick) {
            mTv.setOnClickListener(this);
        } else {
            mTv.setOnClickListener(null);
        }
        mToggleView = findViewById(mExpandCollapseToggleId);
        mExpandIndicatorController.setView(mToggleView);
        mExpandIndicatorController.changeState(mCollapsed);
        mToggleView.setOnClickListener(this);
    }
    public void setOverlap(){
        if(mTv == null || mToggleView == null){
            return;
        }
        String showContent = getText() == null ? "" :getText().toString();
        if(TextUtils.isEmpty(showContent) || !(mToggleView instanceof TextView)){
            return;
        }
        if(mCollapsedWidth > 0){
            handlerOverlap();
        }
        mTv.post(new Runnable() {
            @Override
            public void run() {
                mCollapsedWidth = mTv.getMeasuredWidth();
                handlerOverlap();
            }
        });
    }
    private void handlerOverlap(){
        TextView mExpandFootView = (TextView) mToggleView;
        LinearLayout.LayoutParams params = (LayoutParams) mExpandFootView.getLayoutParams();
        float lineLeft ;
        float lineRight;
        boolean isSpace = false;
        if(mTv.getLayout() != null){
            lineLeft = mTv.getLayout().getLineLeft(mTv.getLineCount() -1);
            lineRight = mTv.getLayout().getLineRight(mTv.getLineCount() -1);
            isSpace = mCollapsedWidth - (lineRight - lineLeft) >=
                    mExpandFootView.getPaint().measureText(mExpandFootView.getText().toString());
        }
        if(mCollapsed || isSpace){
            int bgPadding = DeviceUtil.instance().dip2px(2,getContext());//背景图和line间距偏移
            params.topMargin =  - (mTv.getLineHeight() + bgPadding);
        }else{
            params.topMargin = 0;
        }
        mExpandFootView.setLayoutParams(params);
    }
    private static boolean isPostLolipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
    public void resetMeasure(){
        mRelayout = true;
        requestLayout();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        Resources resources = context.getResources();
        if (isPostLolipop()) {
            return resources.getDrawable(resId, context.getTheme());
        } else {
            return resources.getDrawable(resId);
        }
    }

    private static int getRealTextViewHeight(@NonNull TextView textView) {
        int textHeight = textView.getLayout().getLineTop(textView.getLineCount());
        int padding = textView.getCompoundPaddingTop() + textView.getCompoundPaddingBottom();
        return textHeight + padding;
    }

    public static int getTextViewLineTop(TextView textView, int num){
        int textHeight = textView.getLayout().getLineTop(num);
        int padding = textView.getCompoundPaddingTop() + textView.getCompoundPaddingBottom();
        return textHeight + padding;
    }

    private static ExpandIndicatorController setupExpandToggleController(@NonNull Context context, TypedArray typedArray) {
        final int expandToggleType = typedArray.getInt(R.styleable.ExpandableTextView_expandToggleType, DEFAULT_TOGGLE_TYPE);
        final ExpandIndicatorController expandIndicatorController;
        switch (expandToggleType) {
            case EXPAND_INDICATOR_IMAGE_BUTTON:
                Drawable expandDrawable = typedArray.getDrawable(R.styleable.ExpandableTextView_expandIndicator);
                Drawable collapseDrawable = typedArray.getDrawable(R.styleable.ExpandableTextView_collapseIndicator);
                if (expandDrawable == null) {
                    expandDrawable = getDrawable(context, R.drawable.a_small_developdown);
                }
                if (collapseDrawable == null) {
                    collapseDrawable = getDrawable(context, R.drawable.a_small_developup);
                }
                expandIndicatorController = new ImageButtonExpandController(expandDrawable, collapseDrawable);
                break;
            case EXPAND_INDICATOR_TEXT_VIEW:
                String expandText = typedArray.getString(R.styleable.ExpandableTextView_expandIndicator);
                String collapseText = typedArray.getString(R.styleable.ExpandableTextView_collapseIndicator);
                expandIndicatorController = new TextViewExpandController(expandText, collapseText);
                break;
            default:
                throw new IllegalStateException("Must be of enum: ExpandableTextView_expandToggleType, one of EXPAND_INDICATOR_IMAGE_BUTTON or EXPAND_INDICATOR_TEXT_VIEW.");
        }

        return expandIndicatorController;
    }

    public interface OnExpandStateChangeListener {
        /**
         * Called when the expand/collapse animation has been finished
         *
         * @param textView - TextView being expanded/collapsed
         * @param isExpanded - true if the TextView has been expanded
         */
        void onExpandStateChanged(TextView textView, boolean isExpanded);
    }

    interface ExpandIndicatorController {
        void changeState(boolean collapsed);

        void setView(View toggleView);
    }

    static class ImageButtonExpandController implements ExpandIndicatorController {

        private final Drawable mExpandDrawable;
        private final Drawable mCollapseDrawable;

        private ImageButton mImageButton;

        public ImageButtonExpandController(Drawable expandDrawable, Drawable collapseDrawable) {
            mExpandDrawable = expandDrawable;
            mCollapseDrawable = collapseDrawable;
        }

        @Override
        public void changeState(boolean collapsed) {
            mImageButton.setImageDrawable(collapsed ? mExpandDrawable : mCollapseDrawable);
        }

        @Override
        public void setView(View toggleView) {
            mImageButton = (ImageButton) toggleView;
        }
    }

    static class TextViewExpandController implements ExpandIndicatorController {

        private final String mExpandText;
        private final String mCollapseText;

        private TextView mTextView;

        public TextViewExpandController(String expandText, String collapseText) {
            mExpandText = expandText;
            mCollapseText = collapseText;
        }

        @Override
        public void changeState(boolean collapsed) {
            mTextView.setText(collapsed ? mExpandText : mCollapseText);
        }

        @Override
        public void setView(View toggleView) {
            mTextView = (TextView) toggleView;
        }
    }
    public void clear(){
        if(mTv !=null){
            mTv.clear();
        }
    }
}