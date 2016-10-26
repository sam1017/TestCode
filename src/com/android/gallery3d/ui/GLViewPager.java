package com.android.gallery3d.ui;
/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月24日 --- 下午2:27:21
 *
 */

import com.android.gallery3d.app.AbstractGalleryActivity;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.GLCanvas;

public class GLViewPager extends GLView {
    private static final String TAG = "GLViewPager";

    public static final int SCROLL_STATE_IDLE = ViewPager.SCROLL_STATE_IDLE;
    public static final int SCROLL_STATE_DRAGGING = ViewPager.SCROLL_STATE_DRAGGING;
    public static final int SCROLL_STATE_SETTLING = ViewPager.SCROLL_STATE_SETTLING;
    private int mScrollState = SCROLL_STATE_IDLE;

    public static int TAB_COUNT = 2;
 
    private static final int INDEX_NONE = -1;
    public static final int TAB_INDEX_CAMERA = 0;
    public static final int TAB_INDEX_ALL = 1;

    private GLView[] mGlViews;
    private Rect[] mBoxs;
 
    private int mCurrentIndex = 0;
    private int mPageScroll = 0;
    private int mScrollX;
    private boolean mHorizontalEnable;
    private Context mContext;
    private AbstractGalleryActivity mActivity;
    private int mScrollLimit;
    private int mDefaultBackground;
    private final DownUpDetector mDownUpDetector;
    private boolean mUserOperate;

    private GLView mMotionTarget;
    private boolean hasDispatchMotionEventToChild;

    private int mStartVisible;
    private int mStartEnd;

    private static final int MIN_FLING_VELOCITY = 400; // dips

    public GLViewPager(AbstractGalleryActivity activity) {
        mContext = activity.getAndroidContext();
        mActivity = activity;
        mDefaultBackground = activity.getResources().getColor(R.color.default_background);
        mDownUpDetector = new DownUpDetector(new MyDownUpListener());
        mGlViews = new GLView[TAB_COUNT];
        mBoxs = new Rect[TAB_COUNT];

        for (int i = 0; i < TAB_COUNT; i++) {
            mGlViews[i] = new GLViewPagerRoot();
            addComponent(mGlViews[i]);
        }
    	Log.w(TAG,"GLViewPager");
        initViewPager();
    }

    private class MyDownUpListener implements DownUpDetector.DownUpListener {
        @Override
        public void onDown(MotionEvent e) {
            mUserOperate = true;
        }

        @Override
        public void onUp(MotionEvent e) {
            mUserOperate = false;
        }
    }

    void initViewPager() {
        final Context context = mContext;
        mScroller = new Scroller(context, sInterpolator);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        // mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        final float density = context.getResources().getDisplayMetrics().density;
        mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        // final float density =
        // context.getResources().getDisplayMetrics().density;
        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
        mCloseEnough = (int) (CLOSE_ENOUGH * density);
        mDefaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);
    	Log.w(TAG,"initViewPager");
    }

    public void setContentPane(int tabIndex, GLView content) {
        Log.w(TAG,"setContentPane tabIndex = " + tabIndex);
        Utils.assertTrue(tabIndex >= 0 && tabIndex < TAB_COUNT, "tabIndex is %s not in [0,%s]",
                tabIndex, TAB_COUNT);
        mGlViews[tabIndex].removeAllComponents();
        if (content != null) {
            mGlViews[tabIndex].addComponent(content);
        }
        requestLayout();
    }

    private void updateVisible() {
        int position = mScroller.getCurrX();
        int start = 0;
        int end = start;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (mBoxs[i].contains(position, 0)) {
                start = i;
                break;
            }
        }
        end = start;
        position += getWidth() - 1;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (mBoxs[i].contains(position, 0)) {
                end = i;
                break;
            }
        }
        mStartVisible = start;
        mStartEnd = end;
    }

    @Override
    protected void render(GLCanvas canvas) {
        canvas.fillRect(0, 0, getWidth(), getHeight(), mDefaultBackground);

        Log.w(TAG,"render mHorizontalEnable = " + mHorizontalEnable);
        if (mHorizontalEnable) {
            boolean more = mScroller.computeScrollOffset();
            int scrollX = mScroller.getCurrX();
            if (scrollX < 0) {
                scrollX = 0;
            } else if (scrollX > (TAB_COUNT - 1) * getWidth()) {
                scrollX = (TAB_COUNT - 1) * getWidth();
            }

            int finalX = mScroller.getFinalX();
            updateVisible();
            for (int i = mStartVisible; i <= mStartEnd; i++) {
                renderVisibleItem(canvas, i, scrollX);
            }
            if (more) {
                pageScrolled(scrollX);
                invalidate();
            }
            if (DEBUG) {
                Log.d(TAG, "render1: " + scrollX + ", " + finalX + ", mScrollState="
                        + mScrollState + ", more=" + more + ", mIsBeingDragged=" + mIsBeingDragged
                        + ", mUserOperate=" + mUserOperate);
            }
            if (!mUserOperate && (!more || scrollX == finalX)) {
                mScroller.abortAnimation();
                int destX = mCurrentIndex * getWidth();
                if (scrollX != destX) {
                    smoothScrollTo(destX, 0, 2000);
                } else {
                    completeScroll();
                }
            }
            if (DEBUG) {
                Log.i(TAG, "render2: " + mScroller.getCurrX() + ", " + mScroller.getFinalX()
                        + ", mScrollState=" + mScrollState + ", more=" + more
                        + ", mIsBeingDragged=" + mIsBeingDragged + ", mUserOperate=" + mUserOperate);
            }
        } else {
            canvas.save(GLCanvas.SAVE_FLAG_ALL);
            canvas.clipRect(0, 0, getWidth(), getHeight());
            mGlViews[mCurrentIndex].render(canvas);
            canvas.restore();
        }
    }

    private void renderVisibleItem(GLCanvas canvas, int index, int scrollX) {
        int Xoffset = -scrollX + index * getWidth();
        canvas.save(GLCanvas.SAVE_FLAG_ALL);
        canvas.translate(Xoffset, 0);
        canvas.clipRect(0, 0, getWidth(), getHeight());
        mGlViews[index].render(canvas);
        canvas.restore();
    }

    private void renderCentre(GLCanvas canvas, int scrollX) {
        int index = mCurrentIndex;
        int Xoffset = -scrollX + index * getWidth();
        canvas.save(GLCanvas.SAVE_FLAG_ALL);
        canvas.translate(Xoffset, 0);
        canvas.clipRect(0, 0, getWidth(), getHeight());
        mGlViews[mCurrentIndex].render(canvas);
        canvas.restore();
    }

    private void renderRight(GLCanvas canvas, int scrollX) {
        if (mCurrentIndex + 1 >= TAB_COUNT) {
            return;
        }

        int index = mCurrentIndex + 1;
        int Xoffset = -scrollX + index * getWidth();
        canvas.save(GLCanvas.SAVE_FLAG_ALL);
        canvas.translate(Xoffset, 0);
        canvas.clipRect(0, 0, getWidth(), getHeight());
        mGlViews[mCurrentIndex + 1].render(canvas);
        canvas.restore();
    }

    private void renderLeft(GLCanvas canvas, int scrollX) {
        if (mCurrentIndex - 1 < 0) {
            return;
        }
        int index = mCurrentIndex - 1;
        int Xoffset = -scrollX + index * getWidth();
        canvas.save(GLCanvas.SAVE_FLAG_ALL);
        canvas.translate(Xoffset, 0);
        canvas.clipRect(0, 0, getWidth(), getHeight());
        renderChild(canvas, mGlViews[index]);
        canvas.restore();
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        mScrollLimit = (TAB_COUNT - 1) * (right - left);
		Log.w(TAG,"onLayout mHorizontalEnable = " + mHorizontalEnable);
        if (mHorizontalEnable) {
            for (int i = 0; i < TAB_COUNT; i++) {
                mGlViews[i].layout(left, top, right, bottom);
            }
        } else {
            mGlViews[mCurrentIndex].layout(left, top, right, bottom);
        }

        layoutBox(right - left, bottom - top);
        mScroller.abortAnimation();
        mScroller.setFinalX(mCurrentIndex * (right - left));
        super.onLayout(changeSize, left, top, right, bottom);
    }

    private void layoutBox(int w, int h) {
        for (int i = 0; i < TAB_COUNT; i++) {
            mBoxs[i] = new Rect(i * w, 0, (i + 1) * w, h);
        }
    }

    public GLView getCurrentGLViewRoot() {
        return mGlViews[mCurrentIndex];
    }

    @Override
    protected boolean onTouch(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        boolean needsInvalidate = false;

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (!mIsBeingDragged) {
                    final int pointerIndex = MotionEventCompat.findPointerIndex(ev,
                            mActivePointerId);
                    final float x = MotionEventCompat.getX(ev, pointerIndex);
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float y = MotionEventCompat.getY(ev, pointerIndex);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (DEBUG)
                        Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
                    if (xDiff > mTouchSlop && xDiff > yDiff) {
                        if (DEBUG)
                            Log.v(TAG, "Starting drag!");
                        mIsBeingDragged = true;
                        mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop
                                : mInitialMotionX - mTouchSlop;
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                // Not else! Note that mIsBeingDragged can be set above.
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                            mActivePointerId);
                    final float x = MotionEventCompat.getX(ev, activePointerIndex);
                    performDrag(x);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(velocityTracker,
                            mActivePointerId);
                    mPopulatePending = true;
                    final int width = getWidth();
                    mScroller.forceFinished(true);
                    final int scrollX = mScroller.getCurrX();
                    final int currentPage = infoForCurrentScrollPosition();
                    final float pageOffset = ((float) scrollX / width - currentPage);
                    final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                            mActivePointerId);
                    final float x = MotionEventCompat.getX(ev, activePointerIndex);
                    final int totalDelta = (int) (x - mInitialMotionX);
                    int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
                            totalDelta);
                    setCurrentItemInternal(nextPage, true, true, initialVelocity);

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    setCurrentItemInternal(mCurrentIndex, true, true);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                final float x = MotionEventCompat.getX(ev, index);
                mLastMotionX = x;
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionX = MotionEventCompat.getX(ev,
                        MotionEventCompat.findPointerIndex(ev, mActivePointerId));
                break;
        }

        invalidate();
        return true;
    }

    private void setScrollState(int newState) {
        if (mScrollState == newState) {
            return;
        }

        mScrollState = newState;
        if (mScrollState == SCROLL_STATE_DRAGGING) {
            mPageScroll = mCurrentIndex;
        }
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(newState);
        }
    }

    @Override
    protected boolean dispatchTouchEvent(MotionEvent event) {
        if (mHorizontalEnable) {
            mDownUpDetector.onTouchEvent(event);
            if (mIsBeingDragged) {
                onTouch(event);
            } else {
                onInterceptTouchEvent(event);
            }
        }

        if (!mIsBeingDragged) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hasDispatchMotionEventToChild = true;
            }
            dispatchTouchEventToChild(event);
        } else if (mIsBeingDragged && hasDispatchMotionEventToChild) {
            hasDispatchMotionEventToChild = false;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            dispatchTouchEventToChild(cancel);
        }

        return true;
    }

    private boolean dispatchTouchEventToChild(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int action = event.getAction();
        if (mMotionTarget != null) {
            if (action == MotionEvent.ACTION_DOWN) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                dispatchTouchEvent(cancel, x, y, mMotionTarget, false);
                mMotionTarget = null;
            } else {
                dispatchTouchEvent(event, x, y, mMotionTarget, false);
                if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    mMotionTarget = null;
                }
                return true;
            }
        }

        if (action == MotionEvent.ACTION_DOWN) {
            // in the reverse rendering order
            GLView component = getComponent(mCurrentIndex);
            if (component.getVisibility() == GLView.VISIBLE) {
                if (dispatchTouchEvent(event, x, y, component, true)) {
                    mMotionTarget = component;
                    return true;
                }
            }
        }

        return true;
    }

    private class GLViewPagerRoot extends GLView {
        @Override
        protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
            if (getComponentCount() > 0) {
                getComponent(0).layout(left, top, right, bottom);
            }
            super.onLayout(changeSize, left, top, right, bottom);
        }
    }

    public void switchTo(int tabIndex) {
    	Log.w(TAG,"switchTo tabIndex = " + tabIndex);
        setCurrentItemInternal(tabIndex, true, false);
        invalidate();
    }

    public void setCurrentTabIndex(int tabIndex) {
    	Log.w(TAG,"setCurrentTabIndex tabIndex = " + tabIndex);
        setCurrentItemInternal(tabIndex, false, false);
        invalidate();
    }

    public int getCurrentTabIndex() {
        return mCurrentIndex;
    }

    public void setHorizontalEnable(boolean enable) {
		Log.w(TAG,"setHorizontalEnable enable = " + enable + " mHorizontalEnable = " + mHorizontalEnable);
        if (enable && !mHorizontalEnable) {
            requestLayout();
        }

        mHorizontalEnable = enable;
        if (mHorizontalEnable) {
            for (int i = 0; i < TAB_COUNT; i++) {
                mGlViews[i].setVisibility(GLView.VISIBLE);
            }
        } else {
            for (int i = 0; i < TAB_COUNT; i++) {
                if (i != mCurrentIndex) {
                    mGlViews[i].setVisibility(GLView.INVISIBLE);
                }
            }
            mGlViews[mCurrentIndex].setVisibility(GLView.VISIBLE);
        }
    }

    // the next codes, see more in android.support.v4.view.ViewPager
    private static final boolean DEBUG = true;
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mFlingDistance;
    private int mCloseEnough;
    private float mInitialMotionX;
    private boolean mIsBeingDragged;
    private boolean mIsUnableToDrag;
    private boolean mIgnoreGutter;
    private int mDefaultGutterSize;
    private int mGutterSize;
    private int mTouchSlop;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mActivePointerId = INVALID_POINTER;
    private static final int INVALID_POINTER = -1;
    // jinpeng modify++
    private Scroller mScroller;
    // private StepperScroller mScroller;
    // jinpeng modify--
    private boolean mPopulatePending;
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int DEFAULT_DURATION = 600; // ms
    private static final int MAX_SETTLE_DURATION = 600; // ms
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
    private static final int CLOSE_ENOUGH = 2; // dp
    private static final int DEFAULT_GUTTER_SIZE = 16; // dips
    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    // see ViewPager.onInterceptTouchEvent(MotionEvent ev)
    private boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG)
                Log.d(TAG, "onInterceptTouchEvent Intercept done!");
            mIsBeingDragged = false;
            mIsUnableToDrag = false;
            mActivePointerId = INVALID_POINTER;
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG)
                    Log.d(TAG, "onInterceptTouchEvent\t Intercept returning true!");
                return true;
            }
            if (mIsUnableToDrag) {
                if (DEBUG)
                    Log.d(TAG, "onInterceptTouchEvent\t Intercept returning false!");
                return false;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have
                 * caught it. Check whether the user has moved far enough from
                 * his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionY is set to the y value
                 * of the down event.
                 */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on
                    // content.
                    break;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float dx = x - mLastMotionX;
                final float xDiff = Math.abs(dx);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = Math.abs(y - mLastMotionY);
                if (DEBUG)
                    Log.d(TAG, "onInterceptTouchEvent\t " + "Moved x to " + x + "," + y + " diff="
                            + xDiff + "," + yDiff);

                if (xDiff > mTouchSlop && xDiff > yDiff) {
                    if (DEBUG)
                        Log.d(TAG, "onInterceptTouchEvent\t Starting drag!");
                    mIsBeingDragged = true;
                    setScrollState(SCROLL_STATE_DRAGGING);
                    mLastMotionX = dx > 0 ? mInitialMotionX + mTouchSlop : mInitialMotionX
                            - mTouchSlop;
                } else {
                    if (yDiff > mTouchSlop) {
                        // The finger has moved enough in the vertical
                        // direction to be counted as a drag... abort
                        // any attempt to drag horizontally, to work correctly
                        // with children that have scrolling containers.
                        if (DEBUG)
                            Log.d(TAG, "onInterceptTouchEvent\t Starting unable to drag!");
                        mIsUnableToDrag = true;
                    }
                }
                if (mIsBeingDragged) {
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch. ACTION_DOWN always refers to
                 * pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsUnableToDrag = false;

                mScroller.computeScrollOffset();
                if (mScrollState == SCROLL_STATE_SETTLING
                        && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    mScroller.abortAnimation();
                    mPopulatePending = false;
                    mIsBeingDragged = true;
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else {
                    mIsBeingDragged = false;
                }

                if (DEBUG)
                    Log.d(TAG, "onInterceptTouchEvent\t " + "Down at " + mLastMotionX + ","
                            + mLastMotionY + " mIsBeingDragged=" + mIsBeingDragged
                            + "mIsUnableToDrag=" + mIsUnableToDrag);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    // see ViewPager.onSecondaryPointerUp(MotionEvent ev)
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    // see ViewPager.isGutterDrag(float x, float dx)
    private boolean isGutterDrag(float x, float dx) {
        return (x < mGutterSize && dx > 0) || (x > getWidth() - mGutterSize && dx < 0);
    }

    private void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternal(item, smoothScroll, always, 0);
    }

    private OnPageChangeListener mOnPageChangeListener;

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    private void notifyPageChange(int newTabIndex) {
        if (newTabIndex != mCurrentIndex) {
            Log.d(TAG, "notifyPageChange: from " + mCurrentIndex + " to " + newTabIndex);
            mCurrentIndex = newTabIndex;
            if (mOnPageChangeListener != null) {
                mOnPageChangeListener.onPageSelected(newTabIndex);
            }
        }
    }

    private void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        // jinpeng modify++
        // if (item == mCurrentIndex && mScroller.getCurrX() == item * getWidth()) {
        if (!always && item == mCurrentIndex /*&& mScroller.getCurrX() == item * getWidth()*/) {
        // jinpeng modify--
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= TAB_COUNT) {
            item = TAB_COUNT - 1;
        }

        int destX = item * getWidth();
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity);
        } else {
            mScroller.startScroll(mScroller.getCurrX(), 0, destX, 0);
        }
        notifyPageChange(item);
    }

    void smoothScrollTo(int x, int y, int velocity) {
        int sx = mScroller.getCurrX();
        int dx = x - sx;
        if (dx == 0) {
            setScrollState(SCROLL_STATE_IDLE);
            return;
        }

        setScrollState(SCROLL_STATE_SETTLING);

        final int width = getWidth();
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth
                * distanceInfluenceForSnapDuration(distanceRatio);

        // jinpeng modify++
        // if (true) { // use default duration
        if (false) {
        // jinpeng modify--
            mScroller.startScroll(sx, 0, dx, 0, DEFAULT_DURATION);
        } else {
            int duration = 0;
            velocity = Math.abs(velocity);
            if (velocity > 0) {
                duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
            } else {
                final float pageDelta = (float) Math.abs(dx) / (getWidth());
                // jinpeng modify++
                // duration = (int) ((pageDelta + 1) * 100);
                duration = (int) ((pageDelta + 1) * 200);
                // jinpeng modify--
            }
            duration = Math.min(duration, MAX_SETTLE_DURATION);
            mScroller.startScroll(sx, 0, dx, 0, duration);
        }
        // jinpeng add++
        invalidate();
        // jinpeng add--
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    private void performDrag(float x) {
        final float deltaX = mLastMotionX - x;
        mLastMotionX = x;

        float oldScrollX = mScroller.getCurrX();
        float scrollX = oldScrollX + deltaX;
        final int width = getWidth();

        //kexiuhua@Plf.MediaApp, modify for : 540605, bound will invalid movement when left or right
        float leftBound = 0;/*-width / 4*/
        float rightBound = width * (TAB_COUNT - 1);/* + width / 4*/
        //kexiuhua@Plf.MediaApp, end

        if (scrollX < leftBound) {
            scrollX = leftBound;
        } else if (scrollX > rightBound) {
            scrollX = rightBound;
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - (int) scrollX;
        mScroller.abortAnimation();
        mScroller.setFinalX((int) scrollX);
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private int infoForCurrentScrollPosition() {
        int index = 0;
        for (int i = 0; i < TAB_COUNT; i++) {
            if (i * getWidth() <= mScroller.getCurrX()
                    && i * getWidth() + getWidth() - 1 > mScroller.getCurrX()) {
                index = i;
                break;
            }
        }
        return index;
    }

    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
        int targetPage;
        if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            // jinpeng modify++
            // targetPage = (int) (currentPage + pageOffset + 0.5f);
            targetPage = (int) (currentPage + pageOffset + 0.7f);
            // jinpeng modify--
        }
        targetPage = Math.max(0, Math.min(targetPage, TAB_COUNT - 1));
        if (mGlViews[targetPage].getComponentCount() > 0) {
            return targetPage;
        } else {
            return currentPage;
        }
    }

    private void completeScroll() {
        if (mScrollState == SCROLL_STATE_SETTLING) {
            setScrollState(SCROLL_STATE_IDLE);
        } else if (!mIsBeingDragged) {
            setScrollState(SCROLL_STATE_IDLE);
        }
    }

    private int mLastXpos;
    private int mLastOffsetPixels;

    private void pageScrolled(int xpos) {
        if (mLastXpos == xpos || xpos < 0 || xpos > (TAB_COUNT - 1) * getWidth()) {
            return;
        }

        final int position = xpos / getWidth();
        final float pageOffset = ((float) xpos / getWidth()) - position;
        final int offsetPixels = (int) (pageOffset * getWidth());
        if (mLastOffsetPixels != offsetPixels) {
            mLastOffsetPixels = offsetPixels;
            onPageScrolled(position, pageOffset, offsetPixels);
        }
    }

    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
    }
}

