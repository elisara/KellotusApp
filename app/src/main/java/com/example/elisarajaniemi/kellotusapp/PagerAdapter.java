
package com.example.elisarajaniemi.kellotusapp;

/**
 * Created by Kade on 27.9.2016.
 */

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;

public class PagerAdapter extends FragmentStatePagerAdapter  {
    final int mNumOfTabs = 2;
    private String tabTitles[] = new String[]{"KELLOTUS","RESULTS"};

    public PagerAdapter(FragmentManager fm) {
        super(fm);

    }

    @Override
    public int getItemPosition(Object object){
        return POSITION_NONE;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                KellotusFragment kellotus = new KellotusFragment();
                return kellotus;
            case 1:
                ResultsFragment results = new ResultsFragment();
                return results;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
    public CharSequence getPageTitle(int position){
        return tabTitles[position];
    }

}