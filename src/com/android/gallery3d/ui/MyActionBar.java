package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.util.GalleryUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月25日 --- 上午11:14:20
 *
 */
public class MyActionBar {
    private static final String TAG = "SystemActionBar";
    private final Activity mActivity;
    private ActionBar mActionBar;
    private ActionMode mActionMode;
    private boolean mInSelectedMode = false;
    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private Listener mListener;
    private int mActionBarSize;
    private int mStatusBarSize;
    private boolean mActionBarState;

    public interface Listener {
        public void onMenuItem(MenuItem item);

        public void onDestroyActionMode();
    }

    public MyActionBar(AbstractGalleryActivity activity) {
        mActivity = (Activity) activity;

        int resourceId = mActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
        	mStatusBarSize = mActivity.getResources().getDimensionPixelSize(resourceId);
        }
        TypedArray typedArray = mActivity.obtainStyledAttributes(android.R.styleable.Theme);
        mActionBarSize = typedArray.getDimensionPixelSize(android.R.styleable.Theme_actionBarSize,
                0);
        typedArray.recycle();
    }

    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
        if (mActionMode != null) {
            mActionMode.setTitle(title);
        }
    }

    public void setTitle(int resId) {
        getActionBar().setTitle(resId);
        if (mActionMode != null) {
            mActionMode.setTitle(resId);
        }
    }

    public void setSubTitle(CharSequence subtitle) {
        if (mInSelectedMode) {
            mActionMode.setSubtitle(subtitle);
        } else {
            getActionBar().setSubtitle(subtitle);
        }
    }

    public void setSubTitle(int resId) {
        if (mInSelectedMode) {
            mActionMode.setSubtitle(resId);
        } else {
            getActionBar().setSubtitle(resId);
        }
    }

    public void setBackground(Drawable background) {
        if (!mInSelectedMode) {
            getActionBar().setBackgroundDrawable(background);
        }
    }

    public boolean isInSelectedMode() {
        return mInSelectedMode;
    }

    private void saveActionBarState() {
        mActionBarState = getActionBar().isShowing();
    }

    private void updateActionBarState() {
        if (mActionBarState) {
            getActionBar().show();
        } else {
            getActionBar().hide();
        }
    }

    public int getVisibility() {
        return getActionBar().isShowing() ? View.VISIBLE : View.GONE;
    }

    public void setVisibility(int visibility) {
        if (mInSelectedMode) {
            return;
        }

        if (visibility == View.VISIBLE) {
            if (!getActionBar().isShowing()) {
                getActionBar().show();
            }
        } else {
            if (getActionBar().isShowing()) {
                getActionBar().hide();
            }
        }
    }

    public ActionBar getActionBar() {
        if (mActionBar == null) {
            mActionBar = mActivity.getActionBar();
            // setDisplayOptions(true, true);
        }
        return mActionBar;
    }

    public void setDisplayOptions(boolean displayHomeAsUp, boolean showTitle) {
        if (mActionBar != null) {
            int options = (displayHomeAsUp ? ActionBar.DISPLAY_HOME_AS_UP : 0)
                    | (showTitle ? ActionBar.DISPLAY_SHOW_TITLE : 0);
            mActionBar.setDisplayOptions(options, ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_TITLE);
            mActionBar.setHomeButtonEnabled(displayHomeAsUp);
        }
    }

    public int getActionBarSize() {
        return mStatusBarSize + (mActionBarSize == 0 ? getActionBar().getHeight() : mActionBarSize);
    }

    public int getActionBarSize4DivideScreen(int width, int height) {
        if (GalleryUtils.supportDivideScreenAndOpened((Context) mActivity)) {
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity) mActivity).getWindowManager().getDefaultDisplay().getMetrics(dm);
            if (dm.widthPixels != width || dm.heightPixels != height) {
                return mActionBar.getHeight();
            }
        }
        return getActionBarSize();
    }

    public void setSelectedMode(boolean selected) {
        mInSelectedMode = selected;
        if (mInSelectedMode) {
            saveActionBarState();
            mActionMode = mActivity.startActionMode(mActionModeCallback);
            mActionMode.setTitle(getActionBar().getTitle());
        } else {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            updateActionBarState();
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public Menu getMenu() {
        if (mInSelectedMode) {
            return mActionModeCallback.getMenu();
        }
        return null;
    }

    public void setMenu(Menu menu) {
        // TODO Auto-generated method stub

    }

    public void refreshMenu() {
        // TODO Auto-generated method stub

    }

    public void createMenu(int menuRes) {
        mActionModeCallback.setMenu(menuRes);
    }

    private class ActionModeCallback implements ActionMode.Callback {
        private Menu actionModeMenu;
        private int menuResId = 0;

        public Menu getMenu() {
            return actionModeMenu;
        }

        public void setMenu(int id) {
            menuResId = id;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mListener != null) {
                mListener.onMenuItem(item);
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (menuResId == 0) {
                return true;
            }
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(menuResId, menu);
            actionModeMenu = menu;
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            menuResId = 0;
            if (mListener != null) {
                mListener.onDestroyActionMode();
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

}

