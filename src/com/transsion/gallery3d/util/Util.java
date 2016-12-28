//////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2016-2036  TRANSSION HOLDINGS
//
//  PROPRIETARY RIGHTS of TRANSSION HOLDINGS are involved in the
//  subject matter of this material.  All manufacturing, reproduction, use,
//  and sales rights pertaining to this subject matter are governed by the
//  license agreement.  The recipient of this software implicitly accepts
//  the terms of the license.
//
//  Description: For support move operation
//  Author:      xieweiwei(IB-02533)
//  Version:     V1.0
//  Date:        2016.12.21
//  Modification:
//////////////////////////////////////////////////////////////////////////////////

package com.transsion.util;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import java.util.ArrayList;
import com.android.gallery3d.app.AbstractGalleryActivity;
import android.app.ProgressDialog;
import com.android.gallery3d.R;
import android.content.Context;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.database.Cursor;
import com.android.gallery3d.util.GalleryUtils;

public class Util {
    private static final String TAG = "Gallery/Util";
    public static void moveFile(String resPath, String dest) {
        if (resPath == null || dest == null) {
            // transsion begin, IB-02533, xieweiwei, modify, 2016.12.26
            //Log.e(TAG, "CopyFile: param is null, fileInfo=" + resPath + ", dest=" + dest);
            Log.e(TAG, "moveFile: param is null, fileInfo=" + resPath + ", dest=" + dest);
            // transsion end
            return;
        }
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.26
        //Log.w(TAG,"copyFile resPath = " + resPath + " dest = " + dest);
        Log.w(TAG,"moveFile resPath = " + resPath + " dest = " + dest);
        // transsion end
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(resPath);
            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(resPath);
                FileOutputStream fs = new FileOutputStream(dest);
                byte[] buffer = new byte[1024];
                int length;
                while ( (byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                oldfile.delete();
            }
            // transsion begin, IB-02533, xieweiwei, modify, 2016.12.26
            //Log.w(TAG,"copyFile done");
            Log.w(TAG,"moveFile done");
            // transsion end
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MoveFilesAsync extends AsyncTask {
        private AbstractGalleryActivity mActivity;
        private String mFilePathTo;
        private String mFileNameTo;
        private String mFileFullNameFrom;
        private ArrayList<Uri> mUris;
        private ProgressDialog mProgressDialog = null;
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
        private boolean mMoveOneConshotPic = false;
        private int mGroupId = -1;
        // transsion end

        public MoveFilesAsync(AbstractGalleryActivity activity, ArrayList<Uri> uris, String filePathTo) {
            mActivity = activity;
            // transsion begin, IB-02533, xieweiwei, add, 2016.12.21
            File file = new File(filePathTo);
            if (file != null && file.exists()) {
                mFilePathTo = filePathTo;
            } else {
            // transsion end
            mFilePathTo = getDirectionFromBucketPath(filePathTo);
            // transsion begin, IB-02533, xieweiwei, add, 2016.12.21
            }
            // transsion end
            mUris = uris;
            // transsion begin, IB-02533, xieweiwei, add, 2016.12.26
            mMoveOneConshotPic = false;
            // transsion end
        }

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
        public MoveFilesAsync(AbstractGalleryActivity activity, ArrayList<Uri> uris, String filePathTo, boolean moveOneConshotPic) {
            this(activity, uris, filePathTo);
            mMoveOneConshotPic = moveOneConshotPic;
        }
        // transsion end

        public void showDialog() {
            if(mProgressDialog == null){
                mProgressDialog = new ProgressDialog(mActivity);
            }
            mProgressDialog.setMessage(mActivity.getResources().getString(R.string.please_wait));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        private String getRealPathFromUri(Context context, Uri uri){
            String filePath = null;
            mFileNameTo = null;
            String[] wholeID = mActivity.getDataManager().findPathByUri(uri,
                       "image").split();
            Log.w(TAG,"wholeID = " + wholeID.length);
            for(int i = 0; i < wholeID.length; i++){
                Log.w(TAG,"wholeID[" + i + "] = " + wholeID[i]);
            }

            String id = wholeID[3];
            Log.w(TAG,"id = " + id);

            // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
            String type = wholeID[1];
            if ("image".equals(type)) {
            // transsion end

            String[] projection = { MediaStore.Images.Media.DATA, "_display_name" };
            String selection = MediaStore.Images.Media._ID + "=?";
            String[] selectionArgs = { id };

            Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                selection, selectionArgs, null);
            int columnIndex = cursor.getColumnIndex(projection[0]);
            int titleIndex = cursor.getColumnIndex(projection[1]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
                mFileNameTo = cursor.getString(titleIndex);
                Log.w(TAG,"getRealPathFromUri filePath = " + filePath + " mFileNameTo = " + mFileNameTo);
            }
            cursor.close();

            // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
            } else if ("video".equals(type)) {
                String[] projection = { MediaStore.Video.Media.DATA, "_display_name" };
                String selection = MediaStore.Video.Media._ID + "=?";
                String[] selectionArgs = { id };
                Cursor cursor = context.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                    selection, selectionArgs, null);
                int columnIndex = cursor.getColumnIndex(projection[0]);
                int titleIndex = cursor.getColumnIndex(projection[1]);
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex);
                    mFileNameTo = cursor.getString(titleIndex);
                    Log.w(TAG,"getRealPathFromUri video filePath = " + filePath + " mFileNameTo = " + mFileNameTo);
                }
                cursor.close();
            }
            // transsion end

            return filePath;
        }

        private String getDirectionFromBucketPath(String pathString) {
            if (pathString == null) {
                return null;
            }
            // split bucket id string
            String [] temp = null;
            temp = pathString.split("/");
            String bucketIdString = null;
            if (temp != null) {
                bucketIdString = temp[temp.length - 1];
            }
            // search direction for path
            int buckedId = Integer.parseInt(bucketIdString);
            return searchDirectionForPath(new File("/storage/"), buckedId);
        }

        public String searchDirectionForPath(File dir, int bucketId) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String path = file.getAbsolutePath();
                        if (GalleryUtils.getBucketId(path) == bucketId) {
                            return path;
                        } else {
                            if (path != null) {
                                if (path.startsWith("/storage/sdcard0")) {
                                    path = path.replace("/storage/sdcard0", "/storage/emulated/0");
                                } else if (path.startsWith("/storage/sdcard1")) {
                                    path = path.replace("/storage/sdcard1", "/storage/emulated/1");
                                }
                                if (GalleryUtils.getBucketId(path) == bucketId) {
                                    return path;
                                }
                            }
                            path = searchDirectionForPath(file, bucketId);
                            if (path != null) return path;
                        }
                    }
                }
            }
            return null;
        }

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
        private boolean moveConshotImageByDatabase(Context context, Uri uri, ArrayList<String> comPaths, int groupId, String fileFullPathFrom, String filePathTo) {
            if (groupId <= 0 || fileFullPathFrom == null || "".equals(fileFullPathFrom)) {
                return false;
            }
            String filePath = fileFullPathFrom;
            String path = filePath.substring(0, filePath.lastIndexOf("/"));

            String[] projectionGroup = { MediaStore.Images.Media.DATA, "_display_name"};
            String selectionGroup = MediaStore.Images.ImageColumns.GROUP_ID + "=?";
            String groupIdString = "" + groupId;
            String[] selectionArgsGroup = { groupIdString };

            Cursor cursorGroup = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionGroup,
                    selectionGroup, selectionArgsGroup, null);
            int columnIndexGroup = cursorGroup.getColumnIndex(projectionGroup[0]);
            int titleIndexGroup = cursorGroup.getColumnIndex(projectionGroup[1]);

            String filePathGroup;
            String fileNameToGroup;
            if (cursorGroup != null && cursorGroup.moveToFirst()) {
                int groundIndex = 1;
                String pathGroup;
                do {
                    filePathGroup = cursorGroup.getString(columnIndexGroup);
                    fileNameToGroup = cursorGroup.getString(titleIndexGroup);
                    pathGroup = filePathGroup.substring(0, filePathGroup.lastIndexOf("/"));
                    Log.w(TAG, "moveConshotImageByDatabase filePathGroup = " + filePathGroup + " fileNameToGroup = " + fileNameToGroup + "path = " + path + " pathGroup = " + pathGroup);
                    if (path != null && path.equals(pathGroup)) {
                        if (filePath != null && !filePath.equals(filePathGroup)) {
                            moveFile(filePathGroup, filePathTo + "/" + fileNameToGroup);
                            comPaths.add(filePathTo + "/" + fileNameToGroup);
                            comPaths.add(filePathGroup);
                        }
                    }
                } while (cursorGroup.moveToNext());
            }
            cursorGroup.close();
            return true;
        }
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
        private int getGroupId(Context context, Uri uri) {
            int groupId = 0;
            String[] wholeID = mActivity.getDataManager().findPathByUri(uri,
                       "image").split();
            String id = wholeID[3];
            String type = wholeID[1];
            if ("image".equals(type)) {
                String[] projection = { MediaStore.Images.Media.DATA, "_display_name", "group_id"};
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = { id };
                Cursor cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    selection, selectionArgs, null);
                int groupIdIndex = cursor.getColumnIndex(projection[2]);
                if (cursor != null && cursor.moveToFirst()) {
                    groupId = cursor.getInt(groupIdIndex);
                    mGroupId = groupId;
                }
                cursor.close();
            }
            return groupId;
        }

        private boolean isConshotPicByDatabase(Context context, Uri uri) {
            int groupId = getGroupId(context, uri);
            if (groupId != 0) {
                return true;
            }
            return false;
        }
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
        private boolean moveConshotImage(String fileFullNameFrom, ArrayList<String> comPaths) {
            //getRealPathFromUri filePath = /storage/emulated/0/DCIM/Camera/IMG_20161223_134240_1CS.jpg mFileNameTo = IMG_20161223_134240_1CS.jpg
            //mFileFullNameFrom = /storage/emulated/0/DCIM/Camera/IMG_20161222_185310_6CS.jpg mFilePathTo = /storage/emulated/0/DCIM/aa
            //copyFile resPath = /storage/emulated/0/DCIM/Camera/IMG_20161223_134240_1CS.jpg dest = /storage/emulated/0/DCIM/aa/IMG_20161223_134240_1CS.jpg
            String[] paths = fileFullNameFrom.split("/");
            String fileName = paths[paths.length - 1];
            Log.i(TAG, "fileName = " + fileName);
            String[] token = fileName.split("\\.");
            if (token.length == 0) {
                return false;
            }
            String postfix = token[token.length - 1];
            Log.i(TAG, "postfix = " + postfix);
            if ("jpg".equals(postfix) || "jpeg".equals(postfix)) {
                String prefix = token[0];
                String[] parts = prefix.split("_");
                String lastPart = parts[parts.length - 1];
                Log.i(TAG, "lastPart = " + lastPart);
                if (lastPart != null && lastPart.endsWith("CS")) {
                    String conNumString = lastPart.substring(0, lastPart.length() - 2);
                    int conNum = Integer.parseInt(conNumString);
                    Log.i(TAG, "conNum = " + conNum);
                    String leftFileFullName;
                    String leftFileName;
                    File leftFile;
                    for (int i = conNum + 1; i < 41; i++) {
                        leftFileFullName = "";
                        leftFileName = "";
                        for (int p = 0; p < paths.length - 1; p++) {
                            leftFileFullName += (paths[p] + "/");
                        }
                        for (int t = 0; t < parts.length - 1; t++) {
                            leftFileFullName += (parts[t] + "_");
                            leftFileName += (parts[t] + "_");
                        }
                        leftFileFullName += (i + "CS");
                        leftFileFullName += ("." + postfix);
                        leftFileName += (i + "CS");
                        leftFileName += ("." + postfix);
                        Log.i(TAG, "leftFileFullName = " + leftFileFullName + " leftFileName = " + leftFileName);
                        leftFile = new File(leftFileFullName);
                        if (leftFile != null && leftFile.exists()) {
                            Log.i(TAG, "moveFile leftFileFullName = " + leftFileFullName + " FileNameTo = " + (mFilePathTo + "/" + leftFileName));
                            moveFile(leftFileFullName, mFilePathTo + "/" + leftFileName);
                            comPaths.add(mFilePathTo + "/" + leftFileName);
                            comPaths.add(leftFileFullName);
                        } else {
                            for (int s = 1; s < 10; s++) {
                                String timeString = parts[parts.length - 2];
                                String nextSecondString = computeNextSecondString(timeString, s);
                                leftFileFullName = "";
                                leftFileName = "";
                                for (int p = 0; p < paths.length - 1; p++) {
                                    leftFileFullName += (paths[p] + "/");
                                }
                                for (int t = 0; t < parts.length - 2; t++) {
                                    leftFileFullName += (parts[t] + "_");
                                    leftFileName += (parts[t] + "_");
                                }
                                leftFileFullName += (nextSecondString + "_");
                                leftFileFullName += (i + "CS");
                                leftFileFullName += ("." + postfix);
                                leftFileName += (nextSecondString + "_");
                                leftFileName += (i + "CS");
                                leftFileName += ("." + postfix);
                                Log.i(TAG, "leftFileFullName = " + leftFileFullName + " leftFileName = " + leftFileName);
                                leftFile = new File(leftFileFullName);
                                if (leftFile != null && leftFile.exists()) {
                                    Log.i(TAG, "moveFile leftFileFullName = " + leftFileFullName + " FileNameTo = " + (mFilePathTo + "/" + leftFileName));
                                    moveFile(leftFileFullName, mFilePathTo + "/" + leftFileName);
                                    comPaths.add(mFilePathTo + "/" + leftFileName);
                                    comPaths.add(leftFileFullName);
                                    break;
                                } else {
                                    continue;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
        private String computeNextSecondString(String timeString, int secondAdded) {
            // timeString format is "20161223"
            if (timeString == null || timeString.length() != 6) {
                return null;
            }
            String hourString = timeString.substring(0, 2);
            String minuteString = timeString.substring(2, 4);
            String secondString = timeString.substring(4, 6);
            int hour = Integer.parseInt(hourString);
            int minute = Integer.parseInt(minuteString);
            int second = Integer.parseInt(secondString);

            second = second + secondAdded;
            if (second >= 60) {
                second = 0;
                minute++;
                if (minute >= 60) {
                    minute = 0;
                    hour++;
                    if (hour >= 24) {
                        hour = 0;
                    }
                }
            }
            String time = "";
            time += (hour >= 0 && hour <= 9 ? ("0" + hour) : hour);
            time += (minute >= 0 && minute <= 9 ? ("0" + minute) : minute);
            time += (second >= 0 && second <= 9 ? ("0" + second) : second);
            return time;
        }

        private boolean isConshotPic(String fileFullNameFrom) {
            String[] paths = fileFullNameFrom.split("/");
            String fileName = paths[paths.length - 1];
            Log.i(TAG, "fileName = " + fileName);
            String[] token = fileName.split("\\.");
            if (token.length == 0) {
                return false;
            }
            String postfix = token[token.length - 1];
            Log.i(TAG, "postfix = " + postfix);
            if ("jpg".equals(postfix) || "jpeg".equals(postfix)) {
                String prefix = token[0];
                String[] parts = prefix.split("_");
                String lastPart = parts[parts.length - 1];
                Log.i(TAG, "lastPart = " + lastPart);
                if (lastPart != null && lastPart.endsWith("CS")) {
                    return true;
                }
            }
            return false;
        }
        // transsion end

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.w(TAG,"onPreExecute");
            showDialog();
        }

        @Override
        protected Object doInBackground(Object... params) {
            String[] paths = new String[mUris.size() * 2];
            // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
            ArrayList<String> shotPaths = new ArrayList<String>();
            // transsion end
            for(int i = 0; i < mUris.size(); i++) {
                mFileFullNameFrom = getRealPathFromUri(mActivity, mUris.get(i));
                Log.w(TAG,"mFileFullNameFrom = " + mFileFullNameFrom + " mFilePathTo = " + mFilePathTo);
                moveFile(mFileFullNameFrom, mFilePathTo + "/" + mFileNameTo);
                paths[i] = mFilePathTo + "/" + mFileNameTo;
                paths[i + mUris.size()] = mFileFullNameFrom;
                // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
                boolean needConshot = isConshotPicByDatabase(mActivity, mUris.get(i));
                Log.i(TAG, "mMoveOneConshotPic = " + mMoveOneConshotPic + " needConshot = " + needConshot);
                if (!mMoveOneConshotPic && needConshot) {
                    moveConshotImageByDatabase(mActivity, mUris.get(i), shotPaths, mGroupId, mFileFullNameFrom, mFilePathTo);
                }
                // transsion end
            }
            // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
            if (shotPaths.size() > 0) {
                String[] comPaths = new String[paths.length + shotPaths.size()];
                for (int i = 0; i < paths.length; i++) {
                    comPaths[i] = paths[i];
                    Log.w(TAG, "comPaths[i] = " + comPaths[i] + " i = " + i);
                }
                for (int j = 0; j < shotPaths.size(); j++) {
                    comPaths[paths.length + j] = shotPaths.get(j);
                    Log.w(TAG, "comPaths[paths.length + j] = " + comPaths[paths.length + j] + " i = " + (paths.length + j));
                }
                MediaScannerConnection.scanFile(mActivity, comPaths, null, null);
            } else {
            // transsion end
            MediaScannerConnection.scanFile(mActivity, paths, null, null);
            // transsion begin, IB-02533, xieweiwei, add, 2016.12.23
            }
            // transsion end
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            Log.w(TAG,"onPostExecute");
            mProgressDialog.dismiss();
            mFilePathTo = null;
        }
    }
}