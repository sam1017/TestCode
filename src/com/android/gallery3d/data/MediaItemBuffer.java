package com.android.gallery3d.data;

import java.util.ArrayList;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月18日 --- 下午2:35:58
 *
 */
public class MediaItemBuffer {
    @SuppressWarnings("unused")
    private static final String TAG = "MediaItemBuffer";
    private static final int MAX_BUFFER_SIZE = 1000;
    private long mDataVersion;
    private int mStart;
    private int mCount;
    private int mBufferSize;
    private ArrayList<MediaItem> mMediaItems;

    public MediaItemBuffer() {
        mBufferSize = MAX_BUFFER_SIZE;
    }

    public MediaItemBuffer(int bufferSize) {
        mBufferSize = bufferSize;
    }

    public boolean isBufferValid(long dataVersion, int start, int count) {
        // Debugger.d(TAG, "isBufferValid: current[" + mDataVersion + "," +
        // mStart + "," + mCount
        // + "] to [" + dataVersion + "," + start + "," + count + "]");
        if (dataVersion == mDataVersion && start >= mStart && (start + count) <= (mStart + mCount)) {
            return true;
        }

        return false;
    }

    public boolean isSupport(int count) {
        if (count <= mBufferSize) {
            return true;
        } else {
            return false;
        }
    }

    public int[] getParameters(int start, int count) {
        int[] parameters = new int[2];
        parameters[0] = Math.max(0, start - mBufferSize / 2);
        parameters[1] = parameters[0] + Math.max(count, mBufferSize);
        return parameters;
    }

    @SuppressWarnings("unchecked")
    public void saveMediaItem(long dataVersion, int start, ArrayList<MediaItem> items) {
        if (items == null) {
            mMediaItems = null;
            return;
        }
        // Debugger.d(TAG, "saveMediaItem: in " + dataVersion +","+ start +","+
        // items.size()
        // + this);
        mMediaItems = copy(items, 0, Math.min(items.size(), mBufferSize));
        mDataVersion = dataVersion;
        mStart = start;
        mCount = mMediaItems.size();
        // Debugger.d(TAG, "saveMediaItem: out " + dataVersion +","+ start +","+
        // items.size()
        // + this);
    }

    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        // Debugger.d(TAG, "getMediaItem: start,count" + start + "," + count +
        // this);
        int startIndex = start - mStart;
        int endIndex = startIndex + Math.min(count, mCount - startIndex);
        return copy(mMediaItems, startIndex, endIndex);
    }

    private ArrayList<MediaItem> copy(ArrayList<MediaItem> source, int start, int end) {
        ArrayList<MediaItem> result = new ArrayList<MediaItem>();
        for (int i = start; i < end; i++) {
            result.add(source.get(i));
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format(" Buffer[DataVersion,Start,Count]=[%s,%s,%s] ", mDataVersion, mStart,
                mCount);
    }
}

