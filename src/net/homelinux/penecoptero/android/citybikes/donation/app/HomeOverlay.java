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
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class HomeOverlay extends Overlay {

	public static final int MOTION_CIRCLE_STOP = 100;
	private GeoPoint point;

	private float radiusInPixels;
	private int radiusInMeters = 500;

	private float centerXInPixels;
	private float centerYInPixels;

	private int status = 0;

	private float smallCircleX;
	private float smallCircleY;
	private float smallCircleRadius = 10;

	private float angle = 0;

	private Handler handler;

	public HomeOverlay(GeoPoint center, Handler handler) {
		point = center;
		this.handler = handler;
	}
	

	public void moveCenter(GeoPoint point){
		this.point = point;
	}
	
	public void setRadius(int meters) {
		this.radiusInMeters = meters;
	}

	public int getRadius() {
		return this.radiusInMeters;
	}

	public GeoPoint getPoint() {
		return this.point;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		try {
			Projection astral = mapView.getProjection();
			Point screenPixels = astral.toPixels(this.point, null);
			this.radiusInPixels = astral
					.metersToEquatorPixels(this.radiusInMeters);
			this.centerXInPixels = screenPixels.x;
			this.centerYInPixels = screenPixels.y;

			Paint paint = new Paint();

			paint.setARGB(100, 147, 186, 228);
			paint.setStrokeWidth(2);
			paint.setAntiAlias(true);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawCircle(screenPixels.x, screenPixels.y,
					this.radiusInPixels, paint);

			paint.setStyle(Paint.Style.FILL);
			paint.setAlpha(20);
			canvas.drawCircle(screenPixels.x, screenPixels.y,
					this.radiusInPixels, paint);

			Paint txtPaint = new Paint();
			txtPaint.setARGB(255, 255, 255, 255);
			txtPaint.setAntiAlias(true);
			txtPaint.setTextSize(this.radiusInPixels / 4);
			String text;
			if (this.radiusInMeters > 1000) {
				int km = this.radiusInMeters / 1000;
				int m = this.radiusInMeters % 1000;
				text = Integer.toString(km) + " km, " + Integer.toString(m)
						+ " m";
			} else {
				text = Integer.toString(this.radiusInMeters) + " m";
			}

			float x = (float) (this.centerXInPixels + this.radiusInPixels
					* Math.cos(Math.PI));
			float y = (float) (this.centerYInPixels + this.radiusInPixels
					* Math.sin(Math.PI));

			// lol
			txtPaint.setTextAlign(Paint.Align.CENTER);
			Path tPath = new Path();
			tPath.moveTo(x, y + this.radiusInPixels / 3);
			tPath.lineTo(x + this.radiusInPixels * 2, y + this.radiusInPixels
					/ 3);
			canvas.drawTextOnPath(text, tPath, 0, 0, txtPaint);
			canvas.drawPath(tPath, txtPaint);

			drawArrow(canvas, screenPixels, this.radiusInPixels, angle);
		} catch (Exception e) {

		}
		return super.draw(canvas, mapView, shadow, when);
	}

	public void drawArrow(Canvas canvas, Point sPC, float length, double angle) {
		Paint paint = new Paint();
		paint.setARGB(255, 147, 186, 228);
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStyle(Paint.Style.FILL);
		float x = (float) (sPC.x + length * Math.cos(angle));
		float y = (float) (sPC.y + length * Math.sin(angle));
		canvas.drawLine(sPC.x, sPC.y, x, y, paint);

		// canvas.drawCircle(x, y, 10, paint);

		canvas.drawCircle(sPC.x, sPC.y, 5, paint);

		smallCircleX = x;
		smallCircleY = y;

		canvas.drawCircle(x, y, 8, paint);

	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		// TODO Auto-generated method stub

		return super.onTap(p, mapView);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		// TODO Auto-generated method stub
		float x = e.getX();
		float y = e.getY();

		int action = e.getAction();

		boolean onCircle = CircleHelper.isOnCircle(x, y, this.smallCircleX,
				this.smallCircleY, this.smallCircleRadius + 20);

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (onCircle) {
				this.status = 1;
			} else
				this.status = 0;
			break;
		case MotionEvent.ACTION_UP:
			if (this.status == 1) {
				this.status = 0;
				handler.sendEmptyMessage(MOTION_CIRCLE_STOP);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (this.status == 1) {
				double dist = Math.sqrt(Math.pow(Math.abs(this.centerXInPixels
						- x), 2)
						+ Math.pow(Math.abs(this.centerYInPixels - y), 2));
				this.radiusInMeters = (int) ((int) (dist * this.radiusInMeters) / this.radiusInPixels);

				// Recalculate angle
				float opp = this.centerYInPixels - y;
				float adj = this.centerXInPixels - x;
				float tan = Math.abs(opp) / Math.abs(adj);
				this.angle = (float) Math.atan(tan);
				if (opp > 0) {

					if (adj > 0) {
						this.angle += Math.PI;
					} else {
						this.angle = this.angle * -1;
					}
				} else {
					if (adj > 0) {
						this.angle = (float) Math.PI - this.angle;
					} else {
						// Okay
					}
				}
				
				handler.sendEmptyMessage(MOTION_CIRCLE_STOP);
			}
			break;
		}
		return this.status == 1;
	}
}
