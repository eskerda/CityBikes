package net.homelinux.penecoptero.android.citybikes.donation.app;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class Locator {
	public static final int LOCATION_CHANGED = 101;
	public static final int STOPPED = 0;
	public static final int RUNNING = 1;
	public static final int UNKNOWN = 2;
	
	private Handler handler;
	private Location currentLocation;
	private GeoPoint currentGeoPoint;
	
	private Location fallbackLocation = null;
	
	private boolean locked = false;
	
	private List<LocationListener> listeners;
	
	private LocationManager locationManager;
	
	private int status = UNKNOWN;
	
	public Locator(Context context, Handler handler){
		this.handler = handler;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		startUpdates(true);
	}
	
	public Location getLastKnownLocation() {
		Location location = locationManager.getLastKnownLocation("gps");
		if (location == null) {
			location = locationManager.getLastKnownLocation("network");
		}
		if (location == null && fallbackLocation != null){
			location = fallbackLocation;
			//Log.i("CityBikes","Setting fallback locatioN!");
		}
		return location;
	}
	
	public GeoPoint getCurrentGeoPoint(){
		return currentGeoPoint;
	}
	
	public Location getCurrentLocation(){
		return currentLocation;
	}
	
	public void setFallbackLocation(Location fallback){
		fallbackLocation = fallback;
	}
	
	public void startUpdates(boolean instantLastLocation){
		status = RUNNING;
		//Log.i("CityBikes","Starting all location updates");
		listeners = new LinkedList<LocationListener>();
		LocationListener ll;
		
		for (Iterator<String> i = locationManager.getProviders(true).iterator(); i.hasNext(); ){
			ll = new LocationListener(){

				@Override
				public void onLocationChanged(Location location) {
					if (!locked)
						update(location);
				}

				@Override
				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStatusChanged(String provider, int status,
						Bundle extras) {
					// TODO Auto-generated method stub
					
				}
			};
			listeners.add(ll);
			locationManager.requestLocationUpdates(i.next(), 60000, 25,ll);
		}
		if (instantLastLocation){
			update(getLastKnownLocation());
		}
	}
	
	public void stopUpdates(){
		//Log.i("CityBikes","Stopping all location updates");
		for( Iterator<LocationListener> ll = listeners.iterator(); ll.hasNext(); ){
			locationManager.removeUpdates(ll.next());
		}
		status = STOPPED;
	}
	
	public void restartUpdates(){
		stopUpdates();
		startUpdates(false);
	}
	
	private void update(Location newLocation){
		if (newLocation!=null){
			currentLocation = newLocation;
			currentGeoPoint = new GeoPoint((int) (currentLocation.getLatitude()*1E6), (int) (currentLocation.getLongitude()*1E6)); 
			Message msg = new Message();
			msg.what = LOCATION_CHANGED;
			msg.arg1 = currentGeoPoint.getLatitudeE6(); 
			msg.arg2 = currentGeoPoint.getLongitudeE6();
			msg.obj = currentLocation;
			handler.sendMessage(msg);
		}
	}
	
	public void lockCenter(GeoPoint center){
		locked = true;
		Location dummy = new Location("dummy");
		dummy.setLatitude(center.getLatitudeE6()/1E6);
		dummy.setLongitude(center.getLongitudeE6()/1E6);
		update(dummy);
	}
	
	public void unlockCenter(){
		locked = false;
		update(getLastKnownLocation());
	}
	
	public int getStatus(){
		return status;
	}
}
