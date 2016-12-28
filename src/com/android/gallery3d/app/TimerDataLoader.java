package com.android.gallery3d.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MixAlbum;
import com.android.gallery3d.data.MixAlbum.Range;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.common.Utils;

public class TimerDataLoader {

	public static interface DataListener {
        public void onContentChanged(int index);

        public void onSizeChanged(int size, Object params);
        public void freeze();
        public void unfreeze();
    }

	private static final String TAG = "TimerDataLoader";
    private static final int DATA_CACHE_SIZE = 1000;

    private static final int MSG_LOAD_START = 1;
    private static final int MSG_LOAD_FINISH = 2;
    private static final int MSG_RUN_OBJECT = 3;
    private static final int MSG_UNFREEZE = 4;
    private static final int FREEZE_TIME = 600;

    public static final int MIN_LOAD_COUNT = 32;//8;32
    private static final int MAX_LOAD_COUNT = 64;//16;64
    
    private final MediaSet mSource;
    private final MediaItem[] mData;
    private final long[] mItemVersion;
    private final long[] mSetVersion;

    private DataListener mDataListener;
    private LoadingListener mLoadingListener;
    
    private final Handler mMainHandler;
    private boolean mNeedUnfreeze = false;
    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private int mContentStart = 0;
    private int mContentEnd = 0;
    private int mSize = 0;
    private boolean mFirstAfterResume = false;
    private ReloadTask mReloadTask;
    protected ArrayList<Range> mDaysInfo;
    private MySourceListener mSourceListener = new MySourceListener();
    private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

	public TimerDataLoader(AbstractGalleryActivity context, MediaSet mediaSet) {
		// TODO Auto-generated constructor stub
        mSource = mediaSet;

        mData = new MediaItem[DATA_CACHE_SIZE];
        mItemVersion = new long[DATA_CACHE_SIZE];
        mSetVersion = new long[DATA_CACHE_SIZE];
        Arrays.fill(mItemVersion, MediaObject.INVALID_DATA_VERSION);
        Arrays.fill(mSetVersion, MediaObject.INVALID_DATA_VERSION);

        mMainHandler = new SynchronizedHandler(context.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                case MSG_UNFREEZE:
                    unfreeze();
                    break;
                case MSG_RUN_OBJECT:
                    ((Runnable) message.obj).run();
                    return;
                case MSG_LOAD_START:
                    if (mLoadingListener != null)
                        mLoadingListener.onLoadingStarted();
                    return;
                case MSG_LOAD_FINISH:
                    if (mLoadingListener != null)
                        mLoadingListener.onLoadingFinished(false);
                    return;
                }
            }
        };
        
        mContentStart = 0;
        mContentEnd = MIN_LOAD_COUNT;
        mActiveStart = 0;
        mActiveEnd = MIN_LOAD_COUNT;
	}

	protected void unfreeze() {
		// TODO Auto-generated method stub
        if (mNeedUnfreeze) {
            mNeedUnfreeze = false;
            if (mDataListener != null) {
            	Log.w(TAG,"unfreeze");
                mDataListener.unfreeze();
            }
        }
	}

    public int size() {
        return mSize;
    }

    public void setLoadingListener(LoadingListener listener) {
        mLoadingListener = listener;
    }

    public void setDataListener(DataListener listener) {
        mDataListener = listener;
    }

    // Returns the index of the MediaItem with the given path or
    // -1 if the path is not cached
    public int findItem(Path id) {
        for (int i = mContentStart; i < mContentEnd; i++) {
            MediaItem item = mData[i % DATA_CACHE_SIZE];
            if (item != null && id == item.getPath()) {
                return i;
            }
        }
        return -1;
    }

    public MediaItem get(int index) {
        if (!isActive(index)) {
            throw new IllegalArgumentException(String.format("%s not in (%s, %s)", index,
                    mActiveStart, mActiveEnd));
        }
        return mData[index % mData.length];
    }

    public boolean isActive(int index) {
        return index >= mActiveStart && index < mActiveEnd;
    }

    public void resume() {
        mFirstAfterResume = true;
        if(mReloadTask != null) {
            return;
        }
        mSource.addContentListener(mSourceListener);
        mReloadTask = new ReloadTask();
        mReloadTask.start();
    }

    private class MySourceListener implements ContentListener {
        public void onContentDirty() {
            if (mIsSourceSensive && mReloadTask != null)
                mReloadTask.notifyDirty();
        }
    }
    
    private static class UpdateInfo {
        public long version;
        public int reloadStart;
        public int reloadCount;

        public int size;
        public ArrayList<MediaItem> items;
    }
    
    private class GetUpdateInfo implements Callable<UpdateInfo> {
        private final long mVersion;

        public GetUpdateInfo(long version) {
            mVersion = version;
        }

        public UpdateInfo call() throws Exception {
            UpdateInfo info = new UpdateInfo();
            long version = mVersion;
            info.version = mSourceVersion;
            info.size = mSize;
            long setVersion[] = mSetVersion;
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                int index = i % DATA_CACHE_SIZE;
                if (setVersion[index] != version) {
                    info.reloadStart = i;
                    info.reloadCount = Math.min(MAX_LOAD_COUNT, n - i);
                    return info;
                }
            }
            return mSourceVersion == mVersion ? null : info;
        }
    }

    private class UpdateContent implements Callable<Void> {

        private UpdateInfo mUpdateInfo;

        public UpdateContent(UpdateInfo info) {
            mUpdateInfo = info;
        }

        @Override
        public Void call() throws Exception {
            boolean sizeChanged = false;
            boolean hasDeleted = false;
            try {
                UpdateInfo info = mUpdateInfo;
                mSourceVersion = info.version;
                if (mSize != info.size) {
                    hasDeleted = mSize > info.size ? true : false;
                    mSize = info.size;
                    mNeedUnfreeze = true;
                    sizeChanged = true;
                    if (mDataListener != null) {
                        if(!mFirstAfterResume && hasDeleted){
                            mDataListener.freeze();
                            mMainHandler.sendEmptyMessageDelayed(MSG_UNFREEZE, FREEZE_TIME);
                        }
                        mFirstAfterResume = false;
                        mDataListener.onSizeChanged(mSize, mDaysInfo);
                    }
                    if (mContentEnd > mSize)
                        mContentEnd = mSize;
                    if (mActiveEnd > mSize)
                        mActiveEnd = mSize;
                } else if (info.size == 0) {
                    mDataListener.onSizeChanged(info.size, mDaysInfo);
                    if (mContentEnd > mSize)
                        mContentEnd = mSize;
                    if (mActiveEnd > mSize)
                        mActiveEnd = mSize;
                }

                ArrayList<MediaItem> items = info.items;

                if (items == null) {
                    unfreeze();
                    return null;
                }
                int start = Math.max(info.reloadStart, mContentStart);
                int end = Math.min(info.reloadStart + items.size(), mContentEnd);

                for (int i = start; i < end; ++i) {
                    int index = i % DATA_CACHE_SIZE;
                    mSetVersion[index] = info.version;
                    MediaItem updateItem = items.get(i - info.reloadStart);
                    long itemVersion = updateItem.getDataVersion();
                    if (mItemVersion[index] != itemVersion) {
                        mItemVersion[index] = itemVersion;
                        mData[index] = updateItem;
                        if (mDataListener != null && i >= mActiveStart && i < mActiveEnd) {
                            mDataListener.onContentChanged(i);
                            sizeChanged = false;
                        }
                    }
                }
                if (sizeChanged)
                    mDataListener.onContentChanged(-1);

                return null;

            } catch (Exception e) {
                // TODO: handle exception
            }
            return null;
        }
    }

    private class ReloadTask extends Thread {

        private volatile boolean mActive = true;
        private volatile boolean mDirty = true;
        private boolean mIsLoading = false;

        private void updateLoading(boolean loading) {
            if (mIsLoading == loading)
                return;
            mIsLoading = loading;
            mMainHandler.sendEmptyMessage(loading ? MSG_LOAD_START : MSG_LOAD_FINISH);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            boolean updateComplete = false;
            while (mActive) {
                synchronized (this) {
                    if (mActive && !mDirty && updateComplete) {
                        updateLoading(false);
                        Utils.waitWithoutInterrupt(this);
                        continue;
                    }
                }
                mDirty = false;
                updateLoading(true);
                long version;
                synchronized (DataManager.LOCK) {
                    version = mSource.reload();
                    if (mSource instanceof MixAlbum) {
                        mDaysInfo = ((MixAlbum) mSource).getDaysInfo();
                    }
                }
                UpdateInfo info = null;
                try {
                    info = new GetUpdateInfo(version).call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                info = executeAndWait(new GetUpdateInfo(version));
                updateComplete = info == null;
                if (updateComplete) {
                    unfreeze();
                    continue;
                }
                synchronized (DataManager.LOCK) {
                    if (info.version != version) {
                        info.size = mSource.getMediaItemCount();
                        info.version = version;
                    }
                    if (info.reloadCount > 0) {
                        info.items = mSource.getMediaItem(info.reloadStart, info.reloadCount);
                    }
                }
                try {
                    synchronized (this) {
                        if (mActive) {
                            new UpdateContent(info).call();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                executeAndWait(new UpdateContent(info));
            }
            updateLoading(false);
        }

        public synchronized void notifyDirty() {
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }
    }

    public void setActiveWindow(int start, int end) {
        if (start == mActiveStart && end == mActiveEnd)
            return;

        Utils.assertTrue(start <= end && end - start <= mData.length && end <= mSize);

        int length = mData.length;
        mActiveStart = start;
        mActiveEnd = end;

        // If no data is visible, keep the cache content
        if (start == end)
            return;

        int contentStart = Utils.clamp((start + end) / 2 - length / 2, 0,
                Math.max(0, mSize - length));

        int contentEnd = Math.min(end + MIN_LOAD_COUNT, mSize);
        if (contentEnd > mContentEnd) {
            contentEnd = Math.min(Math.max(contentEnd, mContentEnd + MAX_LOAD_COUNT), mSize);
        }
        if (mContentStart > start || mContentEnd < end || mContentStart > contentStart
                || mContentEnd < contentEnd
                || Math.abs(contentStart - mContentStart) > MIN_LOAD_COUNT) {
            setContentWindow(contentStart, contentEnd);
        }
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd)
            return;
        int end = mContentEnd;
        int start = mContentStart;

        // We need change the content window before calling reloadData(...)
        synchronized (this) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
        }

        if (contentStart >= end || start >= contentEnd) {
            for (int i = start, n = end; i < n; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
        } else {
            for (int i = start; i < contentStart; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
            for (int i = contentEnd, n = end; i < n; ++i) {
                clearSlot(i % DATA_CACHE_SIZE);
            }
        }
        if (mReloadTask != null)
            mReloadTask.notifyDirty();
    }

    private void clearSlot(int slotIndex) {
        mData[slotIndex] = null;
        mItemVersion[slotIndex] = MediaObject.INVALID_DATA_VERSION;
        mSetVersion[slotIndex] = MediaObject.INVALID_DATA_VERSION;
    }

	public void pause() {
        unfreeze();
        if(mReloadTask == null) {
            return;
        }
        mReloadTask.terminate();
        mReloadTask = null;
        mSource.removeContentListener(mSourceListener);
	}

    public int getActiveStart() {
        return mActiveStart;
    }
    
    private <T> T executeAndWait(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<T>(callable);
        mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_RUN_OBJECT, task));
        try {
            return task.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            Log.e(TAG, "executeAndWait, e : ", e);
            return null;
        }
    }

    /// M: [PERF.ADD] add for delete many files performance improve @{
    private volatile boolean mIsSourceSensive = true;

    /**
     * Set if data loader is sensitive to change of data.
     *
     * @param isProviderSensive
     *            If data loader is sensitive to change of data
     */
    public void setSourceSensive(boolean isSourceSensive) {
        mIsSourceSensive = isSourceSensive;
    }

    /**
     * Notify MySourceListener that the content is dirty and trigger some
     * operations that only occur when content really changed.
     */
    public void fakeSourceChange() {
        mSourceListener.onContentDirty();
    }
    /// @}
}
