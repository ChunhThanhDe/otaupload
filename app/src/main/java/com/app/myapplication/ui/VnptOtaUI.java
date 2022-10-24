package com.app.myapplication.ui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import com.app.myapplication.R;
import com.app.myapplication.common.NotifyManager;
import com.app.myapplication.common.RecoveryUtil;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.download.DownloadManager;
import com.app.myapplication.download.DownloadUtils;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfo;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoManager;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoUtils;

public class VnptOtaUI extends Activity implements OnClickListener, Handler.Callback {

	private static final String LOG_TAG = "VnptOtaUI";
	//	private TextView tvDownloadPercent;
	private Button btnDownload, btnUpgrade, btnPause;
	private DownloadManager dlMgr;
	private String filePath;
	private FirmwareInfo firmwareInfo;
	private FirmwareInfoManager manualFirmwareInfo = null;
	private TextView tvFirmwareVersion1, tvReleaseDate, tvFirmwareSize, tvFirmwareDes, tvDevice;
	private TextView tvTotalSize, tvFileSize, tvFileName, tvPercent, tvFirmwareVersion2;
	private RelativeLayout rlFirmawareVersion, rlProgressDownload;
	private ProgressBar pbDownload;
	private VnptOtaState otaState;
	public static boolean isRunning = false;
	private File fwrFile = null;
	private File fwBasicFile = null; 	//nctmanh: 02112016 - for new ota flow
	private URL fwBasicFileUrl = null; 	//nctmanh: 02112016 - for new ota flow
	private String fwBasicFilePath = null; 	//nctmanh: 02112016 - for new ota flow
	private boolean isDownloadedFwBasic; 	//nctmanh: 02112016 - for new ota flow
	private boolean isDownloadedFwBasicDone = false; 	//nctmanh: 02112016 - for new ota flow
	private boolean isUseBasicFw = true;
	private boolean isUseDeltaFw = true;
	private long totalSizeDownload;
	private int mErrorCode = 0;
	private int downloadingSizeByIntend;
	
	ProgressDialog prgWait;
	NotifyManager notifyMgr;  // remove notification download

	private class MSG_TYPE {
		public final static int DOWNLOAD_COMPLETED = 1;
		public final static int DOWNLOAD_INPROGESS = 2;
		public final static int DOWNLOAD_ERROR = 3;
		public final static int REBOOT_RECOVERY = 4;
		public final static int ERROR_FIRMWARE_FILE = 5;
		public final static int VALIDATE_SUCCESSFULLY = 6;
		public final static int DOWNLOAD_COMPLETED_FW_BASIC = 7;
	}

	Handler handlerUI = new Handler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vnpt_ota_ui);

		otaState = VnptOtaState.getInstance(getApplicationContext());

		Intent itent = getIntent();
		
		// KHAICUCAI modify for download by Intend
		VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI start onCreate in VnptOtaUI!");

		try {
			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI get fw info!");


			// Get fw info in SharedPreferences in Download by Intend mode
			if (otaState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS || 
				otaState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD ||
				otaState.getState() == VnptOtaState.STATE_DOWNLOAD_COMPLETED) {
				VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI continue download by Intend in VnptOtaUI!");
				firmwareInfo = otaState.getFirmwareInfoFromSharedPreferences();

				// Query new fw info if it dosen't exist in SharedPreferences
				if (firmwareInfo.getFirmwareVersion() == null){
					VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI no fw info in SharedPreferences, start query new fw info in VnptOtaUI!");
					manualFirmwareInfo = new FirmwareInfoManager(getApplicationContext(), manualFimwareInfoListener);
					manualFirmwareInfo.execute();
				}
			} else {
				VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI get fw info from Intend in VnptOtaUI!");
				firmwareInfo = FirmwareInfoUtils.getFirmwareInfoFromIntent(itent);
			}
		} catch (Exception e){}

		VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI handle fw info in VnptOtaUI!");

		// KHAICUCAI add
		registerUpdateUIReceiver();

		rlFirmawareVersion = (RelativeLayout) findViewById(R.id.firmware_version_relativelayout);
		rlProgressDownload = (RelativeLayout) findViewById(R.id.progress_relativelayout);

		tvDevice = (TextView) findViewById(R.id.tv_device);
		tvDevice.setText(Build.MODEL);
		tvFirmwareVersion1 = (TextView) findViewById(R.id.tv_firmware_version1);
		tvReleaseDate = (TextView) findViewById(R.id.tv_release_date);
		tvFirmwareSize = (TextView) findViewById(R.id.tv_firmware_size);
		tvFirmwareDes = (TextView) findViewById(R.id.tv_firmware_descripton);

		tvTotalSize = (TextView) findViewById(R.id.tv_total_size);
		tvFileSize = (TextView) findViewById(R.id.tv_file_size);
		tvFileName = (TextView) findViewById(R.id.tv_file_name);
		
		tvPercent = (TextView) findViewById(R.id.tv_download_percent);
		tvFirmwareVersion2 = (TextView) findViewById(R.id.tv_firmware_version2);

		pbDownload = (ProgressBar) findViewById(R.id.progress_download_percent);
		btnDownload = (Button) findViewById(R.id.download_file);
		btnDownload.setOnClickListener(this);
		btnUpgrade = (Button) findViewById(R.id.btn_upgrade);
		btnUpgrade.setOnClickListener(this);
		btnPause = (Button) findViewById(R.id.btn_pause);
		btnPause.setOnClickListener(this);

		filePath = VnptOtaUtils.FIRMWARE_FILE_LOCATION;
		isUseBasicFw = !TextUtils.isEmpty(firmwareInfo.getFwBasicUrl());
		isUseDeltaFw = !TextUtils.isEmpty(firmwareInfo.getFirmwareUrl());
		if(isUseBasicFw) {
			try {
				fwBasicFileUrl = new URL(firmwareInfo.getFwBasicUrl()); //nctmanh: 02112016 - for new ota flow
				isDownloadedFwBasic = isDownloadedFwBasic(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION, new File(fwBasicFileUrl.getFile()).getName());
			} catch (MalformedURLException e) {
				fwBasicFileUrl = null;
				isDownloadedFwBasic = true;
			}
		} else{//not need Basic FW, only use delta FW to upgrade
			isDownloadedFwBasic = true;
		}
		VnptOtaUtils.LogError(LOG_TAG, "isDownloadedFwBasic: " + isDownloadedFwBasic);
		isDownloadedFwBasicDone = isDownloadedFwBasic;

		// KHAICUCAI doesn't delete FW delta if download by intend 
		// delOldFwDelta();					
		// dlMgr = new DownloadManager(this, filePath, firmwareInfo.getFirmwareUrl(), firmwareInfo.getFwBasicUrl(), isDownloadedFwBasic);  //nctmanh: 02112016 - for new ota flow 
		// dlMgr.addListener(dlListener);

		IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(networkReceiver, filter);
		updateUI();
		tvFirmwareDes.setText(firmwareInfo.getFirmwareDescription());
		isRunning = true;
		if(Build.VERSION.SDK_INT < 21)
			notifyMgr = new NotifyManager(this);
		else
			notifyMgr = null;


		VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI check VnptOtaState in VnptOtaUI: " + otaState.getState());
		if (otaState.getState() != VnptOtaState.STATE_DOWNLOAD_INPROGRESS && otaState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD){
			otaState.setState(VnptOtaState.STATE_SHOW_UI);
			delOldFwDelta();					// KHAICUCAI deletes FW delta in download normal mode 
		}

		otaState.setVnptOtaUiIsRunning(VnptOtaState.VNPTOTA_UI_IS_RUNNING);

		VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI before enter to sendInternalDownloadCompleteBroadcast in VnptOtaUI!");
		if (itent.getIntExtra(VnptOtaUtils.INTEND_MSG_TYPE, -1) == MSG_TYPE.DOWNLOAD_COMPLETED){
			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI enter to sendInternalDownloadCompleteBroadcast in VnptOtaUI!");



			if (!TextUtils.isEmpty(itent.getStringExtra(VnptOtaUtils.INTEND_BASIC_FILE_PATH_EXTEND))){
				VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI intent has fw basic filepath: " + itent.getStringExtra(VnptOtaUtils.INTEND_BASIC_FILE_PATH_EXTEND));
				fwBasicFile = new File(itent.getStringExtra(VnptOtaUtils.INTEND_BASIC_FILE_PATH_EXTEND));
				isDownloadedFwBasicDone = true;
				isDownloadedFwBasic = false;
				downloadingSizeByIntend = firmwareInfo.getFirmwareSize();
			} else {
				downloadingSizeByIntend = firmwareInfo.getFwBasicSize();
			}

			Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_INPROGESS, 
					downloadingSizeByIntend, 100);
			m.sendToTarget();	

			sendInternalDownloadCompleteBroadcast(itent);
		}
	}
	//nctmanh: 02112016 - for new ota flow - @{
	private boolean isDownloadedFwBasic(String filePath, String fileName){
		File file = new File(filePath, fileName);
		fwBasicFilePath = file.getAbsolutePath();
		VnptOtaUtils.LogError(LOG_TAG, "check fw basic exists, path: " + fwBasicFilePath);
		if(file.exists()){
			if(file.length() == firmwareInfo.getFwBasicSize()){
				VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI delete fw basic 1");
				return true;
			}
			if(file.length() > firmwareInfo.getFwBasicSize()){
				VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI delete fw basic 1");
				delOldFwBasic();
			}
		} else {
			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI delete fw basic 1");
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
	
	private void delOldFwDelta(){
		File dir = new File(VnptOtaUtils.FIRMWARE_FILE_LOCATION); 
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();	//nctmanh: 02112016 - for new ota flow
			for (int i = 0; i < children.length; i++) {
				children[i].delete();
			}
		}
	}
	//@}
	@Override
	protected void onResume() {
		VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: onResume!");
		super.onResume();
		updateUI();
		registerUpdateUIReceiver();
	}

	@Override
	protected void onDestroy() {
		VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: onDestroy!");
		super.onDestroy();
		if(notifyMgr != null)
			notifyMgr.clearNotification(NotifyManager.NOTIFY_DOWNLOADING);
		unregisterReceiver(networkReceiver);
		VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: isRunning: " + isRunning);
		VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: getState: " + otaState.getState());
		isRunning = false;

		// KHAICUCAI modify to download by Intend
		if(otaState.getState() != VnptOtaState.STATE_SHOW_UI_MANUAL && 
			otaState.getState() != VnptOtaState.STATE_DOWNLOAD_INPROGRESS && 
			otaState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD){
			otaState.setState(VnptOtaState.STATE_IDLE);
		}

		otaState.setVnptOtaUiIsRunning(VnptOtaState.VNPTOTA_UI_IS_NOT_RUNNING);

		unregisterReceiver(broadcastUpdateUIReceiver);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.download_file:
		// VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: Click to Download Button!");
			///nctmanh: 05082015 - allow check new ota fw in mobile data connection -@{
			//kiem tra neu dang su dung mang 3G thi hien thong tin canh bao.
			//Neu dang su dung wifi thi khong hien
			if(VnptOtaUtils.isConnectedMobile(this)){
				showUpdateAlertDialog();
			} else{
				// dlMgr.startDownload();
				sendButtonClickBroadcast(VnptOtaUtils.ACTION_MANUAL_DOWNLOAD);
				otaState.setState(VnptOtaState.STATE_MANUAL_DOWNLOAD);
				updateUI();
			}
			
			///@}
			break;
		case R.id.btn_upgrade:
			upgradeFirmware(getApplicationContext());
			break;
		case R.id.btn_pause:
		// VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: Click to Pause Button!");
			otaState.setState(VnptOtaState.STATE_DOWNLOAD_PAUSE);
			// dlMgr.pauseDownload();
			sendButtonClickBroadcast(VnptOtaUtils.ACTION_MANUAL_PAUSE);
			updateUI();
		default:
			break;
		}
	}

	// KHAICUCAI add: send a Broadcast message when click button
    private void sendButtonClickBroadcast(String broadCastAction) {
        // VnptOtaUtils.LogDebug(LOG_TAG, "Enter to sendButtonClickBroadcast");
        Intent intent = new Intent();
        intent.setAction(broadCastAction);
        intent.putExtra("sendButtonClickBroadcast", broadCastAction);
        sendBroadcast(intent);
    }

    // KHAICUCAI add: send a internal DOWNLOAD_COMPLETED Broadcast 
    private void sendInternalDownloadCompleteBroadcast(Intent intentOnCreate) {
        // VnptOtaUtils.LogDebug(LOG_TAG, "Enter to sendButtonClickBroadcast");
        Intent intent = new Intent();
		intent.setAction(VnptOtaUtils.ACTION_DOWNLOAD_LISTENER);
		intent.putExtra(VnptOtaUtils.INTEND_DL_LISTENER, VnptOtaUtils.ACTION_DOWNLOAD_LISTENER);
		intent.putExtra(VnptOtaUtils.INTEND_MSG_TYPE, MSG_TYPE.DOWNLOAD_COMPLETED);
		intent.putExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, intentOnCreate.getIntExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, -1));
		intent.putExtra(VnptOtaUtils.INTEND_FILE_PATH, intentOnCreate.getStringExtra(VnptOtaUtils.INTEND_FILE_PATH));
        sendBroadcast(intent);
    }

    // KHAICUCAI add: BroadcastReceiver to update UI
    BroadcastReceiver broadcastUpdateUIReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			int messageIntendType;
			String temp = "";

			if (action.equalsIgnoreCase(VnptOtaUtils.ACTION_DOWNLOAD_LISTENER)) {
				// VnptOtaUtils.LogError(LOG_TAG, "Start Update UI by intend!");

				messageIntendType = intent.getIntExtra(VnptOtaUtils.INTEND_MSG_TYPE, -1);

				switch (messageIntendType) {
				case MSG_TYPE.DOWNLOAD_COMPLETED:
					if (intent.getIntExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, -1) == VnptOtaUtils.INTEND_DL_BASIC_DONE){
						VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI download fw basic success!");

						fwBasicFile = new File(intent.getStringExtra(VnptOtaUtils.INTEND_FILE_PATH));
						isDownloadedFwBasicDone = true;
						if(!isUseDeltaFw){// not use delta FW => download complete
							otaState.setState(VnptOtaState.STATE_DOWNLOAD_COMPLETED);

							String successString = "download successlly";
							Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_COMPLETED, successString);
							m.sendToTarget();
						}
					} else if (intent.getIntExtra(VnptOtaUtils.INTEND_DL_FW_STATUS, -1) == VnptOtaUtils.INTEND_DL_DELTA_DONE){
						VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI download fw delta success!");

						otaState.setState(VnptOtaState.STATE_DOWNLOAD_COMPLETED);

						fwrFile = new File(intent.getStringExtra(VnptOtaUtils.INTEND_FILE_PATH));
						String successString = "download successlly";
						Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_COMPLETED, successString);
						m.sendToTarget();
					}

					break;
				case MSG_TYPE.DOWNLOAD_INPROGESS:
					int downloadingSize = intent.getIntExtra(VnptOtaUtils.INTEND_DOWLOADING_SIZE, -1);
					int progress = intent.getIntExtra(VnptOtaUtils.INTEND_PROGRESS, -1);

					if (downloadingSize != -1 && progress != -1) {
						Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_INPROGESS, (int) downloadingSize, (int) progress);
						m.sendToTarget();
					} else {
						VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI error get Download Infor in Intend!");
					}


					break;
				case MSG_TYPE.DOWNLOAD_ERROR:
					int errorCode = intent.getIntExtra(VnptOtaUtils.INTEND_ERROR_CODE, -1);
					VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI get error from Intend --- errorCode: " + errorCode);

					if (errorCode != -1){
						Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_ERROR, Integer.valueOf(errorCode));
						m.sendToTarget();
					}

					break;
				}
			}
		}
	};

	// KHAICUCAI add: register Receiver to Update UI
	private void registerUpdateUIReceiver(){
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(VnptOtaUtils.ACTION_DOWNLOAD_LISTENER);
		registerReceiver(broadcastUpdateUIReceiver, intentFilter);
	}

	// KHAICUCAI add manualFimwareInfoListener to query new fw info
	private FirmwareInfoManager.FirmwareInfoListerner manualFimwareInfoListener = new FirmwareInfoManager.FirmwareInfoListerner(){
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
			// otaState.setFirmwareInfoFromSharedPreferences(info);
			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI query new fw info DONE in VnptOtaUI!");
			firmwareInfo = info;
		}
	};

	///nctmanh: 05082015 - allow check new ota fw in mobile data connection - @{
	private void showUpdateAlertDialog(){
		VnptOtaUtils.LogDebug(LOG_TAG, "showUpdateAlertDialog");
		AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
		mBuilder.setTitle(R.string.alert_title_warning)
		.setIcon(R.drawable.ic_warning)
		.setMessage(R.string.download_fw_in_data_connection_mode)
		.setPositiveButton(R.string.alert_btn_continue, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
				// dlMgr.startDownload();
				// KHAICUCAI modify to download by Intend
				sendButtonClickBroadcast(VnptOtaUtils.ACTION_MANUAL_DOWNLOAD);
				otaState.setState(VnptOtaState.STATE_MANUAL_DOWNLOAD);
				updateUI();
			}
		})
		.setNegativeButton(R.string.alert_btn_later, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.create().show();
	}
	///@}
	private void updateUI() {
		if (otaState.getState() == VnptOtaState.STATE_IDLE) {
			rlFirmawareVersion.setVisibility(View.VISIBLE);
			rlProgressDownload.setVisibility(View.GONE);
			tvFirmwareVersion1.setText(firmwareInfo.getFirmwareVersion());
			tvReleaseDate.setText(VnptOtaUtils.convetDateToString(firmwareInfo.getFirmwareDate()));
			if(isUseBasicFw && isUseDeltaFw) {
				if(isDownloadedFwBasic){
					tvFirmwareSize.setText(String.valueOf(((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
				}else{
					tvFirmwareSize.setText(String.valueOf((firmwareInfo.getFirmwareSize() + firmwareInfo.getFwBasicSize())/(1024*1024)) + " MB"); //nctmanh: 02112016 - for new ota flow
				}
			} else if(isUseBasicFw) {
				tvFirmwareSize.setText(String.valueOf(firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
			} else if(isUseDeltaFw){
				tvFirmwareSize.setText(String.valueOf(((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
			}
			btnDownload.setVisibility(View.VISIBLE);
			btnUpgrade.setVisibility(View.GONE);
			btnPause.setVisibility(View.GONE);
		} else if (otaState.getState() == VnptOtaState.STATE_DOWNLOAD_COMPLETED) {
			rlFirmawareVersion.setVisibility(View.GONE);
			rlProgressDownload.setVisibility(View.VISIBLE);
			if(isUseBasicFw && isUseDeltaFw) {
				if(isDownloadedFwBasicDone){
					tvFileSize.setText(String.valueOf(((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
					tvTotalSize.setText(String.valueOf("/" + ((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
				}else{
					tvFileSize.setText(String.valueOf(firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
					tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
				}
			} else if(isUseBasicFw) {
				tvFileSize.setText(String.valueOf(firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
				tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
			} else if(isUseDeltaFw) {
				tvFileSize.setText(String.valueOf(((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
				tvTotalSize.setText(String.valueOf("/" + ((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
			}
			tvPercent.setText("100");
			pbDownload.setProgress(100);
			tvFirmwareVersion2.setText(firmwareInfo.getFirmwareVersion());
			btnDownload.setVisibility(View.GONE);
			btnUpgrade.setVisibility(View.VISIBLE);
			btnPause.setVisibility(View.GONE);
		} else if (otaState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS || otaState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD) {
			rlFirmawareVersion.setVisibility(View.GONE);
			rlProgressDownload.setVisibility(View.VISIBLE);
			tvFileSize.setText("0 kB");
			if(isUseBasicFw && isUseDeltaFw) {
				if(isDownloadedFwBasicDone){
					tvTotalSize.setText(String.valueOf("/" + ((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
				}else{
					tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
				}
			} else if(isUseBasicFw) {
				tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
			} else if(isUseDeltaFw) {
				tvTotalSize.setText(String.valueOf("/" + ((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
			}

			tvPercent.setText("0");
			tvFirmwareVersion2.setText(firmwareInfo.getFirmwareVersion());
			btnDownload.setVisibility(View.GONE);
			btnUpgrade.setVisibility(View.GONE);
			btnPause.setVisibility(View.VISIBLE);
		} else if (otaState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE) {
			btnDownload.setVisibility(View.VISIBLE);
			btnUpgrade.setVisibility(View.GONE);
			btnPause.setVisibility(View.GONE);
		} else {
			rlFirmawareVersion.setVisibility(View.GONE);
			rlProgressDownload.setVisibility(View.VISIBLE);
			tvFileSize.setText("0 kB");
			if(isUseBasicFw && isUseDeltaFw) {
				if(isDownloadedFwBasic){
					tvTotalSize.setText(String.valueOf("/" + ((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
				}else{
					tvTotalSize.setText(String.valueOf("/" + (firmwareInfo.getFirmwareSize() + firmwareInfo.getFwBasicSize())/(1024*1024)) + " MB");	//nctmanh: 02112016 - for new ota flow
				}
			} else if(isUseBasicFw) {
				tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFwBasicSize()/(1024*1024)) + " MB");
			} else if(isUseDeltaFw) {
				tvTotalSize.setText(String.valueOf("/" + ((firmwareInfo.getFirmwareSize()/(1024*1024)) > 1 ? (firmwareInfo.getFirmwareSize()/(1024*1024)) : 1)) + " MB");
			}
			tvPercent.setText("0");
			tvFirmwareVersion2.setText(firmwareInfo.getFirmwareVersion());
			btnDownload.setVisibility(View.VISIBLE);
			btnUpgrade.setVisibility(View.GONE);
			btnPause.setVisibility(View.GONE);
		}
	}

	BroadcastReceiver networkReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if ((VnptOtaUtils.isNetworkConnected(context)) && 
					(otaState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE)) {
				dlMgr.resumeDownload();
				btnDownload.setEnabled(false);
			}
		}

	};

	private DownloadManager.DownloaderListener dlListener = new DownloadManager.DownloaderListener() {
		@Override
		public void downloadSuccess(File tempFile) {
			fwrFile = tempFile;
			String successString = "download successlly";
			Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_COMPLETED, successString);
			m.sendToTarget();
		}
		
		@Override
		public void downloadSuccessFileBasic(File tempFwBasicFile) {
			fwBasicFile = tempFwBasicFile;
			isDownloadedFwBasicDone = true;
			if(!isUseDeltaFw){// not use delta FW => download complete
				String successString = "download successlly";
				Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_COMPLETED, successString);
				m.sendToTarget();
			}
		}
		@Override
		public void downloadError(int errorCode) {
			Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_ERROR, Integer.valueOf(errorCode));
			m.sendToTarget();
		}

		@Override
		public void downloadProgess(long downloadingSize, long progress) {
			Message m = Message.obtain(handlerUI, MSG_TYPE.DOWNLOAD_INPROGESS, 
					(int) downloadingSize, (int) progress);
			m.sendToTarget();			
		}
	};

	Runnable validateFile = new Runnable() {		
		@Override
		public void run() {
			try {
				if(isUseBasicFw && isUseDeltaFw) {
					VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 1");
					if(isDownloadedFwBasic){
						if ((fwrFile != null) && 
								VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
								(RecoveryUtil.verifyPackage(fwrFile))){
							VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 2");
							Message m = Message.obtain(handlerUI, MSG_TYPE.VALIDATE_SUCCESSFULLY);
							m.sendToTarget();

							if (prgWait.isShowing())
								prgWait.dismiss();
						} else {
							VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 3");
							if (prgWait.isShowing())
								prgWait.dismiss();
							Message m = Message.obtain(handlerUI, MSG_TYPE.ERROR_FIRMWARE_FILE);
							m.sendToTarget();
						}
					}else{
						if ((fwrFile != null) && 
							VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
							(RecoveryUtil.verifyPackage(fwrFile)) &&
							(fwBasicFile !=null) &&
							VnptOtaUtils.getMd5sum(fwBasicFile).equalsIgnoreCase(firmwareInfo.getFwBasicMd5()) &&
							(RecoveryUtil.verifyPackage(fwBasicFile))
						){
							VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 4");
							Message m = Message.obtain(handlerUI, MSG_TYPE.VALIDATE_SUCCESSFULLY);
							m.sendToTarget();
							if (prgWait.isShowing())
								prgWait.dismiss();
						} else {
							VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 5");
							if (prgWait.isShowing())
								prgWait.dismiss();
							Message m = Message.obtain(handlerUI, MSG_TYPE.ERROR_FIRMWARE_FILE);
							m.sendToTarget();
						}
					}
				} else if (isUseBasicFw) {
					VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 6");
					if ((fwBasicFile !=null) &&
						VnptOtaUtils.getMd5sum(fwBasicFile).equalsIgnoreCase(firmwareInfo.getFwBasicMd5()) &&
						(RecoveryUtil.verifyPackage(fwBasicFile))
					){
						VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 7");
						Message m = Message.obtain(handlerUI, MSG_TYPE.VALIDATE_SUCCESSFULLY);
						m.sendToTarget();
						if (prgWait.isShowing())
							prgWait.dismiss();
					} else {
						VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 8");
						if (prgWait.isShowing())
							prgWait.dismiss();
						Message m = Message.obtain(handlerUI, MSG_TYPE.ERROR_FIRMWARE_FILE);
						m.sendToTarget();
					}
				} else if (isUseDeltaFw) {
					if ((fwrFile != null) && 
						VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
						(RecoveryUtil.verifyPackage(fwrFile))
					){
						VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 9");
						Message m = Message.obtain(handlerUI, MSG_TYPE.VALIDATE_SUCCESSFULLY);
						m.sendToTarget();
						if (prgWait.isShowing())
							prgWait.dismiss();
					} else {
						VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI: validateFile 10");
						if (prgWait.isShowing())
							prgWait.dismiss();
						Message m = Message.obtain(handlerUI, MSG_TYPE.ERROR_FIRMWARE_FILE);
						m.sendToTarget();
					}
				}
				
			} catch (Exception e){
				if (prgWait.isShowing())
					prgWait.dismiss();
				Message m = Message.obtain(handlerUI, MSG_TYPE.ERROR_FIRMWARE_FILE);
				m.sendToTarget();
			}
		}
	};

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_TYPE.DOWNLOAD_COMPLETED:
			prgWait = new ProgressDialog(this);
			prgWait.setMessage(getApplicationContext().
					getResources().getString(R.string.progress_bar_wait));
			prgWait.setCancelable(false);
			prgWait.show();

			// KHAICUCAI add clear Fw infow if DOWNLOAD_COMPLETED
			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: clear Fw infow if DOWNLOAD_COMPLETED");
			otaState.resetFirmwareInfo();
			
			tvFileName.setText(R.string.download_Completed);
			
			Thread mThread = new Thread(validateFile);
			mThread.start();

			btnDownload.setEnabled(true);

			Intent mIntent = new Intent(getApplicationContext(), VnptOtaUI.class);
			FirmwareInfoUtils.addFirmwareInfoToIntent(mIntent, firmwareInfo);
			mIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			mIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			if(notifyMgr != null){
				notifyMgr.clearNotification(NotifyManager.NOTIFY_DOWNLOADING);
				notifyMgr.showDownloadCompletedNotification(mIntent);
			}
			break;
		case MSG_TYPE.DOWNLOAD_INPROGESS:
			int progress = msg.arg2;
			int downloadingSize = msg.arg1;

			pbDownload.setProgress(progress);
			tvPercent.setText(String.valueOf(progress));
			if(isUseBasicFw && isUseDeltaFw){
				if(isDownloadedFwBasicDone){
					tvFileName.setText(R.string.downloading_fw_delta);
					tvFileSize.setText(String.valueOf(downloadingSize/1024) + " kB");
					tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFirmwareSize()/(1024*1024)) + " MB");
				}else{
					tvFileName.setText(R.string.downloading_fw_basic);
					tvFileSize.setText(String.valueOf(downloadingSize/1024) + " kB");
				}
			} else if(isUseBasicFw){
				tvFileName.setText(R.string.downloading_fw_basic);
				tvFileSize.setText(String.valueOf(downloadingSize/1024) + " kB");
			} else if(isUseDeltaFw){
				tvFileName.setText(R.string.downloading_fw_delta);
				tvFileSize.setText(String.valueOf(downloadingSize/1024) + " kB");
				tvTotalSize.setText(String.valueOf("/" + firmwareInfo.getFirmwareSize()/(1024*1024)) + " MB");
			}
			if(notifyMgr != null)
				notifyMgr.showDownloadingNotification(progress, true);
			break;
		case MSG_TYPE.DOWNLOAD_ERROR:
			Integer errorCodeTemp  = (Integer) msg.obj;
			int errorCode = errorCodeTemp;
			if (errorCode == DownloadUtils.NETWORK_NOT_CONNECT || errorCode == DownloadUtils.DOWNLOAD_ERROR_USER_CANCEL) {
				otaState.setState(VnptOtaState.STATE_DOWNLOAD_PAUSE);
			} else {
				otaState.setState(VnptOtaState.STATE_DOWNLOAD_STOP);
			}
			String errorCodeString;
			if (!((errorCodeString = VnptOtaUtils.convertErrCodeToString(
					VnptOtaUI.this, errorCode)) == null)) {
				Toast.makeText(getApplicationContext(), errorCodeString, 
						Toast.LENGTH_SHORT).show();
			} 
			btnDownload.setEnabled(true);
			btnDownload.setVisibility(View.VISIBLE);
			btnUpgrade.setVisibility(View.GONE);
			btnPause.setVisibility(View.GONE);
			if(notifyMgr != null)
				notifyMgr.clearNotification(NotifyManager.NOTIFY_DOWNLOADING);
			String alarmString1 = getResources().getString(R.string.download_error_file_stream) + " (" + Integer.toString(errorCode) + ")!";
			String dialogTitle = getResources().getString(R.string.dialog_error_tile);
			if(errorCode == DownloadUtils.DOWNLOAD_ERROR_USER_CANCEL) {
				alarmString1 = getResources().getString(R.string.download_pause_file_stream) + " (" + Integer.toString(errorCode) + ")!";
				dialogTitle = getResources().getString(R.string.dialog_pause_tile);
			}
			AlertDialog.Builder mBuilder1 = new AlertDialog.Builder(this);
			mBuilder1.setTitle(dialogTitle)
			.setMessage(alarmString1)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create().show();
			
			break;
		case MSG_TYPE.REBOOT_RECOVERY:
			try {
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				pm.reboot("recovery");
			} catch (Exception e) {}
			break;

		case MSG_TYPE.ERROR_FIRMWARE_FILE:
			otaState.setState(VnptOtaState.STATE_IDLE);

			// KHAICUCAI add clear Fw infow if ERROR_FIRMWARE_FILE
			VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI: clear Fw infow if ERROR_FIRMWARE_FILE");
			otaState.resetFirmwareInfo();

			if ((fwrFile != null) && 
					VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
					(RecoveryUtil.verifyPackage(fwrFile))){
				fwrFile.delete();
			}
			if(!isDownloadedFwBasic || isUseBasicFw){
				if ((fwBasicFile !=null) &&
						VnptOtaUtils.getMd5sum(fwBasicFile).equalsIgnoreCase(firmwareInfo.getFwBasicMd5()) &&
						(RecoveryUtil.verifyPackage(fwBasicFile))){
					fwBasicFile.delete();
				}
			}
			btnDownload.setVisibility(View.VISIBLE);
			btnUpgrade.setVisibility(View.GONE);
			
			String alarmString = getResources().getString(R.string.file_note_valid) + " (" + Integer.toString(mErrorCode) + ")!";
			AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
			mBuilder.setTitle(R.string.dialog_error_tile)
			.setMessage(alarmString)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			})			
			.create().show();

			break;
		case MSG_TYPE.VALIDATE_SUCCESSFULLY:
			otaState.setState(VnptOtaState.STATE_DOWNLOAD_COMPLETED);
			btnDownload.setVisibility(View.GONE);
			btnUpgrade.setVisibility(View.VISIBLE);
			upgradeFirmware(getApplicationContext());
			break;
		default:
			break;
		}
		return false;
	}

	private void upgradeFirmware(Context context) {
		AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
		mBuilder.setTitle(R.string.dialog_upgrade_tile)
		.setMessage(R.string.dialog_upgrade_content)
		.setCancelable(false)
		.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				otaState.setState(VnptOtaState.STATE_URGRADE_FIRMWARE);
				otaState.setFirmwareVersion(firmwareInfo.getFirmwareVersion());
				boolean commandExist = false;
				if(isUseBasicFw && isUseDeltaFw){
					VnptOtaUtils.LogDebug(LOG_TAG, "file delta path: " + fwrFile.getAbsolutePath());
					if(isDownloadedFwBasic){
						VnptOtaUtils.LogDebug(LOG_TAG, "file fw Basic path: " + fwBasicFilePath);
						commandExist = RecoveryUtil.upgradeDeltaPackage(context, fwBasicFilePath, fwrFile.getAbsolutePath());	//nctmanh: 02112016 - for new ota flow
					}else{
						VnptOtaUtils.LogDebug(LOG_TAG, "1.file fw Basic path: " + fwBasicFile.getAbsolutePath());
						commandExist = RecoveryUtil.upgradeDeltaPackage(context, fwBasicFile.getAbsolutePath(), fwrFile.getAbsolutePath());	//nctmanh: 02112016 - for new ota flow
					}
				} else if(isUseBasicFw){
					VnptOtaUtils.LogDebug(LOG_TAG, "2.file fw Basic path: " + fwBasicFile.getAbsolutePath());
					commandExist = RecoveryUtil.upgradePackage(context, fwBasicFile.getAbsolutePath());
				} else if(isUseDeltaFw){
					VnptOtaUtils.LogDebug(LOG_TAG, "3.file fw Delta path: " + fwrFile.getAbsolutePath());
					commandExist = RecoveryUtil.upgradePackage(context, fwrFile.getAbsolutePath());
				}

				if(commandExist){
					Message m = Message.obtain(handlerUI, MSG_TYPE.REBOOT_RECOVERY);
					m.sendToTarget();
				} else {
					Message m = Message.obtain(handlerUI, MSG_TYPE.ERROR_FIRMWARE_FILE);
					m.sendToTarget();
				}
			}
		})
		.setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				VnptOtaUI.this.finish();
			}
		})
		.create().show();
	}
}












