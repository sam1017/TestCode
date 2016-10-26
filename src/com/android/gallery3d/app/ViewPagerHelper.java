package com.android.gallery3d.app;

import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.GLViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import com.android.gallery3d.common.Utils;

/**
 *
 *@author  Create by liangchangwei   
 *@data    2016年10月24日 --- 下午2:26:47
 *
 */
public class ViewPagerHelper {
    private static final String TAG = "ViewPagerHelper";
    private final GLViewPager mGLViewPager;

    public ViewPagerHelper(AbstractGalleryActivity activity) {
    	Log.w(TAG,"ViewPagerHelper");
        mGLViewPager = new GLViewPager(activity);
    }

    public GLViewPager getGlViewPager() {
        return mGLViewPager;
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mGLViewPager.setOnPageChangeListener(listener);
    }

    public void setContentPane(int tabIndex, GLView content) {
        mGLViewPager.setContentPane(tabIndex, content);
    }

    public void setCurrentTabIndex(int tabIndex) {
        Utils.assertTrue(tabIndex >= 0 && tabIndex < GLViewPager.TAB_COUNT,
                "tabIndex is %s not in [0,%s]", tabIndex, GLViewPager.TAB_COUNT);
        mGLViewPager.setCurrentTabIndex(tabIndex);
    }

    public int getCurrentTabIndex(){
        return mGLViewPager.getCurrentTabIndex();
    }

    public void setHorizontalEnable(boolean enable) {
        mGLViewPager.setHorizontalEnable(enable);
    }

    public void switchTo(int newIndex) {
        mGLViewPager.switchTo(newIndex);
    }
}
