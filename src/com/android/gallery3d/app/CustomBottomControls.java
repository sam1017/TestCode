/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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

//////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2016-2036  TRANSSION HOLDINGS
//
//  PROPRIETARY RIGHTS of TRANSSION HOLDINGS are involved in the
//  subject matter of this material.  All manufacturing, reproduction, use,
//  and sales rights pertaining to this subject matter are governed by the
//  license agreement.  The recipient of this software implicitly accepts
//  the terms of the license.
//
//  Description: For support albumset bottom controls
//  Author:      IB-02533
//  Version:     V1.0
//  Date:        2016.11.21
//  Modification:
//////////////////////////////////////////////////////////////////////////////////

package com.android.gallery3d.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

import com.android.gallery3d.R;

import java.util.HashMap;
import java.util.Map;

public class CustomBottomControls implements OnClickListener {
    public interface Delegate {
        public boolean canDisplayBottomControls();
        public boolean canDisplayBottomControl(int control);
        public void onBottomControlClicked(int control);
        public void refreshBottomControlsWhenReady();
    }

    private Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap<View, Boolean>();

    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    private static final int CONTAINER_ANIM_DURATION_MS = 200;

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.20
    private static final int SHARE_INDEX = 0;
    private static final int MOVE_INDEX = 1;
    private static final int DELETE_INDEX = 2;
    private static final int MORE_INDEX = 3;
    // transsion end

    private static final int CONTROL_ANIM_DURATION_MS = 150;
    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }

    public CustomBottomControls(Delegate delegate, Context context, ViewGroup layout, boolean isGetMultiImage) {
        mDelegate = delegate;
        mParentLayout = layout;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.w("CustomBottomControls","CustomBottomControls isGetMultiImage = " + isGetMultiImage);
        if(isGetMultiImage){
            mContainer = (ViewGroup) inflater
                    .inflate(R.layout.custom_bottom_select_controls, mParentLayout, false);
        }else{
            mContainer = (ViewGroup) inflater
                    .inflate(R.layout.custom_bottom_controls, mParentLayout, false);
        }
        mParentLayout.addView(mContainer);

        for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshBottomControlsWhenReady();
    }

    public View getMoreButton() {
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.20
        //View child = mContainer.getChildAt(mContainer.getChildCount() - 1);
        View child = mContainer.getChildAt(MORE_INDEX);
        // transsion end
        return child;
    }

    public View getDeleteButton() {
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.20
        //View child = mContainer.getChildAt(mContainer.getChildCount() - 2);
        View child = mContainer.getChildAt(DELETE_INDEX);
        // transsion end
        return child;
    }

    // transsion begin, IB-02533, xieweiwei, add, 2016.11.26
    public View getShareButton() {
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.20
        //View child = mContainer.getChildAt(mContainer.getChildCount() - 3);
        View child = mContainer.getChildAt(SHARE_INDEX);
        // transsion end
        return child;
    }
    // transsion end

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.20
    public View getMoveButton() {
        View child = mContainer.getChildAt(MOVE_INDEX);
        return child;
    }
    // transsion end

    public void hide() {
        mContainer.clearAnimation();
        mContainerAnimOut.reset();
        mContainer.startAnimation(mContainerAnimOut);
        mContainer.setVisibility(View.INVISIBLE);
    }

    public void show() {
        mContainer.clearAnimation();
        mContainerAnimIn.reset();
        mContainer.startAnimation(mContainerAnimIn);
        mContainer.setVisibility(View.VISIBLE);
    }

    public void refresh() {
        boolean visible = mDelegate.canDisplayBottomControls();
        boolean containerVisibilityChanged = (visible != mContainerVisible);
        if (containerVisibilityChanged) {
            if (visible) {
                show();
            } else {
                hide();
            }
            mContainerVisible = visible;
        }
        if (!mContainerVisible) {
            return;
        }
        for (View control : mControlsVisible.keySet()) {
            Boolean prevVisibility = mControlsVisible.get(control);
            boolean curVisibility = mDelegate.canDisplayBottomControl(control.getId());
            if (prevVisibility.booleanValue() != curVisibility) {
                if (!containerVisibilityChanged) {
                    control.clearAnimation();
                    control.startAnimation(getControlAnimForVisibility(curVisibility));
                }
            }
            control.setVisibility(View.VISIBLE);
            control.setEnabled(curVisibility ? true : false); 
            mControlsVisible.put(control, curVisibility);
        }
        // Force a layout change
        mContainer.requestLayout(); // Kick framework to draw the control.
    }

    public void cleanup() {
        mParentLayout.removeView(mContainer);
        mControlsVisible.clear();
    }

    @Override
    public void onClick(View view) {
        Boolean controlVisible = mControlsVisible.get(view);
        if (mContainerVisible && controlVisible != null && controlVisible.booleanValue()) {
            mDelegate.onBottomControlClicked(view.getId());
        }
    }
}
