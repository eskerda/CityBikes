package net.homelinux.penecoptero.android.citybikes.donation.app;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class BikeNetworkActivity extends ListActivity {

	
	private ArrayAdapter <JSONObject> mAdapter;
	private NetworksDBAdapter nDBAdapter;
	private View lastSelected;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.network_list);
		nDBAdapter = new NetworksDBAdapter(this.getApplicationContext());
		fillData();
		
	}
	
	public void fillData(){
		List <JSONObject> networks = new LinkedList <JSONObject>() ;
		
		SharedPreferences settings = getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
		final int network_id = settings.getInt("network_id", -1);
		JSONArray json_networks = new JSONArray();
		try{
			json_networks = nDBAdapter.update();
		}catch(Exception e){
			try{
				nDBAdapter.load();
				json_networks = nDBAdapter.getStored();
			}catch (Exception loadEx){
				loadEx.printStackTrace();
			}
		}
		
		try{
			for (int i = 0 ; i < json_networks.length(); i++){
				networks.add(json_networks.getJSONObject(i));
			}
		}catch (Exception e){
			//Log.i("CityBikes","Error parsing");
		}
		
		mAdapter = new ArrayAdapter <JSONObject>(this,
				R.layout.network_list_item, networks) {
			
			LayoutInflater mInflater = getLayoutInflater();
			@Override
			public View getView(int position, View convertView,
					ViewGroup parent) {
				View row;
				if (convertView == null){
					row = mInflater.inflate(R.layout.network_list_item, null);
				} else{
					row = convertView;
				}
				
				try{
					JSONObject network = (JSONObject) getItem(position);
					TextView tvName = (TextView) row.findViewById(R.id.network_list_item_name);
					tvName.setText("("+network.getString("city")+") "+network.getString("name"));
					row.setId(network.getInt("id"));
					if (network_id!=-1){
						if (network.getInt("id")==network_id){
							row.setBackgroundResource(R.drawable.green_gradient);
							lastSelected = row;
						}
					}
				}catch (Exception e){
					//Log.i("CityBikes",e.getLocalizedMessage());
				}
				
				return row;
			}
			
		};
		
		this.setListAdapter(mAdapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
	        super.onListItemClick(l, v, position, id);
	        //Log.i("CityBikes",Integer.toString(v.getId()));
	        try{
	        	nDBAdapter.setManualNetwork(v.getId());
	        	if (lastSelected!=null)
	        		lastSelected.setBackgroundColor(Color.BLACK);
	        	v.setBackgroundResource(R.drawable.green_gradient);
	        	lastSelected = v;
	        }catch (Exception e){
	        	e.printStackTrace();
	        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

}
