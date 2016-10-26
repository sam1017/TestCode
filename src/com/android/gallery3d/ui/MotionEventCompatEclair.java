package com.android.gallery3d.ui;

import android.view.MotionEvent;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月24日 --- 下午2:40:54
 *
 */
public class MotionEventCompatEclair {
    public static int findPointerIndex(MotionEvent event, int pointerId) {
        return event.findPointerIndex(pointerId);
    }

    public static int getPointerId(MotionEvent event, int pointerIndex) {
        return event.getPointerId(pointerIndex);
    }

    public static float getX(MotionEvent event, int pointerIndex) {
        return event.getX(pointerIndex);
    }

    public static float getY(MotionEvent event, int pointerIndex) {
        return event.getY(pointerIndex);
    }
}

