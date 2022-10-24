package com.app.myapplication.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.app.myapplication.R;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfo;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoManager;

public class AlertDownloadFirmwareDone extends Activity {
	private static final String TAG = "AlertDownloadFirmwareDone";
	private FirmwareInfoManager fmrInfo = null;
	private FirmwareInfo firmwareInfo = null;
	private boolean option_pressed = false;
	private VnptOtaState otaState = null;
	private boolean onlyFWBasic = false;
	private static final int DOWNLOAD_COMPLETED = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		otaState = VnptOtaState.getInstance(getApplicationContext());
		// if(otaState != null)
		// 	otaState.setState(VnptOtaState.STATE_IDLE);
		Intent intent = getIntent();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_download_firmware_done_content)
		.setTitle(R.string.alert_download_firmware_done_title)
		.setCancelable(false)
		.setIcon(R.drawable.stat_download_detected)
		.setPositiveButton(this.getResources().getString(R.string.alert_btn_later), 
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

				VnptOtaUtils.LogDebug(TAG, "press later");

				otaState.setState(VnptOtaState.STATE_DOWNLOAD_COMPLETED);
				option_pressed = true;
				// if(otaState != null)
				// 	otaState.setState(VnptOtaState.STATE_IDLE);
				AlertDownloadFirmwareDone.this.finish();
			}
		})
		.setNegativeButton(this.getResources().getString(R.string.alert_btn_continue), 
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				VnptOtaUtils.LogDebug(TAG, "press continue");

				option_pressed = true;
				AlertDownloadFirmwareDone.this.finish();
				
				Intent mIntent = new Intent(getApplicationContext(), VnptOtaUI.class);

				mIntent.putExtra(VnptOtaUtils.INTEND_MSG_TYPE, DOWNLOAD_COMPLETED);
				mIntent.putExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, intent.getIntExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, -1));
				mIntent.putExtra(VnptOtaUtils.INTEND_FILE_PATH, intent.getStringExtra(VnptOtaUtils.INTEND_FILE_PATH));
				mIntent.putExtra(VnptOtaUtils.INTEND_BASIC_FILE_PATH_EXTEND, intent.getStringExtra(VnptOtaUtils.INTEND_BASIC_FILE_PATH_EXTEND));

				mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(mIntent);
			}
		}).create().show();

		VnptOtaUtils.LogDebug(TAG, "update state to...." + VnptOtaState.STATE_SHOW_UI);
		// if(otaState != null)
		// 	otaState.setState(VnptOtaState.STATE_SHOW_UI);
	}
	
	@Override
	protected void onStop(){

		VnptOtaUtils.LogDebug(TAG, "onStop....");
		
		super.onStop();
		if(firmwareInfo != null && !option_pressed) {
			Intent intent = new Intent(this, AlertDownloadFirmwareDone.class);
			// FirmwareInfoUtils.addFirmwareInfoToIntent(intent, firmwareInfo);
			startActivity(intent);
		}
	}
}
