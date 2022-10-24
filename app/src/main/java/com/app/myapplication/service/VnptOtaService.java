package com.app.myapplication.service;

import java.util.Calendar;

import android.app.DownloadManager;
import android.app.Service;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfo;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoManager;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoUtils;
import com.app.myapplication.ui.AlertUpgradeFirmware;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;



public class VnptOtaService extends Service{

	private static final String LOG_TAG = "VnptOtaService";
	private FirmwareInfoManager fmrInfo = null;
	private Context context;
	private VnptOtaState otaState = null;
	final Handler handler = new Handler();
	private OtaBinder mBinder = new OtaBinder();
	private OtaIntentReceiver mOtaIntentReceiver;
	private VnptOtaDownloadService mDownloadService = null;
	private DownloadManager manualDownloadMgr = null;
	private FirmwareInfoManager manualFirmwareInfo = null;
	private boolean isUseDeltaFw = true;
	private String basicFwFilePath = "";

	private class MSG_TYPE {
		public final static int DOWNLOAD_COMPLETED = 1;
		public final static int DOWNLOAD_INPROGESS = 2;
		public final static int DOWNLOAD_ERROR = 3;
		public final static int REBOOT_RECOVERY = 4;
		public final static int ERROR_FIRMWARE_FILE = 5;
		public final static int VALIDATE_SUCCESSFULLY = 6;
		public final static int DOWNLOAD_COMPLETED_FW_BASIC = 7;
	}

	public class OtaBinder extends Binder {
		public VnptOtaService getService() {
			return VnptOtaService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		VnptOtaUtils.LogError(LOG_TAG, "Start VNPT Tech OTA Service");
		
		registerBroadCast();
		initializeService();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mOtaIntentReceiver);
		if(mDownloadService != null)
			mDownloadService.stopDownload();
	}

	private void initializeService(){

		context = getApplicationContext();
		otaState = VnptOtaState.getInstance(context);

		if (otaState.getState() == VnptOtaState.STATE_URGRADE_FIRMWARE && !VnptOtaUtils.isForceUpdate()) {

			VnptOtaUtils.LogDebug(LOG_TAG, "start warning dialog; firmware version: " + otaState.getFirmwareVersion());

			Intent iWarning = new Intent(context, AlertUpgradeFirmware.class);
			iWarning.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			if (VnptOtaUtils.upgradeSuccessfully(otaState.getFirmwareVersion())) {

				VnptOtaUtils.LogDebug(LOG_TAG, "upgrade successfully");

				iWarning.putExtra(AlertUpgradeFirmware.WARNIG_UPGRADE_SUCCESSFULLY,
						AlertUpgradeFirmware.UPGRADE_SUCCESSFULLY);
				context.startActivity(iWarning);

			} else {
				if(VnptOtaUtils.isBasicFwVersion()){

					VnptOtaUtils.LogDebug(LOG_TAG, "upgrade to firmware basic");

					iWarning.putExtra(AlertUpgradeFirmware.WARNIG_UPGRADE_SUCCESSFULLY,
							AlertUpgradeFirmware.UPGRADE_FART);
					context.startActivity(iWarning);

				}else{

					VnptOtaUtils.LogDebug(LOG_TAG, "upgrade failed");

					iWarning.putExtra(AlertUpgradeFirmware.WARNIG_UPGRADE_SUCCESSFULLY,
							AlertUpgradeFirmware.UPGRADE_FAILD);
					context.startActivity(iWarning);
				}

			}
		}

		if (VnptOtaUtils.isAutoQuery(context)) {

			VnptOtaUtils.LogError(LOG_TAG, "set up auto query");

			if(VnptOtaUtils.isForceUpdate()){
				if(otaState.getState() != VnptOtaState.STATE_WAITING && otaState.getState() != VnptOtaState.STATE_UPGRADE)
					otaState.setState(VnptOtaState.STATE_IDLE);
			} else {
				otaState.setState(VnptOtaState.STATE_IDLE);
			}
			if(!handler.postDelayed(queryFirmware, (5 * 60 * 1000))) {

				VnptOtaUtils.setRepeatAlarm(context, AlarmManager.RTC_WAKEUP, Calendar.getInstance()
								.getTimeInMillis() + (5 * 60 * 1000),
						VnptOtaUtils.AUTO_QUERY_INTERVAL_TIME,
						VnptOtaUtils.ACTION_QUERY_FIRMWARE);
			}
		}
	}

	//đăng ký broadCast
	private void registerBroadCast() {
		mOtaIntentReceiver = new OtaIntentReceiver();
		IntentFilter mFilter = new IntentFilter();

		mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		mFilter.addAction("com.tr069.cmds.UPDATE_FIRMWARE");
		mFilter.addAction(VnptOtaUtils.ACTION_QUERY_FIRMWARE);
		mFilter.addAction(VnptOtaUtils.ACTION_MANUAL_DOWNLOAD);
		registerReceiver(mOtaIntentReceiver, mFilter);
	}

	/**
	 * Receives intents from network, tr069 service and query schedule
	 */
	private class OtaIntentReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE")) {

				VnptOtaUtils.LogError(LOG_TAG, "action connectivity change");

				if ((otaState.getState() == VnptOtaState.STATE_QUERY_FIRMWARE) 
					|| (otaState.getState() == VnptOtaState.STATE_QUERY)) {
					if (VnptOtaUtils.isNetworkConnected(context)) {
						fmrInfo = new FirmwareInfoManager(context, fimwareInfoListener);
						otaState.setState(VnptOtaState.STATE_QUERYING);
						fmrInfo.execute();
					}

				} else if(otaState.getState() == VnptOtaState.STATE_DOWNLOADING
						&& mDownloadService != null && VnptOtaUtils.isNetworkConnected(context)) {
					mDownloadService.startDownload();
				} else if(otaState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS || otaState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD) {
					if (manualDownloadMgr != null && VnptOtaUtils.isNetworkConnected(context)) {

						VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: start download again after have internet!");
						
						manualDownloadMgr.startDownload();
					}
				}
			} else if (action.equalsIgnoreCase("com.tr069.cmds.UPDATE_FIRMWARE")) {
				VnptOtaUtils.LogError(LOG_TAG, "@@@ Received OTA_TRIGGER");
				if ((otaState.getState() == VnptOtaState.STATE_IDLE)
						|| (otaState.getState() == VnptOtaState.STATE_QUERY_FIRMWARE)
						|| (otaState.getState() == VnptOtaState.STATE_QUERY)) {
					if (VnptOtaUtils.isNetworkConnected(context)) {
						Bundle bundle = intent.getExtras();
						String target_fw_version = bundle.getString("fw_version").toString();
						String force_upgrade = bundle.getString("force_upgrade").toString();

						if (!target_fw_version.equals("0.0.0")) // target_fw_version != 0.0.0 -> check force version
						{
							VnptOtaUtils.LogDebug(LOG_TAG, "@@@ target_fw_version != 0.0.0 -> check force version " + target_fw_version);
							VnptOtaUtils.target_fw_version = target_fw_version;
						} else { // target_fw_version = 0.0.0 -> check lastest version
							VnptOtaUtils.LogDebug(LOG_TAG, "@@@ target_fw_version = 0.0.0 -> check lastest version");
							VnptOtaUtils.target_fw_version = "0.0.0";
						}
						VnptOtaUtils.force_upgrade = force_upgrade;
						otaState.setState(VnptOtaState.STATE_QUERYING);
						fmrInfo = new FirmwareInfoManager(context, fimwareInfoListener);
						fmrInfo.execute();
					} else {
						VnptOtaUtils.LogError(LOG_TAG, "@@@ No Network Connected");
					}
				}

			} else if (action.equalsIgnoreCase(VnptOtaUtils.ACTION_QUERY_FIRMWARE)) {
				VnptOtaUtils.LogError(LOG_TAG, "action query firmware; state: " + otaState.getState());
				if ((otaState.getState() == VnptOtaState.STATE_IDLE)
						|| (otaState.getState() == VnptOtaState.STATE_QUERY_FIRMWARE)
						|| (otaState.getState() == VnptOtaState.STATE_QUERY)) {
					if (VnptOtaUtils.isNetworkConnected(context)) {
						otaState.setState(VnptOtaState.STATE_QUERYING);
						fmrInfo = new FirmwareInfoManager(context, fimwareInfoListener);
						fmrInfo.execute();
					}
				}
			} else if (action.equalsIgnoreCase(VnptOtaUtils.ACTION_MANUAL_DOWNLOAD)) {
				VnptOtaUtils.LogError(LOG_TAG, "Start Download manual by OTA Service!");

				manualFirmwareInfo = new FirmwareInfoManager(context, manualFimwareInfoListener);
				manualFirmwareInfo.execute();

				// Intent newIntent = new Intent(context, VnptOtaUI.class);

				// try {
				// 	manualFirmwareInfo = FirmwareInfoUtils.getFirmwareInfoFromIntent(newIntent);
				// } catch (Exception e){
				// 	VnptOtaUtils.LogError(LOG_TAG, "---Download manual: Can't get FirmwareInfo from itent!");
				// }

				// VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: check fwUrl info: " + manualFirmwareInfo.getFirmwareUrl());
				// VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: check fwBasic URL info: " + manualFirmwareInfo.getFwBasicUrl());

				// manualDownloadMgr = new DownloadManager(context, VnptOtaUtils.FIRMWARE_FILE_LOCATION, manualFirmwareInfo.getFirmwareUrl(), manualFirmwareInfo.getFwBasicUrl(), true);  //nctmanh: 02112016 - for new ota flow 
				// manualDownloadMgr.addListener(dlListener);

				// manualDownloadMgr.startDownload();
			}
		}
	}

	private final Runnable queryFirmware = new Runnable(){
		public void run(){
			try {
				if (VnptOtaUtils.isNetworkConnected(context)) {

					VnptOtaUtils.LogError(LOG_TAG, "queryFirmware....");

					VnptOtaUtils.setRepeatAlarm(context, AlarmManager.RTC_WAKEUP, Calendar.getInstance()
						.getTimeInMillis() + VnptOtaUtils.AUTO_QUERY_INTERVAL_TIME, 
						VnptOtaUtils.AUTO_QUERY_INTERVAL_TIME, 
						VnptOtaUtils.ACTION_QUERY_FIRMWARE);

					if ((otaState.getState() == VnptOtaState.STATE_IDLE)
							|| (otaState.getState() == VnptOtaState.STATE_QUERY)
							|| (otaState.getState() == VnptOtaState.STATE_QUERY_FIRMWARE)
						){
						otaState.setState(VnptOtaState.STATE_QUERYING);
						fmrInfo = new FirmwareInfoManager(context, fimwareInfoListener);
						fmrInfo.execute();
					}
				} else {

					VnptOtaUtils.LogError(LOG_TAG, "post queryFirmware again....");

					handler.postDelayed(queryFirmware, (5 * 60 * 1000));
				}  
			}
			catch (Exception e) {
				e.printStackTrace();
			}   
		}
	};

	private FirmwareInfoManager.FirmwareInfoListerner fimwareInfoListener = new FirmwareInfoManager.FirmwareInfoListerner(){
		@Override
		public void onError(int err) {
			if (otaState != null) {
				if (err == FirmwareInfoUtils.NO_FIRMWARE_UPDATE)
					otaState.setState(VnptOtaState.STATE_IDLE);
				else
					otaState.setState(VnptOtaState.STATE_QUERY);
			}
		}

		@Override
		public void haveFirmwareUpdate(FirmwareInfo info) {

			VnptOtaUtils.LogError(LOG_TAG, "firmware is available; start activity");

			if(otaState.getState() == VnptOtaState.STATE_SHOW_UI
					|| otaState.getState() == VnptOtaState.STATE_SHOW_UI_MANUAL
					|| otaState.getState() == VnptOtaState.STATE_QUERYING_MANUAL
					|| otaState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS
					|| otaState.getState() == VnptOtaState.STATE_DOWNLOAD_COMPLETED
					|| otaState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE
					|| otaState.getState() == VnptOtaState.STATE_MANUAL_QUERY
					|| otaState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD
					|| otaState.getState() == VnptOtaState.STATE_MANUAL_PAUSE
			) {

				VnptOtaUtils.LogError(LOG_TAG, "UI is showing -> not show popup alert ...")	;

				return;
			}
			if(VnptOtaUtils.isForceUpdate()) {
				mDownloadService = new VnptOtaDownloadService(context, info);
				mDownloadService.startDownload();
			} else {
				AlertHaveFirmware(context, info);    ///nctmanh: 05082015 - allow check new ota fw in mobile data connection
			}
		}
	};	

	///nctmanh: 05082015 - allow check new ota fw in mobile data connection - @{
	public void AlertHaveFirmware(Context mCtx, FirmwareInfo info){
		if (otaState.getVnptOtaUiIsRunning() == VnptOtaState.VNPTOTA_UI_IS_RUNNING){ 

			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: not show AlertDialog if VNPTOTA_UI_IS_RUNNING");

			return;
		} else {

			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: show AlertDialog because VNPTOTA_UI_NOT_RUNNING");
		
		}

		Intent startIntent = new Intent(mCtx, AlertNewFirmwareAvailable.class);
		FirmwareInfoUtils.addFirmwareInfoToIntent(startIntent, info);
		startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mCtx.startActivity(startIntent);
	}
	///@}

	// KHAICUCAI add
	private DownloaderListener dlListener = new DownloaderListener() {
		String downloadProgessBroadcast = VnptOtaUtils.ACTION_DOWNLOAD_LISTENER;
		Intent dlProgessIntent = new Intent();

		@Override
		public void downloadSuccess(File tempFile) {

			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: check VnptOtaUiIsRunning in Delta: " + otaState.getVnptOtaUiIsRunning());

			if (otaState.getVnptOtaUiIsRunning() == VnptOtaState.VNPTOTA_UI_IS_RUNNING){
				dlProgessIntent.setAction(downloadProgessBroadcast);
	        	dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DL_LISTENER, downloadProgessBroadcast);
				dlProgessIntent.putExtra(VnptOtaUtils.INTEND_MSG_TYPE, MSG_TYPE.DOWNLOAD_COMPLETED);
				dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, VnptOtaUtils.INTEND_DL_DELTA_DONE);
				dlProgessIntent.putExtra(VnptOtaUtils.INTEND_FILE_PATH, tempFile.getAbsolutePath());
				sendBroadcast(dlProgessIntent);

			} else {

				VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: call AlertDownloadFirmwareDone!");
				
				Intent startIntent = new Intent(context, AlertDownloadFirmwareDone.class);

				startIntent.putExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, VnptOtaUtils.INTEND_DL_DELTA_DONE);
				startIntent.putExtra(VnptOtaUtils.INTEND_FILE_PATH, tempFile.getAbsolutePath());

				// Put basic file path in case download both delta an basic
				if (isUseDeltaFw){
					startIntent.putExtra(VnptOtaUtils.INTEND_BASIC_FILE_PATH_EXTEND, basicFwFilePath);
				}

				startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(startIntent);
			}
		}
		
		@Override
		public void downloadSuccessFileBasic(File tempFwBasicFile) {

			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: check VnptOtaUiIsRunning in Basic: " + otaState.getVnptOtaUiIsRunning());

			if (otaState.getVnptOtaUiIsRunning() == VnptOtaState.VNPTOTA_UI_IS_RUNNING){
				dlProgessIntent.setAction(downloadProgessBroadcast);
	        	dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DL_LISTENER, downloadProgessBroadcast);
				dlProgessIntent.putExtra(VnptOtaUtils.INTEND_MSG_TYPE, MSG_TYPE.DOWNLOAD_COMPLETED);
				dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, VnptOtaUtils.INTEND_DL_BASIC_DONE);
				dlProgessIntent.putExtra(VnptOtaUtils.INTEND_FILE_PATH, tempFwBasicFile.getAbsolutePath());
				sendBroadcast(dlProgessIntent);
			} else {
				if(!isUseDeltaFw){// not use delta FW => download complete
					
					VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: call AlertDownloadFirmwareDone!");
					
					Intent startIntent = new Intent(context, AlertDownloadFirmwareDone.class);

					startIntent.putExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, VnptOtaUtils.INTEND_DL_BASIC_DONE);
					startIntent.putExtra(VnptOtaUtils.INTEND_FILE_PATH, tempFwBasicFile.getAbsolutePath());

					startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(startIntent);
				} else {
					basicFwFilePath = tempFwBasicFile.getAbsolutePath();
				}
			}
		}
		@Override
		public void downloadError(int errorCode) {
			// Message m = Message.obtain(handler, MSG_TYPE.DOWNLOAD_ERROR, Integer.valueOf(errorCode));
			// m.sendToTarget();

			dlProgessIntent.setAction(downloadProgessBroadcast);
        	dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DL_LISTENER, downloadProgessBroadcast);
			dlProgessIntent.putExtra(VnptOtaUtils.INTEND_MSG_TYPE, MSG_TYPE.DOWNLOAD_ERROR);
			dlProgessIntent.putExtra(VnptOtaUtils.INTEND_ERROR_CODE, errorCode);
			sendBroadcast(dlProgessIntent);
		}

		@Override
		public void downloadProgess(long downloadingSize, long progress) {
			// Message m = Message.obtain(handler, MSG_TYPE.DOWNLOAD_INPROGESS, 
			// 		(int) downloadingSize, (int) progress);
			// m.sendToTarget();

			dlProgessIntent.setAction(downloadProgessBroadcast);
        	dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DL_LISTENER, downloadProgessBroadcast);
			dlProgessIntent.putExtra(VnptOtaUtils.INTEND_MSG_TYPE, MSG_TYPE.DOWNLOAD_INPROGESS);
			dlProgessIntent.putExtra(VnptOtaUtils.INTEND_DOWLOADING_SIZE, (int) downloadingSize);		
			dlProgessIntent.putExtra(VnptOtaUtils.INTEND_PROGRESS, (int) progress);	
			sendBroadcast(dlProgessIntent);
		}
	};

	private FirmwareInfoListerner manualFimwareInfoListener = new FirmwareInfoListerner(){
		@Override
		public void onError(int err) {
			if (otaState != null) {
				if (err == FirmwareInfoUtils.NO_FIRMWARE_UPDATE)
					otaState.setState(VnptOtaState.STATE_IDLE);
				else
					otaState.setState(VnptOtaState.STATE_QUERY);
			}
		}

		@Override
		public void haveFirmwareUpdate(FirmwareInfo info) {
			URL fwBasicFileUrl = null;
			boolean isDownloadedFwBasic = true;
			boolean isUseBasicFw = !TextUtils.isEmpty(info.getFwBasicUrl());

			if (isUseBasicFw){
				try {
					fwBasicFileUrl = new URL(info.getFwBasicUrl());
					isDownloadedFwBasic = isDownloadedFwBasic(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION, new File(fwBasicFileUrl.getFile()).getName(), info);
				} catch (MalformedURLException e) {
					fwBasicFileUrl = null;
					isDownloadedFwBasic = true;
				}
			}

			otaState.setFirmwareInfoFromSharedPreferences(info);
			isUseDeltaFw = !TextUtils.isEmpty(info.getFirmwareUrl());

			manualDownloadMgr = new DownloadManager(context, VnptOtaUtils.FIRMWARE_FILE_LOCATION, info.getFirmwareUrl(), info.getFwBasicUrl(), isDownloadedFwBasic);
			manualDownloadMgr.addListener(dlListener);

			manualDownloadMgr.startDownload();
		}
	};	

	private boolean isDownloadedFwBasic(String filePath, String fileName, FirmwareInfo firmwareInfo){
		File file = new File(filePath, fileName);
		if(file.exists()){
			if(file.length() == firmwareInfo.getFwBasicSize()){
				return true;
			}
			if(file.length() > firmwareInfo.getFwBasicSize()){
				delOldFwBasic();
			}
		} else {
			delOldFwBasic();
		}
		return false;
	}

	private void delOldFwBasic(){
		File dir = new File(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION); 
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();	//nctmanh: 02112016 - for new ota flow
			for (int i = 0; i < children.length; i++) {
				children[i].delete();
			}
		}
	}
}
