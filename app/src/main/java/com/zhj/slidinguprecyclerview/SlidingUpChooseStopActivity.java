/*
 * Copyright 2014 Soichiro Kashima
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhj.slidinguprecyclerview;

import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.TouchInterceptionFrameLayout;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SlidingUpChooseStopActivity extends BaseActivity implements ObservableScrollViewCallbacks {

    private static final String STATE_SLIDING_STATE = "slidingState";
    protected static final int SLIDING_STATE_TOP = 0;
    protected static final int SLIDING_STATE_MIDDLE = 1;
    protected static final int SLIDING_STATE_BOTTOM = 2;
    protected static final int SLIDING_STATE_INIT = 3;

    @BindView(R.id.header)
    FrameLayout mHeader;
    @BindView(R.id.rl_stop_choose)
    RelativeLayout mRlStopChoose;
    @BindView(R.id.ll_translate)
    LinearLayout mLlTranslate;
    @BindView(R.id.im_point)
    ImageView mImPoint;
    @BindView(R.id.tv_busstation_name)
    TextView mTvBusstationName;
    @BindView(R.id.station_city)
    TextView mTvStationCity;
    @BindView(R.id.checkin_angle_right)
    ImageView mCheckinAngleRight;
    @BindView(R.id.ll_check)
    LinearLayout mLlCheck;
    @BindView(R.id.fram_map_offline_button)
    FrameLayout mFramMapOfflineButton;

    @BindView(R.id.tv_product_route)
    TextView mTvProductRoute;
    @BindView(R.id.tv_product_time)
    TextView mTvProductTime;
    private ObservableRecyclerView mScrollable;
    private TouchInterceptionFrameLayout mInterceptionLayout;

    // Fields that just keep constants like resource values
    private int mIntersectionHeight;
    private int mHeaderBarHeight;
    private int mSlidingSlop;

    // Fields that needs to saved
    private int mSlidingState;

    // Temporary states
    private float mInitialY;
    /**
     * 滑动距离
     */
    private float mMovedDistanceY;
    private float mScrollYOnDownMotion;

    private Button mBtnChooseDestination;
    private LocationManager mLocationManager;
    private ObservableRecyclerView mRecyclerView;
    private ArrayList<String> mItems;
    private int mWindowHeight;
    private int mHeaderHeight;
    private int mCheckinAngleRightHeight;
    private int mTvStopNameHeight;
    /**
     * 禁止滑块滑动
     */
    private int mTopBarHeight;
    private int mSlidY;
    private LinearLayoutManager mLinearLayoutManager;
    /**
     * 初始化recyclerview
     *
     * @return
     */
    protected ObservableRecyclerView createScrollable() {

        mRecyclerView = (ObservableRecyclerView) findViewById(R.id.scroll);
        mRecyclerView.setScrollViewCallbacks(this);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mItems = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            mItems.add("Item " + i);
        }
        //监听得到初始布局高度
        ViewTreeObserver vto2 = mHeader.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {



            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    mHeader.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                } else {
                    mHeader.getViewTreeObserver()
                            .removeGlobalOnLayoutListener(this);
                }
                mHeaderHeight = mHeader.getHeight();
                mCheckinAngleRightHeight = mCheckinAngleRight.getHeight();
            }
        });
        return mRecyclerView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        StatusBarUtils.setWindowStatusBarColor(SlidingUpChooseStopActivity.this, R.color.black);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_slidinguprecyclerview);
        ButterKnife.bind(this);

        mIntersectionHeight = getResources().getDimensionPixelSize(R.dimen.intersection_height);
        mHeaderBarHeight = getResources().getDimensionPixelSize(R.dimen.header_bar_height);
        mSlidingSlop = getResources().getDimensionPixelSize(R.dimen.sliding_slop);
        mScrollable = createScrollable();
        if (savedInstanceState == null) {
            mSlidingState = SLIDING_STATE_BOTTOM;
        }

        initData();
        initView();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // All the related temporary states can be restored by slidingState
        mSlidingState = savedInstanceState.getInt(STATE_SLIDING_STATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SLIDING_STATE, mSlidingState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }

    private void setRect(Rect rect, View getHitRect, int offsetDistance, int paddingleft) {
        getHitRect.getHitRect(rect);
        rect.set(rect.left + paddingleft, rect.top + offsetDistance, rect.right + paddingleft, rect.bottom + offsetDistance);
    }

    /**
     * 触摸拦截
     */
    private TouchInterceptionFrameLayout.TouchInterceptionListener mInterceptionListener = new TouchInterceptionFrameLayout.TouchInterceptionListener() {
        @Override
        public boolean shouldInterceptTouchEvent(MotionEvent ev, boolean moving, float diffX, float diffY) {
            final int minInterceptionLayoutY = -mIntersectionHeight;
            // slight fix for untappable floating action button for larger screens
//            Rect rect = new Rect();
//                if (rect.contains((int) ev.getX(), (int) ev.getY()) || rect.contains((int) ev.getX(), (int) ev.getY())) {
//                    return false;
//                } else {
                    return minInterceptionLayoutY < (int) ViewHelper.getY(mInterceptionLayout)
                            || (moving && mScrollable.getCurrentScrollY() - diffY < 0);
//                }
        }


        @Override
        public void onDownMotionEvent(MotionEvent ev) {
            //得到recyclerview滑动的距离，弃用
            mScrollYOnDownMotion = mScrollable.getCurrentScrollY();
            mInitialY = ViewHelper.getTranslationY(mInterceptionLayout);
        }

        @Override
        public void onMoveMotionEvent(MotionEvent ev, float diffX, float diffY) {
            float translationY = ViewHelper.getTranslationY(mInterceptionLayout) + diffY;
            //控制滑动的范围
            if (translationY < -mIntersectionHeight) {
                translationY = -mIntersectionHeight + 5;
            } else if (getScreenHeight() - mHeaderBarHeight < translationY) {
                translationY = getScreenHeight() - mHeaderBarHeight;
            }

            slideTo(translationY, true);
            mMovedDistanceY = ViewHelper.getTranslationY(mInterceptionLayout) - mInitialY;
            stickToAnchors();
        }

        @Override
        public void onUpOrCancelMotionEvent(MotionEvent ev) {
            stickToAnchors();
        }
    };


    public void changeSlidingState(final int slidingState, boolean animated) {
        //获取屏幕宽高
        Display display = getWindowManager().getDefaultDisplay();
        mWindowHeight = display.getHeight();

        mSlidingState = slidingState;
        float translationY = 0;
        switch (slidingState) {
            case SLIDING_STATE_TOP:
                translationY = mTopBarHeight;
                break;
            case SLIDING_STATE_MIDDLE:
                translationY = getScreenHeight() / 2;
                break;
            case SLIDING_STATE_BOTTOM:
                translationY = getAnchorYBottom();
                break;
            case SLIDING_STATE_INIT:
                translationY = mWindowHeight;
        }
        if (animated) {
            slideWithAnimation(translationY);
        } else {

            slideTo(translationY, false);
        }
    }

    private void slideOnClick() {
        float translationY = ViewHelper.getTranslationY(mInterceptionLayout);
        if (translationY == getAnchorYBottom()) {
            changeSlidingState(SLIDING_STATE_MIDDLE, true);
        }
    }

    private void stickToAnchors() {
        // Slide to some points automatically 松开手自动滑动到某些点
        if (0 < mMovedDistanceY) {
            // Sliding down
            if (mSlidingSlop / 8 < mMovedDistanceY) {
                // Sliding down to an anchor
                if (getScreenHeight() / 2 < ViewHelper.getTranslationY(mInterceptionLayout)) {
                    changeSlidingState(SLIDING_STATE_BOTTOM, true);
                } else {
                    changeSlidingState(SLIDING_STATE_MIDDLE, true);
                }
            } else {
                changeSlidingState(mSlidingState, true);
            }
        } else if (mMovedDistanceY < 0) {
            // Sliding up
            if (mMovedDistanceY < -mSlidingSlop / 8) {
                // Sliding up to an anchor
                if (getScreenHeight() / 2 < ViewHelper.getTranslationY(mInterceptionLayout)) {
                    changeSlidingState(SLIDING_STATE_MIDDLE, true);
                } else {
                    changeSlidingState(SLIDING_STATE_TOP, true);
                }
            } else {
                changeSlidingState(mSlidingState, true);
            }
        }
    }

    protected void slideTo(float translationY, final boolean animated) {
        //移动控件
        ViewHelper.setTranslationY(mInterceptionLayout, translationY);

        if (translationY < 0) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInterceptionLayout.getLayoutParams();
            lp.height = (int) -translationY + getScreenHeight();
            mInterceptionLayout.requestLayout();
        }
    }

    private void slideWithAnimation(float toY) {
        float layoutTranslationY = ViewHelper.getTranslationY(mInterceptionLayout);
        if (layoutTranslationY != toY) {
            ValueAnimator animator = ValueAnimator.ofFloat(ViewHelper.getTranslationY(mInterceptionLayout), toY).setDuration(200);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    slideTo((float) animation.getAnimatedValue(), true);
                }
            });
            animator.start();
        }
    }


    private float getAnchorYBottom() {
        return getScreenHeight() - mHeader.getHeight();
    }

    private void initView() {
//        Drawable fontDrawable = CompanyUtils.getFontDrawable(getResources().getColor(R.color.gray1), 15, SlidingUpChooseStopActivity.this, FontAwesome.Icon.faw_map_marker);
//        mImPoint.setImageDrawable(fontDrawable);

        //滑动的模块
        mInterceptionLayout = (TouchInterceptionFrameLayout) findViewById(R.id.scroll_wrapper);
        mInterceptionLayout.setScrollInterceptionListener(mInterceptionListener);
        //设置滑块初始位置
        changeSlidingState(SLIDING_STATE_INIT, false);
        mFramMapOfflineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSlidingState(SLIDING_STATE_BOTTOM, false);
            }
        });
    }

    //初始化数据
    private void initData() {
    }
}
