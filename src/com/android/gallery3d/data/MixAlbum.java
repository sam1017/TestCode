package com.android.gallery3d.data;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.BaseColumns;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.TraceHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MixAlbum.Range;
import com.mediatek.storage.StorageManagerEx;


public class MixAlbum extends MediaSet {

    public static class Range{
        public int mStart;
        public int mEnd;
        public long mTime;
        public Range(int start, int end, long time) {
            mStart = start;
            mEnd = end;
            mTime = time;
        }
        public String toString() {
            return " mStart:" + mStart + " mEnd:" + mEnd + " mTime:" + mTime;
        }
    }

	private static final String TAG = "MixAlbum";

    public static final Path MIXALBUM_PATH_ALL = Path.fromString("/local/mixalbum/all");

    public static final int INDEX_ID = 0;
    public static final int INDEX_CAPTION = 1;
    public static final int INDEX_MIME_TYPE = 2;
    public static final int INDEX_LATITUDE = 3;
    public static final int INDEX_LONGITUDE = 4;
    public static final int INDEX_DATE_TAKEN = 5;
    public static final int INDEX_DATE_ADDED = 6;
    public static final int INDEX_DATE_MODIFIED = 7;
    public static final int INDEX_DATA = 8;
    public static final int INDEX_ORIENTATION = 9;
    public static final int INDEX_BUCKET_ID = 10;
    public static final int INDEX_SIZE = 11;
    public static final int INDEX_WIDTH = 12;
    public static final int INDEX_HEIGHT = 13;
    
    public static final int INDEX_IS_DRM = 14;
    public static final int INDEX_DRM_METHOD = 15;
    public static final int INDEX_GROUP_ID = 16;
    public static final int INDEX_GROUP_INDEX = 17;
    public static final int INDEX_IS_BEST_SHOT = 18;
    public static final int INDEX_GROUP_COUNT = 19;
    public static final int INDEX_CAMERA_REFOCUS = 20;

    public static final int INDEX_VIDEO_DURATION = 21;
    public static final int INDEX_FILE_MEDIA_TYPE = 22;
    public static final int INDEX_RESOLUTION = 23;

    static final String[] PROJECTION = { ImageColumns._ID, // 0
        ImageColumns.TITLE, // 1
        ImageColumns.MIME_TYPE, // 2
        ImageColumns.LATITUDE, // 3
        ImageColumns.LONGITUDE, // 4
        ImageColumns.DATE_TAKEN, // 5
        ImageColumns.DATE_ADDED, // 6
        ImageColumns.DATE_MODIFIED, // 7
        ImageColumns.DATA, // 8
        ImageColumns.ORIENTATION, // 9
        ImageColumns.BUCKET_ID, // 10
        ImageColumns.SIZE, // 11
        // These should be changed to proper names after they are made
        // public.
        "width", // ImageColumns.WIDTH, // 12
        "height", // ImageColumns.HEIGHT // 13
        ImageColumns.IS_DRM,        // 14
        ImageColumns.DRM_METHOD,    // 15
        ImageColumns.GROUP_ID,      // 16
        ImageColumns.GROUP_INDEX,   // 17
        ImageColumns.IS_BEST_SHOT,  // 18
        ImageColumns.GROUP_COUNT,    // 19
        ImageColumns.CAMERA_REFOCUS, // 20
        VideoColumns.DURATION,// 21
        FileColumns.MEDIA_TYPE,// 22
        VideoColumns.RESOLUTION,// 23
};

	private GalleryApp mApplication;
    private final ContentResolver mResolver;
    private String mBucketName;
	private boolean isWithScreenshot = true;
    private final String mWhereClause;
    private final String mOrderClause;
    private final String[] mProjection;

    private final Uri mAlbumUri;
    private final Path mImageItemPath;
    private final Path mVideoItemPath;
    private int mCachedCount = INVALID_COUNT;
    private int mImageCount = INVALID_COUNT;
    private int mVideoCount = INVALID_COUNT;

    private final ChangeNotifier mNotifier;
    private static StorageManager sStorageManager = null;

    private ArrayList<Range> mDaysInfo;
    private static final Uri[] mWatchUris = new Uri[] { Images.Media.EXTERNAL_CONTENT_URI,
        Video.Media.EXTERNAL_CONTENT_URI };

    private static final int INVALID_COUNT = -1;

    private static final String[] COUNT_PROJECTION = { "count(*)" };

    private static final String[] PROJECTION_BUCKET = { ImageColumns.DATE_TAKEN,
        ImageColumns.DATE_MODIFIED };

    private final MediaItemBuffer mMediaItemBuffer = new MediaItemBuffer();

    private static final String BUCKET_ORDER_BY = "datetaken DESC";

    public MixAlbum(GalleryApp application, Path path) {
        super(path, nextVersionNumber());
        mApplication = application;
        mResolver = application.getContentResolver();
        mBucketName = mApplication.getResources().getString(R.string.tab_photos);

        mWhereClause = getWhereClause(mApplication, isWithScreenshot);

        mAlbumUri = Files.getContentUri("external");

        mOrderClause = ImageColumns.DATE_TAKEN + " DESC, " + BaseColumns._ID + " DESC";
        mProjection = PROJECTION;
        mImageItemPath = LocalImage.ITEM_PATH;
        mVideoItemPath = LocalVideo.ITEM_PATH;
        mNotifier = new ChangeNotifier(this, mWatchUris, application);
    }

    public static String getWhereClause(GalleryApp application, boolean withScreenshot) {
        return getWhereClause(application, withScreenshot, false);
    }

    public static String getWhereClause(GalleryApp application, boolean withScreenshot,
            boolean onlyImage) {
        Context context = application.getAndroidContext();
        Resources res = context.getResources();
        ArrayList<String> folders = new ArrayList<String>();
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context
                    .getSystemService(Context.STORAGE_SERVICE);
        }
        String[] strings = res.getStringArray(R.array.camera_folders);
        for (String folder : strings) {
            addFolder(folders, folder);
        }

        String[] otherStrings = res.getStringArray(R.array.other_folders);
        for (String folder : otherStrings) {
            addFolder(folders, folder);
        }

        String[] volumes = sStorageManager.getVolumePaths();
        String external = Environment.getExternalStorageDirectory().getPath();
        String internal = null;
        for (String str : volumes) {
            if (StorageManagerEx.isExternalSDCard(str)) {
                Log.i(TAG, "<getExternalCacheDir> " + str + " isExternalSDCard");
                internal = str;
                break;
            }
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("(");
        buffer.append(ImageColumns.BUCKET_ID + " IN (");
        for (int i = 0, size = folders.size(); i < size; i++) {
            if (internal != null) {
                buffer.append(GalleryUtils.getBucketId(internal + "/" + folders.get(i)));
                buffer.append(",");
            }

            if (external != null) {
                buffer.append(GalleryUtils.getBucketId(external + "/" + folders.get(i)));
                buffer.append(",");
            }
        }

        if (buffer.lastIndexOf(",") == buffer.length() - 1) {
            buffer.deleteCharAt(buffer.length() - 1);
        }

        if (onlyImage) {
            buffer.append(")");
        } else {
            buffer.append(") AND " + FileColumns.MEDIA_TYPE + " IN ("
                    + FileColumns.MEDIA_TYPE_IMAGE + "," + FileColumns.MEDIA_TYPE_VIDEO + ")");
        }

        buffer.append(" AND ((is_drm=0 OR is_drm IS NULL) OR ((is_drm=1) AND ((((drm_method=1) OR (drm_method=2)) OR (drm_method=4)) OR (drm_method=8)))))");
        buffer.append(" AND ( group_id = 0 OR (group_id IS NOT NULL AND title NOT LIKE 'IMG%CS') "
        		+ "OR group_id IS NULL) OR _id in (SELECT min(_id) FROM images WHERE group_id != 0 "
        		+ "AND title LIKE 'IMG%CS' GROUP BY group_id)");
        Log.w(TAG,"mWhereClause = " + buffer.toString());
        return buffer.toString();
    }

    private static void addFolder(ArrayList<String> list, String folder) {
        if (list != null && !list.contains(folder)) {
            list.add(folder);
        }
    }

    public int getImageCount() {
        if (mImageCount == INVALID_COUNT) {
            Cursor cursor = mResolver.query(Images.Media.EXTERNAL_CONTENT_URI, COUNT_PROJECTION,
                    getWhereClause(mApplication, false, true), null, null);
            if (cursor == null) {
                Log.w(TAG, "query fail");
                return 0;
            }
            try {
                Utils.assertTrue(cursor.moveToNext());
                mImageCount = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }

        return mImageCount;

    }

	@Override
    public int getMediaItemCount() {
        if (mCachedCount == INVALID_COUNT) {
            Uri uri = mAlbumUri.buildUpon().appendQueryParameter("invalid", "0").build();
            Cursor cursor = mResolver.query(uri, COUNT_PROJECTION, mWhereClause, null, null);
            if (cursor == null) {
                Log.w(TAG, "query fail");
                return 0;
            }
            try {
                Utils.assertTrue(cursor.moveToNext());
                mCachedCount = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        return mCachedCount;
    }

    public boolean isEmpty() {
        if (mCachedCount == INVALID_COUNT && mImageCount == INVALID_COUNT
                && mVideoCount == INVALID_COUNT) {
            getMediaItemCount();
        	getImageCount();
        }
        Log.w(TAG,"isEmpty mCachedCount = " + mCachedCount + " mImageCount = " + mImageCount);
        return mCachedCount <= 0 && mImageCount <= 0 && mVideoCount <= 0;
    }
    
	@Override
	public int getIndexOfItem(Path path, int hint) {
		// TODO Auto-generated method stub
		return super.getIndexOfItem(path, hint);
	}

	@Override
	public int getIndexOf(Path path, ArrayList<MediaItem> list) {
		// TODO Auto-generated method stub
		return super.getIndexOf(path, list);
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
		super.delete();
	}

	@Override
	public int getMediaType() {
		// TODO Auto-generated method stub
		return super.getMediaType();
	}

	@Override
	public long getDataVersion() {
		// TODO Auto-generated method stub
		return super.getDataVersion();
	}

	@Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        DataManager dataManager = mApplication.getDataManager();
        Uri uri = mAlbumUri.buildUpon().appendQueryParameter("limit", start + "," + count).build();
        uri = uri.buildUpon().appendQueryParameter("invalid", "0").build();
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        GalleryUtils.assertNotInRenderThread();
        Cursor cursor = mResolver.query(uri, mProjection, mWhereClause, null, mOrderClause);
        if (cursor == null) {
            Log.w(TAG, "query fail: " + uri);
            return list;
        }

        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(INDEX_ID); // _id must be in the first
                                                    // column
                int type = cursor.getInt(INDEX_FILE_MEDIA_TYPE);
                Path childPath = null;
                boolean is_Image = true;
                if (type == FileColumns.MEDIA_TYPE_IMAGE) {
                    is_Image = true;
                    childPath = mImageItemPath.getChild(id);
                } else if (type == FileColumns.MEDIA_TYPE_VIDEO) {
                    is_Image = false;
                    childPath = mVideoItemPath.getChild(id);
                } else {
                    continue;
                }

                MediaItem item = loadOrUpdateItem(childPath, cursor, dataManager, mApplication, is_Image);
                list.add(item);
            }
        } finally {
            cursor.close();
        }
        return list;

    }


    private static MediaItem loadOrUpdateItem(Path path, Cursor cursor,
            DataManager dataManager, GalleryApp app, boolean isImage) {
        synchronized (DataManager.LOCK) {
            /// M: [DEBUG.ADD] @{
            TraceHelper.traceBegin(">>>>LocalAlbum-loadOrUpdateItem-peekMediaObject");
            /// @}
            LocalMediaItem item = (LocalMediaItem) dataManager.peekMediaObject(path);
            /// M: [DEBUG.ADD] @{
            TraceHelper.traceEnd();
            /// @}
            if (item == null) {
                if (isImage) {
                    /// M: [DEBUG.ADD] @{
                    TraceHelper.traceBegin(">>>>LocalAlbum-loadOrUpdateItem-new LocalImage");
                    /// @}
                    item = new LocalImage(path, app, cursor);
                    /// M: [DEBUG.ADD] @{
                    TraceHelper.traceEnd();
                    /// @}
                } else {
                    /// M: [DEBUG.ADD] @{
                    TraceHelper.traceBegin(">>>>LocalAlbum-loadOrUpdateItem-new LocalVideo");
                    /// @}
                    item = new LocalVideo(path, app, cursor, INDEX_RESOLUTION);
                    /// M: [DEBUG.ADD] @{
                    TraceHelper.traceEnd();
                    // @}
                }
            } else {
                /// M: [DEBUG.ADD] @{
                TraceHelper.traceBegin(">>>>LocalAlbum-loadOrUpdateItem-updateContent");
                /// @}
                item.updateContent(cursor);
                /// M: [DEBUG.ADD] @{
                TraceHelper.traceEnd();
                /// @}
            }
            return item;
        }
    }


	@Override
	public boolean isLeafAlbum() {
		// TODO Auto-generated method stub
        return true;
	}


	@Override
	public String getName() {
		// TODO Auto-generated method stub
        return mBucketName;
	}

    private ArrayList<Range> updateDaysInfo() {
        ArrayList<Range> list = new ArrayList<Range>();
        Uri uri = Files.getContentUri("external");
        ContentResolver cr = mApplication.getContentResolver();
        uri = uri.buildUpon().appendQueryParameter("invalid", "0").build();
        Cursor cursor = cr.query(uri, PROJECTION_BUCKET, mWhereClause, null, BUCKET_ORDER_BY);
        if (cursor == null) {
            return list;
        }

        long dateTaken = 0;
        long lastTime = Long.MAX_VALUE;
        int index = 0;
        Range range = null;
        while (cursor.moveToNext()) {
            dateTaken = cursor.getLong(0);
            if (dateTaken <= 0) {
                dateTaken = cursor.getLong(1) * 1000;
            }
            if (dateTaken < lastTime) {
                lastTime = GalleryUtils.cleatHour(dateTaken);
                range = new Range(index, index, lastTime);
                list.add(range);
            } else {
                range = list.get(list.size() - 1);
                range.mEnd += 1;
            }
            index++;
        }
        mCachedCount = cursor.getCount();
        cursor.close();
        return list;
    }
    
	@Override
    public long reload() {
        if (mNotifier.isDirty()) {
            mDataVersion = nextVersionNumber();
            mCachedCount = INVALID_COUNT;
            mImageCount = INVALID_COUNT;
            mVideoCount = INVALID_COUNT;
            long time = SystemClock.uptimeMillis();
            mDaysInfo = updateDaysInfo();
//            Log.e("lhq", "updatePartitionInfo consumetime=" + (SystemClock.uptimeMillis() - time));
        }
        return mDataVersion;
    }

    public int getSupportedOperations() {
        return SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_INFO;
    }

    public ArrayList<Range> getDaysInfo() {
        return mDaysInfo;
    }

}
