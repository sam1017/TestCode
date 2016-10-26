package com.android.gallery3d.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月18日 --- 下午5:10:10
 *
 */
public class VelocityHelper {
    private static final String TAG = "VelocityHelper";
    private static final int MAXIMUM_FLING_VELOCITY_LIST = 3000;
    private final Context mContext;
    private final int mMaximumFlingVelocity;
    private VelocityTracker mVelocityTracker;
    private int mVelocity;
    private int mVelocityX;
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId = INVALID_POINTER;

    public VelocityHelper(Context context) {
        mContext = context;
        mMaximumFlingVelocity = getMaximumFlingVelocity(mContext);
    }

    public void onTouch(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            mActivePointerId = ev.getPointerId(0);
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            int pointerIndex = ev.findPointerIndex(mActivePointerId);
            if (pointerIndex == -1) {
                pointerIndex = 0;
                mActivePointerId = ev.getPointerId(pointerIndex);
            }
            break;
        }

        case MotionEvent.ACTION_POINTER_UP:
            onSecondaryPointerUp(ev);
            if (ev.getPointerCount() == 2) {
                mVelocityTracker.recycle();
                mVelocityTracker = VelocityTracker.obtain();
            }
            break;

        case MotionEvent.ACTION_UP:
            final VelocityTracker velocityTracker = mVelocityTracker;
            velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
            final int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
            mVelocity = initialVelocity;
            mVelocityX = (int) velocityTracker.getXVelocity(mActivePointerId);
            // Debugger.d(TAG, "onTouch: X,Y,mVelocity = " +
            // velocityTracker.getXVelocity()
            // + "," +velocityTracker.getYVelocity() + "," + initialVelocity);
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            mActivePointerId = INVALID_POINTER;
            break;

        case MotionEvent.ACTION_CANCEL:
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            mActivePointerId = INVALID_POINTER;
            break;

        case MotionEvent.ACTION_POINTER_DOWN: {
            final int index = ev.getActionIndex();
            final int id = ev.getPointerId(index);
            mActivePointerId = id;
            break;
        }
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    public int getVelocity() {
        return mVelocity;
    }

    public int getXVelocity() {
        return mVelocityX;
    }

    public static int getMaximumFlingVelocity(Context context) {
        // TODO: get MAXIMUM_FLING_VELOCITY from OPPO_SDK config xml
        final float density = context.getResources().getDisplayMetrics().density;
        return (int) (MAXIMUM_FLING_VELOCITY_LIST * density);

        // final ViewConfiguration configuration =
        // ViewConfiguration.get(context);
        // return configuration.getScaledMaximumFlingVelocity();
    }
}

