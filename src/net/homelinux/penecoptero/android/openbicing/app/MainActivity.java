/*
 * Copyright (C) 2010 Lluís Esquerda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.homelinux.penecoptero.android.openbicing.app;

import java.util.Calendar;
import java.util.List;

import net.homelinux.penecoptero.android.openbicing.utils.CircleHelper;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class MainActivity extends MapActivity {

	private MapView mapView;
	public static final int MENU_ITEM_SYNC = Menu.FIRST;
	public static final int MENU_ITEM_LOCATION = Menu.FIRST + 1;
	public static final int MENU_ITEM_WHATEVER = Menu.FIRST + 2;
	public static final int MENU_ITEM_LIST = Menu.FIRST + 3;
	public static final int MENU_ITEM_SETTINGS = Menu.FIRST + 4;
	public static final int MENU_ITEM_HELP = Menu.FIRST + 5;
	public static final int KEY_LAT = 0;
	public static final int KEY_LNG = 1;
	public static final int SETTINGS_ACTIVITY = 0;
	
	
	private StationOverlayList stations;
	private StationsDBAdapter mDbHelper;
	private InfoLayer infoLayer;
	private boolean view_all = false;
	private HomeOverlay hOverlay;
	private ProgressDialog progressDialog;
	private StationSlidingDrawer mSlidingDrawer;
	private ToggleButton modeButton;
	
	private SharedPreferences settings;

	private Handler infoLayerPopulator;
	
	private boolean getBike = true;
	
	private float scale;
	
	private int zoom = -1;
	
	private Locator locator;
	
	private boolean onC2DMTourMode = false;
	
	private int selected_id = -1;
	
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null){
			selected_id = -1;
		} else {
			selected_id = savedInstanceState.getInt("c2dm_station_id");
		}
		
		if (selected_id == -1){
			selected_id = getIntent().getIntExtra("c2dm_station_id", -1);
		}
		
		Log.i("CityBikes","I should be centering station "+Integer.toString(selected_id));
		
		
		
		setContentView(R.layout.main);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		mapView = (MapView) findViewById(R.id.mapview);
		mSlidingDrawer = (StationSlidingDrawer) findViewById(R.id.drawer);
		infoLayer = (InfoLayer) findViewById(R.id.info_layer);
		scale = getResources().getDisplayMetrics().density;
		//Log.i("CityBikes","ON CREATEEEEEEEEE!!!!!");
		infoLayerPopulator = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == InfoLayer.POPULATE) {
					infoLayer.inflateStation(stations.getCurrent());
				}
				if (msg.what == OpenBicing.BOOKMARK_CHANGE){
					int id = msg.arg1;
					boolean bookmarked;
					if (msg.arg2 == 0){
						bookmarked = false;
					} else{
						bookmarked = true;
					}
					StationOverlay station = stations.getById(id);
					try{
						BookmarkManager bm = new BookmarkManager(getApplicationContext());
						bm.setBookmarked(station.getStation(), !bookmarked);
					}catch (Exception e){
						Log.i("CityBikes","Error bookmarking station");
						e.printStackTrace();
					}
					
					if (!view_all) {
						view_near();
					}
					mapView.postInvalidate();
				}
			}
		};
		
		infoLayer.setHandler(infoLayerPopulator);
		RelativeLayout.LayoutParams zoomControlsLayoutParams = new RelativeLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		zoomControlsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		zoomControlsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		
		
		mapView.addView(mapView.getZoomControls(), zoomControlsLayoutParams);
		
		modeButton = (ToggleButton) findViewById(R.id.mode_button);
		
		
		modeButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				changeMode(!getBike);
			}
			
		});
		
		applyMapViewLongPressListener(mapView);
		
		settings = getSharedPreferences(OpenBicing.PREFERENCES_NAME,0);
		
		List<Overlay> mapOverlays = mapView.getOverlays();
		
		locator = new Locator(this, new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == Locator.LOCATION_CHANGED) {
					GeoPoint point = new GeoPoint(msg.arg1, msg.arg2);
					hOverlay.moveCenter(point);
					mapView.getController().animateTo(point);
					mDbHelper.setCenter(point);
					// Location has changed
					try {
							mDbHelper.updateDistances(point);
							infoLayer.update();
							if (!view_all) {
								view_near();
							}
					} catch (Exception e) {

					};
				}
			}
		});
		
		hOverlay = new HomeOverlay(locator.getCurrentGeoPoint(),new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == HomeOverlay.MOTION_CIRCLE_STOP){
					Log.i("CityBikes","MOTION CIRCLE STOP");
					try {
						if (!view_all) {
							view_near();
						}
						mapView.postInvalidate();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		stations = new StationOverlayList(mapOverlays, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == StationOverlay.TOUCHED && msg.arg1 != -1) {
					// One station has been touched
					stations.setCurrent(msg.arg1, getBike);
					infoLayer.inflateStation(stations.getCurrent());
				}
			}
		});
		
		stations.addOverlay(hOverlay);
		
		mDbHelper = new StationsDBAdapter(this, mapView, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case StationsDBAdapter.FETCH:
					break;
				case StationsDBAdapter.UPDATE_MAP:
					progressDialog.dismiss();
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("reload_network", false);
					editor.commit();
					if (selected_id != -1){
						selectStation(selected_id, false);
					}
					StationOverlay current = stations.getCurrent();
					if (current == null) {
						infoLayer
								.inflateMessage(getString(R.string.no_bikes_around));
					}
					if (current != null) {
						current.setSelected(true,getBike);
						infoLayer.inflateStation(current);
						if (view_all)
							view_all();
						else
							view_near();
					} else {
						
					}
					mapView.invalidate();
					tourC2DM();
					break;
				case StationsDBAdapter.UPDATE_DATABASE:
					
					////Log.i("openBicing", "Database updated");
					break;
				case StationsDBAdapter.NETWORK_ERROR:
					////Log.i("openBicing", "Network error, last update from " + mDbHelper.getLastUpdated());
					Toast toast = Toast.makeText(getApplicationContext(),
							getString(R.string.network_error)
									+ " " + mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
					break;
				}
			}
		}, stations);

		mDbHelper.setCenter(locator.getCurrentGeoPoint());

		mSlidingDrawer.setHandler(new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what){
				case StationSlidingDrawer.ITEMCLICKED:
					StationOverlay clicked = (StationOverlay) msg.obj;
					stations.setCurrent(msg.arg1, getBike);
					Message tmp = new Message();
					tmp.what = InfoLayer.POPULATE;
					tmp.arg1 = msg.arg1;
					infoLayerPopulator.dispatchMessage(tmp);
					mapView.getController().animateTo(
					clicked.getCenter());
				}
			}
		});
		
		if (savedInstanceState != null) {
			locator.unlockCenter();
			hOverlay.setRadius(
					savedInstanceState.getInt("homeRadius"));
			this.view_all = savedInstanceState.getBoolean("view_all");
		} else {
			updateHome();
		}
		
		try {
			mDbHelper.loadStations();
			if (savedInstanceState == null) {
				String strUpdated = mDbHelper.getLastUpdated();
				
				Boolean dirty = settings.getBoolean("reload_network",false);
				
				if (strUpdated == null || dirty) {
					this.fillData(view_all);
				} else {
					Toast toast = Toast.makeText(this.getApplicationContext(),
							"Last Updated: " + mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
					Calendar cal = Calendar.getInstance();
					long now = cal.getTime().getTime();
					
					if (selected_id != -1){
						selectStation(selected_id, true);
					}
					
					if (selected_id != -1 || Math.abs(now - mDbHelper.getLastUpdatedTime()) > 60000 * 5)
						this.fillData(view_all);
				}
			}

		} catch (Exception e) {
		
		};

		if (view_all)
			view_all();
		else
			view_near();
		
	}
	
	private void showC2DMTour(){
		this.onC2DMTourMode = true;
		//Toast toastie = Toast.makeText(this,,Toast.LENGTH_LONG);
		//toastie.show();
		OpenBicing.showCustomToast(this.getApplicationContext(), this, getText(R.string.c2dm_tour_start), Toast.LENGTH_LONG, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
		infoLayer.setOnC2DMTour(true);
		
	}
	
	private boolean isFirstTimeC2DM(){
		SharedPreferences settings = getApplicationContext().getSharedPreferences(OpenBicing.PREFERENCES_NAME,0);
		boolean firstTime = settings.getBoolean("first_time_c2dm", true);
		return firstTime;
	}
	private void saveFirstTimeC2DM(){
		SharedPreferences settings = getApplicationContext().getSharedPreferences(OpenBicing.PREFERENCES_NAME,0);
		Editor editor = settings.edit();
		editor.putBoolean("first_time_c2dm", false);
		editor.commit();

	}
	
	protected void applyMapViewLongPressListener(MapView mapView) {
		final MapView finalMapView = mapView;

		        final GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener(){
		                @Override
		                public void onLongPress(MotionEvent e) {
		                        //Log.i("CityBikes","LONG PRESS!");
		                        Projection astral = finalMapView.getProjection();
		                        GeoPoint center = astral.fromPixels((int) e.getX(),(int) e.getY());
		                        locator.lockCenter(center);
		                }

						@Override
						public boolean onDoubleTap(MotionEvent e) {
							// TODO Auto-generated method stub
							 //Log.i("CityBikes","Double tap!");
		                        Projection astral = finalMapView.getProjection();
		                        GeoPoint center = astral.fromPixels((int) e.getX(),(int) e.getY());
		                        locator.lockCenter(center);
							return super.onDoubleTap(e);
						}
		                
		        });
		        mapView.setOnTouchListener(new OnTouchListener(){
		                @Override
		                public boolean onTouch(View v, MotionEvent ev) {
		                        return gd.onTouchEvent(ev);
		                }
		        });
		}

	private void fillData(boolean all) {

			try{
				selected_id = stations.getCurrent().getStation().getId();
			} catch (Exception e){
				
			}

			Bundle data = new Bundle();
			if (!all) {
				GeoPoint center = locator.getCurrentGeoPoint();
				if (center == null){
						// Barcelona lat/lng 41.3880, 2.1700
						double lat = 41.3880;
						double lng = 2.1700;
						Location fallback = new Location("fallback");
						fallback.setLatitude(lat);
						fallback.setLongitude(lng);
						locator.setFallbackLocation(fallback);
						locator.unlockCenter();
						center = locator.getCurrentGeoPoint();
				}
				data.putInt(StationsDBAdapter.CENTER_LAT_KEY, center
						.getLatitudeE6());
				data.putInt(StationsDBAdapter.CENTER_LNG_KEY, center
						.getLongitudeE6());
				data.putInt(StationsDBAdapter.RADIUS_KEY, hOverlay.getRadius());
			}
			
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle("");
			progressDialog.setMessage(getString(R.string.loading));
			progressDialog.show();
			try {
				mDbHelper.sync(all, data);
			} catch (Exception e) {
				e.printStackTrace();
				progressDialog.dismiss();
			};
	}
	
	public void changeMode(boolean getBike){
		this.getBike = getBike;
		mDbHelper.changeMode(this.getBike);
		this.populateList(this.view_all);
		infoLayer.update();
		mapView.invalidate();
		String text;
		
		if (getBike){
			text = getString(R.string.get_bike_mode);
		} else {
			text = getString(R.string.park_bike_mode);
		}
		OpenBicing.showCustomToast(this.getApplicationContext(), this, text, Toast.LENGTH_SHORT);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isOnGetMode(){
		return this.getBike;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync).setIcon(
				R.drawable.ic_menu_refresh);
		menu.add(0, MENU_ITEM_LOCATION, 0, R.string.menu_location).setIcon(
				android.R.drawable.ic_menu_mylocation);
		menu.add(0, MENU_ITEM_WHATEVER, 0, R.string.menu_view_all).setIcon(
				android.R.drawable.checkbox_off_background);
		menu.add(0, MENU_ITEM_SETTINGS, 0, R.string.menu_settings).setIcon(
				android.R.drawable.ic_menu_preferences);
		return true;
	}

	public void updateHome() {
		try {
			locator.unlockCenter();
			mapView.getController().animateTo(locator.getCurrentGeoPoint());
		} catch (Exception e) {
			//Log.i("CityBikes", "center is null..");
			
		}
		if (zoom == -1){
			zoom = 16;
			mapView.getController().setZoom(zoom);
		}
	}
	
	public void selectStation(int id, boolean should_find){
		Log.i("CityBikes","Selecting a station automatically! "+Integer.toString(id));
		StationOverlay station = stations.getById(id);
		
		if (station != null){
				Log.i("CityBikes","Found the station: "+station.getStation().getName());
				stations.setCurrent(station.getStation().getId(), getBike);
				Message tmp = new Message();
				tmp.what = InfoLayer.POPULATE;
				tmp.arg1 = station.getStation().getId();
				infoLayerPopulator.dispatchMessage(tmp);
				
		} else if (should_find) {
			
				// Check if we are in view near mode (then, we should
				// make the radius bigger, or just put the app in
				// view all mode to center it. If we are already in
				// view all mode, just report the error, or fuck it
				station = mDbHelper.getStationFromAll(id);
				if (station == null)
					Log.i("CityBikes","I don't know about this station, fuck you");
				else{
					Log.i("CityBikes","Station is not on radius, trying a guess..");
					Log.i("CityBikes","It might be at.. "+Integer.toString((int) station.getStation().getMetersDistance())+" m");
					hOverlay.setRadius((int) station.getStation().getMetersDistance()+500);
					selectStation(id, false);
				}
		}
	}

	public void view_all() {
		try {
			Log.i("CityBikes","Viewing all");
			mDbHelper.populateStations();
			populateList(true);
		} catch (Exception e) {

		};
	}

	public void view_near() {
		try {
			Log.i("CityBikes","Viewing near");
			mDbHelper.populateStations(locator.getCurrentGeoPoint(), hOverlay.getRadius());
			populateList(false);
			
			if (!infoLayer.isPopulated()) {
				StationOverlay current = stations.getCurrent();
				if (current != null) {
					infoLayer.inflateStation(current);
					current.setSelected(true, this.getBike);
				} else {
					infoLayer.inflateMessage(getString(R.string.no_bikes_around));
				}
			}
		} catch (Exception e) {

		};
	}

	public void tourC2DM(){
		Log.i("CityBikes","Starting new feature showroom!");
		if (OpenBicing.isC2DMReady(this) && isFirstTimeC2DM()){
			saveFirstTimeC2DM();
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.setTitle(this.getString(R.string.new_feature));
			alertDialog.setMessage(this.getString(R.string.c2dm_new_feature));
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,this.getString(R.string.sure), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showC2DMTour();
				}
			});
			
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,this.getString(R.string.nope), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.i("C2DM","Dismissing this shit!");
				}
			});
			
			alertDialog.show();

		}
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_SYNC:
			try {
				this.fillData(view_all);
			} catch (Exception e) {

			}
			;
			return true;
		case MENU_ITEM_LOCATION:
			updateHome();
			return true;
		case MENU_ITEM_WHATEVER:
			if (!view_all) {
				item.setIcon(android.R.drawable.checkbox_on_background);
				view_all();
			} else {
				item.setIcon(android.R.drawable.checkbox_off_background);
				view_near();
			}
			view_all = !view_all;
			return true;
		case MENU_ITEM_SETTINGS:
			this
					.startActivityForResult(new Intent(this,
							SettingsActivity.class), SETTINGS_ACTIVITY);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		////Log.i("openBicing", "RESUME!");
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		////Log.i("openBicing", "SaveInstanceState!");
		outState.putInt("homeRadius", hOverlay.getRadius());
		outState.putBoolean("view_all", view_all);
		outState.putInt("zoom", mapView.getZoomLevel());
	}

	@Override
	protected void onPause() {
		super.onPause();
		////Log.i("openBicing", "PAUSE!");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		////Log.i("openBicing", "DESTROY!");

	}

	@Override
	protected void onStop() {
		super.onStop();
		////Log.i("openBicing", "STOP!");
		locator.stopUpdates();
		if (this.isFinishing())
			this.finish();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.i("CityBikes", "Activity Result");
		if (requestCode == SETTINGS_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				locator.restartUpdates();
			}
		}

		Boolean dirty = settings.getBoolean("reload_network",false);
		if (dirty){
			this.fillData(view_all);	
		}
	}

	public void populateList(boolean all) {
		try {
			List sts;
			if (all) {
				sts = mDbHelper.getMemory();
			} else {
				sts = mDbHelper.getMemory(hOverlay.getRadius());
			}

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int height = dm.heightPixels;
				int calc = (sts.size() * CircleHelper.dip2px(50, scale) + CircleHelper.dip2px(45, scale));
				if (calc > height - CircleHelper.dip2px(145, scale))
					calc = height - CircleHelper.dip2px(145, scale);
				else if (sts.size() == 0)
					calc = 0;
			
			mSlidingDrawer.setStations(sts);
			mSlidingDrawer.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT, calc));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
