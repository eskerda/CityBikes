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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class CityBikes {
	public static final String PREFERENCES_NAME = "citybikes";
	public static final int BOOKMARK_CHANGE = 42;
	
	public static final boolean isC2DMReady(Context ctx) {
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		Log.i("CityBikes","SDK Version is "+Integer.toString(SDK_INT));
		SharedPreferences settings = ctx.getSharedPreferences(CityBikes.PREFERENCES_NAME,0);
		String netnick = settings.getString("network_name", "error");
		//For now it only works on SDK 2.2 and Barcelona Bicing!!!
		boolean ready = SDK_INT >= 8 && netnick.compareTo("bicing") == 0;
		Log.i("CityBikes","Is C2DM Ready: "+Boolean.toString(ready));
		return ready;
	}
	
	public static final void showCustomToast(Context context, Activity ref, CharSequence text, int DURATION){
		showCustomToast(context, ref, text, DURATION, Gravity.CENTER_HORIZONTAL | Gravity.CENTER);
	}
	
	public static final void showCustomToast(Context context, Activity ref, CharSequence text, int DURATION, int GRAVITY){
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View layout = inflater.inflate(R.layout.custom_toast,
		                               (ViewGroup) ref.findViewById(R.id.custom_toast_layout_root));

		TextView textView = (TextView) layout.findViewById(R.id.custom_toast_text);
		textView.setText(text);

		Toast toast = new Toast(context);
		toast.setGravity(GRAVITY, 0, 0);
		toast.setDuration(DURATION);
		toast.setView(layout);
		toast.show();
	}
	
	public static final void showCustomToast(Context context, View ref, CharSequence text, int DURATION){
		showCustomToast(context, ref, text, DURATION, Gravity.CENTER_HORIZONTAL | Gravity.CENTER);
	}
	public static final void showCustomToast(Context context, View ref, CharSequence text, int DURATION, int GRAVITY){
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View layout = inflater.inflate(R.layout.custom_toast,
		                               (ViewGroup) ref.findViewById(R.id.custom_toast_layout_root));

		TextView textView = (TextView) layout.findViewById(R.id.custom_toast_text);
		textView.setText(text);

		Toast toast = new Toast(context);
		toast.setGravity(GRAVITY, 0, 0);
		toast.setDuration(DURATION);
		toast.setView(layout);
		toast.show();
	}
}
