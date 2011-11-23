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

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.TextView;

public class StationSlidingDrawer extends SlidingDrawer {
	
	private Handler handler;
	private List <StationOverlay> stations;
	private ListView listView;
	private FrameLayout frameLayout;

	private int black;
	private int green;
	private int yellow;
	private int red;
	
	public static final int ITEMCLICKED = 200;
	
	private ArrayAdapter <List <StationOverlay> > adapter;
	
	private Context context;
	
	private LayoutInflater mInflater;
	
	private WindowManager wm;
	
	public StationSlidingDrawer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		initVars();
	}

	public StationSlidingDrawer(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		initVars();
	}
	
	private int getWindowHeight(){
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}
	
	public void setStations(List <StationOverlay> sts){
		stations = sts;
		adapter = new StationsAdapter(context, R.layout.stations_list_item, stations);
		listView.setAdapter(adapter);
		this.updateFrame();
	}

	public void initVars(){
		black = R.drawable.black_gradient;
		green = R.drawable.green_gradient;
		yellow = R.drawable.yellow_gradient;
		red = R.drawable.red_gradient;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		listView = new ListView(context);
		stations = new LinkedList<StationOverlay>();
		
		adapter = new StationsAdapter(context, R.layout.stations_list_item, stations);
		listView.setAdapter(adapter);
		wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);	
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				int pos = v.getId();
				if (pos != -1){
					StationOverlay selected = stations.get(position);
					if (selected != null){
						Message msg = new Message();
						msg.what = StationSlidingDrawer.ITEMCLICKED;
						msg.arg1 = selected.getPosition();
						msg.obj = selected;
						handler.sendMessage(msg);
						int height = arg0.getHeight();
						if (height > getWindowHeight() / 2) {
							animateClose();
						}
					}
				}
				
			}
		});
		listView.setBackgroundColor(Color.BLACK);
		listView.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		frameLayout = (FrameLayout) findViewById(R.id.content);
		if (frameLayout != null){
			frameLayout.setBackgroundColor(Color.BLACK);
			frameLayout.removeAllViews();
			frameLayout.addView(listView);
		}
		listView.setAdapter(adapter);
		
	}
	public void updateFrame(){
		frameLayout = (FrameLayout) findViewById(R.id.content);
		if (frameLayout != null){
			frameLayout.setBackgroundColor(Color.BLACK);
			frameLayout.removeAllViews();
			frameLayout.addView(listView);
		}
	}
	
	public void setHandler (Handler h){
		handler = h;
	}
	
	private class StationsAdapter extends ArrayAdapter {
		@SuppressWarnings("unchecked")
		public StationsAdapter(Context context, int textViewResourceId,
				List <StationOverlay> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			if (convertView == null){
				row = mInflater.inflate(R.layout.stations_list_item, null);
			}else{
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
			case StationOverlay.BLACK_STATE:
				bg = black;
				break;
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
	}
}

