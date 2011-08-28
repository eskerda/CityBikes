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

package net.homelinux.penecoptero.android.openbicing.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	PreferenceScreen psLocation;
	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(
				OpenBicing.PREFERENCES_NAME);
		addPreferencesFromResource(R.xml.preferences);

		psLocation = (PreferenceScreen) this.findPreference("openbicing.preferences_location");		
				
		this.context = getApplicationContext();
		
		
		psLocation.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						// TODO Auto-generated method stub
						launchLocationSettings();
						return false;
					}
				});
	}
	
	private void launchLocationSettings() {
		final Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
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
