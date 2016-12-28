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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.support.v4.print.PrintHelper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.Log;
import com.android.gallery3d.ui.MyActionBar;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.galleryfeature.hotknot.HotKnot;
import com.mediatek.galleryfeature.pq.ImageDC;
import com.mediatek.galleryframework.base.MediaFilter;
import com.mediatek.galleryframework.base.MediaFilterSetting;

import android.content.ContentResolver;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import java.io.FileNotFoundException;


// transsion begin, IB-02533, xieweiwei, add, 2016.09.22
import com.mediatek.aal.AalUtils;
// transsion end

// transsion begin, IB-02533, xieweiwei, add, 2016.12.08
import com.transsion.gallery3d.ui.FloatingActionBar;
// transsion end

public class AbstractGalleryActivity extends Activity implements GalleryContext {
    private static final String TAG = "Gallery2/AbstractGalleryActivity";
    private GLRootView mGLRootView;
    private StateManager mStateManager;
    private GalleryActionBar mActionBar;
    private OrientationManager mOrientationManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private boolean mDisableToggleStatusBar;
    private PanoramaViewHelper mPanoramaViewHelper;
    protected ViewPagerHelper mViewPagerHelper;
    protected MyActionBar mMyActionBar;
    protected TabViewManager mTabViewManager;

    private AlertDialog mAlertDialog = null;
    /// M: [BUG.ADD] sign gallery status. @{
    private volatile boolean mHasPausedActivity;
    private volatile boolean mIsActivityResume = false;

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.15
    protected FloatingActionBar mFloatingActionBar;
    // transsion end

    /// @}
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /// M: [BUG.MODIFY] we don't care about SD card content;
            // As long as the card is mounted, dismiss the dialog @{
            /* if (getExternalCacheDir() != null) onStorageReady();
            */
            onStorageReady();
        }
            /// @}
    };
    /// M: [BUG.MODIFY] @{
    /*private IntentFilter mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);*/
    private IntentFilter mMountFilter = null;
    /// @}

    /// M: [FEATURE.ADD] HotKnot @{
    private HotKnot mHotKnot;
    /// @}
    // transsion begin, IB-02750, liangchangwei add, 2016.08.22
    private static int mSettingBrightnessValue = 0;
    private static final int SCREEN_BRIGHTNESS_THRESHOLD = (int)(255 * 0.8);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.01
        //getWindow().setBackgroundDrawable(null);
        // transsion end
        mPanoramaViewHelper = new PanoramaViewHelper(this);
        mPanoramaViewHelper.onCreate();
        doBindBatchService();
        /// M: [FEATURE.ADD] @{
        initializeMediaFilter();
        /// @}
        /// M: [FEATURE.ADD] HotKnot @{
        mHotKnot = new HotKnot(this);
        /// @}
        /// M: [BUG.ADD] leave selection mode when plug out sdcard. @{
        registerStorageReceiver();
        /// @}
        registerLocaleReceiver();
        
        /// M: [FEATURE.ADD] Image DC @{
        ImageDC.resetImageDC(((Context) this));
        /// @}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        getGalleryActionBar().onConfigurationChanged();
        invalidateOptionsMenu();
        toggleStatusBarByOrientation();
        Log.w(TAG,"onConfigurationChanged ");
        mStateManager.onConfigurationChange(config);
        /// M: [BUG.ADD] @{
        // the picture show abnormal after rotate device to landscape mode,
        // lock device, rotate device to portrait mode,
        // unlock device, to resolve this problem, let it show dark color
        if (mHasPausedActivity) {
            mGLRootView.setVisibility(View.GONE);
        }
        /// @}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    @Override
    public DataManager getDataManager() {
        return ((GalleryApp) getApplication()).getDataManager();
    }

    @Override
    public ThreadPool getThreadPool() {
        return ((GalleryApp) getApplication()).getThreadPool();
    }

    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this);
        }
        return mStateManager;
    }

    public GLRoot getGLRoot() {
        return mGLRootView;
    }

    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.19
        //mViewPagerHelper = new ViewPagerHelper(this);
        // transsion end
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
        /// M: [FEATURE.ADD] @{
        PhotoPlayFacade.registerMedias(this.getAndroidContext(),
                mGLRootView.getGLIdleExecuter());
        /// @}
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.19
        //mGLRootView.setContentPane(mViewPagerHelper.getGlViewPager());
        // transsion end
    }

    protected void onStorageReady() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
            unregisterReceiver(mMountReceiver);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        /// M: [BUG.ADD] if we're viewing a non-local file/uri, do NOT check storage@{
        // or pop up "No storage" dialog
        Log.d(TAG, "<onStart> mShouldCheckStorageState = " + mShouldCheckStorageState);
        if (!mShouldCheckStorageState) {
            return;
        }
        /// @}
        /// M: [BUG.MODIFY] @{
        /*if (getExternalCacheDir() == null) {*/
        if (FeatureHelper.getExternalCacheDir(this) == null
                && (!FeatureHelper.isDefaultStorageMounted(this))) {
        /// @}
            OnCancelListener onCancel = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            };
            OnClickListener onClick = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_external_storage_title)
                    .setMessage(R.string.no_external_storage)
                    .setNegativeButton(android.R.string.cancel, onClick)
                    .setOnCancelListener(onCancel);
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(builder);
            } else {
                builder.setIcon(android.R.drawable.ic_dialog_alert);
            }
            mAlertDialog = builder.show();
            /// M: [BUG.ADD] @{
            if (mMountFilter == null) {
                mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
                mMountFilter.addDataScheme("file");
            }
            /// @}
            registerReceiver(mMountReceiver, mMountFilter);
        }
        mPanoramaViewHelper.onStart();
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    private static void setAlertDialogIconAttribute(
            AlertDialog.Builder builder) {
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAlertDialog != null) {
            unregisterReceiver(mMountReceiver);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mPanoramaViewHelper.onStop();
        /// M: [BUG.ADD] @{
        mGLRootView.setVisibility(View.GONE);
        /// @}
    }

    @Override
    protected void onResume() {
        /// M: [DEBUG.ADD] @{
        if (SystemProperties.getInt("gallery.debug.renderlock", 0) == 1) {
            mDebugRenderLock = true;
            mGLRootView.startDebug();
        }
        /// @}
        super.onResume();
        mIsActivityResume = true;
        // transsion begin, IB-02533, xieweiwei, add, 2016.09.22
        // turn off back light balance
        AalUtils.getInstance().setEnabled(this, false);
        // transsion end

    // transsion begin, IB-02750, liangchangwei add, 2016.08.22
        mSettingBrightnessValue = getScreenBrightness(this);

        if (mSettingBrightnessValue < SCREEN_BRIGHTNESS_THRESHOLD) {
            setScreenBrightness(this, SCREEN_BRIGHTNESS_THRESHOLD);
        }
        /// M: [FEATURE.ADD] @{
        restoreFilter();
        PhotoPlayFacade.registerMedias(this.getAndroidContext(),
                mGLRootView.getGLIdleExecuter());
        /// @}
        mGLRootView.lockRenderThread();
        /// M: [BUG.ADD] @{
        // when default storage has been changed, we should refresh bucked id,
        // or else the icon showing on the album set slot can not update
        MediaSetUtils.refreshBucketId();
        /// @}
        try {
            getStateManager().resume();
            getDataManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        mGLRootView.onResume();
        mOrientationManager.resume();
        /// M: [BUG.ADD] save activity status. @{
        mHasPausedActivity = false;
        /// @}
        /// M: [BUG.ADD] fix abnormal screen @{
        mGLRootView.setVisibility(View.VISIBLE);
        /// @}
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityResume = false;
        Log.w(TAG,"onPause");
        // transsion begin, IB-02533, xieweiwei, add, 2016.09.22
        // turn on back light balance
        AalUtils.getInstance().setEnabled(this, true);
        // traanssion end

	// transsion begin, IB-02750, liangchangwei add, 2016.08.22
        if (mSettingBrightnessValue > 0) {
            setScreenBrightness(this, mSettingBrightnessValue);
        }
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        GalleryBitmapPool.getInstance().clear();
        MediaItem.getBytesBufferPool().clear();
        /// M: [BUG.ADD] save activity status. @{
        // the picture show abnormal after rotate device
        // to landscape mode,lock device, rotate device to portrait
        // mode, unlock device. to resolve this problem, let it show dark color
        mHasPausedActivity = true;
        /// @}
        /// M: [DEBUG.ADD] @{
        if (mDebugRenderLock) {
            mGLRootView.stopDebug();
            mDebugRenderLock = false;
        }
        /// @}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG,"onDestroy");
        mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        doUnbindBatchService();
        /// M: [FEATURE.ADD] @{
        removeFilter();
        /// @}
        /// M: [BUG.ADD] leave selection mode when plug out sdcard @{
        unregisterReceiver(mStorageReceiver);
        unregisterReceiver(mLocaleReceiver);
        /// @}

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.15
        //FloatingActionBar.getInstance(this).destroy();
        getFloatingActionBar().destroy();
        // transsion end
        // transsion end
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            /// M: [FEATURE.ADD] [Runtime permission] @{
            // When the critical permission of gallery is denied,
            // there is no activity state at all,
            // so check state count at first to avoid assert fail.
            if (getStateManager().getStateCount() == 0) {
                Log.i(TAG, "<onActivityResult> no state, return");
                return;
            }
            /// @}
            getStateManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    public GalleryActionBar getGalleryActionBar() {
        if (mActionBar == null) {
            mActionBar = new GalleryActionBar(this);
        }
        return mActionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    protected void disableToggleStatusBar() {
        mDisableToggleStatusBar = true;
    }

    // Shows status bar in portrait view, hide in landscape view
    public void toggleStatusBarByOrientation() {
        if (mDisableToggleStatusBar) return;

        Log.w(TAG,"toggleStatusBarByOrientation ");
        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

    public PanoramaViewHelper getPanoramaViewHelper() {
        return mPanoramaViewHelper;
    }

    protected boolean isFullscreen() {
        return (getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    private BatchService mBatchService;
    private boolean mBatchServiceIsBound = false;
    private ServiceConnection mBatchServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBatchService = ((BatchService.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBatchService = null;
        }
    };

    private void doBindBatchService() {
        bindService(new Intent(this, BatchService.class), mBatchServiceConnection, Context.BIND_AUTO_CREATE);
        mBatchServiceIsBound = true;
    }

    private void doUnbindBatchService() {
        if (mBatchServiceIsBound) {
            // Detach our existing connection.
            unbindService(mBatchServiceConnection);
            mBatchServiceIsBound = false;
        }
    }

    public ThreadPool getBatchServiceThreadPoolIfAvailable() {
        if (mBatchServiceIsBound && mBatchService != null) {
            return mBatchService.getThreadPool();
        } else {
            throw new RuntimeException("Batch service unavailable");
        }
    }

    public void printSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        String path = ImageLoader.getLocalPathFromUri(this, uri);
        if (path != null) {
            Uri localUri = Uri.parse(path);
            path = localUri.getLastPathSegment();
        } else {
            path = uri.getLastPathSegment();
        }
        PrintHelper printer = new PrintHelper(this);
        try {
            printer.printBitmap(path, uri);
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "Error printing an image", fnfe);
        }
    }


    //********************************************************************
    //*                              MTK                                 *
    //********************************************************************

    public boolean mShouldCheckStorageState = true;
    private boolean mDebugRenderLock = false;

    private MediaFilter mMediaFilter;
    private String mDefaultPath;

    private void initializeMediaFilter() {
        mMediaFilter = new MediaFilter();
        mMediaFilter.setFlagFromIntent(getIntent());
        boolean isFilterSame = MediaFilterSetting.setCurrentFilter(this, mMediaFilter);
        if (!isFilterSame) {
            Log.i(TAG, "<initializeMediaFilter> forceRefreshAll~");
            getDataManager().forceRefreshAll();
        }
    }

    private void restoreFilter() {
        boolean isFilterSame = MediaFilterSetting.restoreFilter(this);
        boolean isFilePathSame = !isDefaultPathChange();
        Log.i(TAG, "<restoreFilter> isFilterSame = " + isFilterSame
                + ", isFilePathSame = " + isFilePathSame);
        if (!isFilterSame || !isFilePathSame) {
            Log.i(TAG, "<restoreFilter> forceRefreshAll");
            getDataManager().forceRefreshAll();
        }
    }

    private void removeFilter() {
        MediaFilterSetting.removeFilter(this);
    }

    /**
     * Check if current activity is in paused status.
     *
     * @return if current activity is in paused status, return true, or else
     *         return false
     */
    public boolean hasPausedActivity() {
        return mHasPausedActivity;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        mGLRootView.dispatchKeyEventView(event);
        return super.dispatchKeyEvent(event);
    }

    /// M: [FEATURE.ADD] HotKnot @{
    public HotKnot getHotKnot() {
        return mHotKnot;
    }
    /// @}

    /// M: [BUG.ADD] @{
    private EjectListener mEjectListener;

    /**
     * Notify when plug out SD card.
     */
    public interface EjectListener {
        /**
         * Call back after eject SD card.
         */
        public void onEjectSdcard();
    }

    public void setEjectListener(EjectListener listener) {
        mEjectListener = listener;
    }

    private BroadcastReceiver mStorageReceiver;
    private void registerStorageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        mStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                    if (mEjectListener != null) {
                        mEjectListener.onEjectSdcard();
                    }
                }
            }
        };
        registerReceiver(mStorageReceiver, filter);
    }
    /// @}
    private BroadcastReceiver mLocaleReceiver;
    private void registerLocaleReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        mLocaleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                    Log.w(TAG,"context = " + context + " intent = " + intent);
                    finish();
                }
            }
        };
        registerReceiver(mLocaleReceiver, filter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return getStateManager().onPrepareOptionsMenu(menu);
    }

    private boolean isDefaultPathChange() {
        if (!mShouldCheckStorageState) {
            return false;
        }
        String newPath = FeatureHelper.getDefaultPath();
        boolean res = (mDefaultPath != null && !mDefaultPath.equals(newPath));
        mDefaultPath = newPath;
        Log.i(TAG, "<isDefaultPathChange> mDefaultPath = " + mDefaultPath);
        return res;
    }

    // transsion begin, IB-02750, liangchangwei add, 2016.08.22
    public static int getScreenBrightness(Activity activity) {
        int value = 0;
        ContentResolver cr = activity.getContentResolver();
        try {
            value = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
        }

        return value;
    }

    public static void setScreenBrightness(Activity activity, int value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = value / 255f;

        activity.getWindow().setAttributes(params);
    }

    public ViewPagerHelper getViewPagerHelper() {
        return mViewPagerHelper;
    }

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
    public void initViewPagerHelper() {
        if (mViewPagerHelper == null) {
            mViewPagerHelper = new ViewPagerHelper(this);
        }
    }
    // transsion end

    public TabViewManager getTabViewManager() {
        return mTabViewManager;
    }

    public MyActionBar getMyActionBar() {
        if (mMyActionBar == null) {
            mMyActionBar = new MyActionBar(this);
        }
        return mMyActionBar;
    }

    public boolean getIsActivityResume(){
        return mIsActivityResume;
    }

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.15
    public FloatingActionBar getFloatingActionBar() {
        if (mFloatingActionBar == null) {
            mFloatingActionBar = new FloatingActionBar(this, true);
        }
        return mFloatingActionBar;
    }
    // transsion end
}
