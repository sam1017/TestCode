package com.android.gallery3d.ui;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Rect;
import android.text.TextPaint;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.ui.SlotView.SlotRenderer;
import com.android.gallery3d.ui.TimerSlotView.OnVisibleItemChangedListener;
import com.android.gallery3d.ui.TimerSlotView.Spec;
import com.android.gallery3d.ui.SlotView.IntegerAnimation;
import com.android.gallery3d.data.MixAlbum.Range;

public class TimerSlotViewLayout {

    private static final boolean WIDE = false;
	private static final boolean DEBUG = false;
	private static final String TAG = "TimerSlotViewLayout";
	private static final int INDEX_NONE = -1;
	private AbstractGalleryActivity mActivity;
    protected Resources mResources;
	private SlotRenderer mRenderer;
	private Spec mSpec;
	public int mSlotWidth;
	public int mSlotHeight;
	public int mVisibleStart;
	public int mVisibleEnd;
	public int mSlotCount;
    protected IntegerAnimation mVerticalPadding = new IntegerAnimation();
    protected IntegerAnimation mHorizontalPadding = new IntegerAnimation();
    public boolean mEnableCountShow = false;
    protected int mCountStringResId = -1;
    public StringTexture mCountTexture = null;
	public int mSlotTopMargin;
	public int mTitleLeftMargin;
	public int mTitleTopMargin;
	private int mSlotGap;
	private int mWidth;
	private int mHeight;
	private int mUnitCount;
	private int[] mDayItemCounts;
	public int mDayCounts;
	private int[] mDayItemFromRows;
	private int[] mTopOfDayItems;
	private Rect[] mDayItemsBounds;
	private int mContentLength;
    public ArrayList<Range> mDaysInfo;
    public int mVisibleDayStart;
    public int mVisibleDayEnd;
    public int mHeaderY = 0;
    protected int mScrollPosition;
	private OnVisibleItemChangedListener mOnVisibleItemChangedListener;
    protected Rect mHintRect = new Rect();

	public TimerSlotViewLayout(AbstractGalleryActivity activity) {
		// TODO Auto-generated constructor stub
        mActivity = activity;
        mResources = activity.getResources();
	}

    public int getSlotIndexByPosition(float x, float y) {
    	
        if(mDaysInfo == null || mDaysInfo.isEmpty()) {
            return INDEX_NONE;
        }

        int absoluteX = Math.round(x) + (WIDE ? mScrollPosition : 0);
        int absoluteY = Math.round(y) + (WIDE ? 0 : mScrollPosition);

        absoluteX -= mHorizontalPadding.get();
        absoluteY -= mVerticalPadding.get();

        if (absoluteX < 0 || absoluteY < 0) {
            return INDEX_NONE;
        }


        if (absoluteX > mWidth - mHorizontalPadding.get()*2) {
            return INDEX_NONE;
        }

        int indexOfDay = -1;
        for (int i = 0; i <= mDayCounts; i++) {
            if (absoluteY < mTopOfDayItems[i]) {
                indexOfDay = Math.max(0, i - 1);
                break;
            }
        }
        if (indexOfDay == -1) {
            indexOfDay = mDayCounts - 1;
        }
        Range range = mDaysInfo.get(indexOfDay);
        int pos;
        if (indexOfDay == mVisibleDayStart) {
            pos = absoluteY - mScrollPosition - mHeaderY - mSlotTopMargin;
            if (pos < 0) {
                return INDEX_NONE;
            }
        }

        pos = absoluteY - mTopOfDayItems[indexOfDay] - mSlotTopMargin;

        if (pos < 0) {
            return INDEX_NONE;
        }
        int row = pos / (mSlotHeight + mSlotGap);
        int col = absoluteX / (mSlotWidth + mSlotGap);
        int index = range.mStart + row * mUnitCount + col;
        if (DEBUG) Log.d(TAG, "getSlotIndexByPosition() range=" + range + ", row=" + row
            + ", col=" + col + ", index=" + index + ", pos=" + pos);
        if (index > range.mEnd) {
            return INDEX_NONE;
        }

        return index >= mSlotCount ? INDEX_NONE : index;
        
    }

	public void setSlotSpec(Spec spec) {
		// TODO Auto-generated method stub
        mSpec = spec;
	}

    public void setSlotRenderer(SlotRenderer slotDrawer) {
        mRenderer = slotDrawer;
    }

    public int getVisibleStart() {
        return mVisibleStart;
    }

    public int getVisibleEnd() {
        return mVisibleEnd;
    }

    public boolean setSlotCount(int slotCount) {
        if (slotCount == mSlotCount)
            return false;
        if (mSlotCount != 0) {
            mHorizontalPadding.setEnabled(true);
            mVerticalPadding.setEnabled(true);
        }
        mSlotCount = slotCount;
        mEnableCountShow = mSpec.enableCountShow;
        mCountStringResId = mSpec.countStringResId;
        if (mEnableCountShow && mCountStringResId != -1) {
            String countString = mResources.getQuantityString(mCountStringResId, mSlotCount,
                    mSlotCount);

            TextPaint paint = new TextPaint();
            // paint.setTextSize(36);
            paint.setTextSize(mSpec.countStringTextSize);
            paint.setAntiAlias(true);
            // paint.setColor(0xff010101);
            paint.setColor(mSpec.countStringColor);
            // paint.setShadowLayer(1f, 0f, 0f, Color.BLACK);
            // paint.setFakeBoldText(true);
            mCountTexture = StringTexture.newInstance(countString, paint);
        }

        int hPadding = mHorizontalPadding.getTarget();
        int vPadding = mVerticalPadding.getTarget();
        initLayoutParameters();
        return vPadding != mVerticalPadding.getTarget()
                || hPadding != mHorizontalPadding.getTarget();
    }

    protected void initLayoutParameters() {
        mEnableCountShow = mSpec.enableCountShow;
        mCountStringResId = mSpec.countStringResId;

        mSlotTopMargin = mSpec.slotTopMargin;
        mTitleLeftMargin = mSpec.titleLeftMargin;
        mTitleTopMargin = mSpec.titleTopMargin;

        // Initialize mSlotWidth and mSlotHeight from mSpec
		if(DEBUG) Log.d(TAG, "initLayoutParameters mSpec.slotWidth = " + mSpec.slotWidth);
        if (mSpec.slotWidth != -1) {
            mSlotGap = 0;
            mSlotWidth = mSpec.slotWidth;
            mSlotHeight = mSpec.slotHeight;
        } else {
            int rows = (mWidth > mHeight) ? mSpec.rowsLand : mSpec.rowsPort;
            mSlotGap = mSpec.slotGap;

			if(DEBUG) Log.d(TAG, "initLayoutParameters rows = " + rows + " mSlotGap = " + mSlotGap + " mWidth = " + mWidth + " mHeight = " + mHeight);
            if (WIDE) {
                mSlotHeight = Math.max(1, (mHeight - (rows - 1) * mSlotGap) / rows);
                mSlotWidth = mSlotHeight;
            } else {
				if(DEBUG) Log.d(TAG, "initLayoutParameters mSpec.horizontalPadding = " + mSpec.horizontalPadding);
                if (mSpec.horizontalPadding != -1) {
                    mSlotWidth = Math.max(1, (mWidth - 2 * mSpec.horizontalPadding - (rows - 1)
                            * mSlotGap)
                            / rows);
                } else {
                    mSlotWidth = Math.max(1, (mWidth - (rows + (mSpec.bNeedSideGap ? 1 : -1))
                            * mSlotGap)
                            / rows);
                }
                mSlotHeight = mSlotWidth;
            }

        }

		if(DEBUG) Log.d(TAG, "initLayoutParameters mSlotWidth = " + mSlotWidth);
		if(DEBUG) Log.d(TAG, "initLayoutParameters mSlotHeight = " + mSlotHeight);
        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mSlotWidth, mSlotHeight);
        }

        int[] padding = new int[2];
        if (WIDE) {
            initLayoutParameters(mWidth, mHeight, mSlotWidth, mSlotHeight, padding);
            mVerticalPadding.startAnimateTo(padding[0]);
            mHorizontalPadding.startAnimateTo(padding[1]);
        } else {
            initLayoutParameters(mHeight, mWidth, mSlotHeight, mSlotWidth, padding);
            mVerticalPadding.startAnimateTo(padding[1]);
            mHorizontalPadding.startAnimateTo(padding[0]);
        }

        computeHintRect();
        updateVisibleSlotRange();
    }

    protected void updateVisibleSlotRange() {
        int position = mScrollPosition;

        if (WIDE) {
            int startCol = position / (mSlotWidth + mSlotGap);
            int start = Math.max(0, mUnitCount * startCol);
            int endCol = (position + mWidth + mSlotWidth + mSlotGap - 1) / (mSlotWidth + mSlotGap);
            int end = Math.min(mSlotCount, mUnitCount * endCol);
            setVisibleRange(start, end);
        } else {
        	if(DEBUG) Log.d(TAG, "mDayCounts = " + mDayCounts);
            if (mDayCounts == 0) {
                setVisibleDayRange(0, -1);
                setVisibleRange(0, 0);
            } else {
                int startIndexOfDay = -1;
                int endIndexOfDay = -1;
                Range range;
                boolean isLastDay = false;
                for (int i = 0; i <= mDayCounts; i++) {
                    if (position < mTopOfDayItems[i]) {
                        startIndexOfDay = Math.max(0, i - 1);
                        break;
                    }
                }
                if (startIndexOfDay == -1) {
                    startIndexOfDay = mDayCounts - 1;
                }
                if (startIndexOfDay == (mDayCounts - 1)) {
                    isLastDay = true;
                }
                range = mDaysInfo.get(startIndexOfDay);
                int startRow = (position - mTopOfDayItems[startIndexOfDay] - mSlotTopMargin) 
                        / (mSlotHeight + mSlotGap) - 1;
                if (DEBUG)
                    Log.d(TAG, "updateVisibleSlotRange() start position=" + position
                        +", startIndexOfDay=" + startIndexOfDay + ", startRow=" + startRow);
                if (startRow < 0) {
                    startRow = 0;
                }

                int start = Math.max(0, range.mStart + mUnitCount * startRow);
                
                int endPosition = position + mHeight;
                if (isLastDay) {
                    endIndexOfDay = mDayCounts - 1;
                } else {
                    for (int i = 0; i < mDayCounts; i++) {
                        if (endPosition < mTopOfDayItems[i]) {
                            endIndexOfDay = Math.max(0, i - 1);
                            break;
                        }
                    }
                    if (endIndexOfDay == -1) {
                        endIndexOfDay = mDayCounts - 1;
                    }
                }
                range = mDaysInfo.get(endIndexOfDay);
                int endRow = (endPosition - mTopOfDayItems[endIndexOfDay] - mSlotTopMargin - 1)
                        / (mSlotHeight + mSlotGap) + 1;
                if (endRow < 0) {
                    endRow = 0;
                }
                if (DEBUG)
                    Log.d(TAG, "updateVisibleSlotRange() end position=" + endPosition
                        +", endIndexOfDay=" + endIndexOfDay + ", endRow=" + endRow + ", unitHeight="
                        + (mSlotHeight + mSlotGap) + ", length2="
                        + (position - mTopOfDayItems[endIndexOfDay] - mSlotTopMargin));
                int end = Math.min(mSlotCount, range.mStart + mUnitCount * endRow);
                setVisibleDayRange(startIndexOfDay, endIndexOfDay);
                mHeaderY = 0;
                int b = mTopOfDayItems[mVisibleDayStart + 1];
                if (b - position - mSlotTopMargin < 0) {
                    mHeaderY = b - position - mSlotTopMargin;
                }
                setVisibleRange(start, end);
            }
            // jinpeng modify--
        }
    }

    protected void setVisibleDayRange(int start, int end) {
        if (start == mVisibleDayStart && end == mVisibleDayEnd)
            return;
        mVisibleDayStart = start;
        mVisibleDayEnd = end;
        if (mOnVisibleItemChangedListener != null) {
            mOnVisibleItemChangedListener.onVisibleItemChanged(mVisibleDayStart, mVisibleDayEnd);
        }
    }

	protected void setVisibleRange(int start, int end) {
        if (DEBUG) Log.d(TAG, "setVisibleRange() start=" + start +", end=" + end
                + ", mVisibleStart=" + mVisibleStart + ", mVisibleEnd=" + mVisibleEnd);
        if (start == mVisibleStart && end == mVisibleEnd)
            return;
        if (start < end) {
            mVisibleStart = start;
            mVisibleEnd = end;
        } else {
            mVisibleStart = mVisibleEnd = 0;
        }
        if (mRenderer != null) {
            mRenderer.onVisibleRangeChanged(mVisibleStart, mVisibleEnd);
        }
    }

    protected void computeHintRect() {
        int y;
        int bottomOptionBarHeight = 0;
        //if (mActivity.getOppoOptionMenuBar().getMajorOperationVisibility() == View.VISIBLE) {
        //    bottomOptionBarHeight = mActivity.getOppoOptionMenuBar().getMajorOptionMenuBarHeight();
        //}
        bottomOptionBarHeight = mActivity.getGalleryActionBar().getHeight();
        if (mContentLength > mHeight) {
            y = mContentLength - bottomOptionBarHeight;
        } else {
            y = mHeight - bottomOptionBarHeight;
        }

        mHintRect.set(0, y, mWidth, y);
    }

	// Calculate
    // (1) mUnitCount: the number of slots we can fit into one column (or row).
    // (2) mContentLength: the width (or height) we need to display all the
    // columns (rows).
    // (3) padding[]: the vertical and horizontal padding we need in order
    // to put the slots towards to the center of the display.
    //
    // The "major" direction is the direction the user can scroll. The other
    // direction is the "minor" direction.
    //
    // The comments inside this method are the description when the major
    // directon is horizontal (X), and the minor directon is vertical (Y).
    protected void initLayoutParameters(int majorLength, int minorLength, /*
                                                                         * The
                                                                         * view
                                                                         * width
                                                                         * and
                                                                         * height
                                                                         */
            int majorUnitSize, int minorUnitSize, /* The slot width and height */
            int[] padding) {
        int unitCount = (minorLength + mSlotGap) / (minorUnitSize + mSlotGap);
        if (unitCount == 0)
            unitCount = 1;
        mUnitCount = unitCount;

        calcEveryDayCount();

        // We put extra padding above and below the column.
        int availableUnits = Math.min(mUnitCount, mSlotCount);
        int usedMinorLength = mUnitCount * minorUnitSize + (availableUnits - 1) * mSlotGap;

        padding[0] = (minorLength - usedMinorLength) / 2;
        // Then calculate how many columns we need for all slots.
        int count = ((mSlotCount + mUnitCount - 1) / mUnitCount);

        if (mDayCounts != 0) {
            Rect rect = new Rect();
            rect = getSlotRect(mSlotCount - 1, rect);
            mContentLength = rect.bottom + mSlotGap;
            if (DEBUG) Log.d(TAG, "initLayoutParameters() mContentLength=" + mContentLength
                + ", rect=" + rect);
        } else {
            mContentLength = count * majorUnitSize + (count - 1) * mSlotGap;
            mContentLength += mSlotGap + mSpec.topPadding;
        }
        mContentLength += 114;

        padding[1] = mSpec.topPadding;
    }

    public Rect getSlotRect(int index, Rect rect) {
   	
        int col, row;
        int dayIndex = getIndexOfDay(index);
        if (dayIndex == -1) {
            row = index / mUnitCount;
            col = index - row * mUnitCount;
        } else {
            Range range = mDaysInfo.get(dayIndex);
            int relativeIndex = index - range.mStart;
            row = relativeIndex / mUnitCount;
            col = relativeIndex - row * mUnitCount;
        }
        if (DEBUG) Log.d(TAG, "getSlotRect\t dayIndex = " + dayIndex
             + ", row=" + row + ", col=" + col);

        int x = mHorizontalPadding.get() + col * (mSlotWidth + mSlotGap);
        if (!mSpec.bNeedSideGap) {
            x -= mSlotGap;
        }

        int y = mVerticalPadding.get() + row * (mSlotHeight + mSlotGap);
        if (dayIndex != -1) {
            y += mTopOfDayItems[dayIndex] + mSlotTopMargin;
        }
        if (DEBUG) Log.d(TAG, "getSlotRect\t index=" + index + ", x = " + x + ", y=" + y);
        rect.set(x, y, x + mSlotWidth, y + mSlotHeight);
        return rect;
        
    }

    public int getIndexOfDay(int index) {
        int dayIndex = -1;
        
        for (int i = 0; i < mDayCounts; i++) {
            Range range = mDaysInfo.get(i);
            if (index >= range.mStart && index <= range.mEnd) {
                dayIndex = i;
                break;
            }
        }
        
        return dayIndex;
    }

	private void calcEveryDayCount() {
		
        mDayItemCounts = new int[mDayCounts];
        mDayItemFromRows = new int[mDayCounts];
        mTopOfDayItems = new int[mDayCounts + 1];
        mDayItemsBounds = new Rect[mDayCounts + 1];
        int rowCount = 0;
        int contentLength = 0;
        for (int i = 0; i < mDayCounts; i++) {
            mDayItemsBounds[i] = new Rect();
            mDayItemFromRows[i] = rowCount;
            mTopOfDayItems[i] = contentLength;
            Range range = mDaysInfo.get(i);
            mDayItemCounts[i] = range.mEnd - range.mStart + 1;
            int row = (mDayItemCounts[i] - 1)/ mUnitCount + 1;
            rowCount += row;
            contentLength += mSlotTopMargin + row * (mSlotHeight + mSlotGap);
            if (i == (mDayCounts - 1)) {
                mTopOfDayItems[i + 1] = contentLength;
            }
            if (DEBUG) Log.d(TAG, "calcEveryDayCount i=" + i + ", mDayItemCounts="
                + mDayItemCounts[i] + ", mDayItemFromRows=" + mDayItemFromRows[i]
                + ", mTopOfDayItems=" + mTopOfDayItems[i]);
        }
        
	}

    public boolean setSlotCount(int slotCount, Object daysInfo) {
        ArrayList<Range> daysRange = (ArrayList<Range>) daysInfo;
        mDaysInfo = daysRange;
        mDayCounts = mDaysInfo.size();
        if (DEBUG) Log.d(TAG, "setSlotCount() slotCount=" + slotCount
            + ", daysInfo=" + daysInfo + ", mDayCounts=" + mDayCounts);
        if (mSlotCount != 0) {
            mHorizontalPadding.setEnabled(true);
            mVerticalPadding.setEnabled(true);
        }
        mSlotCount = slotCount;

        mEnableCountShow = mSpec.enableCountShow;
        mCountStringResId = mSpec.countStringResId;
        if (mEnableCountShow && mCountStringResId != -1) {
            String countString = mResources.getQuantityString(mCountStringResId, mSlotCount,
                    mSlotCount);

            TextPaint paint = new TextPaint();
            // paint.setTextSize(36);
            paint.setTextSize(mSpec.countStringTextSize);
            paint.setAntiAlias(true);
            paint.setColor(mSpec.countStringColor);
            // paint.setShadowLayer(1f, 0f, 0f, Color.BLACK);
            // paint.setFakeBoldText(true);
            mCountTexture = StringTexture.newInstance(countString, paint);
        }

        int hPadding = mHorizontalPadding.getTarget();
        int vPadding = mVerticalPadding.getTarget();
        initLayoutParameters();
        return vPadding != mVerticalPadding.getTarget()
                || hPadding != mHorizontalPadding.getTarget();
    }

    public boolean advanceAnimation(long animTime) {
        // use '|' to make sure both sides will be executed
        return mVerticalPadding.calculate(animTime) | mHorizontalPadding.calculate(animTime);
    }

    public void setScrollPosition(int position) {
        if (mScrollPosition == position)
            return;
        mScrollPosition = position;
        updateVisibleSlotRange();
    }

	public int getScrollLimit() {
		// TODO Auto-generated method stub
        int limit = WIDE ? mContentLength - mWidth : mContentLength - mHeight;
        return limit <= 0 ? 0 : limit;
	}

	public int getmVerticalPadding() {
		// TODO Auto-generated method stub
        return mVerticalPadding.get();
	}

    public Rect getItemBounds(int index) {
        if (index >= mDayItemsBounds.length) {
            Log.w(TAG, "getItemBounds, index is overtop!");
            return null;
        }
        return mDayItemsBounds[index];
    }

	
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        initLayoutParameters();
    }

    public void setOnVisibleItemChangedListener(OnVisibleItemChangedListener listener) {
        mOnVisibleItemChangedListener = listener;
    }

    public int getTitleTop(int index) {
        if (index < 0 || index > mDayCounts - 1) {
            return -1;
        }
        int top = mVerticalPadding.get() + mTopOfDayItems[index] + mTitleTopMargin;
        return top;
    }

    public int calcPositionOfItems(int x, int y) {
//      Log.d(TAG, "calcPositionOfItems, x:" + x + ",y:" + y);
      for (int i = mVisibleDayStart; i <= mVisibleDayEnd; i++) {
          Rect rect = mDayItemsBounds[i];
//          Log.d(TAG, "calcPositionOfItems, left:" + rect.left + ",top:" + rect.top
//                  + ",right:" + rect.right + ",bottom:" + rect.bottom);
          if (rect.contains(x, y)) {
              return i;
          }
      }
      return -1;
  }
}
