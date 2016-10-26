package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.TimerDataLoader;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalVideo;
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

import android.content.Context;

public class TimerSlotRender extends AbstractSlotRenderer{

    private class MyDataModelListener implements TimerSlidingWindow.Listener {
        @Override
        public void onContentChanged() {
            // mSlotView.invalidate();
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
	private static final boolean DEBUG = true;
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

	public TimerSlotRender(AbstractGalleryActivity activity, TimerSlotView slotView, SelectionManager selectionManager) {
		// TODO Auto-generated constructor stub
        super((Context) activity);
		if(DEBUG) Log.w(TAG,"TimerSlotRender");
        mActivity = activity;
        mTimerSlotView = slotView;
        mSelectionManager = selectionManager;
        //mTimeVideoLabelMaker = new TimeVideoLabelMaker(activity);
        mWaitLoadingTexture = new ColorTexture(mActivity.getResources().getColor(R.color.default_background));
        mWaitLoadingTexture.setSize(1, 1);

        mSelectedTexture = new NinePatchTexture(activity.getAndroidContext(),
                R.drawable.grid_selected);
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

        Texture content = null;
        synchronized (entry.textureLock) {

            content = checkTexture(entry.content);
            if (content == null) {
                content = mWaitLoadingTexture;
                entry.isWaitDisplayed = true;
            } else if (entry.isWaitDisplayed) {
                entry.isWaitDisplayed = false;
//                content = new FadeInTexture(mPlaceholderColor, entry.bitmapTexture);
                entry.content = content;
            }
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if ((content instanceof FadeInTexture) && ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
        }

        if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO) {
            LocalVideo localVideo = (LocalVideo) entry.item;
            int time = localVideo.durationInSec;
            //drawVideoOverlay(canvas, width, height,mTimeVideoLabelMaker.getTexture(time));
        } else if (entry.item != null) {
            String mimeType = entry.item.getMimeType();
            //if(mimeType != null && mimeType.contains("gif")){
            //    drawGifOverlay(canvas, width, height);
            //} else if (mimeType != null && mimeType.equalsIgnoreCase(MediaItem.MIME_TYPE_UBIFOCUS)) {
            //    drawUbiFocusOverlay(canvas, width, height);
            //} else 
            if (entry.item instanceof LocalImage) {
                LocalImage localImage = (LocalImage) entry.item;
            }
        }

        // LiuJunChao@Plf.MediaApp, 2013-1-17, Remove for:
        // album_support_irregular
        /*
         * if (entry.isPanorama) { drawPanoramaBorder(canvas, width, height); }
         */
        // #endif /* VENDOR_EDIT */

        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);

        return renderRequestFlags;
    }

    private int renderOverlay(GLCanvas canvas, int index, TimerSlidingWindow.AlbumEntry entry,
            int width, int height) {
        int renderRequestFlags = 0;
        if (mPressedIndex == index) {
            if (mAnimatePressedUp) {
                // drawPressedUpFrame(canvas, width, height);
                renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
                if (isPressedUpFrameFinished()) {
                    mAnimatePressedUp = false;
                    mPressedIndex = -1;
                }
            } else {
                // drawSelectedFrame(canvas, width,
                // height);//drawPressedFrame(canvas, width, height);
                renderSelected(canvas, width, height);
            }
        } else if ((entry.path != null) && (mHighlightItemPath == entry.path)) {
            // drawSelectedFrame(canvas, width, height);
            renderSelected(canvas, width, height);
        } else if (mInSelectionMode && mSelectionManager.isItemSelected(entry.path)) {
            // drawSelectedFrame(canvas, width, height);
            renderSelected(canvas, width, height);
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
