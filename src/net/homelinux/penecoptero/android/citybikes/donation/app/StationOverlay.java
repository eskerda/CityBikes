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

import net.homelinux.penecoptero.android.citybikes.utils.CircleHelper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class StationOverlay extends Overlay {
		
	private int status;
	private Handler handler;
	private boolean selected = false;
	
	private boolean getMode = true;

	public static final int BLACK_STATE = 0;
	public static final int RED_STATE = 1;
	public static final int YELLOW_STATE = 2;
	public static final int GREEN_STATE = 3;

	public static final int TOUCHED = 10;

	private int radiusInPixels;
	private int radiusInMeters;

	private static final int RED_STATE_MAX = 0;
	private static final int YELLOW_STATE_MAX = 8;

	private static final int BLACK_STATE_RADIUS = 80;
	private static final int RED_STATE_RADIUS = 80;
	private static final int YELLOW_STATE_RADIUS = 80;
	private static final int GREEN_STATE_RADIUS = 80;
	private static final int SELECTED_STATE_RADIUS = 120;
	
	private static final int STROKE_WIDTH = 4;

	private Paint currentPaint;
	private Paint currentBorderPaint;
	private Paint selectedPaint;
	
	private float scale;
	
	private Station station;

	public StationOverlay(Station station, boolean mode){
		this.station = station;
		scale = station.getContext().getResources().getDisplayMetrics().density;
		this.initPaint();
		this.updateStatus(mode);
	}
	
	public StationOverlay(Station station){
		this.station = station;
		scale = station.getContext().getResources().getDisplayMetrics().density;
		this.initPaint();
	}
	
	private void initPaint(){
		this.currentPaint = new Paint();
		this.currentBorderPaint = new Paint();
		this.selectedPaint = new Paint();

		this.currentPaint.setAntiAlias(true);

		this.currentBorderPaint.setStyle(Paint.Style.STROKE);
		this.currentBorderPaint.setStrokeWidth(CircleHelper.dip2px(STROKE_WIDTH, scale));

		this.selectedPaint = new Paint();
		this.selectedPaint.setARGB(75, 0, 0, 0);
		this.selectedPaint.setAntiAlias(true);
		this.selectedPaint.setStrokeWidth(CircleHelper.dip2px(STROKE_WIDTH, scale));
		this.selectedPaint.setStyle(Paint.Style.STROKE);
	}

	public int getPosition() {
		return this.station.getId();
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	public int getState() {
		return this.status;
	}
	
	public Station getStation(){
		return this.station;
	}
	
	public GeoPoint getCenter() {
		return this.station.getCenter();
	}
	
	public void updateStatus (boolean onGetMode){
		getMode = onGetMode;
		if (station.getFree() == 0 && station.getBikes() == 0){
			this.status = BLACK_STATE;
			this.radiusInMeters = BLACK_STATE_RADIUS;
			this.currentPaint.setARGB(50, 200, 200, 200);
			this.currentBorderPaint.setARGB(75, 190, 190, 190);
		}
		else if (onGetMode){
			updateStatus();
		}else{
			if (station.getFree() > YELLOW_STATE_MAX) {
				this.status = GREEN_STATE;
				this.radiusInMeters = GREEN_STATE_RADIUS;
				this.currentPaint.setARGB(85, 146, 186, 43);
				this.currentBorderPaint.setARGB(100, 146, 186, 43);
			} else if (station.getFree() > RED_STATE_MAX) {
				this.status = YELLOW_STATE;
				this.radiusInMeters = YELLOW_STATE_RADIUS;
				this.currentPaint.setARGB(85, 251,184,41);
				this.currentBorderPaint.setARGB(100, 255, 210, 72);

			} else {
				this.status = RED_STATE;
				this.radiusInMeters = RED_STATE_RADIUS;
				this.currentPaint.setARGB(85, 240, 35, 17);
				this.currentBorderPaint.setARGB(100, 240, 35, 17);
			}
		}
	}

	public void updateStatus(){
		if (station.getBikes() > YELLOW_STATE_MAX) {
			this.status = GREEN_STATE;
			this.radiusInMeters = GREEN_STATE_RADIUS;
			//#FF92BA2B
			this.currentPaint.setARGB(85, 146, 186, 43);
			this.currentBorderPaint.setARGB(100, 146, 186, 43);
		} else if (station.getBikes() > RED_STATE_MAX) {
			this.status = YELLOW_STATE;
			this.radiusInMeters = YELLOW_STATE_RADIUS;
			this.currentPaint.setARGB(85, 251,184,41);
			this.currentBorderPaint.setARGB(100, 255, 210, 72);

		} else {
			this.status = RED_STATE;
			this.radiusInMeters = RED_STATE_RADIUS;
			this.currentPaint.setARGB(85, 240, 35, 17);
			this.currentBorderPaint.setARGB(100, 240, 35, 17);
		}
	}

	
	public void setSelected(boolean selected) {
		this.selected = selected;
		updateStatus(getMode);
		if (this.selected) {
			this.radiusInMeters = SELECTED_STATE_RADIUS;
		}
			
	}
	
	public void setSelected(boolean selected, boolean mode) {
		this.selected = selected;
		updateStatus(mode);
		if (this.selected) {
			this.radiusInMeters = SELECTED_STATE_RADIUS;
		}
	}

	public void update() {
		// TODO Update aviability.. :D
		this.updateStatus(getMode);
	}

	private void calculatePixelRadius(MapView mapView) {
		this.radiusInPixels = (int) mapView.getProjection()
				.metersToEquatorPixels(this.radiusInMeters);
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		// TODO Auto-generated method stub
		return super.draw(canvas, mapView, shadow, when);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		calculatePixelRadius(mapView);

		Projection astral = mapView.getProjection();
		Point screenPixels = astral.toPixels(this.getCenter(), null);

		RectF oval = new RectF(screenPixels.x - this.radiusInPixels,
				screenPixels.y - this.radiusInPixels, screenPixels.x
						+ this.radiusInPixels, screenPixels.y
						+ this.radiusInPixels);
		
		if (this.station.isBookmarked()){
			canvas.drawPath(createStar(5, screenPixels, (float) (this.radiusInPixels * 1.5) , (float) (this.radiusInPixels * 1.5 / 2)), this.currentPaint);
			if (this.selected)
				canvas.drawPath(createStar(5, screenPixels, (float) (this.radiusInPixels * 1.5) , (float) (this.radiusInPixels * 1.5 / 2)), this.selectedPaint);
		} else {
			canvas.drawOval(oval, this.currentPaint);
			if (this.selected) {
				canvas.drawCircle(screenPixels.x, screenPixels.y,
						this.radiusInPixels, this.selectedPaint);
			}
		}
	}
	
	public static Path createStar(int arms, Point center, float rOuter, float rInner)
	{
	    double angle = Math.PI / arms;
	    
	    Path path = new Path();
	    for (int i = 0; i < arms * 2; i++){
	    	float d;
	    	if (i % 2 == 0)
	    		d = rOuter;
	    	else
	    		d = rInner;
	    	double tangle = angle * i;
	    	
	    	if (i == 0){
			    path.moveTo(center.x + (float) Math.cos(tangle) * d, 
		    			center.y + (float) Math.sin(tangle) * d);
	    	} else {
			    path.lineTo(center.x + (float) Math.cos(tangle) * d, 
		    			center.y + (float) Math.sin(tangle) * d);
	    	}
	    }
	    path.close();
	    return path;
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		// TODO Auto-generated method stub

		if ((p.getLatitudeE6() <= this.getCenter().getLatitudeE6() + 800 && p
				.getLatitudeE6() >= this.getCenter().getLatitudeE6() - 800)
				&& (p.getLongitudeE6() <= this.getCenter().getLongitudeE6() + 800 && p
						.getLongitudeE6() >= this.getCenter().getLongitudeE6() - 800)) {

			if (this.handler != null) {
				Message msg = new Message();
				msg.what = TOUCHED;
				msg.arg1 = this.station.getId();
				msg.obj = this.station;
				this.handler.sendMessage(msg);
			}
		}

		return super.onTap(p, mapView);
	}

	public boolean getSelected() {
		return this.selected;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(e, mapView);
	}

}
