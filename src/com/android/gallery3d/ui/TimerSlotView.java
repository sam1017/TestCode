package com.android.gallery3d.ui;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Parcelable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.content.Context;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.ui.SlotView.Listener;
import com.android.gallery3d.ui.SlotView.SlotAnimation;
import com.android.gallery3d.ui.SlotView.SlotRenderer;
import com.android.gallery3d.ui.TimerSlotView.Spec;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;

public class TimerSlotView extends GLView {

	private final TimerSlotViewLayout mLayout;
    private final GestureDetector mGestureDetector;
    private final VelocityHelper mVelocityHelper;
    private static final int INDEX_NONE = -1;
	private static final boolean WIDE = false;
	private static final boolean DEBUG = false;
	private static final String TAG = "TimerSlotView";
    public static final int OVERSCROLL_3D = 0;
    public static final int OVERSCROLL_SYSTEM = 1;
    public static final int OVERSCROLL_NONE = 2;
    public static final int RENDER_MORE_PASS = 1;
    public static final int RENDER_MORE_FRAME = 2;
    private static final int SCROLL_ANTI_SHAKE = 15;
    private final Paper mPaper = new Paper();
    private Listener mListener = null;
    private final ScrollerHelper mScroller;
    private final Handler mHandler;
	private Resources mResources;
    private int mOverscrollEffect = SlotView.OVERSCROLL_3D;
	private Spec mSpec;
	private final TimerTitleLabelMaker mTimerTitleLabelMaker;
	private SlotRenderer mRenderer;
	private int mStartIndex;
    private SlotAnimation mAnimation = null;
    private int[] mRequestRenderSlots = new int[16];
    private boolean mMoreAnimation = false;
    private boolean mIsEventIdle = true;
	private UserInteractionListener mUIListener;
    private final Rect mTempRect = new Rect();
    private int mDayStart = 0;
    private int mDayEnd = 0;
	private int mScrollAntishake;
	private boolean mDownInScrolling;
    private int mDefaultBackground;
	private int topBarHeight = 0 ;
    
    public interface OnVisibleItemChangedListener {
        public void onVisibleItemChanged(int start, int end);
    }

	public class MyGestureListener implements GestureDetector.OnGestureListener {
		private boolean isDown;

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
            GLRoot root = getGLRoot();
            root.lockRenderThread();
            try {
                if (isDown)
                    return;
                int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY() +( mScrollY < 0 ? mScrollY : 0));
                if (index != INDEX_NONE) {
                    isDown = true;
                    mListener.onDown(index);
                }
            } finally {
                root.unlockRenderThread();
            }
			
		}

        private void cancelDown(boolean byLongPress) {
            if (!isDown) return;
            isDown = false;
            mListener.onUp(byLongPress);
        }

        
		@Override
        public boolean onSingleTapUp(MotionEvent e) {
            cancelDown(false);
            if (mDownInScrolling)
                return true;
            if (mLayout.mDayCounts == 0) {
                return true;
            }
            int position = -1;
            if (mListener != null && (position = mLayout.calcPositionOfItems((int)e.getX(), (int)e.getY())) != -1) {    
                //mListener.onClickMap(((mLayout.mDaysInfo).get(position)).mTime);
                return true;
            }
            int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY() + (mScrollY < 0 ? mScrollY : 0));
            if (index != INDEX_NONE) {
                mListener.onSingleTapUp(index);
            }
            return true;
        }

		@Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

		
		cancelDown(false);
		float distance = WIDE ? distanceX : distanceY;
		int overDistance = mScroller.startScroll(
				Math.round(distance), 0, mLayout.getScrollLimit());
		if (mOverscrollEffect == OVERSCROLL_3D && overDistance != 0) {
			mPaper.overScroll(overDistance);
		}
		invalidate();
		return true;

/*            cancelDown(false);
            float distance = WIDE ? distanceX : distanceY;

            if (true) {
                final int currPos = mScroller.getPosition();
                // jinpeng modify++
                float halfHeight = 0.5f * getHeight();
                int scrollDistance = mScroller.getOverscrollDistance();
                // jinpeng modify--

                // jinpeng modify++
                if (currPos + distance > mLayout.getScrollLimit() + scrollDistance) {
                    distance = mLayout.getScrollLimit() + scrollDistance - currPos;
                } else if (currPos + distance < -scrollDistance) {
                    distance = -scrollDistance - currPos;
                }
                // jinpeng modify--

                if (currPos > mLayout.getScrollLimit() || currPos < 0) {
                    int scrollBy = (currPos < 0) ? currPos : (currPos - mLayout.getScrollLimit());
                    distance = (float) ScrollerHelper.calcRealOverScrollDist(distance, scrollBy, scrollDistance);
                    // jinpeng modify--
                }

                mScrollAntishake += distance;
                if (Math.abs(mScrollAntishake) > SCROLL_ANTI_SHAKE) {
                    mScroller.startScroll(Math.round(distance), 0, 0);
                }
            } else {
                int overDistance = mScroller.startScroll(Math.round(distance), 0,
                        mLayout.getScrollLimit());
                if (mOverscrollEffect == OVERSCROLL_3D && overDistance != 0) {
                    mPaper.overScroll(overDistance);
                }
            }

            invalidate();
            return true;
            */
        }

		@Override
        public void onLongPress(MotionEvent e) {
            cancelDown(true);
            if (mDownInScrolling)
                return;
            lockRendering();
            try {
                int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY() + (mScrollY < 0 ? mScrollY : 0));
                if (index != INDEX_NONE)
                    mListener.onLongTap(index);
            } finally {
                unlockRendering();
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            cancelDown(false);
            int scrollLimit = mLayout.getScrollLimit();
            if (scrollLimit == 0)
                return false;
            // float velocity = WIDE ? velocityX : velocityY;
            float velocity = mVelocityHelper.getVelocity();
            mScroller.fling((int) -velocity, 0, scrollLimit);
            if (mUIListener != null)
                mUIListener.onUserInteractionBegin();
            invalidate();
            return true;
        }

	}

    public void setNonVisibleActive(boolean active) {
        //mRenderer.setNonVisibleActive(active);
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        if (!changeSize)
            return;

        // Make sure we are still at a resonable scroll position after the size
        // is changed (like orientation change). We choose to keep the center
        // visible slot still visible. This is arbitrary but reasonable.
        int visibleIndex = (mLayout.getVisibleStart() + mLayout.getVisibleEnd()) / 2;
        mLayout.setSize(r - l, b - t);
        topBarHeight = t;
        makeSlotVisible(visibleIndex);
        if (mOverscrollEffect == OVERSCROLL_3D) {
            mPaper.setSize(r - l, b - t);
        }
    }

    public void makeSlotVisible(int index) {
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int visibleBegin = WIDE ? mScrollX : mScrollY;
        int visibleLength = WIDE ? getWidth() : getHeight();
        int visibleEnd = visibleBegin + visibleLength;
        int slotBegin = WIDE ? rect.left : rect.top;
        int slotEnd = WIDE ? rect.right : rect.bottom;

        int position = visibleBegin;
        if (visibleLength < slotEnd - slotBegin) {
            position = visibleBegin;
        } else if (slotBegin < visibleBegin + mLayout.mSlotTopMargin) {
            position = slotBegin - mLayout.mSlotTopMargin;
        } else if (slotEnd > visibleEnd) {
            position = slotEnd - visibleLength;
        }
        setScrollPosition(position);
    }

	public TimerSlotView(AbstractGalleryActivity activity, Spec spec) {
    	
		if(DEBUG) Log.w(TAG,"TimerSlotView");
        mLayout = new TimerSlotViewLayout(activity);
        mLayout.setOnVisibleItemChangedListener(
                new OnVisibleItemChangedListener() {

					@Override
                    public void onVisibleItemChanged(int start, int end) {
                        recycleTitles(mDayStart, start - 1);
                        recycleTitles(end + 1, mDayEnd);
                        mDayStart = start;
                        mDayEnd = end;
                    }
                }
            );

        mGestureDetector = new GestureDetector(activity, new MyGestureListener());
        mVelocityHelper = new VelocityHelper((Context)activity);
        mScroller = new ScrollerHelper(activity);
        mHandler = new SynchronizedHandler(activity.getGLRoot());
        setSlotSpec(spec);
        mResources = activity.getResources();
        mDefaultBackground = mResources.getColor(R.color.default_background);

        setOverscrollEffect(SlotView.OVERSCROLL_SYSTEM);

        mTimerTitleLabelMaker = new TimerTitleLabelMaker(activity, spec);
        mSpec = spec;
    }

	/* (non-Javadoc)
	 * @see com.android.gallery3d.ui.GLView#render(com.android.gallery3d.glrenderer.GLCanvas)
	 */
	   @Override
	    protected void render(GLCanvas canvas) {
	        super.render(canvas);
	        if (mRenderer == null)
	            return;
	        mRenderer.prepareDrawing();

	        renderBackground(canvas);
	        long animTime = AnimationTime.get();
	        boolean more = mScroller.advanceAnimation(animTime);
	        more |= mLayout.advanceAnimation(animTime);
	        int oldX = mScrollX;
	        updateScrollPosition(mScroller.getPosition(), false);

	        boolean paperActive = false;
	        if (mOverscrollEffect == OVERSCROLL_3D) {
	            // Check if an edge is reached and notify mPaper if so.
	            int newX = mScrollX;
	            int limit = mLayout.getScrollLimit();
	            if (oldX > 0 && newX == 0 || oldX < limit && newX == limit) {
	                float v = mScroller.getCurrVelocity();
	                if (newX == limit)
	                    v = -v;

	                // I don't know why, but getCurrVelocity() can return NaN.
	                if (!Float.isNaN(v)) {
	                    mPaper.edgeReached(v);
	                }
	            }
	            paperActive = mPaper.advanceAnimation();
	        }

	        more |= paperActive;

	        if (mAnimation != null) {
	            more |= mAnimation.calculate(animTime);
	        }

	        if (mLayout.mEnableCountShow && mLayout.mSlotCount > 0) {
	            renderCountString(canvas, mScroller.getPosition());
	        }

	        //if (OppoModifyList.USE_NO_PHOTO_HIPS && mNoPhotoHipsEnable && mLayout.mSlotCount <= 0) {
	        //    renderNoPhotoHips(canvas);
	        //}
	        canvas.translate(-mScrollX, -mScrollY);

	        // render title
	        for (int i = mLayout.mVisibleDayStart ; i <= mLayout.mVisibleDayEnd; i++) {
	            int y = mLayout.getTitleTop(i);
	            more |= renderTitle(canvas, i, y, -mScrollY);
	        }

	        int requestCount = 0;
	        int requestedSlot[] = expandIntArray(mRequestRenderSlots, mLayout.mVisibleEnd
	                - mLayout.mVisibleStart);

	        for (int i = mLayout.mVisibleEnd - 1; i >= mLayout.mVisibleStart; --i) {
	            int r = renderItem(canvas, i, 0, paperActive);
	            if ((r & RENDER_MORE_FRAME) != 0)
	                more = true;
	            if ((r & RENDER_MORE_PASS) != 0)
	                requestedSlot[requestCount++] = i;
	        }

	        for (int pass = 1; requestCount != 0; ++pass) {
	            int newCount = 0;
	            for (int i = 0; i < requestCount; ++i) {
	                int r = renderItem(canvas, requestedSlot[i], pass, paperActive);
	                if ((r & RENDER_MORE_FRAME) != 0)
	                    more = true;
	                if ((r & RENDER_MORE_PASS) != 0)
	                    requestedSlot[newCount++] = i;
	            }
	            requestCount = newCount;
	        }

	        canvas.translate(mScrollX, mScrollY);

	        // jinpeng add++
	        if (DEBUG) Log.d(TAG, "render scrollY=" + mScrollY + ", mstart="
	            + mLayout.mVisibleDayStart + ", end=" + mLayout.mVisibleDayEnd);
			/*
	        if (mLayout.mDayCounts > 0) {
	            int top = mLayout.mTitleTopMargin + mLayout.getmVerticalPadding();
	            int offset = mScrollY;
	            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
	            if (mScrollY >= 0) {
	                canvas.translate(mScrollX, mLayout.mHeaderY);
	                offset = mLayout.mHeaderY;
	            } else {
	                canvas.translate(mScrollX, - mScrollY);
	                offset = - mScrollY;
	            }
	            canvas.fillRect(0, 0, getWidth(), mLayout.mSlotTopMargin + mLayout.getmVerticalPadding(),
	            		mResources.getColor(R.color.default_background));
	            more |= renderTitle(canvas, mLayout.mVisibleDayStart, top, offset);
	            canvas.restore();
	        }*/
	        // jinpeng add--

	        if (more)
	            invalidate();

	        final UserInteractionListener listener = mUIListener;
	        if (mMoreAnimation && !more && mIsEventIdle && listener != null) {
	            mHandler.removeCallbacksAndMessages(null);
	            mHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    listener.onUserInteractionEnd();
	                }
	            }, 0);
	        }
	        mMoreAnimation = more;
	    }

	    public boolean renderTitle(GLCanvas canvas, int index, int y, int offset) {
	        if (DEBUG) Log.d(TAG, "renderTitle index=" + index + ", y=" + y);
	        //int tmpX = 0;
	        if (mLayout.mDayCounts == 0) {
	            return true;
	        }
	        int mapX = 0;
	        int mapBaseline = 0;
	        int x = mLayout.mTitleLeftMargin;
	        long time = ((mLayout.mDaysInfo).get(index)).mTime;
	        BitmapTexture texture = mTimerTitleLabelMaker.getTexture(time);
	        if (texture != null) {
	            texture.draw(canvas, x, y);
	            mapX = x + mTimerTitleLabelMaker.getContentWidth(time);
	            mapBaseline = (int) (y + texture.getHeight() * 0.6667);
	        } else {
	            return true;
	        }

	        return true; //renderMapTitle(canvas, index, mapX, getWidth() - mapX, mapBaseline, y + offset);
	    }

	    private void recycleTitles(int from, int to) {
	        for(int i = from ; i <= to ; i++) {
	            if (i < mLayout.mDayCounts - 1) {
	                mTimerTitleLabelMaker.recycle(((mLayout.mDaysInfo.get(i))).mTime);
	            }
	        }
	    }
	    
		private int renderItem(GLCanvas canvas, int index, int pass, boolean paperActive) {
	        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
	        if (DEBUG) Log.d(TAG, "renderItem index=" + index + ", pass=" + pass
	            + ", paperActive=" + paperActive);
	        Rect rect = mLayout.getSlotRect(index, mTempRect);
	        // Debugger.d(TAG, "renderItem: itme rect = " +rect.flattenToString() +
	        // "; index = " + index);
	        if (paperActive) {
	            canvas.multiplyMatrix(mPaper.getTransform(rect, mScrollX), 0);
	        } else {
	            canvas.translate(rect.left, rect.top, 0);
	        }
	        if (mAnimation != null && mAnimation.isActive()) {
	            mAnimation.apply(canvas, index, rect);
	        }
	        int result = mRenderer.renderSlot(canvas, index, pass, rect.right - rect.left, rect.bottom
	                - rect.top);
	        canvas.restore();
	        return result;
	    }

		private static int[] expandIntArray(int array[], int capacity) {
	        int destCapacity = array.length;
	        while (destCapacity < capacity) {
	            destCapacity *= 2;
	        }

	        array = new int[destCapacity];
	        return array;
	    }

		private void updateScrollPosition(int position, boolean force) {
	        if (!force && (WIDE ? position == mScrollX : position == mScrollY))
	            return;
	        if (WIDE) {
	            mScrollX = position;
	        } else {
	            mScrollY = position;
	        }
	        mLayout.setScrollPosition(position);
	        onScrollPositionChanged(position);
	    }

	    public void destroy() {
	        mTimerTitleLabelMaker.recycleAll();
	    }

	private void onScrollPositionChanged(int newPosition) {
			// TODO Auto-generated method stub
        int limit = mLayout.getScrollLimit();
        mListener.onScrollPositionChanged(newPosition, limit);
		}

	private void setOverscrollEffect(int kind) {
		// TODO Auto-generated method stub
        mOverscrollEffect = kind;
        mScroller.setOverfling(kind == SlotView.OVERSCROLL_SYSTEM);
	}

	private void setSlotSpec(Spec spec) {
		// TODO Auto-generated method stub
        mLayout.setSlotSpec(spec);
	}

	public static class Spec {
        public int slotWidth = -1;
        public int slotHeight = -1;

        public int rowsLand = -1;
        public int rowsPort = -1;
        public int slotGap = -1;
        public int topPadding = -1;
        public int horizontalPadding = -1;

        public boolean bNeedSideGap = true;
        public boolean enableCountShow = false;
        public int countStringResId = -1;
        public int countStringColor;
        public float countStringTextSize;
        public boolean bIrregular = false;

        // jinpeng add++
        public int slotTopMargin;
        public int titleTopMargin;
        public int titleSize;
        public int titleColor;
        public int titleLeftMargin;
        public int subTitleSize;
        public int subTitleColor;

        public int mapTitleSize;
        public int mapTitleColor;
        public int mapTitleLeftMargin;
        public int mapTitlePadding;

        public int selectLeftPadding;
        public int selectRightPadding;
        public int selectTopPadding;
        public int arrowTopMargin;
        public int mapIconTopMargin;
        // jinpeng add--
    }

    public void setSlotRenderer(SlotRenderer slotDrawer) {
        mRenderer = slotDrawer;
        mLayout.setSlotRenderer(slotDrawer);
        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mLayout.mSlotWidth, mLayout.mSlotHeight);
            mRenderer.onVisibleRangeChanged(getVisibleStart(), getVisibleEnd());
        }
    }

    public int getVisibleStart() {
        return mLayout.getVisibleStart();
    }

    public int getVisibleEnd() {
        return mLayout.getVisibleEnd();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setUserInteractionListener(UserInteractionListener listener) {
        mUIListener = listener;
    }

    public boolean setSlotCount(int slotCount) {
        boolean changed = mLayout.setSlotCount(slotCount);

        // mStartIndex is applied the first time setSlotCount is called.
        if (mStartIndex != INDEX_NONE) {
            setCenterIndex(mStartIndex);
            mStartIndex = INDEX_NONE;
        }
        // Reset the scroll position to avoid scrolling over the updated limit.
        setScrollPosition(WIDE ? mScrollX : mScrollY);
        return changed;
    }

    public void setScrollPosition(int position) {
        position = Utils.clamp(position, 0, mLayout.getScrollLimit());
        mScroller.setPosition(position);
        updateScrollPosition(position, false);
    }

    public void setCenterIndex(int index) {
        int slotCount = mLayout.mSlotCount;
        if (index < 0 || index >= slotCount) {
            return;
        }
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int position = WIDE ? (rect.left + rect.right - getWidth()) / 2
                : (rect.top + rect.bottom - getHeight()) / 2;
        setScrollPosition(position);
    }

    public boolean setSlotCount(int slotCount, Object daysInfo) {
        if (DEBUG) Log.d(TAG, "setSlotCount() slotCount=" + slotCount
            + ", daysInfo=" + daysInfo + ", mStartIndex=" + mStartIndex);
        boolean changed = mLayout.setSlotCount(slotCount, daysInfo);

        // mStartIndex is applied the first time setSlotCount is called.
        if (mStartIndex != INDEX_NONE) {
            setCenterIndex(mStartIndex);
            mStartIndex = INDEX_NONE;
        }
        // Reset the scroll position to avoid scrolling over the updated limit.
        setScrollPosition(WIDE ? mScrollX : mScrollY);
        return changed;
    }

	public Rect getSlotRect(int slotIndex, GLView mRootPane) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
    protected boolean onTouch(MotionEvent event) {
        if (mUIListener != null)
            mUIListener.onUserInteraction();
        mVelocityHelper.onTouch(event);
        mGestureDetector.onTouchEvent(event);
        mIsEventIdle = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mScrollAntishake = 0;
                mDownInScrolling = !mScroller.isFinished();
                mScroller.forceFinished();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mScrollAntishake = 0;
                mPaper.onRelease();

                final int currPos = mScroller.getPosition();
                int distance = 0;
                if (currPos < 0) {
                    distance = -currPos;
                } else if (currPos > mLayout.getScrollLimit()) {
                    distance = mLayout.getScrollLimit() - currPos;
                }

                if (distance != 0) {
                    mScroller.forceFinished();
                    mScroller.startScroll(distance, 0, ScrollerHelper.SPRING_BACK_DURATION);
                }
                mIsEventIdle = true;
                invalidate();
                break;
        }
        return true;
    }
	
	public Rect getSlotRect(int index) {
		// TODO Auto-generated method stub
        return mLayout.getSlotRect(index, new Rect());
	}

    public int getScrollX() {
        return mScrollX;
    }

    public int getScrollY() {
        return mScrollY;
    }
    
    private void renderCountString(GLCanvas canvas, int position) {
        if (position < 0 && mLayout.mCountTexture != null) {
            StringTexture stringTexture = mLayout.mCountTexture;
            int width = getWidth();
            canvas.save(GLCanvas.SAVE_FLAG_ALL);
            int x = (width - stringTexture.getWidth()) / 2;
            int y = (GalleryUtils.dpToPixel(50) - stringTexture.getHeight()) / 2;
            stringTexture.draw(canvas, x, y);
            canvas.restore();
        }
    }

	public void resume() {
		// TODO Auto-generated method stub
	}

	public void pause() {
		// TODO Auto-generated method stub
        mScroller.forceFinished();
	}

    @Override
    protected void renderBackground(GLCanvas canvas) {
        // final boolean isPort = (mOrientationManager.getDisplayRotation()%360
        // == 0);
        // int topBarHeight = (int)
        // mResources.getDimensionPixelSize(R.dimen.top_bar_height);
        // canvas.drawTexture(isPort ? mDefaultBackgroundPort :
        // mDefaultBackgroundLand, 0, -topBarHeight, getWidth(), getHeight() +
        // topBarHeight);
    	if(DEBUG) Log.v(TAG, "renderBackground topBarHeight = " + topBarHeight);
        canvas.fillRect(0, 0 , getWidth(), getHeight(), mDefaultBackground);
    }

}
