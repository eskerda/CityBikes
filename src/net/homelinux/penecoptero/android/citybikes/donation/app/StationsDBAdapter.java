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

package net.homelinux.penecoptero.android.citybikes.donation.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.homelinux.penecoptero.android.citybikes.utils.CircleHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class StationsDBAdapter implements Runnable {
	public static final String CENTER_LAT_KEY = "sCenterLat";
	public static final String CENTER_LNG_KEY = "sCenterLng";
	public static final String RADIUS_KEY = "sRadius";
	public static final String VIEW_ALL_KEY = "sViewAll";
	public static final String PREF_NAME = "citybikes";
	

	public static final int FETCH = 0;
	public static final int UPDATE_MAP = 1;
	public static final int UPDATE_MAP_LESS = 2;
	public static final int UPDATE_DATABASE = 3;
	public static final int NETWORK_ERROR = 4;
	public static final String TIMESTAMP_FORMAT = "HH:mm:ss dd/MM/yyyy";

	private StationOverlayList stationsDisplayList;

	private List<StationOverlay> stationsMemoryMap;

	private RESTHelper mRESTHelper;

	private MapView mapView;

	private Context mCtx;

	private Handler handlerOut;

	private Bundle threadData;

	private Queue<Integer> toDo;

	private String RAWstations;

	private String last_updated;
	
	private long last_updated_time;

	private GeoPoint center;
	
	private boolean getBike = true;

	public StationsDBAdapter(Context ctx, MapView mapView, Handler handler,
			StationOverlayList stationsDisplayList) {
		this.mCtx = ctx;
		this.mapView = mapView;
		this.handlerOut = handler;
		this.stationsDisplayList = stationsDisplayList;
		
		this.mRESTHelper = new RESTHelper(false, null, null);

		this.toDo = new LinkedList();
	}

	public StationsDBAdapter(Context ctx, Handler handler) {
		this.mRESTHelper = new RESTHelper(false, null, null);
		this.handlerOut = handler;
		this.toDo = new LinkedList();
		this.mCtx = ctx;
	}

	public String fetchStations(String provider) throws Exception {
		return mRESTHelper.restGET(provider);
	}

	public String getLastUpdated() {
		return last_updated;
	}
	
	public Long getLastUpdatedTime(){
		return last_updated_time;
	}

	public void setCenter(GeoPoint point) {
		this.center = point;
	}

	public void loadStations() throws Exception {
		this.retrieve();
		if (this.center != null)
			buildMemory(new JSONArray(this.RAWstations), this.center);
		else
			buildMemory(new JSONArray(this.RAWstations));
	}

	public void buildMemory(JSONArray stations) throws Exception {
		this.stationsMemoryMap = new LinkedList<StationOverlay>();
		JSONObject station = null;
		int lat, lng, bikes, free, id;
		String timestamp, name;
		GeoPoint point;
		BookmarkManager bm = new BookmarkManager(mCtx);
		for (int i = 0; i < stations.length(); i++) {
			station = stations.getJSONObject(i);
			id = station.getInt("id");
			name = station.getString("name");
			lat = Integer.parseInt(station.getString("y"));
			lng = Integer.parseInt(station.getString("x"));
			bikes = station.getInt("bikes");
			free = station.getInt("free");
			timestamp = station.getString("timestamp");

			point = new GeoPoint(lat, lng);
			Station stat = new Station(id, name, bikes, free, timestamp, mCtx, point);

			if (bm.isBookmarked(stat))
				stat.setBookmarked(true);
			
			StationOverlay memoryStation = new StationOverlay(stat, getBike);
			stationsMemoryMap.add(memoryStation);
		}
	}

	public List<StationOverlay> getMemory() throws Exception {
		return stationsMemoryMap;
	}

	public List<StationOverlay> getMemory(int radius) throws Exception {
		List<StationOverlay> res = new LinkedList<StationOverlay>();
		StationOverlay tmp;
		Iterator<StationOverlay> i = stationsMemoryMap.iterator();
		while (i.hasNext()) {
			tmp = i.next();
			if ((tmp.getStation().getMetersDistance() + tmp.getStation().getMetersDistance() * 0.35) <= radius
			||
			tmp.getStation().isBookmarked()
			) {
				res.add(tmp);
			}
		}
		return res;
	}

	public void updateDistances(GeoPoint center) {
		Iterator<StationOverlay> i = stationsMemoryMap.iterator();
		while (i.hasNext()) {
			StationOverlay memoryStation = i.next();
			memoryStation.getStation().setMetersDistance(CircleHelper.gp2m(center,
					memoryStation.getCenter()));
			memoryStation.getStation().populateStrings();
		}
	}

	public void buildMemory(JSONArray stations, GeoPoint center)
			throws Exception {
		this.stationsMemoryMap = new LinkedList<StationOverlay>();
		JSONObject station = null;
		int lat, lng, bikes, free, id;
		String timestamp, name;
		GeoPoint point;
		BookmarkManager bm = new BookmarkManager(mCtx);
		for (int i = 0; i < stations.length(); i++) {
			station = stations.getJSONObject(i);
			id = station.getInt("id");
			name = station.getString("name");
			lat = Integer.parseInt(station.getString("lat"));
			lng = Integer.parseInt(station.getString("lng"));
			bikes = station.getInt("bikes");
			free = station.getInt("free");
			timestamp = station.getString("timestamp");
			point = new GeoPoint(lat, lng);
			Station stat = new Station(id, name, bikes, free, timestamp, mCtx, point);

			if (bm.isBookmarked(stat))
				stat.setBookmarked(true);
			StationOverlay memoryStation = new StationOverlay(stat, getBike);

			memoryStation.getStation().setMetersDistance(CircleHelper.gp2m(center, point));
			memoryStation.getStation().populateStrings();
			stationsMemoryMap.add(memoryStation);
		}

		Collections.sort(stationsMemoryMap, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (o1 instanceof StationOverlay
						&& o2 instanceof StationOverlay) {
					StationOverlay stat1 = (StationOverlay) o1;
					StationOverlay stat2 = (StationOverlay) o2;
					if (stat1.getStation().getMetersDistance() > stat2.getStation().getMetersDistance())
						return 1;
					else
						return -1;
				} else {
					if (o1 instanceof HomeOverlay) {
						return 1;
					} else if (o2 instanceof HomeOverlay) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		});
	}

	public void reorder() {
		Collections.sort(stationsMemoryMap, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (o1 instanceof StationOverlay
						&& o2 instanceof StationOverlay) {
					StationOverlay stat1 = (StationOverlay) o1;
					StationOverlay stat2 = (StationOverlay) o2;
					if (stat1.getStation().getMetersDistance() > stat2.getStation().getMetersDistance())
						return 1;
					else
						return -1;
				} else {
					if (o1 instanceof HomeOverlay) {
						return 1;
					} else if (o2 instanceof HomeOverlay) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		});
	}

	public void store() {
		SharedPreferences settings = this.mCtx.getSharedPreferences(PREF_NAME,
				0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("stations", this.RAWstations);
		editor.putString("last_updated", this.last_updated);
		editor.putLong("last_updated_time",this.last_updated_time);
		editor.commit();
	}

	public void retrieve() throws Exception {
		SharedPreferences settings = this.mCtx.getSharedPreferences(PREF_NAME,
				0);
		RAWstations = settings.getString("stations", "[]");
		last_updated = settings.getString("last_updated", null);
		last_updated_time = settings.getLong("last_updated_time", 0);
		String network_url = settings.getString("network_url", "");
	}

	public void sync(boolean all, Bundle data) throws Exception {
		this.threadData = data;
		toDo.add(FETCH);
		if (all)
			toDo.add(UPDATE_MAP);
		else
			toDo.add(UPDATE_MAP_LESS);
		toDo.add(UPDATE_DATABASE);
		Thread awesomeThread = new Thread(this);
		awesomeThread.start();
	}

	public void populateStations() throws Exception {
		stationsDisplayList.clearStationOverlays();
		Iterator<StationOverlay> i = stationsMemoryMap.iterator();
		while (i.hasNext()) {
			stationsDisplayList.addStationOverlay(i.next());
		}
		mapView.postInvalidate();
	}

	public void populateStations(GeoPoint center, int radius) throws Exception {
		stationsDisplayList.clearStationOverlays();
		Iterator<StationOverlay> i = stationsMemoryMap.iterator();
		this.reorder();
		StationOverlay tmp;
		while (i.hasNext()) {
			tmp = i.next();
			if ((tmp.getStation().getMetersDistance() + tmp.getStation().getMetersDistance() * 0.35) <= radius
			||
			tmp.getStation().isBookmarked()
			) {
				stationsDisplayList.addStationOverlay(tmp);
			}
		}
		mapView.postInvalidate();
	}

	public void changeMode (boolean getBike){
		this.getBike = getBike;
		Iterator i = stationsMemoryMap.iterator();
		while (i.hasNext()){
			Object tmp = i.next();
			if ( tmp instanceof StationOverlay){
				StationOverlay st = (StationOverlay) tmp;
				st.updateStatus(getBike);
			}
		}
	}
	
	@Override
	public void run() {
		while (!toDo.isEmpty()) {
			Integer action = toDo.poll();
			switch (action) {
			case FETCH:
				try {
					SharedPreferences settings = this.mCtx.getSharedPreferences(PREF_NAME,0);
					String network_url = settings.getString("network_url","");
					RAWstations = fetchStations(network_url);
					SimpleDateFormat sdf = new SimpleDateFormat(
							TIMESTAMP_FORMAT);
					Calendar cal = Calendar.getInstance();
					last_updated = sdf.format(cal.getTime());
					last_updated_time = cal.getTime().getTime();
					buildMemory(new JSONArray(RAWstations), this.center);
				} catch (Exception fetchError) {
					handlerOut.sendEmptyMessage(NETWORK_ERROR);
					fetchError.printStackTrace();
					try {
						retrieve();
						buildMemory(new JSONArray(RAWstations), this.center);
					} catch (Exception internalError) {
						// FUCK EVERYTHING!
					}
				}
				handlerOut.sendEmptyMessage(FETCH);
				break;
			case UPDATE_MAP:
				try {
					populateStations();
				} catch (Exception populateError) {

				}
				handlerOut.sendEmptyMessage(UPDATE_MAP);
				break;
			case UPDATE_MAP_LESS:
				try {
					GeoPoint center = new GeoPoint(threadData
							.getInt(CENTER_LAT_KEY), threadData
							.getInt(CENTER_LNG_KEY));
					populateStations(center, threadData.getInt(RADIUS_KEY));
				} catch (Exception populateError) {

				}
				handlerOut.sendEmptyMessage(UPDATE_MAP);
				break;
			case UPDATE_DATABASE:
				store();
				handlerOut.sendEmptyMessage(UPDATE_DATABASE);
				break;
			}
		}
	}
}
