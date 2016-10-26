package com.android.gallery3d.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.gallery3d.util.Future;
import com.android.gallery3d.data.ClusterAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MixAlbum;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.filtershow.crop.CropExtras;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.PhotoFallbackEffect.PositionProvider;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TimerSlotRender;
import com.android.gallery3d.ui.TimerSlotView;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryfeature.config.FeatureConfig;
import com.mediatek.galleryframework.util.GalleryPluginUtils;
import com.android.gallery3d.util.GalleryUtils;

public class NewTimerShaftPage extends ActivityState implements SelectionManager.SelectionListener,
MediaSet.SyncListener, GalleryActionBar.OnAlbumModeSelectedListener,
AbstractGalleryActivity.EjectListener {

	public static final String TAG = "NewTimerShaftPage";
	private static final boolean DEBUG = true;
    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SHOW_CLUSTER_MENU = "cluster-menu";
    public static final String KEY_AUTO_SELECT_ALL = "auto-select-all";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";

    public boolean mInitialized = false;

    private boolean mIsActive = false;
    private boolean mVisitorMode;
    private boolean mLoadingFailed;

	private SelectionManager mSelectionManager;

	
    private Config.NewTimerShaftPage mConfig;

	private TimerSlotView mSlotView;

	private TimerSlotRender mAlbumView;

	private ActionModeHandler mActionModeHandler;

	private Path mMediaSetPath;

	private String mParentMediaSetString;

	private MediaSet mMediaSet;

	private TimerDataLoader mAlbumDataAdapter;
    private RelativePosition mOpenCenter = new RelativePosition();

    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;
    private static final int MSG_PICK_PHOTO = 0;
    public static final int REQUEST_PHOTO = 2;
    private static final float USER_DISTANCE_METER = 0.3f;

    private int mSyncResult;
	private boolean mGetContent;
    private boolean mShowClusterMenu;
	private MyDetailsSource mDetailsSource;
    private boolean mLaunchedFromPhotoPage;
	private boolean mInCameraApp;
    private Handler mHandler;
    private PhotoFallbackEffect mResumeEffect;
    private PhotoFallbackEffect.PositionProvider mPositionProvider =
            new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mSlotView.getSlotRect(index);
            Rect bounds = mSlotView.bounds();
            rect.offset(bounds.left - mSlotView.getScrollX(),
                    bounds.top - mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mSlotView.getVisibleStart();
            int end = mSlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mAlbumDataAdapter.get(i);
                if (item != null && item.getPath() == path) return i;
            }
            return -1;
        }
    };
	private boolean mNeedUpdateSelection;
	private boolean mRestoreSelectionDone;
    private boolean mInitialSynced = false;
    private Future<Integer> mSyncTask = null;
	protected int mUserDistance;

	@Override
	protected void onCreate(Bundle data, Bundle storedState) {
		// TODO Auto-generated method stub
		super.onCreate(data, storedState);
        
		if(DEBUG) Log.w(TAG,"onCreate");
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        initializeViews();
        initializeData(data);
        
        mInitialized = true;

        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mShowClusterMenu = data.getBoolean(KEY_SHOW_CLUSTER_MENU, false);
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();

        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        /// M: [FEATURE.MODIFY] Container @{

        /// M: [FEATURE.MODIFY] [Camera independent from Gallery] @{
        /*mInCameraApp = data.getBoolean(PhotoPage.KEY_APP_BRIDGE, false);*/
        mInCameraApp = data.getBoolean(PhotoPage.KEY_LAUNCH_FROM_CAMERA, false);
        /// @}

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_PHOTO: {
                        pickPhoto(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };

        /// M: [FEATURE.ADD] Gallery picker plugin @{
        mActionModeHandler = GalleryPluginUtils.getGalleryPickerPlugin()
                .onCreate(mActivity, data, mActionModeHandler, mSelectionManager);
        
        }

	protected void pickPhoto(int slotIndex) {
		// TODO Auto-generated method stub
        pickPhoto(slotIndex, false);
	}

    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
        /// M: [DEBUG.ADD] @{
        Log.i(TAG, "<pickPhoto> slotIndex = " + slotIndex + ", startInFilmstrip = "
                + startInFilmstrip);
        /// @}
        if (!mIsActive) return;

        /// M: [BUG.MARK] @{
        /* if (!startInFilmstrip) {
            // Launch photos in lights out mode
            mActivity.getGLRoot().setLightsOutMode(true);
        }*/
        /// @}

        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return; // Item not ready yet, ignore the click

        /// M: [BUG.ADD] @{
        // setLightsOutMode after check if item is null
        if (!startInFilmstrip) {
            // Launch photos in lights out mode
            mActivity.getGLRoot().setLightsOutMode(true);
        }
        /// @}

        if (mGetContent) {
            onGetContent(item);
        } else if (mLaunchedFromPhotoPage) {
            TransitionStore transitions = mActivity.getTransitionStore();
            transitions.put(
                    PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_PICKED);
            transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
            onBackPressed();
        } else {
            /// M: [FEATURE.ADD] @{
            if (!FeatureConfig.SUPPORT_SLIDE_VIDEO_PLAY) {
                /// M:[FEATURE.ADD] play video directly. @{
                if (!startInFilmstrip && canBePlayed(item)) {
                    Log.i(TAG, "<pickPhoto> item.getName()");
                    playVideo(mActivity, item.getPlayUri(), item.getName());
                    return;
                }
                /// @}
            }
            /// @}

            // Get into the PhotoPage.
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                    getSlotRect(slotIndex));
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                    mMediaSetPath.toString());
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH,
                    item.getPath().toString());
            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_STARTED);
            data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP,
                    startInFilmstrip);
            data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, mMediaSet.isCameraRoll());
            /// M: [FEATURE.ADD] [Camera independent from Gallery] @{
            // Many times page switch lead to KEY_LAUNCH_FROM_CAMERA value changed.
            // So that PhotoPage behavior not be coincident.
            data.putBoolean(PhotoPage.KEY_LAUNCH_FROM_CAMERA, mInCameraApp);
            /// @}
            if (startInFilmstrip) {
                mActivity.getStateManager().switchState(this, FilmstripPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                            SinglePhotoPage.class, REQUEST_PHOTO, data);
            }
        }
    }

    private Rect getSlotRect(int slotIndex) {
        // Get slot rectangle relative to this root pane.
        Rect offset = new Rect();
        mRootPane.getBoundsOf(mSlotView, offset);
        Rect r = mSlotView.getSlotRect(slotIndex);
        r.offset(offset.left - mSlotView.getScrollX(), offset.top - mSlotView.getScrollY());
        return r;
    }
		
    public void playVideo(Activity activity, Uri uri, String title) {
        Log.i(TAG, "<playVideo> enter playVideo");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            activity.startActivityForResult(intent, PhotoPage.REQUEST_PLAY_VIDEO);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

	private boolean canBePlayed(MediaItem item) {
        int supported = item.getSupportedOperations();
        return ((supported & MediaItem.SUPPORT_PLAY) != 0
                && MediaObject.MEDIA_TYPE_VIDEO == item.getMediaType());
    }

	private void onGetContent(final MediaItem item) {
        DataManager dm = mActivity.getDataManager();
        Activity activity = mActivity;
        /// M: [FEATURE.ADD] @{
        MediaData md = item.getMediaData();
        if (md.mediaType == MediaData.MediaType.CONTAINER
                && md.subType == MediaData.SubType.CONSHOT) {
            PlatformHelper.enterContainerPage(activity, md, true, mData);
            return;
        }
        /// @}
        if (mData.getString(GalleryActivity.EXTRA_CROP) != null) {
            Uri uri = dm.getContentUri(item.getPath());
            Intent intent = new Intent(CropActivity.CROP_ACTION, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(getData());
            if (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null) {
                intent.putExtra(CropExtras.KEY_RETURN_DATA, true);
            }
            /// M: [DEBUG.ADD] @{
            Log.d(TAG, "<onGetContent> start CropActivity for extra crop, uri: " + uri);
            /// @}
            activity.startActivity(intent);
            activity.finish();
        } else {
            Intent intent = new Intent(null, item.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.setResult(Activity.RESULT_OK, intent);
            /// M: [DEBUG.ADD] @{
            Log.d(TAG, "<onGetContent> return uri: " + item.getContentUri());
            /// @}
            activity.finish();
        }
    }
	
	private void initializeData(Bundle data) {
		// TODO Auto-generated method stub
        mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH));
        mParentMediaSetString = data.getString(KEY_PARENT_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
		if(DEBUG) Log.w(TAG,"initializeData mMediaSet = " + mMediaSet);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
        }

        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumDataAdapter = new TimerDataLoader(mActivity, mMediaSet);
        mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumView.setModel(mAlbumDataAdapter);
	}

	private void initializeViews() {
		// TODO Auto-generated method stub
        mSelectionManager = new SelectionManager(mActivity, true);
        mSelectionManager.setSelectionListener(this);
		
		if(DEBUG) Log.w(TAG,"initializeViews");
        mConfig = Config.NewTimerShaftPage.get(mActivity);
        mSlotView = new TimerSlotView(mActivity, mConfig.slotViewSpec);
        mAlbumView = new TimerSlotRender(mActivity, mSlotView, mSelectionManager);

        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                NewTimerShaftPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                NewTimerShaftPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                NewTimerShaftPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                NewTimerShaftPage.this.onLongTap(slotIndex);
            }

            @Override
            public void onScrollPositionChanged(int position, int total) {
            }
            
        });

        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return onItemSelected(item);
            }
            /// M: [BEHAVIOR.ADD] @{
            public boolean onPopUpItemClicked(int itemId) {
                // return if restoreSelection has done
                return false; //mRestoreSelectionDone;
            }
        });
	}

	protected void onLongTap(int slotIndex) {
		// TODO Auto-generated method stub
        mSlotView.invalidate();
        if (mGetContent) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        /// M: [BUG.ADD] fix menu display abnormal @{
        if (mActionModeHandler != null) {
            mActionModeHandler.closeMenu();
        }
        /// @}
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(item.getPath());
        mSlotView.invalidate();
	}

	protected void onSingleTapUp(int slotIndex) {
		// TODO Auto-generated method stub
        if (!mIsActive)
            return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mAlbumDataAdapter.get(slotIndex);
            if (item == null)
                return; // Item not ready yet, ignore the click
            mSelectionManager.toggle(item.getPath());
            mDetailsSource.findIndex(slotIndex);
            mSlotView.invalidate();
        } else {
            // Show pressed-up animation for the single-tap.

            mAlbumView.setPressedIndex(slotIndex);
            mAlbumView.setPressedUp();
            mHandler.removeMessages(MSG_PICK_PHOTO);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                    0);//180
        }
	}

	protected void onUp(boolean followedByLongPress) {
		// TODO Auto-generated method stub
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumView.setPressedIndex(-1);
        } else {
            mAlbumView.setPressedUp();
        }
		
	}

	protected void onDown(int index) {
		// TODO Auto-generated method stub
        mAlbumView.setPressedIndex(index);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
        mIsActive = false;

        mAlbumView.setSlotFilter(null);
        mActionModeHandler.pause();
        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        DetailsHelper.pause();
        /// M: [BEHAVIOR.MODIFY] behavior change: display album title when share via BT @{
        /*
        if (!mGetContent) {
            mActivity.getGalleryActionBar().disableAlbumModeMenu(true);
        }
        */
        // need to remove AlbumModeListener when pause,
        // otherwise no response when doCluster in AlbumSetPage
        mActivity.getGalleryActionBar().removeAlbumModeListener();
        /// @}
        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
        /// M: [BUG.ADD] leave selection mode when plug out sdcard @{
        mActivity.setEjectListener(null);
        /// @}
        /// M: [BUG.ADD] no need to update selection manager after pause @{
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mSelectionManager.saveSelection();
            mNeedUpdateSelection = false;
        }
        /// @}

        /// M: [FEATURE.ADD] Gallery picker plugin @{
        GalleryPluginUtils.getGalleryPickerPlugin().onPause();
}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.w(TAG,"onResume");
		mIsActive = true;
        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mAlbumView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }
        
        setContentPane(mRootPane);
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mNeedUpdateSelection = true;
            // set mRestoreSelectionDone as false if we need to retore selection
            mRestoreSelectionDone = false;
        } else {
            // set mRestoreSelectionDone as true there is no need to retore selection
            mRestoreSelectionDone = true;
        }

        mAlbumDataAdapter.resume();

        mAlbumView.resume();
        mAlbumView.setPressedIndex(-1);
        mActionModeHandler.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(this);
        }

        /// M: [BUG.ADD] leave selection mode when plug out sdcard @{
        mActivity.setEjectListener(this);
        /// @}

        /// M: [FEATURE.ADD] Gallery picker plugin @{
        GalleryPluginUtils.getGalleryPickerPlugin().onResume(mSelectionManager);
        /// @}

	}

	@Override
	protected boolean onCreateActionBar(Menu menu) {
		// TODO Auto-generated method stub
		Log.w(TAG,"onCreateActionBar");
		return true;
		//return super.onCreateActionBar(menu);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void renderBackground(GLCanvas view) {
            view.clearBuffer();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = 0;
            //slotViewTop = mActivity.getMyActionBar().getActionBarSize4DivideScreen(right - left, bottom - top);

            slotViewTop = mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;

            mAlbumView.setHighlightItemPath(null);

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            if(DEBUG) Log.v(TAG, "onLayout left:" + left + " slotViewTop:" + slotViewTop + " right:" + right + " bottom:" + bottom);
            mSlotView.layout(left, slotViewTop, right, bottom);

            GalleryUtils.setViewPointMatrix(mMatrix, (right - left) / 2, (bottom - top) / 2,
                    -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            if (!mIsActive) {
                return;
            }
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            //if(DEBUG) Log.v(TAG, "render:");
            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                    mAlbumView.setSlotFilter(null);
                    mAlbumView.stopFallbackAnim();
                }
                invalidate();
            }
            canvas.restore();
        }
    };
    
    private boolean mShowDetails;
	private DetailsHelper mDetailsHelper;

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mAlbumView.setHighlightItemPath(null);
        mSlotView.invalidate();
    }

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else if (mVisitorMode) {
            super.onBackPressed();
        } else {
            // TODO: fix this regression
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            onUpPressed();
        }
    }
    
    private void onUpPressed() {
        if (mInCameraApp) {
            GalleryUtils.startGalleryActivity(mActivity);
        } else if (mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
        } else if (mParentMediaSetString != null) {
            Bundle data = new Bundle(getData());
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, mParentMediaSetString);
            mActivity.getStateManager().switchState(
                    this, AlbumSetPage.class, data);
        }else{
            super.onBackPressed();
        }
    }

	@Override
	public void onEjectSdcard() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAlbumModeSelected(int mode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncDone(MediaSet mediaSet, final int resultCode) {
        ((Activity) mActivity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                    if (resultCode == MediaSet.SYNC_RESULT_ERROR && mIsActive
                            && (mAlbumDataAdapter.size() <= 0)) {
                        // show error toast only if the album is empty
                        Toast.makeText((Context) mActivity, R.string.sync_album_error,
                                Toast.LENGTH_LONG).show();
                    }
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

	@Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionModeHandler.startActionMode();
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                mActionModeHandler.finishActionMode();
                mRootPane.invalidate();
                break;
            }
            /// M: [BEHAVIOR.ADD] @{
            // when click deselect all in menu, not leave selection mode
            case SelectionManager.DESELECT_ALL_MODE:
            /// @}
            case SelectionManager.SELECT_ALL_MODE: {
                mActionModeHandler.updateSupportedOperation();
                mRootPane.invalidate();
                break;
            }
        }
    }

	@Override
	public void onSelectionChange(Path path, boolean selected) {
		// TODO Auto-generated method stub
        int count = mSelectionManager.getSelectedCount();
        String format = mActivity.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        mActionModeHandler.setTitle(String.format(format, count));
        mActionModeHandler.updateSupportedOperation(path, selected);
	}

	@Override
	public void onSelectionRestoreDone() {
		// TODO Auto-generated method stub
		
	}

    private int mLoadingBits = 0;

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (!mVisitorMode && mAlbumDataAdapter.size() <= 0
                    && MixAlbum.MIXALBUM_PATH_ALL != mMediaSet.getPath()) {
                // Toast.makeText((Context) mActivity,
                // R.string.empty_album, Toast.LENGTH_LONG).show();
                mActivity.getStateManager().finishState(NewTimerShaftPage.this);
            }
        }
    }

    private class MyLoadingListener implements LoadingListener {

		@Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = false;
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = loadingFailed;
            showSyncErrorIfNecessary(loadingFailed);

            /// M: [BEHAVIOR.ADD] Restore selection status after load finish @{
            // We have to notify SelectionManager about data change,
            // and this is the most proper place we could find till now
            boolean inSelectionMode = (mSelectionManager != null && mSelectionManager
                    .inSelectionMode());
            int itemCount = mMediaSet != null ? mMediaSet.getMediaItemCount()
                    : 0;
            if(DEBUG) Log.d(TAG, "onLoadingFinished: item count=" + itemCount);
            mSelectionManager.onSourceContentChanged();
            boolean restore = false;
            if (itemCount > 0 && inSelectionMode) {
                mActionModeHandler.updateSupportedOperation();
                mActionModeHandler.updateSelectionMenu();
            }
            mRootPane.invalidate();
        }
    }

    private void showSyncErrorIfNecessary(boolean loadingFailed) {
        if ((mLoadingBits == 0) && (mSyncResult == MediaSet.SYNC_RESULT_ERROR) && mIsActive
                && (loadingFailed || (mAlbumDataAdapter.size() == 0))) {
            Toast.makeText(mActivity, R.string.sync_album_error,
                    Toast.LENGTH_LONG).show();
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        @Override
        public int size() {
            return mAlbumDataAdapter.size();
        }
       
        public int findIndex(int indexHint) {
            if (mAlbumDataAdapter.isActive(indexHint)) {
                mIndex = indexHint;
            } else {
                mIndex = mAlbumDataAdapter.getActiveStart();
                if (!mAlbumDataAdapter.isActive(mIndex)) {
                    return -1;
                }
            }
            return mIndex;
        }

		@Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mAlbumDataAdapter.findItem(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mAlbumDataAdapter.get(mIndex);
            if (item != null) {
                mAlbumView.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }
}
