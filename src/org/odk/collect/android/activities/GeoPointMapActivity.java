/*
 * Copyright (C) 2011 University of Washington
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

import java.text.DecimalFormat;
import java.util.List;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.utilities.InfoLogger;
import org.odk.collect.android.widgets.GeoPointWidget;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GeoPointMapActivity extends FragmentActivity implements
		LocationListener, OnMarkerDragListener, OnMapLongClickListener {

	private static final String LOCATION_COUNT = "locationCount";

	private TextView mLocationStatus;

	private LocationManager mLocationManager;
	private GoogleMap mMap;
	private MarkerOptions mMarkerOption;
	private Marker mMarker = null;

	private Location mLocation;
	private Button mAcceptLocation;
	private Button mCancelLocation;
	private Button mRefreshLocation;
	private LatLng mLatLng;

	private boolean mCaptureLocation = true;
	private boolean isDragged = false;
	private Button mShowLocation;

	private boolean mGPSOn = false;
	private boolean mNetworkOn = false;

	private double mLocationAccuracy;
	private int mLocationCount = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(getClass().getName(), "onCreate()");

		if (savedInstanceState != null) {
			Log.i(getClass().getName(), "onCreate() : savedInstance not null");
			mLocationCount = savedInstanceState.getInt(LOCATION_COUNT);
		}

		boolean withLoc = true;

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.geopoint_layout);

		Intent intent = getIntent();

		mLocationAccuracy = GeoPointWidget.DEFAULT_LOCATION_ACCURACY;

		mMarkerOption = new MarkerOptions();

		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		mMap.setOnMarkerDragListener(this);
		
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(43.2551, 5.4681),
				10));

		if (intent != null && intent.getExtras() != null) {

			// No geolocation
			if (intent.hasExtra("noGPS")) {
				Log.i(getClass().getName(), "onCreate() : noGPS");
				withLoc = false;
				mMap.setOnMapLongClickListener(this);
				mCaptureLocation = false;
				Toast.makeText(getApplicationContext(), R.string.marker_create,
						Toast.LENGTH_LONG).show();
			}

			// Show previous location
			if (intent.hasExtra(GeoPointWidget.LOCATION)) {
				Log.i(getClass().getName(),
						"onCreate() : previous location saved");
				double[] location = intent
						.getDoubleArrayExtra(GeoPointWidget.LOCATION);
				mLatLng = new LatLng(location[0], location[1]);
				mMarkerOption.position(mLatLng);
				mMarker = mMap.addMarker(mMarkerOption);
				mMarker.setDraggable(true);
				if (withLoc) {
					Log.i(getClass().getName(),
							"onCreate() : previous location saved and geoloc allowed");
					Toast.makeText(getApplicationContext(),
							R.string.marker_draggable, Toast.LENGTH_LONG)
							.show();
				}
				mCaptureLocation = false;
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						16));
			}

			if (intent.hasExtra(GeoPointWidget.ACCURACY_THRESHOLD)) {
				mLocationAccuracy = intent.getDoubleExtra(
						GeoPointWidget.ACCURACY_THRESHOLD,
						GeoPointWidget.DEFAULT_LOCATION_ACCURACY);
			}
		}

		// Use providers only if we want geolocation
		if (mCaptureLocation || withLoc) {
			Log.i(getClass().getName(), "onCreate() : finding providers");

			// make sure we have a good location provider before continuing

			mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

			List<String> providers = mLocationManager.getProviders(true);
			for (String provider : providers) {
				if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
					mGPSOn = true;
					Log.i(getClass().getName(), "onCreate() : useGPS");
				}
				if (provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
					mNetworkOn = true;
					Log.i(getClass().getName(), "onCreate() : useNetwork");
				}
			}
			if (!mGPSOn && !mNetworkOn) {
				Toast.makeText(getBaseContext(),
						getString(R.string.provider_disabled_error),
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}

		mCancelLocation = (Button) findViewById(R.id.cancel_location);
		mCancelLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "cancelLocation", "cancel");
				finish();
			}
		});

		mAcceptLocation = (Button) findViewById(R.id.accept_location);
		mAcceptLocation.setVisibility(View.VISIBLE);
		mAcceptLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "acceptLocation", "OK");
				returnResult();
			}
		});

		if (mGPSOn) {
			Location loc = mLocationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (loc != null) {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(GPS) lat: " + loc.getLatitude()
						+ " long: " + loc.getLongitude() + " acc: "
						+ loc.getAccuracy());
			} else {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(GPS) null location");
			}
		}

		if (mNetworkOn) {
			Location loc = mLocationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc != null) {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(Network) lat: "
						+ loc.getLatitude() + " long: " + loc.getLongitude()
						+ " acc: " + loc.getAccuracy());
			} else {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(Network) null location");
			}
		}

		mShowLocation = ((Button) findViewById(R.id.show_location));
		mShowLocation.setVisibility(View.VISIBLE);
		mShowLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "showLocation", "onClick");
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						16));
			}
		});
		mShowLocation.setClickable(mMarker != null);

		if (mCaptureLocation) {
			// Case where we show the current location according to the provider
			Log.i(getClass().getName(), "onCreate() : refresh button added");
			mLocationStatus = (TextView) findViewById(R.id.location_status);
			mRefreshLocation = ((Button) findViewById(R.id.refresh_location));
			mRefreshLocation.setVisibility(View.VISIBLE);

			mRefreshLocation.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Collect.getInstance()
							.getActivityLogger()
							.logInstanceAction(this, "refreshLocation",
									"onClick");
					onResume();
					mMarker.setDraggable(false);
				}
			});
			mRefreshLocation.setClickable(false);

		} else {
			// Case where we only show the saved location
			Log.i(getClass().getName(),
					"onCreate() : show location without geolocation");
			((TextView) findViewById(R.id.location_status))
					.setVisibility(View.GONE);
			mAcceptLocation.setClickable(false);

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

	private void returnLocation() {
		if (mLocation != null) {
			Log.i(getClass().getName(),
					"returnLocation Lat : " + mLocation.getLatitude()
							+ " Long : " + mLocation.getLongitude() + " Alt : "
							+ mLocation.getAltitude() + " Acc : "
							+ mLocation.getAccuracy());
			Intent i = new Intent();
			i.putExtra(
					FormEntryActivity.LOCATION_RESULT,
					mLocation.getLatitude() + " " + mLocation.getLongitude()
							+ " " + mLocation.getAltitude() + " "
							+ mLocation.getAccuracy());
			setResult(RESULT_OK, i);
		}
		finish();
	}

	private void returnDragLocation() {
		Log.i(getClass().getName(), "returnDragLocation Lat : "
				+ mLatLng.latitude + " Long : " + mLatLng.longitude + " Alt : "
				+ 0 + " Acc : " + 0);
		Intent i = new Intent();
		i.putExtra(FormEntryActivity.LOCATION_RESULT, mLatLng.latitude + " "
				+ mLatLng.longitude + " " + 0 + " " + 0);
		setResult(RESULT_OK, i);
		finish();
	}

	private String truncateFloat(float f) {
		return new DecimalFormat("#.##").format(f);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(mLocationManager != null){
			mLocationManager.removeUpdates(this);
		}
		Log.i(getClass().getName(), "onPause()");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(getClass().getName(), "onResume()");
		if (mGPSOn) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		if (mNetworkOn) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, this);
		}

	}

	@Override
	public void onLocationChanged(Location location) {
		if (mCaptureLocation) {
			mLocation = location;
			if (mLocation != null) {
				// Bug report: cached GeoPoint is being returned as the first
				// value.
				// Wait for the 2nd value to be returned, which is hopefully not
				// cached?
				++mLocationCount;
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis() + " onLocationChanged("
						+ mLocationCount + ") lat: " + mLocation.getLatitude()
						+ " long: " + mLocation.getLongitude() + " acc: "
						+ mLocation.getAccuracy());
				if (mLocationCount > 1) {
					Log.i(getClass().getName(), "onLocationChanged mLocationCount : "+mLocationCount+" lat : "+mLocation.getLatitude()+ "long : "+mLocation.getLongitude());
					mLocationStatus.setText(getString(
							R.string.location_provider_accuracy,
							mLocation.getProvider(),
							truncateFloat(mLocation.getAccuracy())));
					mLatLng = new LatLng(mLocation.getLatitude(),
							mLocation.getLongitude());
					mMap.animateCamera(CameraUpdateFactory.newLatLng(mLatLng));

					// TODO choose a relevant accuracy
					// if (mLocation.getAccuracy() <= mLocationAccuracy) {
					if (mLocation.getAccuracy() <= 35) {
						Log.i(getClass().getName(), "onLocationChanged accuracy <= 35");
						Toast.makeText(getApplicationContext(),
								R.string.marker_draggable, Toast.LENGTH_LONG)
								.show();
						mLocationManager.removeUpdates(this);
						mRefreshLocation.setClickable(true);
						mMarker.setDraggable(true);
						mLatLng = new LatLng(mLocation.getLatitude(),
								mLocation.getLongitude());
					}
				}
				mLatLng = new LatLng(mLocation.getLatitude(),
						mLocation.getLongitude());
				if (mMarker == null) {
					Log.i(getClass().getName(), "onLocationChanged : init marker on map");
					mMarkerOption.position(mLatLng);
					mMarker = mMap.addMarker(mMarkerOption);
					mShowLocation.setClickable(true);
				} else {
					Log.i(getClass().getName(), "onLocationChanged : update marker on map");
					mMarker.setPosition(mLatLng);
				}
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						16));
			} else {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis() + " onLocationChanged("
						+ mLocationCount + ") null location");
			}
		}else{
			Log.w(getClass().getName(), "onLocationChanged() : mCaptureLocation is "+mCaptureLocation);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.i(getClass().getName(), "Provider : " + provider);
		if ((provider != LocationManager.GPS_PROVIDER)
				&& (provider != LocationManager.NETWORK_PROVIDER)) {
			Log.i(getClass().getName(), "Provider (neither GPS or network : " + provider);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onMarkerDrag(Marker marker) {

	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		Log.i(getClass().getName(), "onMarkerDragEnd()");
		mLatLng = marker.getPosition();
		mAcceptLocation.setClickable(true);
		isDragged = true;
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
	}

	@Override
	public void onMarkerDragStart(Marker marker) {

	}

	private void returnResult() {
		Log.i(getClass().getName(), "returnResult()");
		if (isDragged) {
			returnDragLocation();
		} else {
			returnLocation();
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {
		Log.i(getClass().getName(), "onMapLongClick");
		mMarkerOption.position(point);
		mLatLng = point;
		if (mMarker != null) {
			mMarker.remove();
		} else {
			Toast.makeText(getApplicationContext(), R.string.marker_draggable,
					Toast.LENGTH_LONG).show();
		}
		mMarker = mMap.addMarker(mMarkerOption);
		mMarker.setDraggable(true);
		mAcceptLocation.setClickable(true);
		mShowLocation.setClickable(true);
		isDragged = true;
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
	}

}
