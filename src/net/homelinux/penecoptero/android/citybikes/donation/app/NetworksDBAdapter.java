package net.homelinux.penecoptero.android.citybikes.donation.app;

import java.util.List;
import java.util.Locale;

import net.homelinux.penecoptero.android.citybikes.utils.CircleHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.maps.GeoPoint;


public class NetworksDBAdapter {
	public static final String NETWORKS_FEED = "http://api.citybik.es/networks.json";
	private RESTHelper mRESTHelper;
	private String RAWNetworks;
	private JSONArray networks;
	private Context mCtx;
	private SharedPreferences settings;
	
	public NetworksDBAdapter (Context ctx){
		mCtx = ctx;
		this.mRESTHelper = new RESTHelper(false, null, null);
		settings = this.mCtx.getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
	}
	
	public JSONArray update() throws Exception{
		RAWNetworks = mRESTHelper.restGET(NETWORKS_FEED);
		networks = new JSONArray(RAWNetworks);
		this.store();
		return networks;
	}
	
	public JSONArray getStored() throws Exception{
		this.load();
		return networks;
	}
	
	public void load() throws Exception{
		RAWNetworks = settings.getString("networks", "[]");
		networks = new JSONArray(RAWNetworks);
	}
	
	public void store() {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("networks", this.RAWNetworks);
		editor.commit();
	}
	
	public JSONObject getAutomaticNetworkMethod1(GeoPoint center) throws Exception{
		this.load();
		Geocoder oracle = new Geocoder(mCtx,Locale.ENGLISH);
		List <Address> results = oracle.getFromLocation(center.getLatitudeE6()/1E6, center.getLongitudeE6()/1E6,5);
		//Log.i("CityBikes","Oh HAI!");
		//Log.i("CityBikes",results.toString());
		//Log.i("CityBikes",Integer.toString(results.size()));
		for (int i = 0; i < results.size(); i++){
			//Log.i("CityBikes",results.get(i).getLocality());
			/*for (int j = 0; j < networks.length(); j++){
				//Log.i("CityBikes",networks.getJSONObject(j).getString("city_en"));
				if (networks.getJSONObject(j).getString("city_en").compareToIgnoreCase(results.get(i).getLocality())==0){
					//Log.i("CityBikes","Network found!");
					return networks.getJSONObject(j);
				}
			}*/
		}
		return null;
	}
	
	public JSONObject getAutomaticNetwork(GeoPoint center, int method) throws Exception{
		this.load();
		//Log.i("CityBikes","Current database: "+RAWNetworks);
		//Log.i("CityBikes","Trying automatic -> "+center.toString()+" "+Integer.toString(method));
		if (method==0)
			return getAutomaticNetworkMethod0(center);
		else if (method==1)
			return getAutomaticNetworkMethod1(center);
		else
			return null;
			
	}
	
	public JSONObject getAutomaticNetworkMethod0(GeoPoint center) throws Exception{
		this.load();
		for (int i = 0; i < networks.length(); i++){
			JSONObject network = networks.getJSONObject(i);
			GeoPoint netCenter = new GeoPoint(network.getInt("lat"),network.getInt("lng"));
			//Log.i("CityBikes","Testing: "+network.getString("name"));
			if (CircleHelper.isOnCircle(center,netCenter, network.getInt("radius"))){
				return network;
			}else{
				//Log.i("CityBikes","Nope");
			}
		}
		return null;
	}
	public JSONObject getAutomaticNetwork(GeoPoint center) throws Exception{
		return getAutomaticNetworkMethod0(center);
	}
	
	public void setAutomaticNetwork(GeoPoint center) throws Exception{
		JSONObject net = getAutomaticNetwork(center);
		if (net != null){
			if(settings.getInt("network_id", -1)!= net.getInt("id")){
				setManualNetwork(net.getInt("id"));
			}
		}
	}
	public JSONObject getNetworks(int id) throws Exception{
		return networks.getJSONObject(id);
	}
	
	public void setManualNetwork(int id) throws Exception{
		String url = networks.getJSONObject(id).getString("url");
		String name = networks.getJSONObject(id).getString("name");
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("network_name", name);
		editor.putString("network_url", url);
		editor.putBoolean("reload_network", true);
		editor.putInt("network_id", id);
		editor.commit();
	}
	
	public boolean isConfigured(){
		int net_id = settings.getInt("network_id", -1);
		return net_id!=-1;
	}
}
