/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.ui;

import android.content.Context;
import android.view.ViewConfiguration;

import com.android.gallery3d.common.OverScroller;
import com.android.gallery3d.common.Utils;

public class ScrollerHelper {
    public static int SPRING_BACK_DURATION = 300;
	private OverScroller mScroller;
    private int mOverflingDistance;
    private boolean mOverflingEnabled;

    public ScrollerHelper(Context context) {
        mScroller = new OverScroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mOverflingDistance = mOverscrollDistance = configuration.getScaledOverflingDistance();
    }

    public void setOverfling(boolean enabled) {
        mOverflingEnabled = enabled;
    }

    /**
     * Call this when you want to know the new location. The position will be
     * updated and can be obtained by getPosition(). Returns true if  the
     * animation is not yet finished.
     */
    public boolean advanceAnimation(long currentTimeMillis) {
        return mScroller.computeScrollOffset();
    }

    public boolean isFinished() {
        return mScroller.isFinished();
    }

    public void forceFinished() {
        mScroller.forceFinished(true);
    }

    public int getPosition() {
        /// M: [FEATURE.ADD] fancy layout @{
        if (mScrollMax != -1 && mScrollMin != -1 && mScroller.isOverScrolled()) {
            return Utils.clamp(mScroller.getCurrX(), mScrollMin, mScrollMax);
        }
        /// @}
        return mScroller.getCurrX();
    }

    public float getCurrVelocity() {
        return mScroller.getCurrVelocity();
    }

    public void setPosition(int position) {
        mScroller.startScroll(
                position, 0,    // startX, startY
                0, 0, 0);       // dx, dy, duration

        // This forces the scroller to reach the final position.
        mScroller.abortAnimation();
    }

    public void fling(int velocity, int min, int max) {
        /// M: [FEATURE.ADD] fancy layout @{
        mScrollMax = max;
        mScrollMin = min;
        mScroller.setMaxScrollLength(max);
        mScroller.setMinScrollLength(min);
        /// @}
        int currX = getPosition();
        mScroller.fling(
                currX, 0,      // startX, startY
                velocity, 0,   // velocityX, velocityY
                min, max,      // minX, maxX
                0, 0,          // minY, maxY
                mOverflingEnabled ? mOverflingDistance : 0, 0);
    }

    // Returns the distance that over the scroll limit.
    public int startScroll(int distance, int min, int max) {
        /// M: [FEATURE.ADD] fancy layout @{
        mScrollMax = max;
        mScrollMin = min;
        mScroller.setMaxScrollLength(max);
        mScroller.setMinScrollLength(min);
        /// @}
        int currPosition = mScroller.getCurrX();
        int finalPosition = mScroller.isFinished() ? currPosition :
                mScroller.getFinalX();
        int newPosition = Utils.clamp(finalPosition + distance, min, max);
        if (newPosition != currPosition) {
            mScroller.startScroll(
                currPosition, 0,                    // startX, startY
                newPosition - currPosition, 0, 0);  // dx, dy, duration
        }
        return finalPosition + distance - newPosition;
    }

//********************************************************************
//*                              MTK                                 *
//********************************************************************
    /// M: [FEATURE.ADD] fancy layout @{
    private int mScrollMax = -1;
    private int mScrollMin = -1;
    /// @}
	private static int mOverscrollDistance;

	public int getOverscrollDistance() {
		// TODO Auto-generated method stub
        return mOverscrollDistance;
	}

	public static float calcRealOverScrollDist(float distance, int scrollBy,
			int scrollDistance) {
		// TODO Auto-generated method stub
        return (int) (distance * (1.0f - Math.abs(scrollBy) * 1.0f / mOverscrollDistance) / 3);
	}
}
