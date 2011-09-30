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

import net.homelinux.penecoptero.android.citybikes.view.FlingTooltip;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

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
	
	private FlingTooltip flingTooltip;
	private Handler handler;
	private Drawable oldBackground;
	private ViewFlipper vf;

	private LayoutInflater inflater;

	private Context ctx;

	private int black, red, green, yellow;

	public static final int NEXT_STATION = 200;
	public static final int PREV_STATION = 201;
	public static final int POPULATE = 202;
	
	private ToggleButton bookmarkButton;

	private boolean populated = false;
	
	private int lastDisplayedChild = -1;


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
	
	private void checkFirstTime(){
		if (isFirstTime()){
			if (vf!=null && flingTooltip != null){
				flingTooltip.setVisibility(View.VISIBLE);
			}
		}
	}
	private boolean isFirstTime(){
		SharedPreferences settings = ctx.getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
		boolean firstTime = settings.getBoolean("first_time", true);
		return firstTime;
	}

	private void saveFirstTime(){
		SharedPreferences settings = ctx.getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
		Editor editor = settings.edit();
		editor.putBoolean("first_time", false);
		editor.commit();
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
		black = R.drawable.alpha_black_gradient;
		green = R.drawable.alpha_green_gradient;
		yellow = R.drawable.alpha_yellow_gradient;
		red = R.drawable.alpha_red_gradient;
		inflater = (LayoutInflater) ctx
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	}

	public void inflateStation(StationOverlay tmp) {
		if (tmp != null) {
			this.station = tmp;
			this.removeAllViews();
			inflater.inflate(R.layout.infolayer, this);

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
			LinearLayout sq = (LinearLayout) findViewById(R.id.station_list_item_square);
			sq.setBackgroundResource(bg);
			// //Log.i("openBicing", "Inflated: " + this.station.getName());
			populated = true;
			vf = (ViewFlipper) findViewById(R.id.stationViewFlipper);
			bookmarkButton = (ToggleButton) findViewById(R.id.bookmark_station);
			flingTooltip = (FlingTooltip) findViewById(R.id.FlingTooltip);
			flingTooltip.setVisibility(View.INVISIBLE);
			
			if (bookmarkButton != null){
				bookmarkButton.setChecked(station.getStation().isBookmarked());
				bookmarkButton.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						station.getStation().setBookmarked(!station.getStation().isBookmarked());
						Message msg = new Message();
						msg.what = CityBikes.BOOKMARK_CHANGE;
						msg.arg1 = station.getStation().getId();
						if (station.getStation().isBookmarked())
							msg.arg2 = 1;
						else
							msg.arg2 = 0;
						handler.sendMessage(msg);
					}});			
				
			}
			checkFirstTime();
			
		}

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
			case StationOverlay.BLACK_STATE:
				bg = this.black;
				break;
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
		if (vf != null && flingTooltip != null){
			int action = event.getAction();
			int direction;
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					
					if (event.getX() > getMeasuredWidth() / 2){
						direction = 1;
					} else {
						direction = 0;
					}
					showFlingTooltip(direction);
					break;
				case MotionEvent.ACTION_UP:
					if (lastDisplayedChild == vf.getDisplayedChild()){
						if (event.getX() > getMeasuredWidth() / 2){
							direction = 1;
						} else {
							direction = 0;
						}
					} else {
						if (event.getX() > getMeasuredWidth() / 2){
							direction = 0;
						}else{
							direction = 1;
						}
					}
					hideFlingTooltip(direction);
					break;
				case MotionEvent.ACTION_MOVE:
					break;
			}
			lastDisplayedChild = vf.getDisplayedChild();
			if (isFirstTime()){
				saveFirstTime();
			}
		}
		
		return true;
	}

	private Animation inFromRightAnimation() {
		Animation inFromRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRight.setDuration(250);
		inFromRight.setInterpolator(new AccelerateInterpolator());
		return inFromRight;
	}

	private Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoLeft.setDuration(250);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		return outtoLeft;
	}

	private Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromLeft.setDuration(250);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}

	private Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoRight.setDuration(250);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}
	
	public void showFlingTooltip (int direction){
		
		Animation fadeInAnimation = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in);
		fadeInAnimation.setDuration(500);
		fadeInAnimation.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				flingTooltip.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
		});
		flingTooltip.setDirection(direction);
		flingTooltip.setVisibility(View.VISIBLE);
		flingTooltip.startAnimation(fadeInAnimation);

	}
	
	public void hideFlingTooltip (int direction) {
		Animation fadeOutAnimation = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out);
		fadeOutAnimation.setDuration(500);
		fadeOutAnimation.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				flingTooltip.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
		});
		flingTooltip.setDirection(direction);
		flingTooltip.startAnimation(fadeOutAnimation);
		
	}

	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
					// //Log.i("CityBikes", "down?");
					return false;
				}
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// Do thingy!!!!

					vf.setInAnimation(inFromRightAnimation());
					vf.setOutAnimation(outToLeftAnimation());
					vf.showNext();

				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

					vf.setInAnimation(inFromLeftAnimation());
					vf.setOutAnimation(outToRightAnimation());
					vf.showPrevious();
				}
			} catch (Exception e) {
				// nothing
			}
			return false;
		}
	}
}
