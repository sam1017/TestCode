package com.android.gallery3d.data;

import java.io.FileDescriptor;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.android.gallery3d.common.Utils;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月17日 --- 下午2:01:53
 *
 */
public class BitmapPool {
    private static final String TAG = "BitmapPool";

    private final ArrayList<Bitmap> mPool;
    private final int mPoolLimit;

    // mOneSize is true if the pool can only cache Bitmap with one size.
    private final boolean mOneSize;
    private final int mWidth, mHeight; // only used if mOneSize is true

    // Construct a BitmapPool which caches bitmap with the specified size.
    public BitmapPool(int width, int height, int poolLimit) {
        mWidth = width;
        mHeight = height;
        mPoolLimit = poolLimit;
        mPool = new ArrayList<Bitmap>(poolLimit);
        mOneSize = true;
    }

    // Construct a BitmapPool which caches bitmap with any size;
    public BitmapPool(int poolLimit) {
        mWidth = -1;
        mHeight = -1;
        mPoolLimit = poolLimit;
        mPool = new ArrayList<Bitmap>(poolLimit);
        mOneSize = false;
    }

    // Get a Bitmap from the pool.
    public synchronized Bitmap getBitmap() {
        Utils.assertTrue(mOneSize);
        int size = mPool.size();
        return size > 0 ? mPool.remove(size - 1) : null;
    }

    // Get a Bitmap from the pool with the specified size.
    public synchronized Bitmap getBitmap(int width, int height) {
        Utils.assertTrue(!mOneSize);
        for (int i = mPool.size() - 1; i >= 0; i--) {
            Bitmap b = mPool.get(i);
            if (b.getWidth() == width && b.getHeight() == height) {
                return mPool.remove(i);
            }
        }
        return null;
    }

    // Put a Bitmap into the pool, if the Bitmap has a proper size. Otherwise
    // the Bitmap will be recycled. If the pool is full, an old Bitmap will be
    // recycled.
    public void recycle(Bitmap bitmap) {
        if (bitmap == null)
            return;
        if (mOneSize && ((bitmap.getWidth() != mWidth) || (bitmap.getHeight() != mHeight))) {
            bitmap.recycle();
            return;
        }
        synchronized (this) {
            if (mPool.size() >= mPoolLimit) {
                Bitmap bitmap2 = mPool.remove(0);
                bitmap2.recycle();
                bitmap2 = null;
            }
            mPool.add(bitmap);
        }
    }

    public synchronized ArrayList<Bitmap> getBitmapListRef() {
        ArrayList<Bitmap> refList = new ArrayList<Bitmap>();
        for (Bitmap bitmap : mPool) {
            refList.add(bitmap);
        }
        return refList;
    }

    public synchronized void clearReference() {
        mPool.clear();
    }

    public synchronized void clear() {
        for (Bitmap bitmap : mPool) {
            bitmap.recycle();
            bitmap = null;
        }
        mPool.clear();
    }

    private Bitmap findCachedBitmap(JobContext jc, byte[] data, int offset, int length,
            Options options) {
        if (mOneSize)
            return getBitmap();
        DecodeUtils.decodeBounds(jc, data, offset, length, options);
        return getBitmap(options.outWidth, options.outHeight);
    }

    private Bitmap findCachedBitmap(JobContext jc, FileDescriptor fileDescriptor, Options options) {
        if (mOneSize)
            return getBitmap();
        DecodeUtils.decodeBounds(jc, fileDescriptor, options);
        return getBitmap(options.outWidth, options.outHeight);
    }

    public Bitmap decode(JobContext jc, byte[] data, int offset, int length,
            BitmapFactory.Options options) {
        if (options == null)
            options = new BitmapFactory.Options();
        if (options.inSampleSize < 1)
            options.inSampleSize = 1;
        options.inPreferredConfig = Config.ARGB_8888;
        options.inBitmap = (options.inSampleSize == 1) ? findCachedBitmap(jc, data, offset, length,
                options) : null;
        try {
            Bitmap bitmap = DecodeUtils.decode(jc, data, offset, length, options);
            if (options.inBitmap != null && options.inBitmap != bitmap) {
                recycle(options.inBitmap);
                options.inBitmap = null;
            }
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null)
                throw e;

            Log.w(TAG, "decode fail with a given bitmap, try decode to a new bitmap");
            recycle(options.inBitmap);
            options.inBitmap = null;
            return DecodeUtils.decode(jc, data, offset, length, options);
        }
    }

    // This is the same as the method above except the source data comes
    // from a file descriptor instead of a byte array.
    public Bitmap decode(JobContext jc, FileDescriptor fileDescriptor, Options options) {
        if (options == null)
            options = new BitmapFactory.Options();
        if (options.inSampleSize < 1)
            options.inSampleSize = 1;
        options.inPreferredConfig = Config.ARGB_8888;
        options.inBitmap = (options.inSampleSize == 1) ? findCachedBitmap(jc, fileDescriptor,
                options) : null;
        try {
            Bitmap bitmap = DecodeUtils.decode(jc, fileDescriptor, options);
            if (options.inBitmap != null && options.inBitmap != bitmap) {
                recycle(options.inBitmap);
                options.inBitmap = null;
            }
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null)
                throw e;

            Log.w(TAG, "decode fail with a given bitmap, try decode to a new bitmap");
            recycle(options.inBitmap);
            options.inBitmap = null;
            return DecodeUtils.decode(jc, fileDescriptor, options);
        }
    }
}
