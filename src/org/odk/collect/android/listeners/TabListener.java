package org.odk.collect.android.listeners;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;

import com.actionbarsherlock.app.SherlockListFragment;

public class TabListener<T extends Fragment> implements ActionBar.TabListener{
	  private SherlockListFragment mFragment;
	  private final Activity mActivity;
	  private final String mTag;
	  private final Class<T> mClass;

	  public TabListener(Activity activity, String tag, Class<T> clz) {
	    mActivity = activity;
	    mTag = tag;
	    mClass = clz;
	  }

	  public void onTabSelected(Tab tab, FragmentTransaction ft) {
	    // Check if the fragment is already initialized
	    if (mFragment == null) {
	      // If not, instantiate and add it to the activity
	      mFragment = (SherlockListFragment) Fragment.instantiate(
	                        mActivity, mClass.getName());
	      ft.add(android.R.id.tabcontent, mFragment, mTag);
	    } else {
	      // If it exists, simply attach it in order to show it
	      ft.attach(mFragment);
	    }

	  }

	  public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	    if (mFragment != null) {
	      // Detach the fragment, because another one is being attached
	      ft.detach(mFragment);
	    }
	  }

	  public void onTabReselected(Tab tab, FragmentTransaction ft) {
	    // User selected the already selected tab. Usually do nothing.
	  }
	}