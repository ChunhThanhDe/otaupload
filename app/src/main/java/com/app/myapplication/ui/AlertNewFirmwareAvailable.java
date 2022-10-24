/*nctmanh: 05082015 - allow check new ota fw in mobile data connection
 * add file to show alert dialog
*/
package com.app.myapplication.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.app.myapplication.R;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfo;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoManager;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoUtils;


public class AlertNewFirmwareAvailable extends Activity {
	private static final String TAG = "AlertNewFirmwareAvailable";
	private FirmwareInfoManager fmrInfo = null;
	private FirmwareInfo firmwareInfo = null;
	private boolean option_pressed = false;
	private VnptOtaState otaState = null;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		otaState = VnptOtaState.getInstance(getApplicationContext());
		if(otaState != null)
			otaState.setState(VnptOtaState.STATE_IDLE);
		Intent itent = getIntent();
		try {
			firmwareInfo = FirmwareInfoUtils.getFirmwareInfoFromIntent(itent);
		} catch (Exception e){

			VnptOtaUtils.LogError(TAG, "Can not get FW info");
		}		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_have_firmware_content)
		.setTitle(R.string.alert_have_firmware_title)
		.setCancelable(false)
		.setIcon(R.drawable.stat_download_detected)
		.setPositiveButton(this.getResources().getString(R.string.alert_btn_later), 
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

				VnptOtaUtils.LogDebug(TAG, "press later");

				option_pressed = true;
				if(otaState != null)
					otaState.setState(VnptOtaState.STATE_IDLE);
				AlertNewFirmwareAvailable.this.finish();
			}
		})
		.setNegativeButton(this.getResources().getString(R.string.alert_btn_continue), 
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				VnptOtaUtils.LogDebug(TAG, "press continue");

				option_pressed = true;
				AlertNewFirmwareAvailable.this.finish();
				
				Intent mIntent = new Intent(getApplicationContext(), VnptOtaUI.class);
				FirmwareInfoUtils.addFirmwareInfoToIntent(mIntent, firmwareInfo);
				mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(mIntent);
			}
		}).create().show();

		VnptOtaUtils.LogDebug(TAG, "update state to...." + VnptOtaState.STATE_SHOW_UI);

		if(otaState != null)
			otaState.setState(VnptOtaState.STATE_SHOW_UI);
	}
	
	@Override
	protected void onStop(){

		VnptOtaUtils.LogDebug(TAG, "onStop....");
		
		super.onStop();
		if(firmwareInfo != null && !option_pressed) {
			Intent intent = new Intent(this, AlertNewFirmwareAvailable.class);
			FirmwareInfoUtils.addFirmwareInfoToIntent(intent, firmwareInfo);
			startActivity(intent);
		}
	}
}
