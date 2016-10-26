package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.TimerDataLoader;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TextureUploader;
import com.android.gallery3d.ui.TimerSlidingWindow.AlbumEntry;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.JobLimiter;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Future;

public class TimerSlidingWindow implements TimerDataLoader.DataListener{

	private static final String TAG = "TimerSlidingWindow";
    private static final boolean DEBUG = true;
    private static final int MSG_UPDATE_ENTRY = 0;

    public static interface Listener {
        public void onSizeChanged(int size, Object params);

        public void onContentChanged();
    }
    
    public static class AlbumEntry {
        public MediaItem item;
        public Path path;
        public boolean isPanorama;
        public int rotation;
        public int mediaType;
        public boolean isWaitDisplayed;
        public BitmapTexture bitmapTexture;
        public Texture content;
        private BitmapLoader contentLoader;
        public byte[] textureLock = new byte[0];
    }

	private AbstractGalleryActivity mActivity;
    private final TimerDataLoader mSource;
    private final AlbumEntry mData[];
    private int mSize = 0;
    private HandlerThread mThread;
    private SynchronizedHandler mHandler;
	public Handler mDelayHandler;
    private final JobLimiter mThreadPool;
    private final TextureUploader mTextureUploader;
    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;
	private Listener mListener;
	private boolean mNonVisibleActive;
	private boolean mIsActive;
	private boolean mNeedRefresh = true;
    private int mActiveRequestCount = 0;
    private boolean mIsLoadContentThumbNail = true;
    private boolean mInFallbackAnimClose = false;
    private int mHistoryActiveStart = 0;
    private int mHistoryActiveEnd = 0;
    private static final int JOB_LIMIT = 4;


    public TimerSlidingWindow(AbstractGalleryActivity activity,
            TimerDataLoader source, int cacheSize) {
        mActivity = activity;
		if(DEBUG) Log.w(TAG,"TimerSlidingWindow");
        source.setDataListener(this);
        mSource = source;
        mData = new AlbumEntry[cacheSize];
        mSize = source.size();
        mThread = new MyHandlerThread("AlbumSlidingWindow");
        mThread.start();
        mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == MSG_UPDATE_ENTRY);
                ((ThumbnailLoader) message.obj).updateEntry();
            }
        };

        mThreadPool = new JobLimiter(activity.getThreadPool(), JOB_LIMIT);
        mTextureUploader = new TextureUploader(activity.getGLRoot());
        mContentStart = 0;
        mContentEnd = TimerDataLoader.MIN_LOAD_COUNT;
        mActiveStart = 0;
        mActiveEnd = TimerDataLoader.MIN_LOAD_COUNT;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }    
    

    @Override
    public void onContentChanged(int index) {
		if(DEBUG) Log.w(TAG,"onContentChanged index = " + index);
        if (index == -1) {
            mListener.onContentChanged();
            return;
        }
        if (index >= mContentStart && index < mContentEnd && mIsActive) {
            freeSlotContent(index);
            prepareSlotContent(index);
            updateAllImageRequests();
            if (mListener != null && isActiveSlot(index)) {
                mListener.onContentChanged();
            }
        }
    }

    @Override
    public void onSizeChanged(int size, Object params) {
		if(DEBUG) Log.w(TAG,"onSizeChanged size = " + size);
        if (mSize != size) {
            mSize = size;
            if (mListener != null)
                mListener.onSizeChanged(mSize, params);
            if (mContentEnd > mSize)
                mContentEnd = mSize;
            if (mActiveEnd > mSize)
                mActiveEnd = mSize;
        } else if (size == 0) {
            if (mContentEnd > mSize)
                mContentEnd = mSize;
            if (mActiveEnd > mSize)
                mActiveEnd = mSize;
        }
    }


    @Override
    public void freeze() {
        // TODO Auto-generated method stub
        mActivity.getGLRoot().freeze();
    }

    @Override
    public void unfreeze() {
        // TODO Auto-generated method stub
        mActivity.getGLRoot().unfreeze();
    }

    public class MyHandlerThread extends HandlerThread {

		public MyHandlerThread(String name) {
			super(name);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onLooperPrepared() {
			// TODO Auto-generated method stub
			super.onLooperPrepared();
            mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
                @Override
                public void handleMessage(Message message) {
                    Utils.assertTrue(message.what == MSG_UPDATE_ENTRY);
                    ((ThumbnailLoader) message.obj).updateEntry();
                }
            };
            
            mDelayHandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    loadContentThumbNail();
                }
            };
            mDelayHandler.sendMessageDelayed(mDelayHandler.obtainMessage(), 500);
		}

	}

    public void loadContentThumbNail() {
        if (!mIsLoadContentThumbNail) {
            mIsLoadContentThumbNail = true;
            updateAllImageRequests();
            Log.d(TAG, "setContentThumbNailLoadStatus, mIsLoadContentThumbNail:" + mIsLoadContentThumbNail);
        }
    }

    public void setActiveWindow(int start, int end, boolean isPack) {
        if (!isPack) {
            setActiveWindow(start, end);
            return;
        }

        if (!(start <= end && end - start <= mData.length && end <= mSize)) {
            Utils.fail("%s, %s, %s, %s", start, end, mData.length, mSize);
        }

        mActiveStart = start;
        mActiveEnd = end;
        setContentWindow(start, end);
        updateTextureUploadQueue();
        if (mIsActive)
            updateAllImageRequests();
    }
    
    public void setActiveWindow(int start, int end) {
        if (!(start <= end && end - start <= mData.length && end <= mSize)) {
            Utils.fail("%s, %s, %s, %s", start, end, mData.length, mSize);
        }
        AlbumEntry data[] = mData;

        mActiveStart = start;
        mActiveEnd = end;

        if (!mNonVisibleActive) {
            setContentWindow(mActiveStart, mActiveEnd);
        } else {
            int contentStart = Utils.clamp((start + end) / 2 - data.length / 2, 0,
                    Math.max(0, mSize - data.length));
            int contentEnd = Math.min(contentStart + data.length, mSize);
            setContentWindow(contentStart, contentEnd);
        }

        updateTextureUploadQueue();
        if (mIsActive)
            updateAllImageRequests();
    }

    public void setNonVisibleActive(boolean active, boolean isPack) {
        if (mNonVisibleActive != active) {
            mNonVisibleActive = active;
            if (!mIsActive || mSize <= 0) {
                return;
            }
            if (!mNonVisibleActive) {
                setActiveWindow(mActiveStart, mActiveEnd, true);
            } else {
                setActiveWindow(mActiveStart, mActiveEnd, isPack);
            }
        }
    }

    
    private void updateAllImageRequests() {
      mActiveRequestCount = 0;
      for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
          if (requestSlotImage(i))
              ++mActiveRequestCount;
      }
      if (mActiveRequestCount == 0) {
          if (!mInFallbackAnimClose) {
              Log.i(TAG, "updateAllImageRequests return");
              return;
          }
          requestNonactiveImages();
      } else {
          cancelNonactiveImages();
      }
  }

    private void cancelNonactiveImages() {
        int range = Math.max((mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            cancelSlotImage(mActiveEnd + i);
            cancelSlotImage(mActiveStart - 1 - i);
        }
    }

    private void cancelSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd)
            return;
        AlbumEntry item = mData[slotIndex % mData.length];
        if(item == null)
            return ;
        if (item.contentLoader != null)
            item.contentLoader.cancelLoad();
    }

	// We would like to request non active slots in the following order:
    // Order: 8 6 4 2 1 3 5 7
    // |---------|---------------|---------|
    // |<- active ->|
    // |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        if (!mIsLoadContentThumbNail) {
            if(DEBUG) Log.w(TAG, "requestNonactiveImages, reject load thumbnail!");
            return;
        }
        int range = Math.max((mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            requestSlotImage(mActiveEnd + i);
            requestSlotImage(mActiveStart - 1 - i);
        }
    }

	private boolean requestSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd)
            return false;
        AlbumEntry entry = mData[slotIndex % mData.length];
        if (entry == null || entry.content != null || entry.item == null) {
            return false;
        }

        entry.contentLoader.startLoad();
        return entry.contentLoader.isRequestInProgress();
    }

	private void updateTextureUploadQueue() {
        if (!mIsActive)
            return;
        mTextureUploader.clear();

        // add foreground textures
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumEntry entry = mData[i % mData.length];
            if (entry != null && entry.bitmapTexture != null) {
                mTextureUploader.addFgTexture(entry.bitmapTexture);
            }
        }

        // add background textures
        int range = Math.max((mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            uploadBgTextureInSlot(mActiveEnd + i);
            uploadBgTextureInSlot(mActiveStart - i - 1);
        }
    }
    
    private void uploadBgTextureInSlot(int index) {
        if (index < mContentEnd && index >= mContentStart) {
            AlbumEntry entry = mData[index % mData.length];
            if (entry != null && entry.bitmapTexture != null) {
                mTextureUploader.addBgTexture(entry.bitmapTexture);
            }
        }
    }

	private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd)
            return;

        if (!mIsActive && mNeedRefresh) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
            mSource.setActiveWindow(contentStart, contentEnd);
            return;
        }
        int oldStart = mContentStart;
        int oldEnd = mContentEnd;
        mContentStart = contentStart;
        mContentEnd = contentEnd;
        if (contentStart >= oldEnd || oldStart >= contentEnd) {
            for (int i = oldStart, n = oldEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        } else {
            for (int i = oldStart; i < contentStart; ++i) {
                freeSlotContent(i);
            }
            for (int i = contentEnd, n = oldEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart, n = oldStart; i < n; ++i) {
                prepareSlotContent(i);
            }
            for (int i = oldEnd; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        }
//        mContentStart = contentStart;
//        mContentEnd = contentEnd;
    }

    private void freeSlotContent(int slotIndex) {
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if(entry == null) return;
        synchronized (entry.textureLock) {

            entry.content = null;
            if (entry.contentLoader != null)
                entry.contentLoader.recycle();
            if (entry.bitmapTexture != null) {
                entry.bitmapTexture.recycle();
                entry.bitmapTexture = null;
            }
        }
        
        data[index] = null;
    }

    private void prepareSlotContent(int slotIndex) {
        AlbumEntry entry = new AlbumEntry();
        MediaItem item = mSource.get(slotIndex); // item could be null;
        entry.item = item;
        entry.isPanorama = GalleryUtils.isPanorama(entry.item);
        entry.mediaType = (item == null) ? MediaItem.MEDIA_TYPE_UNKNOWN : entry.item.getMediaType();
        entry.path = (item == null) ? null : item.getPath();
        entry.rotation = (item == null) ? 0 : item.getRotation();
        entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
        mData[slotIndex % mData.length] = entry;
    }
    
    private class ThumbnailLoader extends BitmapLoader {
        private final int mSlotIndex;
        private final MediaItem mItem;

        public ThumbnailLoader(int slotIndex, MediaItem item) {
            mSlotIndex = slotIndex;
            mItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return mThreadPool.submit(mItem.requestImage(MediaItem.TYPE_MICROTHUMBNAIL), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            if (mHandler != null) {
                mHandler.obtainMessage(MSG_UPDATE_ENTRY, this).sendToTarget();
            }
        }

        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap == null)
                return; // error or recycled

            AlbumEntry entry = mData[mSlotIndex % mData.length];
            if (entry == null) {
                bitmap.recycle();
                return;
            }
            synchronized (entry.textureLock) {

                entry.bitmapTexture = new BitmapTexture(bitmap);
                entry.content = entry.bitmapTexture;
            }

            if (isActiveSlot(mSlotIndex)) {
                mTextureUploader.addFgTexture(entry.bitmapTexture);
                --mActiveRequestCount;
                if (mActiveRequestCount == 0)
                    requestNonactiveImages();
                if (mListener != null)
                    mListener.onContentChanged();
            } else {
                mTextureUploader.addBgTexture(entry.bitmapTexture);
            }
        }
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= mActiveStart && slotIndex < mActiveEnd;
    }

    public AlbumEntry get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
//            Utils.fail("invalid slot: %s outsides (%s, %s)", slotIndex, mActiveStart, mActiveEnd);
            return null;
        }
        return mData[slotIndex % mData.length];
    }

	public void stopFallbackAnim() {
		// TODO Auto-generated method stub
        mInFallbackAnimClose = true;
        if(DEBUG) Log.d(TAG, "stopFallbackAnim");
        updateAllImageRequests();
	}

    public void resume() {
        if (mIsActive) {
            return;
        }
        mIsActive = true;
        if (mNeedRefresh || !mSource.isActive(mHistoryActiveStart)
                || !mSource.isActive(mHistoryActiveEnd - 1)) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                prepareSlotContent(i);
            }
        } else {
            for (int i = mContentStart; i < mHistoryActiveStart; ++i) {
                prepareSlotContent(i);
            }
            for (int i = mHistoryActiveEnd, n = mContentEnd; i < n; ++i) {
                prepareSlotContent(i);
            }
        }
        updateAllImageRequests();
    }

	public void pause() {
		// TODO Auto-generated method stub
        if (!mIsActive) {
            return;
        }
        mIsActive = false;
        mHistoryActiveStart = mActiveStart;
        mHistoryActiveEnd = mActiveEnd;
        mTextureUploader.clear();
        for (int i = mContentStart; i < mContentEnd; ++i) {
            freeSlotContent(i);
        }
	}

    public void destroy() {
        mNeedRefresh = true;
        mTextureUploader.clear();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            freeSlotContent(i);
        }
    }

}
