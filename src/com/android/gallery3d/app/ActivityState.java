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

package com.android.gallery3d.app;

import java.io.File;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.gallery3d.R;
import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.MyActionBar;
import com.android.gallery3d.ui.PreparePageFadeoutTexture;
import com.android.gallery3d.util.GalleryUtils;


// transsion begin, IB-02533, xieweiwei, add, 2016.11.28
import android.view.LayoutInflater;
// transsion end

import android.view.inputmethod.InputMethodManager;
// transsion begin, IB-02533, xieweiwei, add, 2016.11.29
import android.os.Handler;
import android.os.Message;
// transsion end

abstract public class ActivityState {
    protected static final int FLAG_HIDE_ACTION_BAR = 1;
    protected static final int FLAG_HIDE_STATUS_BAR = 2;
    protected static final int FLAG_SCREEN_ON_WHEN_PLUGGED = 4;
    protected static final int FLAG_SCREEN_ON_ALWAYS = 8;
    protected static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 16;
    protected static final int FLAG_SHOW_WHEN_LOCKED = 32;

    public static final int ACTION_FLAG_STANDARD = 0;
    public static final int ACTION_FLAG_TABS = 1;
	private static final int ACTION_FLAG_INDETERMINATE = 2;
    private final String FOLDER_PATH = "/" + Environment.DIRECTORY_DCIM + "/";

    protected AbstractGalleryActivity mActivity;
    protected Bundle mData;
    protected int mFlags;

    protected ResultEntry mReceivedResults;
    protected ResultEntry mResult;

    protected static class ResultEntry {
        public int requestCode;
        public int resultCode = Activity.RESULT_CANCELED;
        public Intent resultData;
    }

    private boolean mDestroyed = false;
    private boolean mPlugged = false;
    boolean mIsFinishing = false;

    private static final String KEY_TRANSITION_IN = "transition-in";
	private static final String TAG = "ActivityState";

    private StateTransitionAnimation.Transition mNextTransition =
            StateTransitionAnimation.Transition.None;
    private StateTransitionAnimation mIntroAnimation;
    private GLView mContentPane;
    protected int mTabIndex;
    private boolean mIsResumed = false;
    private static final int STATE_INVALID = -1;
    private static final int STATE_SHOW = 0;
    private static final int STATE_HIDE = 1;
    private int mMyActionBarState = STATE_INVALID;
    protected static ImageView mCameraImageView;

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.08
    protected static ImageView mNewFolderImageView;
    // transsion end

    // transsion begin, IB-02533, xieweiwei, add, 2016.11.28
    private LayoutInflater mInflater;
    private int mCameraLayoutWidth;
    private int mCameraLayoutHeight;
    protected static View mCameraLayoutView;
    // transsion end

    // transsion begin, IB-02533, xieweiwei, add, 2016.11.30
    protected static final int DO_CREATE = 100;
    protected static final int DO_RESUME = 101;
    protected static final int DO_CREATE_AND_RESUME = 102;
    protected int DELAY_TIME_TO_RESUME = 200;
    protected boolean mHasDoResume = false;
    protected boolean mHasDoCreate = false;
    // transsion end

    protected ActivityState() {
    }

    protected void setContentPane(GLView content) {
        mContentPane = content;
        if (mIntroAnimation != null) {
            mContentPane.setIntroAnimation(mIntroAnimation);
            mIntroAnimation = null;
        }
        mContentPane.setBackgroundColor(getBackgroundColor());
        //mActivity.getGLRoot().setContentPane(mContentPane);

        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.19
        //Log.w(TAG,"setContentPane mTabIndex = " + mTabIndex);
        //mActivity.getViewPagerHelper().setContentPane(mTabIndex, mContentPane);
        mActivity.getGLRoot().setContentPane(mContentPane);
        // transsion end
    }

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
    protected void setTabContentPane(GLView content) {
        mContentPane = content;
        if (mIntroAnimation != null) {
            mContentPane.setIntroAnimation(mIntroAnimation);
            mIntroAnimation = null;
        }
        mContentPane.setBackgroundColor(getBackgroundColor());
        Log.w(TAG,"setContentPane mTabIndex = " + mTabIndex);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.21
        if (mActivity.getViewPagerHelper() != null) {
        // transsion end
        mActivity.getGLRoot().setContentPane(mActivity.getViewPagerHelper().getGlViewPager());
        mActivity.getViewPagerHelper().setContentPane(mTabIndex, mContentPane);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.21
        } else {
            mActivity.getGLRoot().setContentPane(mContentPane);
        }
        // transsion end
    }
    // transsion end

    void initialize(AbstractGalleryActivity activity, Bundle data) {
        mActivity = activity;
        mData = data;

        // transsion begin, IB-02533, xieweiwei, add, 2016.11.28
        mInflater = LayoutInflater.from(mActivity);
        mCameraLayoutWidth = (int)activity.getResources().getDimensionPixelSize(R.dimen.camera_indicator_layout_width);
        mCameraLayoutHeight = (int)activity.getResources().getDimensionPixelSize(R.dimen.camera_indicator_layout_height);
        // transsion end

    }

    void initialize(AbstractGalleryActivity activity, Bundle data, int tabIndex) {
        mActivity = activity;
        mData = data;
        mTabIndex = tabIndex;
        Log.w(TAG,"initialize data = " + data + " tabIndex = " + tabIndex);

        // transsion begin, IB-02533, xieweiwei, add, 2016.11.28
        mInflater = LayoutInflater.from(mActivity);
        mCameraLayoutWidth = (int)activity.getResources().getDimensionPixelSize(R.dimen.camera_indicator_layout_width);
        mCameraLayoutHeight = (int)activity.getResources().getDimensionPixelSize(R.dimen.camera_indicator_layout_height);
        // transsion end

    }

    public int getTabIndex() {
        return mTabIndex;
    }
    
    public Bundle getData() {
        return mData;
    }

    protected void onBackPressed() {
        Log.w(TAG,"onBackPressed finishState " + this);
        mActivity.getStateManager().finishState(this);
    }

    protected void setStateResult(int resultCode, Intent data) {
        if (mResult == null) return;
        mResult.resultCode = resultCode;
        mResult.resultData = data;
    }

    protected void onConfigurationChanged(Configuration config) {
    }

    protected void onSaveState(Bundle outState) {
    }

    protected void onStateResult(int requestCode, int resultCode, Intent data) {
    }

    protected float[] mBackgroundColor;
    private static int stateC;
    private static int stateActivityC;
    protected int mActionFlags = ACTION_FLAG_INDETERMINATE;

    protected int getBackgroundColorId() {
        return R.color.default_background;
    }

    protected float[] getBackgroundColor() {
        return mBackgroundColor;
    }

    protected void onCreate(Bundle data, Bundle storedState) {
        mBackgroundColor = GalleryUtils.intColorToFloatARGBArray(
                mActivity.getResources().getColor(getBackgroundColorId()));
        stateC++;
        Log.d(TAG, "create\t stateC = " + stateC + " , " + this);
        mActionFlags = data.getInt(GalleryActivity.KEY_ACTION_FLAG, ACTION_FLAG_STANDARD);

        //onCreate(data, storedState);
    }

    protected void clearStateResult() {
    }

    BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean plugged = (0 != intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));

                if (plugged != mPlugged) {
                    mPlugged = plugged;
                    setScreenFlags();
                }
            }
        }
    };

    private void setScreenFlags() {
        final Window win = mActivity.getWindow();
        final WindowManager.LayoutParams params = win.getAttributes();
        if ((0 != (mFlags & FLAG_SCREEN_ON_ALWAYS)) ||
                (mPlugged && 0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED))) {
            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        if (0 != (mFlags & FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)) {
            params.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        }
        if (0 != (mFlags & FLAG_SHOW_WHEN_LOCKED)) {
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        }
        win.setAttributes(params);
    }

    protected void transitionOnNextPause(Class<? extends ActivityState> outgoing,
            Class<? extends ActivityState> incoming, StateTransitionAnimation.Transition hint) {
        if (outgoing == SinglePhotoPage.class && incoming == AlbumPage.class) {
            mNextTransition = StateTransitionAnimation.Transition.Outgoing;
        } else if (outgoing == AlbumPage.class && incoming == SinglePhotoPage.class) {
            mNextTransition = StateTransitionAnimation.Transition.PhotoIncoming;
        } else {
            mNextTransition = hint;
        }
    }

    protected void performHapticFeedback(int feedbackConstant) {
        mActivity.getWindow().getDecorView().performHapticFeedback(feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }

    void pause() {
        if (!mIsResumed) {
            return;
        }
        mIsResumed = false;
        stateActivityC--;
        Log.i(TAG, "pause\t stateActivityC = " + stateActivityC + ", " + this);
        saveMyActionBarState();
        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            ((Activity) mActivity).unregisterReceiver(mPowerIntentReceiver);
        }
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mActivity.getTransitionStore().put(KEY_TRANSITION_IN, mNextTransition);
            PreparePageFadeoutTexture.prepareFadeOutTexture(mActivity, mContentPane);
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
        onPause();
    }

    protected void onPause() {
    }

    private void saveMyActionBarState() {
        MyActionBar actionBar = mActivity.getMyActionBar();
        if (actionBar == null) {
            mMyActionBarState = STATE_INVALID;
            return;
        }

        if (actionBar.getVisibility() == View.VISIBLE) {
            mMyActionBarState = STATE_SHOW;
        } else {
            mMyActionBarState = STATE_HIDE;
        }
        Log.w(TAG,"saveMyActionBarState mMyActionBarState = " + mMyActionBarState);
    }

	// should only be called by StateManager
    void resume() {
        if (mIsResumed) {
            return;
        }
        mIsResumed = true;
        stateActivityC++;

        Log.w(TAG,"resume + " + this);
        AbstractGalleryActivity activity = mActivity;
        updateActionNavigationMode();
        updateMyActionBarState();
	    /*	
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            /// M: [BUG.ADD] @{
            // Avoid to set ActionBar visibility in some cases
            if (mNotSetActionBarVisibiltyWhenResume == false) {
            /// @}
                if ((mFlags & FLAG_HIDE_ACTION_BAR) != 0) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
            /// M: [BUG.ADD] @{
            }
            /// @}
            int stateCount = mActivity.getStateManager().getStateCount();
            mActivity.getGalleryActionBar().setDisplayOptions(stateCount > 1, true);
            // Default behavior, this can be overridden in ActivityState's onResume.
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
        */
        activity.invalidateOptionsMenu();

        setScreenFlags();

        boolean lightsOut = ((mFlags & FLAG_HIDE_STATUS_BAR) != 0);
        mActivity.getGLRoot().setLightsOutMode(lightsOut);

        ResultEntry entry = mReceivedResults;
        if (entry != null) {
            mReceivedResults = null;
            onStateResult(entry.requestCode, entry.resultCode, entry.resultData);
        }

        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            // we need to know whether the device is plugged in to do this correctly
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            activity.registerReceiver(mPowerIntentReceiver, filter);
        }

        onResume();

        // the transition store should be cleared after resume;
        mActivity.getTransitionStore().clear();
    }

    private void updateMyActionBarState() {
        MyActionBar actionBar = mActivity.getMyActionBar();
        if (actionBar == null || !isUpdateMenuEnable()) {
            return;
        }

        Log.w(TAG,"updateMyActionBarState mMyActionBarState = " + mMyActionBarState);
        switch (mMyActionBarState) {
            case STATE_INVALID:
                if ((mFlags & FLAG_HIDE_ACTION_BAR) == 0) {
                    actionBar.setVisibility(View.VISIBLE);
                    showStateBar();
                } else {
                    actionBar.setVisibility(View.GONE);
                    hideStateBar();
                }
                break;

            case STATE_SHOW:
                actionBar.setVisibility(View.VISIBLE);
                showStateBar();
                break;

            case STATE_HIDE:
                actionBar.setVisibility(View.GONE);
                //hideStateBar();
                break;

            default:
                throw new IllegalArgumentException("error tab visibility state!");
        }
    }

    protected void showStateBar() {
		Log.w(TAG,"showStateBar");
        int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        ((Activity) mActivity).getWindow().setFlags(flag, flag);
        ((Activity) mActivity).getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        mActivity.getGLRoot().setLightsOutMode(false);
        // transsion end

    }

    protected void hideStateBar() {
		Log.w(TAG,"hideStateBar");
        ((Activity) mActivity).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ((Activity) mActivity).getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        ((Activity) mActivity).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mActivity.getGLRoot().setLightsOutMode(true);
        // transsion end

    }
    
    protected boolean isUpdateMenuEnable() {
        boolean flag = mActivity.getStateManager().isUpdateMenuEnable(this);
        Log.w(TAG,"isUpdateMenuEnable this = " + this + " flag = " + flag);
        return flag;
    }

	protected void updateActionNavigationMode() {
        MyActionBar myActionBar = mActivity.getMyActionBar();
        Log.w(TAG,"updateActionNavigationMode 1");
        if (!isUpdateMenuEnable() || myActionBar.isInSelectedMode()) {
            Log.w(TAG,"updateActionNavigationMode 1 return");
            return;
        }
        ActionBar actionBar = ((Activity) mActivity).getActionBar();
        Log.w(TAG,"updateActionNavigationMode mActionFlags = " + mActionFlags);
        switch (mActionFlags) {
            case ACTION_FLAG_STANDARD:
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
                if (actionBar != null) {
                // transsion end
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                        | ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_HOME_AS_UP
                        | ActionBar.DISPLAY_SHOW_TITLE);
                actionBar.setHomeButtonEnabled(true);
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
                }
                // transsion end
                if (mActivity.getViewPagerHelper() != null) {
                    mActivity.getViewPagerHelper().setHorizontalEnable(false);
                }
                break;

            case ACTION_FLAG_TABS:
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.21
                if (mActivity.getTabViewManager() == null) {
                    return;
                }
                // transsion end
                mActivity.getTabViewManager().lockTabIndex();
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
                if (actionBar != null) {
                // transsion end
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayShowHomeEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
                }
                // transsion end
                int index = mActivity.getTabViewManager().getCurrentTabIndex();
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
                if (actionBar != null) {
                // transsion end
                if (index != actionBar.getSelectedNavigationIndex()) {
                    actionBar.setSelectedNavigationItem(index);
                }
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
                }
                // transsion end
                mActivity.getTabViewManager().unlockTabIndex();
                if (mActivity.getViewPagerHelper() != null) {
                    mActivity.getViewPagerHelper().setHorizontalEnable(
                            !mActivity.getMyActionBar().isInSelectedMode());
                }
                break;

            default:
                if (mActivity.getViewPagerHelper() != null) {
                    mActivity.getViewPagerHelper().setHorizontalEnable(false);
                }
                break;
        }
    }

	// a subclass of ActivityState should override the method to resume itself
    protected void onResume() {
        RawTexture fade = mActivity.getTransitionStore().get(
                PreparePageFadeoutTexture.KEY_FADE_TEXTURE);
        mNextTransition = mActivity.getTransitionStore().get(
                KEY_TRANSITION_IN, StateTransitionAnimation.Transition.None);
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mIntroAnimation = new StateTransitionAnimation(mNextTransition, fade);
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    protected boolean onCreateActionBar(Menu menu) {
        // TODO: we should return false if there is no menu to show
        //       this is a workaround for a bug in system
        return true;
    }

    protected boolean onItemSelected(MenuItem item) {
        return false;
    }

    protected void onDestroy() {
        if (mDestroyed) {
            return;
        }
        stateC--;
        Log.d(TAG, "destroy\t stateC = " + stateC + " " + this);
        mDestroyed = true;
    }

    boolean isDestroyed() {
        return mDestroyed;
    }

    public boolean isFinishing() {
        return mIsFinishing;
    }

    protected MenuInflater getSupportMenuInflater() {
        return mActivity.getMenuInflater();
    }

    //********************************************************************
    //*                              MTK                                 *
    //********************************************************************
    /// M: [BUG.ADD] dataManager object key.@{
    protected static final String KEY_DATA_OBJECT = "data-manager-object";
    protected static final String KEY_PROCESS_ID = "process-id";
    ///@}
    // Avoid to set ActionBar visibility in some cases
    protected boolean mNotSetActionBarVisibiltyWhenResume = false;

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    /// M: [FEATURE.ADD] [Runtime permission] @{
    /**
     * Dispatch the onRequestPermissionsResult call back from Activity to
     * ActivityState.
     *
     * @param requestCode
     *            The request code passed in requestPermissions(Activity,
     *            String[], int)
     * @param permissions
     *            The request permissions. Never null.
     * @param grantResults
     *            The grant results for the corresponding permissions which is
     *            either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
    }
    /// @}

    /// M: [PERF.ADD] add for delete many files performance improve @{
    /**
     * Set if ActivityState is sensitive to change of data.
     *
     * @param isProviderSensive
     *            If ActivityState is sensitive to change of data
     */
    public void setProviderSensive(boolean isProviderSensive) {
    }

    /**
     * Notify that the content is dirty and trigger some operations that only
     * occur when content really changed.
     */
    public void fakeProviderChange() {
    }
    /// @}

	public void onUpdateMenu() {
		// TODO Auto-generated method stub
		
	}

    protected void hideCameraView(){
        Log.w(TAG, "hideCameraView mCameraImageView = " + mCameraImageView);
        if(mCameraImageView != null){
            Log.w(TAG, "hideCameraView setVisibility");
            mCameraImageView.setVisibility(View.INVISIBLE);
            // transsion begin, IB-02533, xieweiwei, add, 2016.11.28
            if(mCameraLayoutView != null) mCameraLayoutView.setVisibility(View.INVISIBLE);
            // transsion end

            // transsion begin, IB-02533,xieweiwei, add, 2016.12.08
            if(mNewFolderImageView != null) mNewFolderImageView.setVisibility(View.GONE);
            // transsion end
        }
    }

    protected void showCameraView() {
	    // TODO Auto-generated method stub
        Log.w(TAG, "showCameraView mCameraImageView = " + mCameraImageView);
        if (mCameraImageView == null && !setupCameraView()) return;
        Log.w(TAG, "showCameraView setVisibility");
        mCameraImageView.setVisibility(View.VISIBLE);
        // transsion begin, IB-02533, xieweiwei, add, 2016.11.28
        if(mCameraLayoutView != null) mCameraLayoutView.setVisibility(View.VISIBLE);
        // transsion end

        // transsion begin, IB-02533,xieweiwei, add, 2016.12.08
        if(mNewFolderImageView != null) mNewFolderImageView.setVisibility(View.GONE);
        // transsion end
	}

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.08
    protected void hideNewFolderView(){
        Log.w(TAG, "hideNewFolderView mNewFolderImageView = " + mNewFolderImageView);
        if(mNewFolderImageView != null){
            if(mCameraImageView != null)  mCameraImageView.setVisibility(View.GONE);
            if(mCameraLayoutView != null) mCameraLayoutView.setVisibility(View.INVISIBLE);
            mNewFolderImageView.setVisibility(View.INVISIBLE);
        }
    }

    protected void showNewFolderView() {
        Log.w(TAG, "showNewFolderView mNewFolderImageView = " + mNewFolderImageView);
        if (mNewFolderImageView == null && !setupCameraView()) return;
        if(mCameraImageView != null) mCameraImageView.setVisibility(View.GONE);
        if(mCameraLayoutView != null) mCameraLayoutView.setVisibility(View.VISIBLE);
        mNewFolderImageView.setVisibility(View.VISIBLE);
    }
    // transsion end

    protected boolean setupCameraView() {
	    // TODO Auto-generated method stub
        if (!GalleryUtils.isCameraAvailable(mActivity)) return false;
        RelativeLayout galleryRoot = (RelativeLayout) ((Activity) mActivity)
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return false;

        // transsion begin, IB-02533, xieweiwei, modify, 2016.11.28
        //mCameraImageView = new ImageView(mActivity);
        mCameraLayoutView = mInflater.inflate(R.layout.camera_indicator, null);
        mCameraImageView = (ImageView)mCameraLayoutView.findViewById(R.id.camera_indicator);
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.08
        mNewFolderImageView = (ImageView)mCameraLayoutView.findViewById(R.id.newfolder_indicator);
        mNewFolderImageView.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                LayoutInflater factory = LayoutInflater.from(mActivity);
                final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
                final EditText inputServer = (EditText)textEntryView.findViewById(R.id.username_edit);
                inputServer.setFocusable(true);
                inputServer.setFocusableInTouchMode(true);
                inputServer.requestFocus();
                mMainHandler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        InputMethodManager inputManager = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(inputServer, 0);
                    }
                }, 80);
                String tempStr = mActivity.getResources().getString(R.string.add_new_gallery_folder);
                int i = 1;
                while(new File(Environment.getExternalStorageDirectory() + FOLDER_PATH + tempStr + i).exists()){
                    i++;
                }
                final String hintStr = tempStr + i;
                inputServer.setHint(hintStr);
                new AlertDialog.Builder(mActivity)
                    //.setTitle(R.string.add_new_gallery_folder)
                    .setView(textEntryView)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /* User clicked OK so do some stuff */
                            if(inputServer.getText().toString().length() == 0){
                                inputServer.setText(hintStr);
                            }
                            Log.w(TAG,"new Folder: " + inputServer.getText().toString());
                            //File dir = new File(Environment.getExternalStorageDirectory() + FOLDER_PATH + inputServer.getText().toString());
                            //if (!dir.exists()){
                            //    Log.d(TAG, "dir not exit,will create this, path = " + Environment.getExternalStorageDirectory() + FOLDER_PATH + inputServer.getText().toString());
                            //    dir.mkdirs();
                                ActivityState state = mActivity.getStateManager().getTopState();
                                if((state instanceof AlbumSetPage)){
                                    state.newfolderpickuppicture(Environment.getExternalStorageDirectory() + FOLDER_PATH + inputServer.getText().toString());
                                }
                            //}
                        }
                    })
                    .setNegativeButton(R.string.review_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /* User clicked cancel so do some stuff */
                        }
                    })
                    .create().show();
            }
        });
        // transsion end

        //mCameraImageView.setImageDrawable(new Drawable(BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.camera_click)));
        mCameraImageView.setImageResource(R.drawable.camera_click);
        mCameraImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                GalleryUtils.startCameraActivity(mActivity);
            }
        });
        // transsion begin, IB-02533, xieweiwei, modify, 2016.11.28
        //RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        //        RelativeLayout.LayoutParams.WRAP_CONTENT,
        //        RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mCameraLayoutWidth, mCameraLayoutHeight);
        // transsion end
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.01
        //lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        // transsion end
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.01
        //lp.rightMargin = 28;
        //lp.bottomMargin = 30;
        // transsion end

        // transsion begin, IB-02533, xieweiwei, modify, 2016.11.28
        //galleryRoot.addView(mCameraImageView, lp);
        galleryRoot.addView(mCameraLayoutView, lp);
        // transsion end
        return true;
	}

    protected void newfolderpickuppicture(String path) {
        // TODO Auto-generated method stub
    }

	// transsion begin, IB-02533, xieweiwei, add, 2016.11.30
    protected void doResume() {
    }

    protected void doCreate() {
    }

    protected void delayDoResume() {
        mHasDoResume = false;
        mMainHandler.sendEmptyMessageDelayed(ActivityState.DO_CREATE_AND_RESUME, DELAY_TIME_TO_RESUME);
    }

    protected Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case DO_CREATE:
                doCreate();
                mHasDoCreate = true;
                break;
            case DO_RESUME:
                doResume();
                mHasDoResume = true;
                break;
            case DO_CREATE_AND_RESUME:
                if (!mHasDoCreate) {
                    doCreate();
                    mHasDoCreate = true;
                }
                doResume();
                mHasDoResume = true;
            default:
                break;
            }
        }
    };
    // transsion end

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.21
    public void resetCameraAndNewFolderView() {
        mCameraImageView = null;
        mNewFolderImageView = null;
    }
    // transsion end
}
