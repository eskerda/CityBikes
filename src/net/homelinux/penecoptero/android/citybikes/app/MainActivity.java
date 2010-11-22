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

package net.homelinux.penecoptero.android.citybikes.app;

import java.util.List;

import net.homelinux.penecoptero.android.citybikes.utils.CircleHelper;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
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
	private FrameLayout fl;
	private SlidingDrawer mSlidingDrawer;
	private ToggleButton modeButton;
	
	private SharedPreferences settings;
	private NetworksDBAdapter mNDBAdapter;

	private Handler infoLayerPopulator;

	private int green, red, yellow;
	
	private boolean getBike = true;
	
	private float scale;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		mapView = (MapView) findViewById(R.id.mapview);
		fl = (FrameLayout) findViewById(R.id.content);
		mSlidingDrawer = (SlidingDrawer) findViewById(R.id.drawer);
		infoLayer = (InfoLayer) findViewById(R.id.info_layer);
		
		scale = getResources().getDisplayMetrics().density;
		//Log.i("CityBikes","ON CREATEEEEEEEEE!!!!!");
		infoLayerPopulator = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == InfoLayer.POPULATE) {
					infoLayer.inflateStation(stations.get(msg.arg1));
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
		
		settings = getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
		
		List<Overlay> mapOverlays = mapView.getOverlays();
		
		
		stations = new StationOverlayList(this, mapOverlays, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == hOverlay.MOTION_CIRCLE_STOP && !view_all) {
					// Home Circle has changed its radius
					try {
						view_near();
					} catch (Exception e) {

					}
				} else if (msg.what == StationOverlay.TOUCHED && msg.arg1 != -1) {
					// One station has been touched
					stations.setCurrent(msg.arg1, getBike);
					infoLayer.inflateStation(stations.getCurrent());
				} else if (msg.what == hOverlay.LOCATION_CHANGED) {
					// Location has changed
					mDbHelper.setCenter(hOverlay.getPoint());
					
						try {
							mDbHelper.updateDistances(hOverlay.getPoint());
							infoLayer.update();

							if (view_all) {
								view_all();
							} else {
								view_near();
							}
						} catch (Exception e) {

						}
						;	
					
					
				}
			}
		});
		mNDBAdapter = new NetworksDBAdapter(getApplicationContext());
		
		mDbHelper = new StationsDBAdapter(this, mapView, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case StationsDBAdapter.FETCH:
					//////Log.i("openBicing", "Data fetched");
					break;
				case StationsDBAdapter.UPDATE_MAP:
					//////Log.i("openBicing", "Map Updated");
					progressDialog.dismiss();
					SharedPreferences settings = getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("reload_network", false);
					editor.commit();
					StationOverlay current = stations.getCurrent();
					if (current == null) {
						infoLayer
								.inflateMessage(getString(R.string.no_bikes_around));
					}
					if (current != null) {
						current.setSelected(true, getBike);
						infoLayer.inflateStation(current);
						if (view_all)
							view_all();
						else
							view_near();
					} else {
						////Log.i("openBicing", "Error getting an station..");
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

		mDbHelper.setCenter(stations.getHome().getPoint());

		if (savedInstanceState != null) {
			stations.updateHome();
			stations.getHome().setRadius(
					savedInstanceState.getInt("homeRadius"));
			this.view_all = savedInstanceState.getBoolean("view_all");
		} else {
			updateHome();
		}

		try {
			mDbHelper.loadStations();
			if (savedInstanceState == null) {
				String strUpdated = mDbHelper.getLastUpdated();
				
				SharedPreferences settings = getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
				Boolean dirty = settings.getBoolean("reload_network",false);
				
				if (strUpdated == null || dirty) {
					this.fillData(view_all);
				} else {
					Toast toast = Toast.makeText(this.getApplicationContext(),
							"Last Updated: " + mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
				}
			}

		} catch (Exception e) {
			////Log.i("openBicing", "SHIT ... SUCKS");
		}
		;

		if (view_all)
			view_all();
		else
			view_near();
		hOverlay = stations.getHome();
		////Log.i("openBicing", "CREATE!");
	}
	
	private void showBikeNetworks(){
		this.startActivityForResult(new Intent(this,
				BikeNetworkActivity.class), SETTINGS_ACTIVITY);
	}
	
	private void showAutoNetworkDialog(int method){
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setIcon(android.R.drawable.ic_dialog_map);
		final int mth = method;
		try {
			mNDBAdapter.update();
			final JSONObject network = mNDBAdapter.getAutomaticNetwork(hOverlay.getPoint(),method);
			alertDialog.setTitle(R.string.bike_network_alert_success_title);
			alertDialog.setMessage(getString(R.string.bike_network_alert_success_text0)+":\n- ("+network.getString("city")+") "+network.getString("name")+"\n"+getString(R.string.bike_network_alert_success_text1));
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.sure), new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						mNDBAdapter.setManualNetwork(network.getInt("id"));
						fillData(view_all);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			});
			alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,getString(R.string.try_again), new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mth == 0)
						showAutoNetworkDialog(1);
					else
						showAutoNetworkDialog(0);
				}
				
			});
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.manual), new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					showBikeNetworks();
				}
				
			});
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			alertDialog.setTitle(R.string.bike_network_alert_error_title);
			
			alertDialog.setMessage(getString(R.string.bike_network_alert_error_text));
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.try_again), new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mth == 0)
						showAutoNetworkDialog(1);
					else
						showAutoNetworkDialog(0);
					
				}
				
			});
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.manual), new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					showBikeNetworks();
				}
				
			});
		}
		alertDialog.show();
		
	}

	private void fillData(boolean all) {
		
		if (mNDBAdapter.isConfigured()){
			Bundle data = new Bundle();
			if (!all) {
				GeoPoint center = stations.getHome().getPoint();
				data.putInt(StationsDBAdapter.CENTER_LAT_KEY, center
						.getLatitudeE6());
				data.putInt(StationsDBAdapter.CENTER_LNG_KEY, center
						.getLongitudeE6());
				data.putInt(StationsDBAdapter.RADIUS_KEY, stations.getHome()
						.getRadius());
			}
			
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle("");
			progressDialog.setMessage(getString(R.string.loading));
			progressDialog.show();
			try {
				mDbHelper.sync(all, data);
			} catch (Exception e) {
				////Log.i("openBicing", "Error Updating?");
				e.printStackTrace();
				progressDialog.dismiss();
			}
			;	
		}else{
			//Log.i("CityBikes","First time!!! :D");
			try{
				mNDBAdapter.update();
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setIcon(android.R.drawable.ic_dialog_map);
				alertDialog.setTitle(R.string.bike_network_alert_title);
				alertDialog.setMessage(getString(R.string.bike_network_alert_text));
				alertDialog.setButton(getString(R.string.automatic), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
					
				});
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.automatic), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						showAutoNetworkDialog(0);
						
					}
					
				});
				alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.manual), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showBikeNetworks();
					}
					
				});
				alertDialog.show();
			}catch (Exception e){
				e.printStackTrace();
				Toast toast = Toast.makeText(getApplicationContext(),getString(R.string.network_error),Toast.LENGTH_LONG);
				toast.show();
			}
		}
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
			stations.updateHome();
			mapView.getController().setCenter(stations.getHome().getPoint());
			mapView.getController().setZoom(16);
		} catch (Exception e) {
			////Log.i("openBicing", "center is null..");
		}
	}

	public void view_all() {
		try {
			mDbHelper.populateStations();
			populateList(true);
		} catch (Exception e) {

		}
		;
	}

	public void view_near() {
		try {
			mDbHelper.populateStations(stations.getHome().getPoint(), stations
					.getHome().getRadius());
			populateList(false);
			if (!infoLayer.isPopulated()) {
				StationOverlay current = stations.getCurrent();
				if (current != null) {
					infoLayer.inflateStation(current);
					current.setSelected(true, this.getBike);
				} else {
					infoLayer
							.inflateMessage(getString(R.string.no_bikes_around));
				}
			}
		} catch (Exception e) {

		}
		;
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
		outState.putInt("homeRadius", stations.getHome().getRadius());
		outState.putBoolean("view_all", view_all);
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
		hOverlay.stopUpdates();
		if (this.isFinishing())
			this.finish();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.i("CityBikes", "Activity Result");
		if (requestCode == SETTINGS_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				hOverlay.restartUpdates();
			}
		}
		SharedPreferences settings = getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
		Boolean dirty = settings.getBoolean("reload_network",false);
		if (dirty){
			this.fillData(view_all);	
		}
	}

	public void populateList(boolean all) {
		try {
			ListView lv = new ListView(this);
			List sts;
			if (all) {
				sts = mDbHelper.getMemory();
			} else {
				sts = mDbHelper.getMemory(hOverlay.getRadius());
			}

			green = R.drawable.green_gradient;
			yellow = R.drawable.yellow_gradient;
			red = R.drawable.red_gradient;
			ArrayAdapter adapter = new ArrayAdapter(this,
					R.layout.stations_list_item, sts) {
				LayoutInflater mInflater = getLayoutInflater();

				@Override
				public View getView(int position, View convertView,
						ViewGroup parent) {
					View row;
					if (convertView == null) {
						row = mInflater.inflate(R.layout.stations_list_item,
								null);
					} else {
						row = convertView;
					}
					StationOverlay tmp = (StationOverlay) getItem(position);
					TextView stId = (TextView) row
							.findViewById(R.id.station_list_item_id);
					stId.setText(tmp.getStation().getName());
					TextView stOc = (TextView) row
							.findViewById(R.id.station_list_item_ocupation);
					stOc.setText(tmp.getStation().getOcupation());
					TextView stDst = (TextView) row
							.findViewById(R.id.station_list_item_distance);
					stDst.setText(tmp.getStation().getDistance());
					TextView stWk = (TextView) row
							.findViewById(R.id.station_list_item_walking_time);
					stWk.setText(tmp.getStation().getWalking());

					int bg;
					switch (tmp.getState()) {
					case StationOverlay.GREEN_STATE:
						bg = green;
						break;
					case StationOverlay.RED_STATE:
						bg = red;
						break;
					case StationOverlay.YELLOW_STATE:
						bg = yellow;
						break;
					default:
						bg = R.drawable.fancy_gradient;
					}
					LinearLayout sq = (LinearLayout) row
							.findViewById(R.id.station_list_item_square);
					sq.setBackgroundResource(bg);
					row.setId(tmp.getStation().getId());
					return row;
				}
			};
			lv.setAdapter(adapter);

			lv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View v,
						int position, long id) {

					int pos = v.getId();
					if (pos != -1) {
						StationOverlay selected = stations.findById(pos);
						if (selected != null) {
							stations.setCurrent(selected.getPosition(), getBike);
							Message tmp = new Message();
							tmp.what = InfoLayer.POPULATE;
							tmp.arg1 = selected.getPosition();
							infoLayerPopulator.dispatchMessage(tmp);
							mapView.getController().animateTo(
									selected.getCenter());
							int height = arg0.getHeight();
							DisplayMetrics dm = new DisplayMetrics();
							getWindowManager().getDefaultDisplay().getMetrics(
									dm);
							int w_height = dm.heightPixels;
							if (height > w_height / 2) {
								mSlidingDrawer.animateClose();
							}
						}
					}
				}
			});
			lv.setBackgroundColor(Color.BLACK);
			lv.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
			fl.setBackgroundColor(Color.BLACK);
			fl.removeAllViews();
			fl.addView(lv);

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int height = dm.heightPixels;
				int calc = (lv.getCount() * CircleHelper.dip2px(50, scale) + CircleHelper.dip2px(45, scale));
				if (calc > height - CircleHelper.dip2px(145, scale))
					calc = height - CircleHelper.dip2px(145, scale);
				else if (lv.getCount() == 0)
					calc = 0;
			
			mSlidingDrawer.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT, calc));
			////Log.i("openBicing", Integer.toString(fl.getHeight()));
		} catch (Exception e) {
			//////Log.i("openBicing", "SHIT THIS SUCKS MEN ARGH FUCK IT!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
