package com.app.myapplication.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.myapplication.getfirmwareinfo.FirmwareInfo;


public class VnptOtaState {

	private static final String DOWNLOAD_STATE_PREFS = "ota_state_prefs";
	private static final String DOWNLOAD_STATE = "ota_state";
	private static final String FIRMWARE_VERSION = "firmware_version";
	private static final String FIRMWARE_BASIC = "firmware_basic";
	private static final String FIRMWARE_BASIC_MD5 = "firmware_basic_md5";
	private static final String FIRMWARE_DELTA = "firmware_delta";
	private static final String FIRMWARE_DELTA_MD5 = "firmware_delta_md5";
	final SharedPreferences mPrefs;
	private static VnptOtaState mDownloadState = null;

	public static final int DOWNLOAD_STATE_UNKNOW = -9;
	
	public static final int STATE_IDLE = 1;
	public static final int STATE_QUERY = 2;
	public static final int STATE_QUERYING = 3;
	public static final int STATE_DOWNLOADING = 4;
	public static final int STATE_VERIFYING = 5;
	public static final int STATE_WAITING = 6;
	public static final int STATE_PREPAIRING = 7;
	public static final int STATE_UPGRADE = 8;
	public static final int STATE_MANUAL_QUERY = 9;
	public static final int STATE_MANUAL_DOWNLOAD = 10;
	public static final int STATE_MANUAL_PAUSE = 11;

	public static final int STATE_DOWNLOAD_INPROGRESS = STATE_DOWNLOADING;
	public static final int STATE_DOWNLOAD_STOP = 12;
	public static final int STATE_DOWNLOAD_PAUSE = STATE_MANUAL_PAUSE;
	public static final int STATE_QUERY_FIRMWARE = 13;
	public static final int STATE_DOWNLOAD_COMPLETED = 14;
	public static final int STATE_URGRADE_FIRMWARE = STATE_UPGRADE;
	public static final int STATE_QUERYING_FIRMWARE = STATE_QUERYING;
	public static final int STATE_QUERYING_MANUAL = STATE_MANUAL_QUERY;
	public static final int STATE_SHOW_UI = 15;
	public static final int STATE_SHOW_UI_MANUAL = 16;

	// KHAICUCAI add for Download from Intend
	// đây là các key shared preferences
	private static final String INTEND_FIRMWARE_NAME = "intend_firmware_name";
	private static final String INTEND_FIRMWARE_VERSION = "intend_firmware_version";
	private static final String INTEND_FIRMWARE_URL = "intend_firmware_url";
	private static final String INTEND_FIRMWARE_ATTRIBUTE = "intend_firmware_attribute";
	private static final String INTEND_FIRMWARE_MD5 = "intend_firmware_md5";
	private static final String INTEND_FIRMWARE_SIZE = "intend_firmware_size";
	private static final String INTEND_FIRMWARE_DATE = "intend_firmware_date";
	private static final String INTEND_FIRMWARE_DESC = "intend_firmware_desc";
	private static final String INTEND_FW_BASIC_URL = "intend_fw_basic_url";
	private static final String INTEND_FW_BASIC_MD5 = "intend_fw_basic_md5";
	private static final String INTEND_FW_BASIC_SIZE = "intend_fw_basic_size";
	private static final String CHECK_VNPTOTA_UI_IS_RUNNING = "check_vnptota_ui_is_running";
	public static final int VNPTOTA_UI_IS_RUNNING = 1;
	public static final int VNPTOTA_UI_IS_NOT_RUNNING = 0;

	private VnptOtaState(Context context){
		mPrefs = context.getSharedPreferences(DOWNLOAD_STATE_PREFS, 0);
	}


	// synchronized là một khối đồng bộ 	
	// Tất cả các khối đồng bộ đồng bộ trên cùng một đối tượng chỉ có thể cùng một lúc có 
	// một thread thực thi bên trong chúng. Tất cả các thread khác cố gắng để nhập khối
	// đồng bộ bị chặn cho đến khi thread bên trong khối đồng bộ thoát khối.
	//
	// nếu chưa có state cho bước này tạo 
	public static synchronized VnptOtaState getInstance(Context context) {
		// sử dụng synchronized ở đây tuyên bố phương thức này là đồng bộ.
		// đánh dấu phương thức tĩnh (static) là đồng bộ, phương thức sẽ được khóa (lock)  
		// trên class không phải object (tức VnptOtaState). Tức là "tại 1 thời điểm chỉ 1 
		// thread được chạy trên 1 class“.
		if (mDownloadState == null) {
			mDownloadState = new VnptOtaState(context);
		} 
		return mDownloadState;
	}

	public void setState(int state) {
		VnptOtaUtils.LogDebug("VnptOtaState", "setState: " + state);
		synchronized (mPrefs) {
			// Các đối tượng được đồng bộ được gọi là các đối tượng giám sát (Monitor Object)
			// (mPrefs). Chỉ một Thread được thực thi bên trong 1 Synchronized block 
			// trên cùng 1 đối tượng giám sát.
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putInt(DOWNLOAD_STATE, state);
			editor.apply();
		}
	}

	public int getState() {
		synchronized (mPrefs) {
			return mPrefs.getInt(DOWNLOAD_STATE, STATE_IDLE);
		}
	}
	
	public void setFirmwareVersion(String firmwareVersion) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(FIRMWARE_VERSION, firmwareVersion);
			editor.apply();
		}
	}

	public String getFirmwareVersion() {
		synchronized (mPrefs) {
			return mPrefs.getString(FIRMWARE_VERSION, null);
		}
	}

	//
	//
	// {FW basic's infos
	//
	//

	public void setFirmwareBasic(String filePath) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(FIRMWARE_BASIC, filePath);
			editor.apply();
		}
	}

	public String getFirmwareBasic() {
		synchronized (mPrefs) {
			return mPrefs.getString(FIRMWARE_BASIC, null);
		}
	}
	
	public void setFirmwareBasicMD5(String mdsum) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(FIRMWARE_BASIC_MD5, mdsum);
			editor.apply();
		}
	}

	public String getFirmwareBasicMD5() {
		synchronized (mPrefs) {
			return mPrefs.getString(FIRMWARE_BASIC_MD5, null);
		}
	}

	//
	//
	//end FW basic's infos}
	//
	//

	//=============================================================================

	//
	//
	//{FW delta's infos
	//
	//

	public void setFirmwareDelta(String filePath) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(FIRMWARE_DELTA, filePath);
			editor.apply();
		}
	}

	public String getFirmwareDelta() {
		synchronized (mPrefs) {
			return mPrefs.getString(FIRMWARE_DELTA, null);
		}
	}
	
	public void setFirmwareDeltaMD5(String md5sum) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(FIRMWARE_DELTA_MD5, md5sum);
			editor.apply();
		}
	}

	public String getFirmwareDeltaMD5() {
		synchronized (mPrefs) {
			return mPrefs.getString(FIRMWARE_DELTA_MD5, null);
		}
	}
	//
	//
	//end FW delta's infos}
	//
	//
	
	public void resetInfo() {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.remove(FIRMWARE_VERSION);
			editor.remove(FIRMWARE_BASIC);
			editor.remove(FIRMWARE_DELTA);
			editor.putInt(DOWNLOAD_STATE, STATE_IDLE);
			editor.apply();
		}
	}

	public void setFirmwareInfoFromSharedPreferences(FirmwareInfo info) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString(INTEND_FIRMWARE_NAME, info.getFirmwareName());
			editor.putString(INTEND_FIRMWARE_VERSION, info.getFirmwareVersion());
			editor.putString(INTEND_FIRMWARE_URL, info.getFirmwareUrl());
			editor.putString(INTEND_FIRMWARE_ATTRIBUTE, info.getFirmwareAttribute());
			editor.putString(INTEND_FIRMWARE_MD5, info.getFirmwareMd5());
			editor.putInt(INTEND_FIRMWARE_SIZE, info.getFirmwareSize());
			editor.putString(INTEND_FIRMWARE_DATE, VnptOtaUtils.convetDateToStringFullInfo(info.getFirmwareDate()));
			editor.putString(INTEND_FIRMWARE_DESC, info.getFirmwareDescription());
			editor.putString(INTEND_FW_BASIC_URL, info.getFwBasicUrl());
			editor.putString(INTEND_FW_BASIC_MD5, info.getFwBasicMd5());
			editor.putInt(INTEND_FW_BASIC_SIZE, info.getFwBasicSize());
			editor.apply();
		}
	}

	public FirmwareInfo getFirmwareInfoFromSharedPreferences() {
		synchronized (mPrefs) {
			return new FirmwareInfo(
				mPrefs.getString(INTEND_FIRMWARE_NAME, null), 
				mPrefs.getString(INTEND_FIRMWARE_VERSION, null), 
				mPrefs.getString(INTEND_FIRMWARE_URL, null), 
				mPrefs.getString(INTEND_FIRMWARE_ATTRIBUTE, null), 
				mPrefs.getString(INTEND_FIRMWARE_MD5, null), 
				mPrefs.getInt(INTEND_FIRMWARE_SIZE, -1), 
				VnptOtaUtils.convetStringToDate(mPrefs.getString(INTEND_FIRMWARE_DATE, null)), 
				mPrefs.getString(INTEND_FIRMWARE_DESC, null),
				mPrefs.getString(INTEND_FW_BASIC_URL, null),	//nctmanh: 02112016 - for new ota flow
				mPrefs.getString(INTEND_FW_BASIC_MD5, null),	//nctmanh: 02112016 - for new ota flow
				mPrefs.getInt(INTEND_FW_BASIC_SIZE, -1));
		}
	}

	public void resetFirmwareInfo() {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.remove(INTEND_FIRMWARE_NAME);
			editor.remove(INTEND_FIRMWARE_VERSION);
			editor.remove(INTEND_FIRMWARE_URL);
			editor.remove(INTEND_FIRMWARE_ATTRIBUTE);
			editor.remove(INTEND_FIRMWARE_MD5);
			editor.remove(INTEND_FIRMWARE_SIZE);
			editor.remove(INTEND_FIRMWARE_DATE);
			editor.remove(INTEND_FIRMWARE_DESC);
			editor.remove(INTEND_FW_BASIC_URL);
			editor.remove(INTEND_FW_BASIC_MD5);
			editor.remove(INTEND_FW_BASIC_SIZE);
			editor.apply();
		}
	}

	public void setVnptOtaUiIsRunning(int isRunning) {
		synchronized (mPrefs) {
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putInt(CHECK_VNPTOTA_UI_IS_RUNNING, isRunning);
			editor.apply();
		}
	}

	public int getVnptOtaUiIsRunning() {
		synchronized (mPrefs) {
			return mPrefs.getInt(CHECK_VNPTOTA_UI_IS_RUNNING, -1);
		}
	}
}














