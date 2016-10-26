package com.android.gallery3d.ui;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.FloatMath;
import android.graphics.PorterDuff;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.BitmapPool;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.ui.TimerSlotView.Spec;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.R;

public class TimerTitleLabelMaker {


    private static final long ONE_DAY = 24 * 60 * 60 * 1000;
    private static final long SIX_DAY = 6 * 24 * 60 * 60 * 1000;
    private static final int POOL_LIMIT = 12;
    private static final int BORDER_SIZE = 1;
	private static final boolean DEBUG = true;
	private static final String TAG = "TimerTitleLabelMaker";
	private AbstractGalleryActivity mActivity;
    private final ThreadPool mThreadPool;
	private Spec mSpec;
	private HashMap<Long, Label> mLabels;
	private TextPaint mTitlePaint;
	private TextPaint mSubTitlePaint;
	private int mWidth;
	private int mHeight;
	private int mSubTitleY;
	private BitmapPool mBitmapPool;

	public TimerTitleLabelMaker(AbstractGalleryActivity activity, Spec spec) {
		// TODO Auto-generated constructor stub
		if(DEBUG) Log.w(TAG,"TimerTitleLabelMaker");
        mActivity = activity;
        mThreadPool = activity.getThreadPool();
        mSpec = spec;
        mLabels = new HashMap<Long, Label>();
	}

    private class Label {
        public long timestamp;
        public long time;
        public BitmapTexture texture;
        public int contentWidth;
        public LabelLoader labelLoader;

        public void recycle() {
            if (labelLoader != null) {
                labelLoader.cancelLoad();
                labelLoader.recycle();
                labelLoader = null;
            }

            if (texture != null) {
                texture.recycle();
                texture = null;
            }
        }
    }
    
    private class LabelLoader extends BitmapLoader {
        private final long mTime;
        private long mTimestamp;
        private LabelJob mJob;

        public LabelLoader(long time, long timestamp) {
            mTime = time;
            mTimestamp = timestamp;
        }

        public void updateTimestamp(long timestamp) {
            mTimestamp = timestamp;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> listener) {
            mJob = new LabelJob(mTime, mTimestamp);
            return mThreadPool.submit(mJob, listener);
        }

        protected void recycleBitmap(Bitmap bitmap) {
            mBitmapPool.recycle(bitmap);
        }

        @Override
        protected synchronized void onLoadComplete(Bitmap bitmap) {
            if (bitmap == null || bitmap.isRecycled() || isRecycled()) {
                return;
            }

            Label label = mLabels.get(mTime);
            if (label != null) {
                BitmapTexture texture = new BitmapTexture(bitmap);
                texture.setOpaque(false);
                label.texture = texture;
                label.contentWidth = mJob.contentWidth;
                mJob = null;
            } else {
                mBitmapPool.recycle(bitmap);
            }
        }
    }
    
    private class LabelJob implements ThreadPool.Job<Bitmap> {
        private final long mTime;
        private final long mTimestamp;
        private String mTitle;
        private String mSubTitle;
        private int contentWidth;

        public LabelJob(long time, long timestamp) {
            mTimestamp = timestamp;
            mTime = time;
        }

        private String transferre() {
            if (mTimestamp == 0) {
                return mActivity.getResources().getString(R.string.today);
            } else if (mTimestamp == ONE_DAY) {
                return mActivity.getResources().getString(R.string.yesterday);
            }

            Calendar date = Calendar.getInstance();
            int todayIndex = date.get(Calendar.DAY_OF_WEEK);
            date.setTimeInMillis(mTime);
            int currentIndex = date.get(Calendar.DAY_OF_WEEK);
            if ((currentIndex > todayIndex && todayIndex != Calendar.SUNDAY)
                    || currentIndex == Calendar.SUNDAY) {
                return mActivity.getResources().getStringArray(R.array.last_week)[currentIndex - 1];
            }
            return mActivity.getResources().getStringArray(R.array.current_week)[currentIndex - 1];
        }

        private void translate() {
            if (mTimestamp >= 0 && mTimestamp <= SIX_DAY) {
                mTitle = transferre();
                mSubTitle = null;
            } else {
                Calendar date = Calendar.getInstance();
                date.setTimeInMillis(mTime);
                mTitle = "";
                mSubTitle = (date.get(Calendar.YEAR)) + "." + (date.get(Calendar.MONTH) + 1) + "." + date.get(Calendar.DAY_OF_MONTH);
            }
        }

        @Override
        public Bitmap run(JobContext jc) {
            Bitmap bitmap;
            contentWidth = 0;
            synchronized (this) {
                if (mBitmapPool == null) {
                    initPool();
                }
                bitmap = mBitmapPool.getBitmap();
            }

            if (bitmap == null) {
                int borders = 2 * BORDER_SIZE;
                bitmap = Bitmap.createBitmap(mWidth + borders, mHeight + borders, Config.ARGB_8888);
            }

            translate();
            Canvas canvas = new Canvas(bitmap);
            canvas.clipRect(BORDER_SIZE, BORDER_SIZE, bitmap.getWidth() - BORDER_SIZE,
                    bitmap.getHeight() - BORDER_SIZE);
            canvas.drawColor(0x00000000, PorterDuff.Mode.SRC);
            canvas.translate(BORDER_SIZE, BORDER_SIZE);
            AlbumLabelMaker.drawText(canvas, 0, 0, mTitle, mWidth, mTitlePaint);
            contentWidth = (int) FloatMath.ceil(mTitlePaint.measureText(mTitle));
            if (mSubTitle != null) {
                int x = (int) FloatMath.ceil(mTitlePaint.measureText(mTitle));
                AlbumLabelMaker.drawText(canvas, x, mSubTitleY, mSubTitle, mWidth - x,
                        mSubTitlePaint);
                contentWidth += (int) FloatMath.ceil(mSubTitlePaint.measureText(mSubTitle));
            }

            return bitmap;
        }

    }

    private void initPool() {
        mTitlePaint = new TextPaint();
        mTitlePaint.setTextSize(mSpec.titleSize);
        mTitlePaint.setAntiAlias(true);
        mTitlePaint.setColor(mSpec.titleColor);

        mSubTitlePaint = new TextPaint();
        mSubTitlePaint.setTextSize(mSpec.subTitleSize);
        mSubTitlePaint.setAntiAlias(true);
        mSubTitlePaint.setColor(mSpec.subTitleColor);

        mWidth = maxWidth();
        mHeight = mTitlePaint.getFontMetricsInt().bottom - mTitlePaint.getFontMetricsInt().top;
        mSubTitleY = (mHeight - (mSubTitlePaint.getFontMetricsInt().bottom - mSubTitlePaint
                .getFontMetricsInt().top)) * 2 / 3;
        mBitmapPool = new BitmapPool(mWidth + 2 * BORDER_SIZE, mHeight + 2 * BORDER_SIZE,
                POOL_LIMIT);
    }

    private int maxWidth() {
        float width = 0f;
        TextPaint paint = mSpec.titleSize > mSpec.subTitleSize ? mTitlePaint : mSubTitlePaint;
        Resources rs = mActivity.getResources();
        width = Math.max(width, getStringWidth(paint, "12.12.2013"));
        width = Math.max(width, getStringWidth(paint, rs.getString(R.string.today)));
        width = Math.max(width, getStringWidth(paint, rs.getString(R.string.yesterday)));
        String[] strings = rs.getStringArray(R.array.last_week);
        for (String string : strings) {
            width = Math.max(width, getStringWidth(paint, string));
        }
        strings = rs.getStringArray(R.array.current_week);
        for (String string : strings) {
            width = Math.max(width, getStringWidth(paint, string));
        }
        return (int) width;
    }

    private static float getStringWidth(TextPaint paint, String string) {
        return FloatMath.ceil(paint.measureText(string));
    }

    public synchronized BitmapTexture getTexture(long time) {
        Label label;
        if (mLabels.containsKey(time)) {
            label = mLabels.get(time);
            return getTexture(label);
        } else {
            label = new Label();
            label.timestamp = GalleryUtils.cleatHour(System.currentTimeMillis()) - time;
            label.time = time;
            label.labelLoader = new LabelLoader(time, label.timestamp);

            mLabels.put(time, label);
            return createTexture(label);
        }
    }

    private BitmapTexture createTexture(Label label) {
        label.labelLoader.startLoad();
        return label.texture;
    }

	private BitmapTexture getTexture(Label label) {
        long time = label.time;
        long timestamp = GalleryUtils.cleatHour(System.currentTimeMillis()) - time;

        if (timestamp == label.timestamp && label.texture != null) {
            return label.texture;
        }

        if (label.labelLoader.isRequestInProgress()) {
            return label.texture;
        }

        label.recycle();
        Label newLabel = new Label();
        newLabel.timestamp = timestamp;
        newLabel.time = time;
        newLabel.labelLoader = new LabelLoader(time, newLabel.timestamp);
        newLabel.labelLoader.startLoad();

        mLabels.put(time, newLabel);
        return newLabel.texture;
    }

    
    public synchronized int getContentWidth(long time) {
        Label label;
        if (mLabels.containsKey(time)) {
            label = mLabels.get(time);
            return label.contentWidth;
        }
        return 0;
    }

	public void recycle(long time) {
		// TODO Auto-generated method stub
        if (mLabels.containsKey(time)) {
            Label label = mLabels.remove(time);
            label.recycle();
            label = null;
        }
	}

    public synchronized void recycleAll() {
        if (mLabels != null) {
            Iterator<Entry<Long, Label>> iterator = mLabels.entrySet().iterator();
            Label label;
            while (iterator.hasNext()) {
                label = iterator.next().getValue();
                label.recycle();
                label = null;
            }
            mLabels.clear();
        }

        if (mBitmapPool != null) {
            mBitmapPool.clear();
        }
    }

}
