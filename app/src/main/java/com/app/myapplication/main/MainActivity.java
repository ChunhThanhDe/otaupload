package com.app.myapplication.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.app.myapplication.R;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.ui.VnptOtaUI;


public class MainActivity extends PreferenceActivity{

	final static String TAG = "OTA_MainActivity";
	protected static final String NOTIF_ACTION = "vnpt.updater.action.NOTIF_ACTION";
	
	private boolean dialogFromNotif = false;
	private boolean checkOnResume = false;
	
	private Preference availUpdatePref;
	private Preference localUpdatePref;

	private VnptOtaState otaState;

	/*
	 * Called when the activity is first created
	 */

	// để loaị bỏ warning không còn dùng nữa 
	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG,"On creating");
		
		addPreferencesFromResource(R.xml.main); // drawing xml
		
		final Preference device = findPreference("device_view");
		Log.d(TAG, "Finding device: " + device.toString());
		device.setSummary(android.os.Build.MODEL);
		
		final Preference rom = findPreference("rom_view");
		Log.d(TAG, "Finding rom: " + rom.toString());
		rom.setSummary(android.os.Build.DISPLAY);
		
		final Preference build = findPreference("otaid_view");
		Log.d(TAG, "Finding build: " + build.toString());
		build.setSummary(R.string.application_version);
		
		Log.d(TAG, "Finding update online: ");
		availUpdatePref = findPreference("avail_updates");
		
		Log.d(TAG, "Finding update local: ");
		localUpdatePref = findPreference("update_usb");	

		otaState = VnptOtaState.getInstance(getApplicationContext());
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference ) {
		if (otaState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS || otaState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD){
			
			Log.d(TAG,"Coming to VnptOtaUI, because of DOWNLOAD_INPROGRESS");

			Intent intent = new Intent(getApplicationContext(), VnptOtaUI.class);
			startActivity(intent);
			return true;
		}

		if (preference == availUpdatePref) {

			Log.d(TAG,"Checking avaiable update");
			//~ checkForRomUpdates();
			Intent intent = new Intent();
			intent.setComponent(new 
				ComponentName("com.vnptt.ota",
						"com.vnptt.ota.UpdateFwUIActivity"));
			startActivity(intent);
			return true;
		} else if (preference == localUpdatePref) {
			Intent intent = new Intent();
			intent.setComponent(new 
				ComponentName("com.vnptt.ota",
						"com.vnptt.ota.main.UpdateLocal"));
			startActivity(intent);
			return true;
		}
		return false;
	}
	
	
	private void checkForRomUpdates() {

		Log.d(TAG,"checkForRomUpdates....");
		
		Toast.makeText(this, getString(R.string.toast_no_updates), 2000)
                .show();
	}
	
}
