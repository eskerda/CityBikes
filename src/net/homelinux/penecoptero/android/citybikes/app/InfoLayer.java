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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class InfoLayer extends LinearLayout {

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 100;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	public static final double ERROR_COEFICIENT = 0.35;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	private StationOverlay station;

	private TextView station_id;
	private TextView ocupation;
	private TextView distance;
	private TextView walking_time;
	private Handler handler;
	private Drawable oldBackground;

	private LayoutInflater inflater;

	private Context ctx;

	private int red, green, yellow;

	public static final int NEXT_STATION = 200;
	public static final int PREV_STATION = 201;
	public static final int POPULATE = 202;

	private boolean populated = false;

	private Animation animShow, animHide;

	public InfoLayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.ctx = context;
		this.init();
	}

	public InfoLayer(Context context) {
		super(context);
		this.ctx = context;
		this.init();
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	private void init() {
		gestureDetector = new GestureDetector(new MyGestureDetector());
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		this.setOnTouchListener(gestureListener);
		green = R.drawable.alpha_green_gradient;
		yellow = R.drawable.alpha_yellow_gradient;
		red = R.drawable.alpha_red_gradient;
		inflater = (LayoutInflater) ctx
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void inflateStation(StationOverlay tmp) {
		this.station = tmp;
		this.removeAllViews();
		inflater.inflate(R.layout.stations_list_item, this);

		TextView stId = (TextView) findViewById(R.id.station_list_item_id);
		stId.setText(tmp.getStation().getName());
		TextView stOc = (TextView) findViewById(R.id.station_list_item_ocupation);
		stOc.setText(tmp.getStation().getOcupation());
		TextView stDst = (TextView) findViewById(R.id.station_list_item_distance);
		stDst.setText(tmp.getStation().getDistance());
		TextView stWk = (TextView) findViewById(R.id.station_list_item_walking_time);
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
		LinearLayout sq = (LinearLayout) findViewById(R.id.station_list_item_square);
		sq.setBackgroundResource(bg);
		////Log.i("openBicing", "Inflated: " + this.station.getName());
		populated = true;
	}

	public boolean isPopulated() {
		return this.populated;
	}

	public void update() {
		if (this.station != null)
			inflateStation(this.station);
	}

	public void inflateMessage(String text) {
		this.populated = false;
		this.removeAllViews();
		inflater.inflate(R.layout.message, this);

		TextView message = (TextView) findViewById(R.id.message);
		message.setText(text);

	}

	public void setStation(StationOverlay station) {
		this.station = station;
	}

	public void populateFields(StationOverlay station) {
		this.setStation(station);
		this.populateFields();
	}

	public void populateFields() {
		if (this.station != null) {
			this.station_id.setText(this.station.getStation().getName());
			this.ocupation.setText(this.station.getStation().getOcupation());
			this.walking_time.setText(this.station.getStation().getWalking());
			this.distance.setText(this.station.getStation().getDistance());
			int bg;
			switch (station.getState()) {
			case StationOverlay.GREEN_STATE:
				bg = this.green;
				break;
			case StationOverlay.RED_STATE:
				bg = this.red;
				break;
			case StationOverlay.YELLOW_STATE:
				bg = this.yellow;
				break;
			default:
				bg = R.drawable.fancy_gradient;
			}
			this.setBackgroundResource(bg);
		}
	}

	public GeoPoint getCurrentCenter() {
		return this.station.getCenter();
	}

	public StationOverlay getCurrent() {
		return this.station;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}

	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
					////Log.i("openBicing", "down?");
					return false;
				}
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					handler.sendEmptyMessage(NEXT_STATION);
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					handler.sendEmptyMessage(PREV_STATION);
				}
			} catch (Exception e) {
				// nothing
			}
			return false;
		}
	}
}
