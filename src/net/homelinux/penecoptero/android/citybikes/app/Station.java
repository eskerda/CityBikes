package net.homelinux.penecoptero.android.citybikes.app;

import android.content.Context;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class Station {
	private int bikes;
	private int free;
	private String timestamp;
	private String name;
	private int id;
	private boolean bookmarked = false;

	private String distanceText = "";
	private String walkingText = "";
	private String ocupationText = "";
	
	private double metersDistance;
	
	private Context context;
	
	private GeoPoint point;
	
	public Station (int id, String name, int bikes, int free, String timestamp, Context context, GeoPoint center){
		this.id = id;
		this.name = name;
		this.bikes = bikes;
		this.free = free;
		this.timestamp = timestamp;
		this.context = context;
		this.point = center;
	}

	public int getId(){
		return id;
	}
	
	public boolean isBookmarked(){
		return bookmarked;
	}
	
	public void setBookmarked( boolean book ){
		bookmarked = book;
	}
	
	public String getName(){
		return name;
	}
	
	public int getBikes(){
		return this.bikes;
	}
	
	public int getFree(){
		return this.free;
	}
	
	public GeoPoint getCenter(){
		return this.point;
	}
	
	public double getMetersDistance() {
		return this.metersDistance;
	}

	public void setMetersDistance(double distance) {
		this.metersDistance = distance;
	}
	
	public void populateStrings() {
		ocupationText = Integer.toString(this.bikes) + " "
				+ context.getString(R.string.bikes) + " / "
				+ Integer.toString(this.free) + " "
				+ context.getString(R.string.free);

		int meters, km;
		double rawMeters;
		rawMeters = this.metersDistance + this.metersDistance
				* InfoLayer.ERROR_COEFICIENT;
		km = (int) rawMeters / 1000;
		meters = (int) rawMeters - (1000 * km);
		distanceText = "";
		if (km > 0) {
			distanceText = Integer.toString(km) + " km ";
		}
		distanceText = distanceText + Integer.toString(meters) + " m";

		double rawMinutes = (rawMeters / 5000) * 60;

		int hours, minutes;
		hours = (int) rawMinutes / 60;
		minutes = (int) rawMinutes - (60 * hours);
		walkingText = "";
		if (hours > 0) {
			walkingText = Integer.toString(hours) + " h ";
		}
		walkingText = walkingText + Integer.toString(minutes) + " min";
	}
	
	public String getOcupation() {
		return this.ocupationText;
	}

	public String getWalking() {
		return this.walkingText;
	}

	public String getDistance() {
		return this.distanceText;
	}
	
	public Context getContext(){
		return this.context;
	}
}
