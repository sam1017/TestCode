package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.TimerDataLoader;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.NinePatchTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.AlbumSlotRenderer.SlotFilter;
import com.android.gallery3d.ui.TimerSlidingWindow.AlbumEntry;
import com.android.gallery3d.ui.TimerSlidingWindow.Listener;
import com.android.gallery3d.R;
import com.mediatek.galleryfeature.config.FeatureConfig;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.PlayEngine;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;

import android.content.Context;

public class TimerSlotRender extends AbstractSlotRenderer implements
PlayEngine.OnFrameAvailableListener{

    private class MyDataModelListener implements TimerSlidingWindow.Listener {
        @Override
        public void onContentChanged() {
            // mSlotView.invalidate();
            updateEngineData();
            if (mTimerSlotView != null) {
                mTimerSlotView.invalidate();
            }
        }

        @Override
        public void onSizeChanged(int size, Object params) {
            //mSlotView.setSlotCount(size);
            if (mTimerSlotView != null) {
                mTimerSlotView.setSlotCount(size, params);
            }
        }
    }

	private static final int CACHE_SIZE = 256;
	private static final boolean DEBUG = false;
	private static final String TAG = "TimerSlotRender";
	private final AbstractGalleryActivity mActivity;
    private final TimerSlotView mTimerSlotView;
	private final SelectionManager mSelectionManager;
	private ColorTexture mWaitLoadingTexture;

    private TimerSlidingWindow mDataWindow;
    private Path mHighlightItemPath = null;
	private SlotFilter mSlotFilter;
	private boolean mInSelectionMode;
    private int mPressedIndex = -1;
	private boolean mAnimatePressedUp;
    private final NinePatchTexture mSelectedTexture;
	private boolean mSupportVTSP;
    private PlayEngine mPlayEngine;

	public TimerSlotRender(AbstractGalleryActivity activity, TimerSlotView slotView, SelectionManager selectionManager) {
		// TODO Auto-generated constructor stub
        super((Context) activity);
		if(DEBUG) Log.w(TAG,"TimerSlotRender");
        mActivity = activity;
        mTimerSlotView = slotView;
        mSelectionManager = selectionManager;
        mSupportVTSP = !FeatureConfig.sIsLowRamDevice;
        //mTimeVideoLabelMaker = new TimeVideoLabelMaker(activity);
        mWaitLoadingTexture = new ColorTexture(mActivity.getResources().getColor(R.color.default_background));
        mWaitLoadingTexture.setSize(1, 1);

        mSelectedTexture = new NinePatchTexture(activity.getAndroidContext(),
                R.drawable.grid_selected);
        if (mSupportVTSP) {
            mPlayEngine = PhotoPlayFacade.createPlayEngineForThumbnail(activity);
            mPlayEngine.setOnFrameAvailableListener(this);
        }
	}

	@Override
	public void prepareDrawing() {
		// TODO Auto-generated method stub
        mInSelectionMode = mSelectionManager.inSelectionMode();
	}

	@Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd);
            updateEngineData();
        }
    }

	@Override
	public void onSlotSizeChanged(int width, int height) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
    	if(DEBUG) Log.w(TAG,"renderSlot index:" + index + " width = " + width + " height = " + height);
        if (mSlotFilter != null && !mSlotFilter.acceptSlot(index))
            return 0;

        TimerSlidingWindow.AlbumEntry entry = mDataWindow.get(index);

        if(DEBUG) Log.w(TAG,"entry = " + entry);
        int renderRequestFlags = 0;

        if (entry == null) {
            return renderRequestFlags;
        }

        Texture content = checkTexture(entry.content);
        /// M: [MEMORY.ADD] Recycle the bitmap after uploaded texture for saving memory @{
        if (content != null) {
            mDataWindow.recycle(entry);
        }
        /// @}
        synchronized (entry.textureLock) {
            if (content == null) {
                content = mWaitLoadingTexture;
                entry.isWaitDisplayed = true;
            } else if (entry.isWaitDisplayed) {
                entry.isWaitDisplayed = false;
//                content = new FadeInTexture(mPlaceholderColor, entry.bitmapTexture);
                content = entry.bitmapTexture;
                entry.content = content;
            }
        }
        /// M: [FEATURE.ADD] VTSP @{
        boolean hasDraw = drawCurrentSlotDynamic(entry, canvas, index, width, height,
                entry.rotation);

        if (!hasDraw) {
            // / @}
            drawContent(canvas, content, width, height, entry.rotation);
            if ((content instanceof FadeInTexture) &&
                    ((FadeInTexture) content).isAnimating()) {
                renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
            }
        /// M: [FEATURE.ADD] VTSP @{
        }

        if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO
                && entry.item.getMediaData() != null
                && !(entry.item.getMediaData().isSlowMotion)) {
            drawVideoOverlay(canvas, width, height);
        }
        
        if (entry.isPanorama) {
            drawPanoramaIcon(canvas, width, height);
        }

        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);
        /// M: [FEATURE.ADD] @{
        FeatureHelper.drawMicroThumbOverLay(mActivity, canvas, width, height, entry.item);

        return renderRequestFlags;
    }

    private final int mPlayCount = PhotoPlayFacade.getThumbPlayCount();

    private boolean drawCurrentSlotDynamic(AlbumEntry entry, GLCanvas canvas,
            int index, int width, int height, int rotation) {
        if (!mSupportVTSP || entry.item == null) {
            return false;
        }
        // draw dynamic
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
        if (rotation != 0) {
            canvas.translate(width / 2, height / 2);
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate(-width / 2, -height / 2);
        }
        boolean hasDraw;
        if (rotation == 90 || rotation == 270) {
            hasDraw = mPlayEngine.draw(entry.item.getMediaData(), index
                    - mDataWindow.getActiveStart(), canvas.getMGLCanvas(), height, width);
        } else {
            hasDraw = mPlayEngine.draw(entry.item.getMediaData(), index
                    - mDataWindow.getActiveStart(), canvas.getMGLCanvas(), width, height);
        }
        canvas.restore();
        return hasDraw;
    }

    private void updateEngineData() {
        if (!mSupportVTSP || !mDataWindow.isAllActiveSlotsFilled()) {
            return;
        }
        MediaData[] data = new MediaData[mPlayCount];
        MediaItem tempItem = null;
        int start = mDataWindow.getActiveStart();
        for (int i = 0; i < mPlayCount; i++) {
            tempItem = mDataWindow.getMediaItem(start + i);
            if (tempItem != null) {
                data[i] = tempItem.getMediaData();
            } else {
                data[i] = null;
            }
        }
        mPlayEngine.updateData(data);
    }

    @Override
    public void onFrameAvailable(int index) {
    	mTimerSlotView.invalidate();
    }

	private int renderOverlay(GLCanvas canvas, int index, TimerSlidingWindow.AlbumEntry entry,
            int width, int height) {
                int renderRequestFlags = 0;
                if (mPressedIndex == index) {
                    if (mAnimatePressedUp) {
                        drawPressedUpFrame(canvas, width, height);
                        renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
                        if (isPressedUpFrameFinished()) {
                            mAnimatePressedUp = false;
                            mPressedIndex = -1;
                        }
                    } else {
                        drawPressedFrame(canvas, width, height);
                    }
                } else if ((entry.path != null) && (mHighlightItemPath == entry.path)) {
                    drawSelectedFrame(canvas, width, height);
                } else if (mInSelectionMode && mSelectionManager.isItemSelected(entry.path)) {
                    drawSelectedFrame(canvas, width, height);
                }
                return renderRequestFlags;
    }

    private void renderSelected(GLCanvas canvas, int width, int height) {
        mSelectedTexture.draw(canvas, 0, 0, width, height);
    }

	private static Texture checkTexture(Texture texture) {
        return (texture instanceof UploadedTexture) && ((UploadedTexture) texture).isUploading() ? null
                : texture;
    }

	public void setModel(TimerDataLoader model) {
		if(DEBUG) Log.w(TAG,"setModel");
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            // mSlotView.setSlotCount(0);
            if (mTimerSlotView != null) {
                mTimerSlotView.setSlotCount(0);
            }
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new TimerSlidingWindow(mActivity, model, CACHE_SIZE);
            mDataWindow.setListener(new MyDataModelListener());
            // mSlotView.setSlotCount(model.size());
            if (mTimerSlotView != null) {
        		if(DEBUG) Log.w(TAG,"mTimerSlotView setSlotCount:" + model.size());
                mTimerSlotView.setSlotCount(model.size());
            }
        }
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path)
            return;
        mHighlightItemPath = path;
        if (mTimerSlotView != null) {
            mTimerSlotView.invalidate();
        }
    }

    public void setSlotFilter(SlotFilter slotFilter) {
        mSlotFilter = slotFilter;
    }

	public void stopFallbackAnim() {
		// TODO Auto-generated method stub
        if (mDataWindow != null) {
            mDataWindow.stopFallbackAnim();
        }
	}

	public void resume() {
		// TODO Auto-generated method stub
        mDataWindow.resume();
        if (mTimerSlotView != null) {
            mTimerSlotView.resume();
        }
        if (mSupportVTSP) {
            mPlayEngine.resume();
        }
        updateEngineData();
	}

	public void setPressedIndex(int index) {
		// TODO Auto-generated method stub
        if (mPressedIndex == index) return;
        mPressedIndex = index;
        mTimerSlotView.invalidate();
	}

	public void pause() {
		// TODO Auto-generated method stub
        mDataWindow.pause();
        if (mTimerSlotView != null) {
            mTimerSlotView.pause();
        }
        if (mSupportVTSP) {
            mPlayEngine.pause();
        }
	}

    public void destroy() {
        mDataWindow.destroy();
    }

    public void setPressedUp() {
        if (mPressedIndex == -1)
            return;
        mAnimatePressedUp = true;
        if (mTimerSlotView != null) {
            mTimerSlotView.invalidate();
        }
    }
    
    public void setNonVisibleActive(boolean active) {
        if (mDataWindow != null) {
            mDataWindow.setNonVisibleActive(active, false);
        }
    }

}
