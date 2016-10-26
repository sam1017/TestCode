package com.android.gallery3d.app;

import android.app.ActionBar;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.app.Activity;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Context;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月24日 --- 下午2:55:47
 *
 */
public class TabViewManager {
    private static final String TAG = "TabViewManager";
    private final AbstractGalleryActivity mActivity;
    private int mCurrentTabIndex = 0;
    private ActionBar mActionBar;
    private Handler mMainHandler;
    private int mStatusBarSize;
    public boolean mLockTab = false;

    public interface Listener {
        public void onChanged(int oldIndex, int newIndex);
    }

    public TabViewManager(AbstractGalleryActivity activity) {
        mActivity = activity;
        int resourceId = mActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
        	mStatusBarSize = mActivity.getResources().getDimensionPixelSize(resourceId);
        }
        Log.w(TAG, "TabViewManager mStatusBarSize = " + mStatusBarSize);
        mActionBar = ((Activity) mActivity).getActionBar();
        mActivity.getViewPagerHelper().setOnPageChangeListener(mOnPageChangeListener);
        mMainHandler = new Handler(mActivity.getMainLooper());
        initTab();
    }

    public int getVisibility() {
        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    public void setVisibility(int visibility) {
        if (mActivity.getViewPagerHelper() != null) {
            mActivity.getViewPagerHelper().setHorizontalEnable(visibility == View.VISIBLE);
        }

        if (visibility == View.VISIBLE) {
            showTab();
        } else {
            hideTab();
        }
    }

    public boolean getTabEnable() {
        return mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS;
    }

    private void hideTab() {
        Log.d(TAG, "hideTab: ");
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.setDisplayShowTitleEnabled(true);
    }

    private void initTab() {
        Tab tab = mActionBar.newTab().setText(R.string.tab_albums).setTabListener(mTabListener);
        mActionBar.addTab(tab, mCurrentTabIndex == 0);

        Tab tab2 = mActionBar.newTab().setText(R.string.tab_photos).setTabListener(mTabListener);
        mActionBar.addTab(tab2, mCurrentTabIndex == 1);
        Log.w(TAG, "TabViewManager initTab ");

    }

    public void lockTabIndex() {
        mSettingShowTab = true;
    }

    public void unlockTabIndex() {
        mSettingShowTab = false;
    }

    public int getCurrentTabIndex() {
        return mCurrentTabIndex;
    }

    private boolean mSettingShowTab;

    private void showTab() {
        Log.d(TAG, "showTab: ");
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setDisplayShowHomeEnabled(false);
        mSettingShowTab = true;
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mSettingShowTab = false;

        mActionBar.setSelectedNavigationItem(mCurrentTabIndex);

        mActionBar.show();
    }

    private ActionBar.TabListener mTabListener = new TabListener() {
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            //if(mLockTab)return;
            if (!mActivity.getStateManager().isActivityStateValid(tab.getPosition())) {
                mActionBar.setSelectedNavigationItem(mCurrentTabIndex);
                return;
            }

            if (mSettingShowTab) {
                if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                        && mActionBar.getSelectedNavigationIndex() != mCurrentTabIndex) {
                    mActionBar.setSelectedNavigationItem(mCurrentTabIndex);
                }
                return;
            }
            setCurrentTabIndex(tab.getPosition());
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    };

    public void enterSelectionMode() {
        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                && mActivity.getViewPagerHelper() != null) {
            mActivity.getViewPagerHelper().setHorizontalEnable(false);
        }
    }

    public void leaveSelectionMode() {
        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                && mActivity.getViewPagerHelper() != null) {
            mActivity.getViewPagerHelper().setHorizontalEnable(true);
        }
    }

    public int getHeight() {
        return mStatusBarSize + mActionBar.getHeight();
    }

    public int getHeight4DivideScreen(int width, int height) {
        if (GalleryUtils.supportDivideScreenAndOpened((Context) mActivity)) {
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity) mActivity).getWindowManager().getDefaultDisplay().getMetrics(dm);
            if (dm.widthPixels != width || dm.heightPixels != height) {
                return mActionBar.getHeight();
            }
        }
        return getHeight();
    }

    public void setCurrentTabIndex(int tabIndex) {
        if (tabIndex == mCurrentTabIndex) {
            return;
        }

        mCurrentTabIndex = tabIndex;

        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                && mActionBar.getSelectedNavigationIndex() != mCurrentTabIndex) {
            mActionBar.setSelectedNavigationItem(mCurrentTabIndex);
        }

        mActivity.getStateManager().setCurrentTabIndex(mCurrentTabIndex);
        mActivity.getViewPagerHelper().switchTo(mCurrentTabIndex);
    }

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        private boolean enableUpdateAnimateTab;

        @Override
        public void onPageSelected(int position) {
            // Log.d(TAG, "onPageSelected: position = " + position);
            setCurrentTabIndex(position);
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset,
                final int positionOffsetPixels) {
            // Log.d(TAG,
            // "onPageScrolled: position, positionOffset, positionOffsetPixels = "
            // + position + ", " + positionOffset + ", " +
            // positionOffsetPixels);
            if (!enableUpdateAnimateTab) {
                return;
            }
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    //mActionBar.updateAnimateTab(position, positionOffset, positionOffsetPixels);
                }
            });
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    enableUpdateAnimateTab = false;
                    // Log.d(TAG,
                    // "onPageScrollStateChanged: SCROLL_STATE_IDLE");
                    break;
                }

                case ViewPager.SCROLL_STATE_DRAGGING:
                    enableUpdateAnimateTab = true;
                    // Log.d(TAG,
                    // "onPageScrollStateChanged: SCROLL_STATE_DRAGGING");
                    break;

                case ViewPager.SCROLL_STATE_SETTLING:
                    // Log.d(TAG,
                    // "onPageScrollStateChanged: SCROLL_STATE_SETTLING");
                    break;

                default:
                    // Log.d(TAG, "onPageScrollStateChanged: default");
                    break;
            }

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    //mActionBar.updateScrollState(state);
                }
            });
        }
    };
}

