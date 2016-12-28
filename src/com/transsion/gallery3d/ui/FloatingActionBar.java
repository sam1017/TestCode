//////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2016-2036  TRANSSION HOLDINGS
//
//  PROPRIETARY RIGHTS of TRANSSION HOLDINGS are involved in the
//  subject matter of this material.  All manufacturing, reproduction, use,
//  and sales rights pertaining to this subject matter are governed by the
//  license agreement.  The recipient of this software implicitly accepts
//  the terms of the license.
//
//  Description: For support floating action bar
//  Author:      xieweiwei(IB-02533)
//  Version:     V1.0
//  Date:        2016.12.13
//  Modification:
//////////////////////////////////////////////////////////////////////////////////

package com.transsion.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;

import android.view.ViewGroup;

import com.android.gallery3d.R;

import android.view.View;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.content.Intent;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.widget.TabHost.OnTabChangeListener;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.transsion.gallery3d.ui.FlexImageView;
import com.transsion.gallery3d.ui.RippleView;

public class FloatingActionBar {

    public interface ButtonClickListener {
        public boolean onStandantBack();
        public boolean onClusterBack();
        public boolean onSelectionModeBack();
        public void onClusterModeClick(int mode);
        public void onSelectionModeAll(boolean selectAll);
    }
    public static abstract class StandantButtonClickListener implements ButtonClickListener {
        public boolean onClusterBack() {
            return false;
        }
        public boolean onSelectionModeBack() {
            return false;
        }
        public void onClusterModeClick(int mode) {
        }
        public void onSelectionModeAll(boolean selectAll) {
        }
    }
    public static abstract class ClusterButtonClickListener implements ButtonClickListener {
        public boolean onStandantBack() {
            return false;
        }
        public boolean onSelectionModeBack() {
            return false;
        }
        public void onSelectionModeAll(boolean selectAll) {
        }
    }
    public static abstract class SelectionModeButtonClickListener implements ButtonClickListener {
        public boolean onStandantBack() {
            return false;
        }
        public boolean onClusterBack() {
            return false;
        }
        public void onClusterModeClick(int mode) {
        }
    }

    private enum ActionBarType {
        ACTIONBAR_TYPE_UNKNOWN,
        ACTIONBAR_TYPE_TAB, // NewTimeShiftPage and AlbumSetPage
        ACTIONBAR_TYPE_STANDANT, // fullscreen mode in PhotoPage
        ACTIONBAR_TYPE_CLUSTER, // grid mode in AlbumPage
        ACTIONBAR_TYPE_CLUSTER_STANDANT, // filmstrip mode in PhotoPage
        ACTIONBAR_TYPE_SELECTION_MODE
    };

    private enum SelectionModeAll {
        SELECTION_MODE_ALL,
        SELECTION_MODE_EMPTY,
    };

    private ActionBarType mActionBarType = ActionBarType.ACTIONBAR_TYPE_UNKNOWN;
    private SelectionModeAll mSelectionModeAll = SelectionModeAll.SELECTION_MODE_EMPTY;

    private ViewGroup mStandantActionBar;
    private ViewGroup mTabActionBar;
    private ViewGroup mSelectionModeActionBar;
    private ViewGroup mClusterActionBar;
    private ViewGroup mCurrentActionBarForSelection;
    private AbstractGalleryActivity mActivity;
    private int mCurrentTabIndex = 0;
    private TabHost mTabHost;
    
    private TextView mStandantTitleTextView;
    private View mStandantBackImageView;
    private ButtonClickListener mStandantClickListener = null;

    private TextView mClusterTitleTextView;
    //private TextView mClusterSubTitleTextView;
    private View mClusterBackImageView;
    private Spinner mClusterSpinner;
    private ButtonClickListener mClusterClickListener = null;

    private TextView mSelectionModeTitleTextView;
    private View mSelectionModeBackImageView;
    private View mSelectionAllImgaeView;
    private ButtonClickListener mSelectionModeClickListener = null;

    public static final int ALBUM_FILMSTRIP_MODE_SELECTED = 0;
    public static final int ALBUM_GRID_MODE_SELECTED = 1;

    public void setStandantClickListener(ButtonClickListener listener) {
        mStandantClickListener = listener;
    }

    public void setClusterClickListener(ButtonClickListener listener) {
        mClusterClickListener = listener;
    }

    public void setSelectionModeClickListener(ButtonClickListener listener) {
        mSelectionModeClickListener = listener;
    }

    private FloatingActionBar(AbstractGalleryActivity context) {
        mStandantActionBar = (ViewGroup)context.findViewById(R.id.floating_actionbar_standant);
        mTabActionBar = (ViewGroup)context.findViewById(R.id.floating_actionbar_tab);
        mSelectionModeActionBar = (ViewGroup)context.findViewById(R.id.floating_actionbar_selection_mode);
        mClusterActionBar = (ViewGroup)context.findViewById(R.id.floating_actionbar_cluster);
        mActivity = context;
    }

    public FloatingActionBar(AbstractGalleryActivity context, boolean notSingleton) {
        this(context);
    }

    private static FloatingActionBar mSingleton = null;

    // transsion begin, IB-02533, xieweiwei, delete, 2016.12.15
    //public static FloatingActionBar getInstance(AbstractGalleryActivity context) {
    //    if (mSingleton == null) {
    //        mSingleton = new FloatingActionBar(context);
    //    }
    //    return mSingleton;
    //}
    // transsion end

    public static void destroy() {
        mSingleton = null;
    }

    public void showStandantActionBar() {
        mStandantActionBar.setVisibility(View.VISIBLE);
        mTabActionBar.setVisibility(View.GONE);
        mSelectionModeActionBar.setVisibility(View.GONE);
        mClusterActionBar.setVisibility(View.GONE);
        mActionBarType = ActionBarType.ACTIONBAR_TYPE_STANDANT;
        mCurrentActionBarForSelection = mStandantActionBar;
    }

    public void showTabActionBar() {
        mStandantActionBar.setVisibility(View.GONE);
        mTabActionBar.setVisibility(View.VISIBLE);
        mSelectionModeActionBar.setVisibility(View.GONE);
        mClusterActionBar.setVisibility(View.GONE);
        mActionBarType = ActionBarType.ACTIONBAR_TYPE_TAB;
        mCurrentActionBarForSelection = mTabActionBar;
    }

    public void showSelectionModeActionBar() {
        mStandantActionBar.setVisibility(View.GONE);
        mTabActionBar.setVisibility(View.GONE);
        mSelectionModeActionBar.setVisibility(View.VISIBLE);
        mClusterActionBar.setVisibility(View.GONE);
        mActionBarType = ActionBarType.ACTIONBAR_TYPE_SELECTION_MODE;
    }

    public void showClusterActionBar() {
        mStandantActionBar.setVisibility(View.GONE);
        mTabActionBar.setVisibility(View.GONE);
        mSelectionModeActionBar.setVisibility(View.GONE);
        mClusterActionBar.setVisibility(View.VISIBLE);
        mActionBarType = ActionBarType.ACTIONBAR_TYPE_CLUSTER;
        mCurrentActionBarForSelection = mClusterActionBar;
    }

    public void showClusterStandantActionBar() {
        mStandantActionBar.setVisibility(View.GONE);
        mTabActionBar.setVisibility(View.GONE);
        mSelectionModeActionBar.setVisibility(View.GONE);
        mClusterActionBar.setVisibility(View.VISIBLE);
        mActionBarType = ActionBarType.ACTIONBAR_TYPE_CLUSTER_STANDANT;
        mCurrentActionBarForSelection = mClusterActionBar;
    }

    public void hideActionBar() {
        mStandantActionBar.setVisibility(View.GONE);
        mTabActionBar.setVisibility(View.GONE);
        mSelectionModeActionBar.setVisibility(View.GONE);
        mClusterActionBar.setVisibility(View.GONE);
    }

    public void restoreActionBarFromSelection() {
        final ViewGroup actionBars [] = {mStandantActionBar
                , mTabActionBar, mClusterActionBar, mSelectionModeActionBar};
        for (ViewGroup actionBar: actionBars) {
            if (actionBar != mCurrentActionBarForSelection) {
                actionBar.setVisibility(View.GONE);
            } else {
                actionBar.setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean isTabActionBarShown() {
        return mActionBarType == ActionBarType.ACTIONBAR_TYPE_TAB;
    }

    public boolean isStandantActionBarShown() {
        return mActionBarType == ActionBarType.ACTIONBAR_TYPE_STANDANT;
    }

    public void initTab() {
        mTabHost =(TabHost) mActivity.findViewById(R.id.tab_host);
        mTabHost.setup();
        mTabHost.clearAllTabs();
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View viewLeft = inflater.inflate(R.layout.floating_actionbar_tab_left, null);
        View viewRight = inflater.inflate(R.layout.floating_actionbar_tab_right, null);
        mTabHost.addTab(mTabHost.newTabSpec("one").setIndicator(viewLeft).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("two").setIndicator(viewRight).setContent(android.R.id.tabcontent));
        mActivity.getViewPagerHelper().setOnPageChangeListener(mOnPageChangeListener);
        mTabHost.setOnTabChangedListener(new OnTabChangedImpl());
    }

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            setCurrentIndex(position);
        }
        @Override
        public void onPageScrolled(final int position, final float positionOffset,
                final int positionOffsetPixels) {
        }
        @Override
        public void onPageScrollStateChanged(final int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING:
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    break;
                default:
                    break;
            }
        }
    };

    public void setCurrentIndex(int index) {
        if (index == mCurrentTabIndex) {
            return;
        }
        mCurrentTabIndex = index;
        if (mTabHost != null) {
            mTabHost.setCurrentTab(index);
        }
        mActivity.getStateManager().setCurrentTabIndex(mCurrentTabIndex);
        mActivity.getViewPagerHelper().switchTo(mCurrentTabIndex);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.19
        // solve the problem of [TFS: 7394]
        mActivity.getTabViewManager().setCurrentTabIndexSimply(mCurrentTabIndex);
        // transsion end

    }

    private class OnTabChangedImpl implements OnTabChangeListener {
        @Override
        public void onTabChanged(String key) {
            int currentIndex = -1;
            if(mActivity.getIsActivityResume()){
                if (mTabHost != null) {
                    currentIndex = mTabHost.getCurrentTab();
                    setCurrentIndex(currentIndex);
                }
            }
        }
    }

    public void initStandant(ButtonClickListener listener) {
        setStandantClickListener(listener);
        mStandantTitleTextView = (TextView)mActivity.findViewById(R.id.floating_actionbar_standant_textview);
        mStandantBackImageView = mActivity.findViewById(R.id.floating_actionbar_standant_back_button);
        if (mStandantBackImageView instanceof FlexImageView) {
            ((FlexImageView)mStandantBackImageView).setOnRippleCompleteListener(mRippleButtonClickImpl);
        } else if (mStandantBackImageView instanceof ImageView) {
            ((ImageView)mStandantBackImageView).setOnClickListener(mButtonClickImpl);
        }
    }

    public View.OnClickListener mButtonClickImpl = new View.OnClickListener() {
        public void onClick(View view) {
            int id = view.getId();
            switch (id) {
                case R.id.floating_actionbar_standant_back_button:
                    if (mStandantClickListener != null) {
                        mStandantClickListener.onStandantBack();
                    }
                    break;
                case R.id.floating_actionbar_cluster_back_button:
                    if (mClusterClickListener != null) {
                        mClusterClickListener.onClusterBack();
                    }
                    break;
                case R.id.floating_actionbar_selection_mode_back_button:
                    if (mSelectionModeClickListener != null) {
                        mSelectionModeClickListener.onSelectionModeBack();
                    }
                    break;
                case R.id.floating_actionbar_selection_mode_selectionall:
                    if (mSelectionModeAll == SelectionModeAll.SELECTION_MODE_EMPTY) {
                        setSelectionAllImageView(SelectionModeAll.SELECTION_MODE_ALL);
                    } else if (mSelectionModeAll == SelectionModeAll.SELECTION_MODE_ALL) {
                        setSelectionAllImageView(SelectionModeAll.SELECTION_MODE_EMPTY);
                    }
                    if (mSelectionModeClickListener != null) {
                        mSelectionModeClickListener.onSelectionModeAll(
                                mSelectionModeAll == SelectionModeAll.SELECTION_MODE_ALL ? true : false);
                    }
                    break;
                default:
                    break;
            }
            
        }
    };

    public FlexImageView.OnRippleCompleteListener mRippleButtonClickImpl = new FlexImageView.OnRippleCompleteListener() {
        public void onComplete(RippleView rippleView) {
            if (mButtonClickImpl != null) {
                mButtonClickImpl.onClick(rippleView);
            }
        }
    };

    public void setStandantTitle(int titleStringId) {
        String title = mActivity.getResources().getString(titleStringId);
        setStandantTitle(title);
    }

    public void setStandantTitle(String title) {
        if (mStandantTitleTextView != null) {
            mStandantTitleTextView.setText(title);
        }
    }

    public void setStandantBackIconVisible(boolean visible) {
        if (mStandantBackImageView != null) {
            if (visible) {
                mStandantBackImageView.setVisibility(View.VISIBLE);
            } else {
                mStandantBackImageView.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void initCluster(ButtonClickListener listener) {
        setClusterClickListener(listener);
        mClusterTitleTextView = (TextView)mActivity.findViewById(R.id.floating_actionbar_cluster_textview);
        mClusterBackImageView = mActivity.findViewById(R.id.floating_actionbar_cluster_back_button);
        // transsion begin, IB-02533, xieweiwei, delete, 2016.12.16
        //mClusterBackImageView.setOnClickListener(mButtonClickImpl);
        // transsion end
        if (mClusterBackImageView instanceof FlexImageView) {
            ((FlexImageView)mClusterBackImageView).setOnRippleCompleteListener(mRippleButtonClickImpl);
        } else if (mClusterBackImageView instanceof ImageView) {
            ((ImageView)mClusterBackImageView).setOnClickListener(mButtonClickImpl);
        }

        mClusterSpinner = (Spinner)mActivity.findViewById(R.id.actionbar_cluster_spinner);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
        if (mClusterSpinner != null) {
        // transsion end
        mClusterSpinner.setSelection(ALBUM_GRID_MODE_SELECTED);
        mClusterSpinner.setOnItemSelectedListener(mClusterSpinnerSelected);
        // transsion begin, IB-02533, xieweiwei, add, 2016.12.16
        }
        // transsion end
    }

    public void setToFilmModeCluster(ButtonClickListener listener) {
        setClusterClickListener(listener);
        mClusterSpinner.setSelection(ALBUM_FILMSTRIP_MODE_SELECTED);
    }

    public void setToGridCluster(ButtonClickListener listener) {
        setClusterClickListener(listener);
        mClusterSpinner.setSelection(ALBUM_GRID_MODE_SELECTED);
    }

    public OnItemSelectedListener mClusterSpinnerSelected = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            int mode = pos;
            // make sure pos 0 means R.string.switch_photo_filmstrip
            //           pos 1 means R.string.switch_photo_grid
            if (mode == ALBUM_FILMSTRIP_MODE_SELECTED) {
                if (mClusterClickListener != null) {
                    mClusterClickListener.onClusterModeClick(mode);
                }
            } else if (mode == ALBUM_GRID_MODE_SELECTED) {
                if (mClusterClickListener != null) {
                    mClusterClickListener.onClusterModeClick(mode);
                }
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    public void setClusterTitle(int titleStringId) {
        String title = mActivity.getResources().getString(titleStringId);
        setClusterTitle(title);
    }

    public void setClusterTitle(String title) {
        if (mClusterTitleTextView != null) {
            mClusterTitleTextView.setText(title);
        }
    }

    public void setClusterBackIconVisible(boolean visible) {
        if (mClusterBackImageView != null) {
            if (visible) {
                mClusterBackImageView.setVisibility(View.VISIBLE);
            } else {
                mClusterBackImageView.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setClusterSpinnerVisible(boolean visible) {
        if (mClusterSpinner != null) {
            if (visible) {
                mClusterSpinner.setVisibility(View.VISIBLE);
            } else {
                mClusterSpinner.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void initSelectionMode(ButtonClickListener listener) {
        setSelectionModeClickListener(listener);
        mSelectionModeTitleTextView = (TextView)mActivity.findViewById(R.id.floating_actionbar_selection_mode_textview);
        mSelectionModeBackImageView = mActivity.findViewById(R.id.floating_actionbar_selection_mode_back_button);
        if (mSelectionModeBackImageView instanceof FlexImageView) {
            ((FlexImageView)mSelectionModeBackImageView).setOnRippleCompleteListener(mRippleButtonClickImpl);
        } else if (mSelectionModeBackImageView instanceof ImageView) {
            ((ImageView)mSelectionModeBackImageView).setOnClickListener(mButtonClickImpl);
        }
        mSelectionAllImgaeView = mActivity.findViewById(R.id.floating_actionbar_selection_mode_selectionall);
        if (mSelectionAllImgaeView instanceof FlexImageView) {
            ((FlexImageView)mSelectionAllImgaeView).setOnRippleCompleteListener(mRippleButtonClickImpl);
        } else if (mSelectionAllImgaeView instanceof ImageView) {
            ((ImageView)mSelectionAllImgaeView).setOnClickListener(mButtonClickImpl);
        }
    }

    public void setSelectionModeTitle(int titleStringId) {
        String title = mActivity.getResources().getString(titleStringId);
        setSelectionModeTitle(title);
    }

    public void setSelectionModeTitle(String title) {
        if (mSelectionModeTitleTextView != null) {
            mSelectionModeTitleTextView.setText(title);
        }
    }

    public void setSelectionAll(boolean selectAll) {
        if (selectAll) {
            setSelectionAllImageView(SelectionModeAll.SELECTION_MODE_ALL);
        } else {
            setSelectionAllImageView(SelectionModeAll.SELECTION_MODE_EMPTY);
        }
    }

    public void setSelectionAllImageView(SelectionModeAll selectAll) {
        mSelectionModeAll = selectAll;
        if (mSelectionModeAll == SelectionModeAll.SELECTION_MODE_EMPTY) {
            if (mSelectionAllImgaeView instanceof FlexImageView) {
                ((FlexImageView)mSelectionAllImgaeView).setImageResource(R.drawable.menu_select_all_un_press);
            } else if (mSelectionAllImgaeView instanceof ImageView) {
                ((ImageView)mSelectionAllImgaeView).setImageResource(R.drawable.menu_select_all_un_press);
            }
        } else if (mSelectionModeAll == SelectionModeAll.SELECTION_MODE_ALL) {
            if (mSelectionAllImgaeView instanceof FlexImageView) {
                ((FlexImageView)mSelectionAllImgaeView).setImageResource(R.drawable.menu_select_all_press);
            } else if (mSelectionAllImgaeView instanceof ImageView) {
                ((ImageView)mSelectionAllImgaeView).setImageResource(R.drawable.menu_select_all_press);
            }
        }
    }
}