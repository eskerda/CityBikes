package net.homelinux.penecoptero.android.citybikes.donation.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;



public class PushActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pushactivity);
		Bundle extras = getIntent().getExtras();
		if(extras !=null) {
			String station = extras.getString("station");
			TextView tv = (TextView) findViewById(R.id.debugText);
			tv.setText(station);
		}
	}
}
