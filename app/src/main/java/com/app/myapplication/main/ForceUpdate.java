package com.app.myapplication.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;

import com.app.myapplication.R;
import com.app.myapplication.common.RecoveryUtil;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;

import java.io.File;



// buộc cập nhật 
public class ForceUpdate extends Activity{

	final static String TAG = "ForceUpdate";

	static boolean mNetworkReceiver = false;
	final Handler mHandler = new ForceUpdateHandler();
	ProgressDialog mProgressDialogWaiting = null;
	ProgressDialog mProgressDialog = null;
	private VnptOtaState otaState = null;
	AlertDialog mAlertDialog = null;
	Thread verifyFwThread;
	public boolean gotoHome = false;
	
	private class MSG_TYPE {
		public final static int START_LAUNCHER = 1;
		public final static int VERIFY_FIRMWARE = 2;
		public final static int VERIFY_SUCCESS = 3;
		public final static int REBOOT_RECOVERY = 4;
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNetworkReceiver = false;
        // instance : yêu cầu 
		otaState = VnptOtaState.getInstance(this);

		VnptOtaUtils.LogDebug(TAG, "onCreate...otaState=" + otaState.getState());

		//bật tính năng cửa sổ mở rộng 
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.force_update);

		if(otaState.getState() == VnptOtaState.STATE_WAITING){

			SystemProperties.set("mbx.system.updating", "true");

			showAlertUpdateFW();

		} else if(otaState.getState() == VnptOtaState.STATE_UPGRADE){
			//hiển thị Kết quả Cập nhật Cảnh báo
			showAlertUpdateResult();

			//ghi đè lên thuộc tính của hệ thống để 
			SystemProperties.set("mbx.system.updating", "false");
		} else {
			goHome();
		}
    }

	public void showAlertUpdateResult(){

		AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);

		//thông báo đã update thành công hay chưa 
		if (VnptOtaUtils.upgradeSuccessfully(otaState.getFirmwareVersion())) {

			mBuilder.setTitle(R.string.alert_upgrade_successfully_title);
			mBuilder.setMessage(R.string.alert_upgrade_successfully_content);

		} else {

			VnptOtaUtils.LogDebug(TAG, "upgrade failed");

			mBuilder.setTitle(R.string.alert_upgrade_faild_title);
			mBuilder.setMessage(R.string.alert_upgrade_faild_content);
		}


		mBuilder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				VnptOtaUtils.LogDebug(TAG, "go home 1");

				goHome();

				ForceUpdate.this.finish();

			}
		});


		mAlertDialog = mBuilder.show();
		mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).requestFocus();
		mAlertDialog.setCanceledOnTouchOutside(false);
		mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				ForceUpdate.this.finish();
			}
		});
		
		mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);
		mHandler.sendEmptyMessageDelayed(MSG_TYPE.START_LAUNCHER, 10*1000);//delay 10 seconds then start Launcher
	}

	// hiển thị Kết quả Cập nhật Cảnh báo
	public void showAlertUpdateFW(){
		mProgressDialog = new ProgressDialog(this/*, R.style.MyTheme*/);
		mProgressDialog.setMessage(getResources().getString(R.string.wait_for_upgrading_system));
		mProgressDialog.setCancelable(false);
		mProgressDialog.show();
		mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);
		mHandler.removeMessages(MSG_TYPE.VERIFY_FIRMWARE);

		verifyFwThread = new Thread(verifyFwFiles);

		mHandler.sendEmptyMessageDelayed(MSG_TYPE.VERIFY_FIRMWARE, 5*1000);
		mHandler.sendEmptyMessageDelayed(MSG_TYPE.START_LAUNCHER, 20*1000);//after 15 seconds, can not detect upgrade requirement and verify fws -> start launcher.
	}


	private final Runnable verifyFwFiles = new Runnable(){
		public void run(){

			String fwBasicFilePath = otaState.getFirmwareBasic();
			String fwDeltaFilePath = otaState.getFirmwareDelta();
			String fwBasicMd5 = otaState.getFirmwareBasicMD5();
			String fwBDeltaMd5 = otaState.getFirmwareDeltaMD5();
			if(TextUtils.isEmpty(fwBasicFilePath) || TextUtils.isEmpty(fwDeltaFilePath) 
				|| TextUtils.isEmpty(fwBasicMd5) || TextUtils.isEmpty(fwBDeltaMd5) 
			){

				VnptOtaUtils.LogError(TAG, "The path of files are not exist or md5s are empty!");
				
				mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);
				mHandler.sendEmptyMessage(MSG_TYPE.START_LAUNCHER);

			} else {
				mHandler.post(verifyFwFiles);
				File fileBasic = new File(fwBasicFilePath);
				File fileDelta = new File(fwDeltaFilePath);
				//verify all fw files before updating
				if(fileBasic.exists() && fileDelta.exists() 
					&& VnptOtaUtils.getMd5sum(fileBasic).equalsIgnoreCase(fwBasicMd5)
					&& VnptOtaUtils.getMd5sum(fileDelta).equalsIgnoreCase(fwBDeltaMd5)
				){
					// KHAIDT: todo... need modify if use Force Update mode
					if (RecoveryUtil.upgradeDeltaPackage(null, fwBasicFilePath, fwDeltaFilePath)) {	//nctmanh: 02112016 - for new ota flow
						mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);

						VnptOtaUtils.LogDebug(TAG, "Verify success. Send msg REBOOT_RECOVERY...");
						
						mHandler.sendEmptyMessageDelayed(MSG_TYPE.REBOOT_RECOVERY, 2*1000);
						mHandler.sendEmptyMessageDelayed(MSG_TYPE.START_LAUNCHER, 15*1000);
					} else {

						VnptOtaUtils.LogError(TAG, "Can not set command to recovery mode");
						
						mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);
						mHandler.sendEmptyMessage(MSG_TYPE.START_LAUNCHER);
					}
				} else {

					VnptOtaUtils.LogError(TAG, "Verify all fw files failed.");
					
					mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);
					mHandler.sendEmptyMessage(MSG_TYPE.START_LAUNCHER);
				}
			}
		}
	};

	public void goHome(){

		VnptOtaUtils.LogDebug(TAG, "go home");

		SystemProperties.set("mbx.system.updating", "false");

		mHandler.removeCallbacksAndMessages(null);
		otaState.setState(VnptOtaState.STATE_IDLE);
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ForceUpdate.this.startActivity(startMain);
		gotoHome = true;
		ForceUpdate.this.finish();
	}
	
	public void rebootRecovery() {
		try {
			otaState.setState(VnptOtaState.STATE_UPGRADE);
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// set reboot_mode: recovery, update, normal_boot, charging
			pm.reboot("recovery");
		} catch (Exception e) {
			e.printStackTrace();
			mHandler.removeMessages(MSG_TYPE.START_LAUNCHER);
			mHandler.sendEmptyMessage(MSG_TYPE.START_LAUNCHER);
		}
	}
    
    /*BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
			if (VnptOtaUtils.isNetworkConnected(context)) {
				if(mProgressDialogWaiting != null && mProgressDialogWaiting.isShowing())
					mProgressDialogWaiting.dismiss();
				if(!gotoHome) {
					Intent startMain = new Intent(Intent.ACTION_MAIN);
					startMain.addCategory(Intent.CATEGORY_HOME);
					startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(startMain);
					gotoHome = true;
				}
			}
        }
    };*/

    /*BroadcastReceiver bootCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
			if(mProgressDialogWaiting != null && mProgressDialogWaiting.isShowing())
				mProgressDialogWaiting.dismiss();
			if(!gotoHome){
				Intent startMain = new Intent(Intent.ACTION_MAIN);
				startMain.addCategory(Intent.CATEGORY_HOME);
				startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startMain);
				gotoHome = true;
			}
        }
    };*/
    
    private class ForceUpdateHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_TYPE.VERIFY_FIRMWARE:
					verifyFwThread.start();
					break;
				case MSG_TYPE.START_LAUNCHER:
					goHome();
					break;
				case MSG_TYPE.REBOOT_RECOVERY:
					VnptOtaUtils.LogDebug(TAG, "Reboot to recovery mode");
					rebootRecovery();
					break;
			}
		}
	}
	
	@Override
	public void onResume() {
		VnptOtaUtils.LogDebug(TAG, "onResume...");
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		VnptOtaUtils.LogDebug(TAG, "onDestroy...");
		
		if(mProgressDialog != null && mProgressDialog.isShowing())
			mProgressDialog.dismiss();
		if(mProgressDialogWaiting != null && mProgressDialogWaiting.isShowing())
			mProgressDialogWaiting.dismiss();
		if(mAlertDialog != null && mAlertDialog.isShowing())
			mAlertDialog.dismiss();
		mHandler.removeCallbacksAndMessages(null);
		//~ if(mNetworkReceiver) {
			//~ unregisterReceiver(networkReceiver);
			//~ unregisterReceiver(bootCompleteReceiver);
		//~ }
		gotoHome = false;
		mNetworkReceiver = false;
	}
	
}
