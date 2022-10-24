package com.app.myapplication.service;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.app.myapplication.common.RecoveryUtil;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class VnptOtaDownloadService {

	private static final String LOG_TAG = "VnptOtaDownloadService";

	private DownloadManager dlMgr = null;
	private String filePath;
	private FirmwareInfo firmwareInfo;

	private VnptOtaState otaState;
	private URL fwBasicFileUrl = null; 	//nctmanh: 02112016 - for new ota flow
	private String fwBasicFilePath = null; 	//nctmanh: 02112016 - for new ota flow
	private boolean isDownloadedFwBasic = false; 	//nctmanh: 02112016 - for new ota flow
	private boolean isDownloadedFwBasicDone = false; 	//nctmanh: 02112016 - for new ota flow
	private int mErrorCode = 0;
	public static boolean isDownloading = false;
	private File fwrFile = null;
	private File fwBasicFile = null; 	//nctmanh: 02112016 - for new ota flow
	private boolean isUseBasicFw = true;
	private boolean isUseDeltaFw = true;
	private int retryDownload = 0;
	//retry download 10 times if it gets error
	private static final int NUMBER_RETRY_DOWNLOAD = 100;
	Context mContext;
	
	ProgressDialog prgWait;

	private class MSG_TYPE {
		public final static int DOWNLOAD_COMPLETED = 1;
		public final static int DOWNLOAD_INPROGESS = 2;
		public final static int DOWNLOAD_ERROR = 3;
		public final static int REBOOT_RECOVERY = 4;
		public final static int ERROR_FIRMWARE_FILE = 5;
		public final static int VALIDATE_SUCCESSFULLY = 6;
		public final static int DOWNLOAD_COMPLETED_FW_BASIC = 7;
		public final static int FORCE_STOP_DOWNLOAD = 8;
	}

	Handler mDownloadHandler;

	//nctmanh: 02112016 - for new ota flow
	public VnptOtaDownloadService(Context context, FirmwareInfo info) {
		firmwareInfo = info;
		mContext = context;
		otaState = VnptOtaState.getInstance(context);
		filePath = VnptOtaUtils.FIRMWARE_FILE_LOCATION;
		isUseBasicFw = !TextUtils.isEmpty(firmwareInfo.getFwBasicUrl());
		isUseDeltaFw = !TextUtils.isEmpty(firmwareInfo.getFirmwareUrl());

		try {
			fwBasicFileUrl = new URL(firmwareInfo.getFwBasicUrl()); //nctmanh: 02112016 - for new ota flow
		} catch (MalformedURLException e) {
			fwBasicFileUrl = null;
		}

		mDownloadHandler = new OtaDownloadHandler();

		if(fwBasicFileUrl != null)
			isDownloadedFwBasic = isDownloadedFwBasic(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION, new File(fwBasicFileUrl.getFile()).getName());
		else
			isDownloadedFwBasic = false;

		VnptOtaUtils.LogDebug(LOG_TAG, "isDownloadedFwBasic: " + isDownloadedFwBasic);

		isDownloadedFwBasicDone = isDownloadedFwBasic;

		URL fwDeltaFileUrl = null;

		try {
			fwDeltaFileUrl = new URL(firmwareInfo.getFirmwareUrl());
		} catch (MalformedURLException e) {
			fwDeltaFileUrl = null;
		}

		boolean isDownloadedFwDelta = false;

		if(fwDeltaFileUrl != null)
			isDownloadedFwDelta = isDownloadedFwDelta(VnptOtaUtils.FIRMWARE_FILE_LOCATION, new File(fwDeltaFileUrl.getFile()).getName());
		else
			isDownloadedFwDelta = false;

		VnptOtaUtils.LogDebug(LOG_TAG, "isDownloadedFwDelta: " + isDownloadedFwDelta);

		if(isDownloadedFwBasic && isDownloadedFwDelta) {
			otaState.setState(VnptOtaState.STATE_WAITING);
		} else {
			//dlMgr = new DownloadManager(context, filePath, firmwareInfo.getFirmwareUrl(), firmwareInfo.getFwBasicUrl(), isDownloadedFwBasic);  //nctmanh: 02112016 - for new ota flow
			//.addListener(dlListener);
		}

		retryDownload = 0;

	}

	//nctmanh: 02112016 - for new ota flow - @{
	private boolean isDownloadedFwBasic(String filePath, String fileName){

		File file = new File(filePath, fileName);
		// getAbsolutePath trả về đường dẫn đầy đủ của File
		fwBasicFilePath = file.getAbsolutePath();

		VnptOtaUtils.LogDebug(LOG_TAG, "check fw basic exists, path: " + fwBasicFilePath);

		if(file.exists() && (file.length() <= firmwareInfo.getFwBasicSize())){
			if(file.length() == firmwareInfo.getFwBasicSize()){
				// kiểm tra hàm băm md5 trên box với hàm băm md5 trên database có giống nhau không 
				if(VnptOtaUtils.getMd5sum(file).equals(firmwareInfo.getFwBasicMd5())
					&& RecoveryUtil.verifyPackage(file)){
					return true;
				} else {
					// xóa fw cũ 
					delOldFwBasic();
					delOldFwDelta();
				}
			}
		} else {
			delOldFwBasic();
			delOldFwDelta();
		}

		return false;
	}


	private boolean isDownloadedFwDelta(String filePath, String fileName){
		File file = new File(filePath, fileName);
		String fwDeltaFilePath = file.getAbsolutePath();

		VnptOtaUtils.LogDebug(LOG_TAG, "check fw delta exists, path: " + fwDeltaFilePath);

		if(file.exists()){
			if(file.length() == firmwareInfo.getFirmwareSize()){
				if(VnptOtaUtils.getMd5sum(file).equals(firmwareInfo.getFirmwareMd5())){
					return true;
				} else {
					delOldFwDelta();
				}
			}
		} else {
			delOldFwDelta();
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

	public void startDownload(){
		if(!isDownloading && (dlMgr != null)
				&& (otaState.getState() == VnptOtaState.STATE_DOWNLOADING
				|| otaState.getState() == VnptOtaState.STATE_QUERYING)
		) {
			otaState.setState(VnptOtaState.STATE_DOWNLOADING);
			//dlMgr.startDownload();
			isDownloading = true;
		}
	}

	public void stopDownload() {
		if(dlMgr != null) {
//			dlMgr.cancelDownload();
//			dlMgr.disposeResoure();
			isDownloading = false;
			retryDownload = 0;
		}
	}

	public void cancelDownload(){
		if(dlMgr != null) {
			//dlMgr.cancelDownload();
			isDownloading = false;
			retryDownload = 0;
		}
	}

	private com.app.myapplication.download.DownloadManager.DownloaderListener dlListener = new com.app.myapplication.download.DownloadManager.DownloaderListener() {
		@Override
		public void downloadSuccess(File tempFile) {
			fwrFile = tempFile;
			mDownloadHandler.sendEmptyMessage(MSG_TYPE.DOWNLOAD_COMPLETED);
		}

		@Override
		public void downloadSuccessFileBasic(File tempFwBasicFile) {
			fwBasicFile = tempFwBasicFile;
			isDownloadedFwBasicDone = true;
		}
		@Override
		public void downloadError(int errorCode) {
			mDownloadHandler.sendEmptyMessage(MSG_TYPE.DOWNLOAD_ERROR);
		}

		@Override
		public void downloadProgess(long downloadingSize, long progress) {
			//OPEN UI to query FW, so force stop dowload service
			if(otaState.getState() == VnptOtaState.STATE_DOWNLOADING) {
				mDownloadHandler.sendEmptyMessage(MSG_TYPE.DOWNLOAD_INPROGESS);
			} else {
				mDownloadHandler.sendEmptyMessage(MSG_TYPE.FORCE_STOP_DOWNLOAD);
			}
		}
	};

	private class OtaDownloadHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_TYPE.DOWNLOAD_COMPLETED:

					VnptOtaUtils.LogError(LOG_TAG, "OtaDownloadHandler: DOWNLOAD_COMPLETED");

					otaState.setState(VnptOtaState.STATE_VERIFYING);
					if(validateFWs()){
						otaState.setState(VnptOtaState.STATE_WAITING);
					} else {
						otaState.setState(VnptOtaState.STATE_QUERY);
					}
					break;
				case MSG_TYPE.DOWNLOAD_ERROR:

					VnptOtaUtils.LogError(LOG_TAG, "OtaDownloadHandler: DOWNLOAD_ERROR. retryDownload=" + retryDownload);
					
					isDownloading = false;
					if(retryDownload < NUMBER_RETRY_DOWNLOAD && VnptOtaUtils.isNetworkConnected(mContext)){
						retryDownload++;
						startDownload();
					} else {
						otaState.setState(VnptOtaState.STATE_QUERY);
					}
					break;
				case MSG_TYPE.DOWNLOAD_INPROGESS:
					retryDownload = 0;
					break;
				case MSG_TYPE.FORCE_STOP_DOWNLOAD:

					VnptOtaUtils.LogError(LOG_TAG, "OtaDownloadHandler: Show UI -> Force stop.");
					
					cancelDownload();
					break;
			}
		}
	}

	public boolean validateFWs(){
		try {
			if(isUseBasicFw && isUseDeltaFw){
				if(isDownloadedFwBasic){
					if ((fwrFile != null) &&
							VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
							(RecoveryUtil.verifyPackage(fwrFile))){
						otaState.setFirmwareVersion(firmwareInfo.getFirmwareVersion());
						otaState.setFirmwareBasic(fwBasicFilePath);
						otaState.setFirmwareDelta(fwrFile.getAbsolutePath());
						otaState.setFirmwareBasicMD5(firmwareInfo.getFwBasicMd5());
						otaState.setFirmwareDeltaMD5(firmwareInfo.getFirmwareMd5());

						VnptOtaUtils.LogDebug(LOG_TAG, "validateFWs: success");

						return true;
					} else {

						
						VnptOtaUtils.LogError(LOG_TAG, "validateFWs failed: " + fwrFile.getAbsolutePath());
						return false;
					}
				}else{
					if ((fwrFile != null) &&
							VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
							(RecoveryUtil.verifyPackage(fwrFile)) &&
							(fwBasicFile !=null) &&
							VnptOtaUtils.getMd5sum(fwBasicFile).equalsIgnoreCase(firmwareInfo.getFwBasicMd5()) &&
							(RecoveryUtil.verifyPackage(fwBasicFile))
							){
						otaState.setFirmwareVersion(firmwareInfo.getFirmwareVersion());
						otaState.setFirmwareBasic(fwBasicFile.getAbsolutePath());
						otaState.setFirmwareDelta(fwrFile.getAbsolutePath());
						otaState.setFirmwareBasicMD5(firmwareInfo.getFwBasicMd5());
						otaState.setFirmwareDeltaMD5(firmwareInfo.getFirmwareMd5());
						
						VnptOtaUtils.LogDebug(LOG_TAG, "1.validateFWs: success");
						
						return true;
					} else {

						VnptOtaUtils.LogError(LOG_TAG, "validateFWs failed: " + fwrFile.getAbsolutePath() + " & " + fwBasicFile.getAbsolutePath());
						
						return false;
					}
				}
			} else if(isUseBasicFw){
				if ((fwBasicFile !=null) &&
						VnptOtaUtils.getMd5sum(fwBasicFile).equalsIgnoreCase(firmwareInfo.getFwBasicMd5()) &&
						(RecoveryUtil.verifyPackage(fwBasicFile))
						){
					otaState.setFirmwareVersion(firmwareInfo.getFirmwareVersion());
					otaState.setFirmwareBasic(fwBasicFile.getAbsolutePath());
					otaState.setFirmwareDelta("");
					otaState.setFirmwareBasicMD5(firmwareInfo.getFwBasicMd5());
					otaState.setFirmwareDeltaMD5("");
					
					VnptOtaUtils.LogDebug(LOG_TAG, "2.validateFWs: success");
					
					return true;
				} else {
					
					VnptOtaUtils.LogError(LOG_TAG, "2.validateFWs failed: " + fwrFile.getAbsolutePath() + " & " + fwBasicFile.getAbsolutePath());
					
					return false;
				}
			} else if(isUseDeltaFw){
				if ((fwrFile != null) &&
						VnptOtaUtils.getMd5sum(fwrFile).equalsIgnoreCase(firmwareInfo.getFirmwareMd5()) &&
						(RecoveryUtil.verifyPackage(fwrFile))
						){
					otaState.setFirmwareVersion(firmwareInfo.getFirmwareVersion());
					otaState.setFirmwareBasic("");
					otaState.setFirmwareDelta(fwrFile.getAbsolutePath());
					otaState.setFirmwareBasicMD5("");
					otaState.setFirmwareDeltaMD5(firmwareInfo.getFirmwareMd5());
					
					VnptOtaUtils.LogDebug(LOG_TAG, "3.validateFWs: success");
					
					return true;
				} else {
					
					VnptOtaUtils.LogError(LOG_TAG, "3.validateFWs failed: " + fwrFile.getAbsolutePath() + " & " + fwBasicFile.getAbsolutePath());
				}
			}
		} catch (Exception e){
			
			VnptOtaUtils.LogError(LOG_TAG, "validateFWs failed: Can not verify all file FWs: " + e );
		}
		return false;
	}

}












