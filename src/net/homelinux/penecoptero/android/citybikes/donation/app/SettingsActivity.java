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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	PreferenceScreen psLocation;
	PreferenceScreen manualNetwork;
	PreferenceScreen clearCache;
	CheckBoxPreference autoNetwork;
	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(
				CityBikes.PREFERENCES_NAME);
		addPreferencesFromResource(R.xml.preferences);

		psLocation = (PreferenceScreen) this.findPreference("citybikes.preferences_location");
		
		//autoNetwork = (CheckBoxPreference) this.findPreference("autofind_network");
		
		manualNetwork = (PreferenceScreen) this.findPreference("citybikes.preferences_network");
		
		clearCache = (PreferenceScreen) this.findPreference("citybikes.preferences_cache_network");
		
		this.context = getApplicationContext();
		
		
		psLocation.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						// TODO Auto-generated method stub
						launchLocationSettings();
						return false;
					}
				});
		/*autoNetwork.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				manualNetwork.setEnabled(!(Boolean) newValue);
				return true;
			}
			
		});
		manualNetwork.setEnabled(!autoNetwork.isChecked());*/
		manualNetwork.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				launchManualNetworkSettings();
				return false;
			}
		});
		
		clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener (){
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences settings = getApplicationContext().getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
				SharedPreferences.Editor editor = settings.edit();
				editor.remove("network_url");
				editor.remove("reload_network");
				editor.remove("network_id");
				editor.remove("last_updated");
				editor.remove("stations");
				editor.remove("networks");
				editor.putBoolean("reload_network", true);
				editor.commit();
				return false;
			}
		});
	}
	
	private void launchLocationSettings() {
		final Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		this.startActivity(intent);
	}
	
	private void launchManualNetworkSettings() {
		final Intent intent = new Intent(this,BikeNetworkActivity.class);
		this.startActivity(intent);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		//Log.i("CityBikes",preference.getKey());
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.setResult(RESULT_OK);
	}
}
