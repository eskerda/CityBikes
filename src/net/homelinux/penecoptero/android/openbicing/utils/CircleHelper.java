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

package net.homelinux.penecoptero.android.openbicing.utils;

import com.google.android.maps.GeoPoint;

public class CircleHelper {
	public static final boolean isOnCircle(float x, float y, float centerX,
			float centerY, double radius) {
		double square_dist = Math.pow(centerX - x, 2)
				+ Math.pow(centerY - y, 2);
		return square_dist <= Math.pow(radius, 2);
	}

	public static final boolean isOnCircle(GeoPoint obj, GeoPoint center,
			float radius) {
		return isOnCircle(obj.getLatitudeE6(), obj.getLongitudeE6(), center
				.getLatitudeE6(), center.getLongitudeE6(), radius * 8.3);
	}

	public static final double gradialDistance(GeoPoint p1, GeoPoint p2) {
		return Math.sqrt(Math.pow(p1.getLatitudeE6() - p2.getLatitudeE6(), 2)
				+ Math.pow(p1.getLongitudeE6() - p2.getLongitudeE6(), 2));
	}

	public static final double gp2m(GeoPoint StartP, GeoPoint EndP) {
		double lat1 = StartP.getLatitudeE6() / 1E6;
		double lat2 = EndP.getLatitudeE6() / 1E6;
		double lon1 = StartP.getLongitudeE6() / 1E6;
		double lon2 = EndP.getLongitudeE6() / 1E6;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
				* Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return 6378140 * c;
	}
	
	public static final int dip2px(float dips, float scale){
		return (int) (dips * scale + 0.5f);
	}
}
