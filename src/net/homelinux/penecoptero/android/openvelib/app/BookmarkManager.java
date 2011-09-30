package net.homelinux.penecoptero.android.openvelib.app;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class BookmarkManager {
	/*
	 * WARNING!
	 * This class is not persistent. Use it, and then destroy it.
	 */
	private SharedPreferences settings;
	private String netnick;
	private JSONObject bookmarks;
	private JSONArray stations;
	
	public BookmarkManager(Context ctx) throws Exception{
		settings = ctx.getSharedPreferences(OpenVelib.PREFERENCES_NAME,0);
		netnick = "velib";
		load();
	}
	
	public void setBookmarked(Station station, boolean bookmarked) throws Exception{
		if (bookmarked){
			//Unset
			try{
				JSONArray tmp = new JSONArray("[]");
				for (int i = 0; i < stations.length(); i++){
					if (stations.getInt(i) != station.getHash()){
						tmp.put(stations.getInt(i));
					}
				}
				stations = tmp;
				store();
			}catch (Exception e){
				//Not in the list, or error. Who cares, not there :)
			}
		}else{
			//Find, and if not, set
			if (!isBookmarked(station)){
				stations.put(station.getHash());
				store();
			}
		}
	}
	
	public int getIndex(Station station) throws Exception{
		for (int i = 0; i < stations.length(); i++){
			/* Can we assume ids will be always the same? .. damn
			 * better use a hash of lat,lng, for example.
			 */
			if (stations.getInt(i) == station.getHash())
					return i;
		}
		return -1;	
	}
	
	public boolean isBookmarked(Station station){
		try{
			if (getIndex(station) < 0)
				return false;
			else
				return true;
		} catch (Exception e){
			Log.i("CityBikes", "Error checking if bookmarked");
			e.printStackTrace();
		}
		return false;
	}
	
	private void load() throws Exception{
		bookmarks = new JSONObject(settings.getString("bookmarks", "{}"));
		try{
			stations = bookmarks.getJSONArray(netnick);
		} catch (Exception e){
			//Doesn't exist, create it!
			stations = new JSONArray("[]");
		}
	}
	
	private void store(){
		Editor editor = settings.edit();
		try {
			bookmarks.put(netnick, stations);
			editor.putString("bookmarks", bookmarks.toString());
			editor.commit();
		} catch (JSONException e) {
			//Unable to store bookmarks.. What should we do?
			Log.i("CityBikes","Unable to store bookmarks");
			e.printStackTrace();
		}
		editor = null;
	}
	
	
}
