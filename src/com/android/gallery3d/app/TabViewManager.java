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

// transsion begin, IB-02533, xieweiwei, add, 2016.12.08
import com.transsion.gallery3d.ui.FloatingActionBar;
// transsion end

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
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
        if (mActivity.getViewPagerHelper() != null) {
        // transsion end
        mActivity.getViewPagerHelper().setOnPageChangeListener(mOnPageChangeListener);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
        }
        // transsion end
        mMainHandler = new Handler(mActivity.getMainLooper());
        initTab();
    }

    public int getVisibility() {
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        return View.GONE;
        // transsion end
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
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end

        return mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS;

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        return false;
        // transsion end
    }

    private void hideTab() {
        Log.d(TAG, "hideTab: ");
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.02
        //mActionBar.setDisplayShowTitleEnabled(true);
        // transsion end

        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.06
        //// transsion begin, IB-02533, xieweiwei, add, 2016.12.06
        //mActionBar.setDisplayShowTitleEnabled(true);
        //// transsion end
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.02
        mActionBar.hide();
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        // transsion end

    }

    private void initTab() {
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.03
        //// transsion begin, IB-02533, xieweiwei, add, 2016.12.02
        ////mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //// transsion end
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
        Tab tab = mActionBar.newTab().setText(R.string.tab_photos).setTabListener(mTabListener);
        mActionBar.addTab(tab, mCurrentTabIndex == 0);

        Tab tab2 = mActionBar.newTab().setText(R.string.tab_albums).setTabListener(mTabListener);
        mActionBar.addTab(tab2, mCurrentTabIndex == 1);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        // transsion end
        Log.w(TAG, "TabViewManager initTab ");

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
        if (mActivity.getActionBar() == null) {
        // transsion begin

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.08
        // transsion begin, IB-02533, xieweiwei, modify, 2016.12.15
        //FloatingActionBar.getInstance(mActivity).initTab();
        //FloatingActionBar.getInstance(mActivity).showTabActionBar();
        getFloatingActionBar().initTab();
        getFloatingActionBar().showTabActionBar();
        // transsion end
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
        }
        // transsion begin

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
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.02
        //mActionBar.setDisplayShowTitleEnabled(false);
        //mActionBar.setDisplayShowHomeEnabled(false);
        // transsion end

        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.06
        //// transsion begin, IB-02533, xieweiwei, add, 2016.12.06
        //mActionBar.setDisplayShowTitleEnabled(false);
        //// transsion end
        // transsion end

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end

        mSettingShowTab = true;
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mSettingShowTab = false;

        mActionBar.setSelectedNavigationItem(mCurrentTabIndex);

        mActionBar.show();

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        // transsion end

    }

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.06
    public void hideDisplayTitle() {
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
        mActionBar.setDisplayShowTitleEnabled(false);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        // transsion end
    }
    // transsion end

    private ActionBar.TabListener mTabListener = new TabListener() {
        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            //if(mLockTab)return;

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
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
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        // transsion end
            setCurrentTabIndex(tab.getPosition());
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    };

    public void enterSelectionMode() {
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end

        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                && mActivity.getViewPagerHelper() != null) {
            mActivity.getViewPagerHelper().setHorizontalEnable(false);
        }
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        } else {

            // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
            if (mActivity.getActionBar() == null) {
            // transsion begin

            // transsion begin, IB-02533, xieweiwei, modify, 2016.12.15
            //if (FloatingActionBar.getInstance(mActivity).isTabActionBarShown() && mActivity.getViewPagerHelper() != null) {
            if (getFloatingActionBar().isTabActionBarShown() && mActivity.getViewPagerHelper() != null) {
            // transsion end
                mActivity.getViewPagerHelper().setHorizontalEnable(false);
            }

            // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
            }
            // transsion begin
        }
        // transsion end
    }

    public void leaveSelectionMode() {
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                && mActivity.getViewPagerHelper() != null) {
        	Log.w(TAG,"leaveSelectionMode ");
            mActivity.getViewPagerHelper().setHorizontalEnable(true);
        }
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        } else {

            // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
            if (mActivity.getActionBar() == null) {
            // transsion begin

            // transsion begin, IB-02533, xieweiwei, modify, 2016.12.15
            //if (FloatingActionBar.getInstance(mActivity).isTabActionBarShown() && mActivity.getViewPagerHelper() != null) {
            if (getFloatingActionBar().isTabActionBarShown() && mActivity.getViewPagerHelper() != null) {
            // transsion end
                mActivity.getViewPagerHelper().setHorizontalEnable(true);
            }

            // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
            }
            // transsion begin
        }
        // transsion end
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

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
    // solve the problem of [TFS: 7394]
    public void setCurrentTabIndexSimply(int tabIndex) {
        mCurrentTabIndex = tabIndex;
    }
    // transsion end

    public void setCurrentTabIndex(int tabIndex) {
        if (tabIndex == mCurrentTabIndex) {
            return;
        }

        mCurrentTabIndex = tabIndex;

        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        if (mActionBar != null) {
        // transsion end
        if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                && mActionBar.getSelectedNavigationIndex() != mCurrentTabIndex) {
            mActionBar.setSelectedNavigationItem(mCurrentTabIndex);
        }
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.10
        }
        // transsion end

        mActivity.getStateManager().setCurrentTabIndex(mCurrentTabIndex);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
        if (mActivity.getViewPagerHelper() != null) {
        // transsion end
        mActivity.getViewPagerHelper().switchTo(mCurrentTabIndex);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
        }
        // transsion end
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

    // transsion begin, IB-02533, xieweiwei, add, 2016.12.15
    public FloatingActionBar getFloatingActionBar() {
        return mActivity.getFloatingActionBar();
    }
    // transsion end
}

