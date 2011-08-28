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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
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
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			}
		};

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
				//Log.i("CityBikes","Message: "+Integer.toString(msg.what)+" "+Integer.toString(msg.arg1));
				if (msg.what == StationOverlay.TOUCHED && msg.arg1 != -1) {
					// One station has been touched
					stations.setCurrent(msg.arg1, getBike);
					infoLayer.inflateStation(stations.getCurrent());
					//Log.i("CityBikes","Station touched: "+Integer.toString(msg.arg1));
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
					break;
				case StationsDBAdapter.UPDATE_DATABASE:
					
					////Log.i("openBicing", "Database updated");
					break;
				case StationsDBAdapter.NETWORK_ERROR:
					////Log.i("openBicing", "Network error, last update from " + mDbHelper.getLastUpdated());
					Toast toast = Toast.makeText(getApplicationContext(),
							getString(R.string.network_error)
									+ mDbHelper.getLastUpdated(),
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
					if (Math.abs(now - mDbHelper.getLastUpdatedTime()) > 60000 * 5)
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
			Bundle data = new Bundle();
			if (!all) {
				GeoPoint center = locator.getCurrentGeoPoint();
				if (center == null){
						//Log.i("CityBikes",network.toString());
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

	public void view_all() {
		try {
			mDbHelper.populateStations();
			populateList(true);
		} catch (Exception e) {

		};
	}

	public void view_near() {
		try {
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
