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
import com.app.myapplication.getfirmwareinfo.FirmwareInfoUtils;


public class AlertUpgradeFirmware extends Activity {

	private static final String TAG = "AlertUpgradeFirmware";

	public static final String WARNIG_UPGRADE_SUCCESSFULLY = "warning_upgrade_successfully";
	public static final int UPGRADE_SUCCESSFULLY = 1;
	public static final int UPGRADE_FAILD = 2;
	public static final int UPGRADE_FART = 3;	//nctmanh: 11012017 - notifiy continue update for new ota flow  
	private FirmwareInfoManager fmrInfo = null;

	private int isUpgradeSuccessfully;
	
	private VnptOtaState otaState = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		VnptOtaUtils.LogDebug(TAG, "onCreate()");
		otaState = VnptOtaState.getInstance(this);
		isUpgradeSuccessfully = getIntent().getIntExtra(WARNIG_UPGRADE_SUCCESSFULLY, -1);

		VnptOtaUtils.LogDebug(TAG, "isUpgradeSuccessfully: " + isUpgradeSuccessfully);
		
		switch (isUpgradeSuccessfully) {
		case UPGRADE_SUCCESSFULLY:
			AlertDialog.Builder mBuilder1 = new AlertDialog.Builder(this);
			mBuilder1.setTitle(R.string.alert_upgrade_successfully_title)
			.setMessage(R.string.alert_upgrade_successfully_content)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					android.os.Process.killProcess(android.os.Process.myPid());
					finish();
				}
			})
			.create().show();
			break;

		case UPGRADE_FAILD:
			AlertDialog.Builder mBuilder2 = new AlertDialog.Builder(this);
			mBuilder2.setTitle(R.string.alert_upgrade_faild_title)
			.setMessage(R.string.alert_upgrade_faild_content)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_btn_download_again, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					fmrInfo = new FirmwareInfoManager(getApplicationContext(), fimwareInfoListener);
					fmrInfo.execute();
				}
			})
			.setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					android.os.Process.killProcess(android.os.Process.myPid());
					finish();
				}
			})
			.create().show();
			break;
		//nctmanh: 11012017 - notifiy continue update for new ota flow-@{
		case UPGRADE_FART:
			AlertDialog.Builder mBuilder3 = new AlertDialog.Builder(this);
			mBuilder3.setTitle(R.string.alert_title_warning)
			.setMessage(R.string.alert_upgrade_part_content)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_btn_continue_upgrade, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					fmrInfo = new FirmwareInfoManager(getApplicationContext(), fimwareInfoListener);
					fmrInfo.execute();
				}
			})
			.setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					android.os.Process.killProcess(android.os.Process.myPid());
					finish();
				}
			})
			.create().show();
			break;
		//@}
		
		default:
			break;
		}
	}
	
	private FirmwareInfoManager.FirmwareInfoListerner fimwareInfoListener = new FirmwareInfoManager.FirmwareInfoListerner(){
		@Override
		public void onError(int err) {
		}

		@Override
		public void haveFirmwareUpdate(FirmwareInfo info) {
			Intent mIntent = new Intent(getApplicationContext(), VnptOtaUI.class);
			FirmwareInfoUtils.addFirmwareInfoToIntent(mIntent, info);
			mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(mIntent);		
		}
	};

}
