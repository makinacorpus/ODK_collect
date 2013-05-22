/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.activities;

import java.util.ArrayList;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.listeners.DeleteInstancesListener;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.receivers.NetworkReceiver;
import org.odk.collect.android.tasks.DeleteInstancesTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores
 * the path to selected form for use by {@link MainMenuActivity}.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

public class InstanceUploaderList extends SherlockListActivity implements DeleteInstancesListener {

	private static final String t = "InstanceUploaderList";
	
	private static final String BUNDLE_SELECTED_ITEMS_KEY = "selected_items";
	private static final String BUNDLE_TOGGLED_KEY = "toggled";

	private static final int MENU_PREFERENCES = Menu.FIRST;
	private static final int INSTANCE_UPLOADER = 0;

	//private boolean mShowUnsent = true;
	private SimpleCursorAdapter mInstances;
	private ArrayList<Long> mSelected = new ArrayList<Long>();
	private boolean mRestored = false;
	private boolean mToggled = false;
	private AlertDialog mAlertDialog;
	DeleteInstancesTask mDeleteInstancesTask = null;

	public Cursor getAllCursor() {
		// get all complete or failed submission instances
		String selection = InstanceColumns.STATUS + "=? or "
				+ InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS
				+ "=?";
		String selectionArgs[] = { InstanceProviderAPI.STATUS_COMPLETE,
				InstanceProviderAPI.STATUS_SUBMISSION_FAILED,
				InstanceProviderAPI.STATUS_SUBMITTED };
		String sortOrder = InstanceColumns.DISPLAY_NAME + " ASC";
		Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection,
				selectionArgs, sortOrder);
		return c;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.instance_uploader_list);
		
		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);


		Cursor c = getAllCursor();

		String[] data = new String[] { InstanceColumns.DISPLAY_NAME,
				InstanceColumns.DISPLAY_SUBTEXT };
		int[] view = new int[] { R.id.text1, R.id.text2 };

		// render total instance view
		mInstances = new SimpleCursorAdapter(this,
				R.layout.two_item_multiple_choice, c, data, view);
		
		setListAdapter(mInstances);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		getListView().setItemsCanFocus(false);

		// set title
		setTitle(getString(R.string.send_data));

		// if current activity is being reinitialized due to changing
		// orientation restore all check
		// marks for ones selected
		if (mRestored) {
			ListView ls = getListView();
			for (long id : mSelected) {
				for (int pos = 0; pos < ls.getCount(); pos++) {
					if (id == ls.getItemIdAtPosition(pos)) {
						ls.setItemChecked(pos, true);
						break;
					}
				}

			}
			mRestored = false;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
	}

	private void uploadSelectedFiles() {
		// send list of _IDs.
		long[] instanceIDs = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++) {
			instanceIDs[i] = mSelected.get(i);
		}

		Intent i = new Intent(this, InstanceUploaderActivity.class);
		i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIDs);
		startActivityForResult(i, INSTANCE_UPLOADER);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Collect.getInstance().getActivityLogger()
				.logAction(this, "onCreateOptionsMenu", "show");
		super.onCreateOptionsMenu(menu);
		
		//creates the select all and upload buttons on the action bar from xml
		getSupportMenuInflater().inflate(R.menu.menu_instance_uploader, menu);
		
		//creates two entries in the overflow : settings and change display aspect
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			Collect.getInstance().getActivityLogger()
					.logAction(this, "onMenuItemSelected", "MENU_PREFERENCES");
			createPreferencesMenu();
			return true;
		
		case R.id.upload_instance:
			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

			if (NetworkReceiver.running == true) {
				//another upload is already running
				Toast.makeText(
						InstanceUploaderList.this,
						"Background send running, please try again shortly",
						Toast.LENGTH_SHORT).show();
			} else if (ni == null || !ni.isConnected()) {
				//no network connection
				Collect.getInstance().getActivityLogger()
						.logAction(this, "uploadButton", "noConnection");

				Toast.makeText(InstanceUploaderList.this,
						R.string.no_connection, Toast.LENGTH_SHORT).show();
			} else {
				Collect.getInstance()
						.getActivityLogger()
						.logAction(this, "uploadButton",
								Integer.toString(mSelected.size()));

				if (mSelected.size() > 0) {
					// items selected
					uploadSelectedFiles();
					mToggled = false;
					mSelected.clear();
					InstanceUploaderList.this.getListView().clearChoices();
				} else {
					// no items selected
					Toast.makeText(getApplicationContext(),
							getString(R.string.noselect_error),
							Toast.LENGTH_SHORT).show();
				}
			}

			return true;
		case R.id.select_all_instance:
			// toggle selections of items to all or none
			ListView ls = getListView();
			mToggled = !mToggled;

			Collect.getInstance()
					.getActivityLogger()
					.logAction(this, "toggleButton",
							Boolean.toString(mToggled));
			// remove all items from selected list
			mSelected.clear();
			for (int pos = 0; pos < ls.getCount(); pos++) {
				ls.setItemChecked(pos, mToggled);
				// add all items if mToggled sets to select all
				if (mToggled)
					mSelected.add(ls.getItemIdAtPosition(pos));
			}
			return true;
		case android.R.id.home:
	         // This is called when the Home (Up) button is pressed
	         // in the Action Bar.
	         Intent parentActivityIntent = new Intent(this, MainMenuActivity.class);
	         parentActivityIntent.addFlags(
	                 Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                 Intent.FLAG_ACTIVITY_NEW_TASK);
	         startActivity(parentActivityIntent);
	         finish();
	         return true;
		case R.id.delete_upload_instance :
			delete();
			return true;
		}
        return super.onOptionsItemSelected(item);
	}

	private void createPreferencesMenu() {
		Intent i = new Intent(this, PreferencesActivity.class);
		startActivity(i);
	}
	
	private void delete () {
		Collect.getInstance().getActivityLogger().logAction(this, "deleteButton", Integer.toString(mSelected.size()));
		if (mSelected.size() > 0) {
			createDeleteInstancesDialog();
		} else {
			Toast.makeText(getApplicationContext(),
					R.string.noselect_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void createDeleteInstancesDialog() {
        Collect.getInstance().getActivityLogger().logAction(this, "createDeleteInstancesDialog", "show");

		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setTitle(getString(R.string.delete_file));
		mAlertDialog.setMessage(getString(R.string.delete_confirm,
				mSelected.size()));
		DialogInterface.OnClickListener dialogYesNoListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON1: // delete
			    	Collect.getInstance().getActivityLogger().logAction(this, "createDeleteInstancesDialog", "delete");
					deleteSelectedInstances();
					break;
				case DialogInterface.BUTTON2: // do nothing
			    	Collect.getInstance().getActivityLogger().logAction(this, "createDeleteInstancesDialog", "cancel");
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.delete_yes),
				dialogYesNoListener);
		mAlertDialog.setButton2(getString(R.string.delete_no),
				dialogYesNoListener);
		mAlertDialog.show();
	}
	
	private void deleteSelectedInstances() {
		if (mDeleteInstancesTask == null) {
			mDeleteInstancesTask = new DeleteInstancesTask();
			mDeleteInstancesTask.setContentResolver(getContentResolver());
			mDeleteInstancesTask.setDeleteListener(this);
			mDeleteInstancesTask.execute(mSelected.toArray(new Long[mSelected
					.size()]));
		} else {
			Toast.makeText(this, getString(R.string.file_delete_in_progress),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// get row id from db
		Cursor c = (Cursor) getListAdapter().getItem(position);
		long k = c.getLong(c.getColumnIndex(BaseColumns._ID));

		Collect.getInstance().getActivityLogger()
				.logAction(this, "onListItemClick", Long.toString(k));

		// add/remove from selected list
		if (mSelected.contains(k))
			mSelected.remove(k);
		else
			mSelected.add(k);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		long[] selectedArray = savedInstanceState
				.getLongArray(BUNDLE_SELECTED_ITEMS_KEY);
		for (int i = 0; i < selectedArray.length; i++)
			mSelected.add(selectedArray[i]);
		mToggled = savedInstanceState.getBoolean(BUNDLE_TOGGLED_KEY);
		mRestored = true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		long[] selectedArray = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++)
			selectedArray[i] = mSelected.get(i);
		outState.putLongArray(BUNDLE_SELECTED_ITEMS_KEY, selectedArray);
		outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			return;
		}
		switch (requestCode) {
		// returns with a form path, start entry
		case INSTANCE_UPLOADER:
			if (intent.getBooleanExtra(FormEntryActivity.KEY_SUCCESS, false)) {
				mSelected.clear();
				getListView().clearChoices();
				if (mInstances.isEmpty()) {
					finish();
				}
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void deleteComplete(int deletedInstances) {
		Log.i(t, "Delete instances complete");
        Collect.getInstance().getActivityLogger().logAction(this, "deleteComplete", Integer.toString(deletedInstances));
		if (deletedInstances == mSelected.size()) {
			// all deletes were successful
			Toast.makeText(this,
					getString(R.string.file_deleted_ok, deletedInstances),
					Toast.LENGTH_SHORT).show();
		} else {
			// had some failures
			Log.e(t, "Failed to delete "
					+ (mSelected.size() - deletedInstances) + " instances");
			Toast.makeText(
					this,
					getString(R.string.file_deleted_error, mSelected.size()
							- deletedInstances, mSelected.size()),
					Toast.LENGTH_LONG).show();
		}
		mDeleteInstancesTask = null;
		mSelected.clear();
		getListView().clearChoices(); // doesn't unset the checkboxes
		for ( int i = 0 ; i < getListView().getCount() ; ++i ) {
			getListView().setItemChecked(i, false);
		}
	}

}
