package com.app.myapplication.common;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.app.myapplication.R;
import com.app.myapplication.download.DownloadUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoUtils;
import com.app.myapplication.settings.OtaSettingsHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class VnptOtaUtils {

	private static final String TAG = "VNPTTech_OTA";
	private static final boolean DEBUG = true;
	//private static final String UPDATE_XML = "http://otatech.mytvnet.vn:8090/update-firmware";
	private static final String UPDATE_XML = "http://ota.vnptsmartphone.vn/lotus/web/web.php/updateFirmware";
	//private static final String UPDATE_XML = "http://10.84.8.26/lotus/web/web.php/updateFirmware";
	//private static final String UPDATE_XML = "http://10.84.8.26/web.php/updateFirmware";
	

	private static final String UPDATE_XML_BUILD_PROP = "ro.vnpt.ota";
	public static final String FIRMWARE_FILE_LOCATION_DEFAULT = "/data/vnpt-firmware";
	public static final String FIRMWARE_FILE_LOCATION = SystemProperties.get("ro.product.otafilelocation", FIRMWARE_FILE_LOCATION_DEFAULT) + "/delta";
	public static final String FIRMWARE_BASIC_FILE_LOCATION = SystemProperties.get("ro.product.otafilelocation", FIRMWARE_FILE_LOCATION_DEFAULT) + "/basic";

	public static final String ACTION_QUERY_FIRMWARE = "com.vnptt.ota.QUERY_FIRMWARE";
	public static final String ACTION_MANUAL_DOWNLOAD = "com.vnptt.ota.MANUAL_DOWNLOAD";
	public static final String ACTION_DOWNLOAD_LISTENER = "com.vnptt.ota.DOWNLOAD_LISTENER";
	public static final String ACTION_MANUAL_PAUSE = "com.vnptt.ota.MANUAL_PAUSE";
	public static final long AUTO_QUERY_INTERVAL_TIME = 24 * 60 * 60 * 1000;

	// KHAICUCAI add INTEND defind
	public static final String INTEND_DL_LISTENER = "DL_LISTENER";
	public static final String INTEND_MSG_TYPE = "MSG_TYPE";
	public static final String INTEND_FILE_PATH = "FILE_PATH";
	public static final String INTEND_BASIC_FILE_PATH_EXTEND = "INTEND_BASIC_FILE_PATH_EXTEND";
	public static final String INTEND_ERROR_CODE = "ERROR_CODE";
	public static final String INTEND_DOWLOADING_SIZE = "DOWLOADING_SIZE";
	public static final String INTEND_PROGRESS = "PROGRESS";
	public static final String INTEND_DL_FW_STATUS = "DL_FW_STATUS";
	public static final int INTEND_DL_BASIC_DONE = 1;
	public static final int INTEND_DL_DELTA_DONE = 2;

	public static final int NO_AUTO_QUERY_ALL_CONNECTION = 1;
	public static final int AUTO_QUERY_ALL_CONNECTION = 2;
	public static final int NO_AUTO_QUERY_ONLY_WIFI = 3;
	public static final int AUTO_QUERY_ONLY_WIFI = 4;
	
	public static String target_fw_version = "0.0.0";
    public static String force_upgrade = "0";

	public static void LogDebug(String className, String log) {
		if (DEBUG || ("true").equals(SystemProperties.get("vnptt.ota.debug", "false")))
			Log.d(TAG, "class " + className + ": " + log);
	}
	public static void LogError(String className, String log) {
		Log.e(TAG, "class " + className + ": " + log);
	}
	
	public static boolean isNetworkConnected(Context ctx) {
		try {
			// ConnectivityManager lớp trả lời các truy vấn về trạng thái kết nối mạng. 
			// Nó cũng thông báo cho các ứng dụng khi kết nối mạng thay đổi.
			ConnectivityManager connMgr = (ConnectivityManager) 
					ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo;
			if(isSettopBox()) {//for STB, only support Wifi and Ethernet
				networkInfo = connMgr.getActiveNetworkInfo();
				if (networkInfo != null && networkInfo.isConnected()
					&& (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET)) {
					return true;
				} else {
					return false;
				}
			} else { //for SmartPhone
//				int settingState = OtaSettingsHelper.getInt(ctx.getContentResolver(),
//						OtaSettingsHelper.OTA_SETTINGS, AUTO_QUERY_ALL_CONNECTION);	//nctmanh: 05082015 - allow check new ota fw in mobile data connection
//				if (settingState == AUTO_QUERY_ALL_CONNECTION) {
//					networkInfo = connMgr.getActiveNetworkInfo();
//					if (networkInfo != null && networkInfo.isConnected()) {
//						return true;
//					} else {
//						return false;
//					}
//				} else if(AUTO_QUERY_ONLY_WIFI == settingState){
//					//only support WIFI
//					networkInfo = connMgr.getActiveNetworkInfo();
//					if (networkInfo != null && networkInfo.isConnected()
//						&& (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
//						return true;
//					} else {
//						return false;
//					}
//				}
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	///nctmanh: 05082015 - allow check new ota fw in mobile data connection - @{
//	public static boolean isConnectedMobile(Context ctx) {
//		try {
//			ConnectivityManager connMgr = (ConnectivityManager)
//					ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
//			NetworkInfo networkInfo;
//			networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//			if (networkInfo != null && networkInfo.isConnected()) {
//				return true;
//			} else {
//				return false;
//			}
//
//		} catch (Exception e) {
//			return false;
//		}
//	}
	///@}
	public static boolean isAutoQuery(Context ctx) {
		//always return true for box
		if(isSettopBox()) {
			return true;
		}
		int settingState = OtaSettingsHelper.getInt(ctx.getContentResolver(), 
		OtaSettingsHelper.OTA_SETTINGS, AUTO_QUERY_ALL_CONNECTION);	//nctmanh: 05082015 - allow check new ota fw in mobile data connection
		if ((settingState == AUTO_QUERY_ALL_CONNECTION) || (settingState == AUTO_QUERY_ONLY_WIFI)) {
			return true;
		}
		return false;
	}
	
	public static boolean isForceUpdate (){
		//kiểm tra xem thuộc tính hiện tại của hệ thống có " " không , nếu có trả về true , bên cạnh là giá trị mặc định 
		return SystemProperties.getBoolean("ro.product.otaforceupdate", false);
	}
	
	public static boolean supportDowngradeFW(){
		return SystemProperties.getBoolean("ro.product.otadowngrade", false);
	}
	
	public static boolean isSettopBox(){
		// SystemProperties xác định thuộc tính hệ thống hiện tại 
		return SystemProperties.getBoolean("ro.product.settopbox", true);
	}

	//kiểm tra xem có link tải firmware trong database không 
	public static String getUpdateXmlLink(Context ctx) {

		//Look up a "vnptt_ota_url" in the database.
		String updateXmlLink =  Settings.System.getString(ctx.getContentResolver(), "vnptt_ota_url");

		if (DEBUG) Log.d(TAG, "updateXmlLink is " + updateXmlLink);

		if(TextUtils.isEmpty(updateXmlLink))
			updateXmlLink = SystemProperties.get("ro.product.otaupdateurl", UPDATE_XML);

		if (DEBUG) Log.d(TAG, "updateXmlLink is " + updateXmlLink);
		return updateXmlLink;
	}
	
	public static String getDeviceId(Context context){
		try{
			if (isSettopBox())
			return getMACUser();
		} catch (Exception e){ // for stb
			VnptOtaUtils.LogDebug(LOG_TAG, "showNewVersionNotification");
		}
//		else { // for smartphone
//			return getIMEI(context);
//		}
	}

//	public static String getIMEI(Context context){
//		String imei = null;
//		try {
//			TelephonyManager mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//			imei = mngr.getDeviceId();
//		} catch (Exception e){}
//
//		if (imei == null) {
//			imei = "888888888888888";
//		}
//		return imei;
//	}
	
	//nctmanh: 05122015 - get MacAddress for vnpt ota client -@{
	public static String getMACUser() {
		String temp = getMACAddress("eth0").replaceAll(":", ""); 
		if(temp ==null){
			temp = "888888888888";
		}
		if (DEBUG) Log.d(TAG, "Mac: " + temp);
		return temp;
	}
	/**
	 * Returns MAC address of the given interface name.
	 * @param interfaceName eth0, wlan0 or NULL=use first interface 
	 * @return  mac address or empty string
	 */
	public static String getMACAddress(String interfaceName) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			
			for (NetworkInterface intf : interfaces) {
				if (interfaceName != null) {
					if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
				}

				byte[] mac = intf.getHardwareAddress();

				if (mac==null) return "";
				StringBuilder buf = new StringBuilder();

				for (int idx=0; idx<mac.length; idx++)
					buf.append(String.format("%02X:", mac[idx]));   

				if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
				return buf.toString();
			}
		} catch (Exception ex) { 
			
		} 
		// for now eat exceptions
		return "";
	}
	//@}

	// Hàm băm dùng MD5 luôn cho ra kết quả là một chuỗi có độ dài cố định 32 ký tự cho dù 
	// đầu vào là gì đi nữa, và kết quả này không thể dịch ngược (không thể giải mã) lại được.
	// TODO: thêm muối cho md5
	public static String md5String (String input) {
		String result = null;
		try {			
			if(input != null) {
				MessageDigest md = MessageDigest.getInstance("MD5"); //or "SHA-1"
				md.update(input.getBytes());
				BigInteger hash = new BigInteger(1, md.digest());
				result = hash.toString(16);
				while(result.length() < 32) { //40 for SHA-1
					result = "0" + result;
				}
			}
			return result;
		} catch (Exception e) {}
		return result;
	}

	public static Date convetStringToDate(String date) {
		if (date == null)
			return null;
		try {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(date);
		}
		catch (ParseException e) {
		}
		return null;
	}
	
	public static Date convetStringToDate2(String date) {
		if (date == null)
			return null;
		try {
			return new SimpleDateFormat("dd/MM/yyyy").parse(date);
		}
		catch (ParseException e) {
		}
		return null;
	}

	public static String convetDateToString(Date date) {
		if (date == null)
			return null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			return sdf.format(date);
		}
		catch (Exception e) {
		}
		return null;
	}

	public static String convetDateToStringFullInfo(Date date) {
		if (date == null)
			return null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			return sdf.format(date);
		}
		catch (Exception e) {
		}
		return null;
	}

	private static final char[] HEX_DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	private static String byteArrToStr(byte[] bytes) {

		StringBuffer str = new StringBuffer();

		for (int q = 0; q < bytes.length; q++) {
			str.append(HEX_DIGITS[(0xF0 & bytes[q]) >>> 4]);
			str.append(HEX_DIGITS[0xF & bytes[q]]);
		}
		return str.toString();
	}

	public static String getMd5sum(File tempFle) {

		MessageDigest mdEnc = null;
		InputStream inputStream = null;
		byte[] buffer = new byte[4096];
		int numRead = 0;

		try {
			inputStream = new FileInputStream(tempFle);
			mdEnc = MessageDigest.getInstance("MD5");
			while (numRead != -1) {
				numRead = inputStream.read(buffer);
				if (numRead > 0)
					mdEnc.update(buffer, 0, numRead);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) { }
			}
		}
		String dlMd5 = byteArrToStr(mdEnc.digest());
		return dlMd5;	
	}


	public static void setRepeatAlarm(Context context, int alarmType, long triggerAtTime, long interval, String action) {
		
		Intent it = new Intent(action);
		// AlarmManager trong Android là một cầu nối giữa ứng dụng và alarm service của hệ thống Android. 
		// Nó có thể gửi một bản tin broadcast tới ứng dụng của bạn ở thời điểm đã được lên lịch trước đó.
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		//Cờ chỉ ra rằng nếu PendingIntent được mô tả đã tồn tại, thì cái hiện tại sẽ bị hủy trước khi tạo cái mới.
		PendingIntent operation = PendingIntent.getBroadcast(context, 0, it, 
				PendingIntent.FLAG_CANCEL_CURRENT);

		if ((alarmMgr != null) && (operation != null)) {
			alarmMgr.cancel(operation);
			alarmMgr.setInexactRepeating(alarmType, triggerAtTime, interval, operation);
		} else {
			Log.d(TAG, "setRepeatAlarm: can not set... " + action);
		}
	}

	//thông báo lại
	public static void setAlarm(Context context, int alarmType, long time, String action) {

		Intent it = new Intent(action);

		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent operation = PendingIntent.getBroadcast(context, 0, it,
				PendingIntent.FLAG_CANCEL_CURRENT);

		if ((alarmMgr != null) && (operation != null)) {
			alarmMgr.cancel(operation);
			alarmMgr.set(alarmType, time, operation);
		}
	}

	//hủy thông báo 
	public static void cancelAlarm(Context context, String action) {
		try {
			Intent it = new Intent(action);

			AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			PendingIntent operation = PendingIntent.getBroadcast(context, 0, it,
					PendingIntent.FLAG_CANCEL_CURRENT);

			if ((alarmMgr != null) && (operation != null)) {
				alarmMgr.cancel(operation);
			}
		} catch (Exception e) {}
	}
	
	// kiểm tra firmwareVersion đã trùng với os Build chưa 
	public static boolean upgradeSuccessfully(String firmwareVersion) {

		Log.d(TAG, "upgradeSuccessfully:  " + firmwareVersion + " vs " + android.os.Build.DISPLAY);

		if (firmwareVersion == null)
			return false;

		String bufCurVer = android.os.Build.DISPLAY;

		if ((bufCurVer == null) || (firmwareVersion == null))
			return false;
		try {
			if (firmwareVersion.equalsIgnoreCase(bufCurVer)) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}

		return false;
	}
	
	//nctmanh: 11012017 - notifiy continue update for new ota flow-@{
	public static boolean isBasicFwVersion() {
		String bufCurVer = android.os.Build.DISPLAY;
		if (bufCurVer == null)
			return false;
		try {
			String fwBasicVer = "1.0.0";
			if (fwBasicVer.equalsIgnoreCase(bufCurVer)) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}

		return false;
	}
	//@}
	
	private static String getprop(String name) {
		// using process getprop to retrieve system property
		// sử dụng process getprop để truy xuất thuộc tính hệ thống

		ProcessBuilder pb = new ProcessBuilder("/system/bin/getprop", name);
		pb.redirectErrorStream(true);

		Process p = null;
		InputStream is = null;
		try {
			p = pb.start();
			is = p.getInputStream();
			Scanner scan = new Scanner(is);
			//xuống dòng khi thấy \n
			scan.useDelimiter("\n");
			String prop = scan.next();
			if (prop.length() == 0)
				return null;
			return prop;
		}
		catch (NoSuchElementException e) {
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (is != null) {
				try {is.close();}
				catch (Exception e) {}
			}
		}
		return null;
	}
	
	public static String convertErrCodeToString (Context context, int errCode) {
		
		String errorCodeString = null;
		switch (errCode) {
		case DownloadUtils.FORMED_URL_EXCEPTION:

			break;
		case DownloadUtils.CREATE_FIRMARE_FOLDER_ERROR:
			errorCodeString = context.getResources().getString(R.string.download_error_create_folder);
			break;
		case DownloadUtils.NETWORK_NOT_CONNECT:
			errorCodeString = context.getResources().getString(R.string.download_error_network_not_connected);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_RANGE_NOT_SATISFIABLE:
			errorCodeString = context.getResources().getString(R.string.download_error_range_not_satisfiable);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_OPEN_CONNECTION:
			errorCodeString = context.getResources().getString(R.string.download_error_open_connection);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_CONNECT_TO_RESOURCE:
			errorCodeString = context.getResources().getString(R.string.download_error_connect_to_resource);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_GET_INPUT_STREAM:
			errorCodeString = context.getResources().getString(R.string.download_error_file_stream);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_WRITE_OUTPUT_STREAM:
			errorCodeString = context.getResources().getString(R.string.download_error_file_stream);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_IN_OUT_STREAM:
			errorCodeString = context.getResources().getString(R.string.download_error_file_stream);
			break;
		case DownloadUtils.DOWNLOAD_ERROR_UNKNOWN:

			break;
		case DownloadUtils.DOWNLOAD_ERROR_USER_CANCEL:

			break;
		case DownloadUtils.TOTAL_SIZE_NOT_MATCH:
			errorCodeString = context.getResources().getString(R.string.download_error_size_not_match);
			break;
		case FirmwareInfoUtils.NETWORK_NOT_CONNECT:
			errorCodeString = context.getResources().getString(R.string.download_error_network_not_connected);
			break;
		case FirmwareInfoUtils.ERROR_CLIENT_PROTOCOL:
			errorCodeString = context.getResources().getString(R.string.get_info_error_client_protocol);
			break;
		case FirmwareInfoUtils.ERROR_PARSER_XML:
			errorCodeString = context.getResources().getString(R.string.get_info_error_parser_xml);
			break;
		case FirmwareInfoUtils.UNKNOWN_ERROR:

			break;
		case FirmwareInfoUtils.ERROR_GET_XML:
			errorCodeString = context.getResources().getString(R.string.get_info_error_no_xml);
			break;
		case FirmwareInfoUtils.ERROR_PARSER_FIRMWARE_INFO:
			errorCodeString = context.getResources().getString(R.string.get_info_error_get_rom_info);
			break;
		case FirmwareInfoUtils.NO_FIRMWARE_UPDATE:
			errorCodeString = context.getResources().getString(R.string.get_info_error_no_firmware);
		default:

			break;
		}
		return errorCodeString;
	}
	
	//nctmanh: 05122015 - add for vnpt ota client -@{
	public static String getCurRomVersion() {
		return (android.os.Build.DISPLAY);
	}
	public static String getVersionApp(Context ctx){
		try {
			PackageManager manager = ctx.getPackageManager();
			PackageInfo info = manager.getPackageInfo(ctx.getPackageName(), 0);
			return (String.valueOf(info.versionName));
		} catch (Exception e){
			return null;
		}
	}
	//@}
}
























