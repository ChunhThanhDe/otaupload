package com.app.myapplication.main;

//public class VnptOtaHelperMgrUtils {
//	public static final int ERROR_BIND_SERVICE_FAILD = 120;
//}

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class VnptOtaHelperMgrUtils {
	public static final int ERROR_BIND_SERVICE_FAILD = 120;

	public static final String OTA_SETTINGS = "ota_settings";	
	public static final int NO_AUTO_QUERY_ALL_CONNECTION = 1;
	public static final int AUTO_QUERY_ALL_CONNECTION = 2;
	public static final int NO_AUTO_QUERY_ONLY_WIFI = 3;
	public static final int AUTO_QUERY_ONLY_WIFI = 4;

	public static final int FORMED_URL_EXCEPTION = 31;
	public static final int CREATE_FIRMARE_FOLDER_ERROR = 32;
	public static final int NETWORK_NOT_CONNECT = 33;
	public static final int HTTP_REQUEST_RANGE_NOT_SATISFIABLE = 416;
	public static final int DOWNLOAD_ERROR_RANGE_NOT_SATISFIABLE = 40;
	public static final int DOWNLOAD_ERROR_OPEN_CONNECTION = 41;
	public static final int DOWNLOAD_ERROR_CONNECT_TO_RESOURCE = 42;
	public static final int DOWNLOAD_ERROR_GET_INPUT_STREAM = 43;
	public static final int DOWNLOAD_ERROR_WRITE_OUTPUT_STREAM = 44;
	public static final int DOWNLOAD_ERROR_IN_OUT_STREAM = 45;
	public static final int DOWNLOAD_ERROR_UNKNOWN= 50;

	public static final int DOWNLOAD_ERROR_USER_CANCEL = 60;
	public static final int TOTAL_SIZE_NOT_MATCH = 61;

	public static final int NETWORK_NOT_CONNECT2 = 93;
	public static final int ERROR_CLIENT_PROTOCOL = 94;
	public static final int ERROR_PARSER_XML = 95;
	public static final int UNKNOWN_ERROR = 96;
	public static final int ERROR_GET_XML = 97;
	public static final int ERROR_PARSER_FIRMWARE_INFO = 98;

	public static final int NO_FIRMWARE_UPDATE = 100;

	public static final int RESPONSE_TYPE_USER_QUERY = 1;
	public static final int RESPONSE_TYPE_AUTO_QUERY = 2;
	public static final int RESPONSE_TYPE_CONFIGURE = 3;

	private static final String ROM_RELEASE_ID = "ro.build.display.id";
	private static final String ROM_RELEASE_DATE = "ro.build.date.utc";

	public static String getCurRomVersion_new() {
		return (android.os.Build.DISPLAY);
		//		return getprop(ROM_RELEASE_ID);
	}
	public static String getCurRomDate_new() {

		long val = Long.parseLong(getprop(ROM_RELEASE_DATE));
		Date date=new Date(val*1000);
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(date);
	}

	private static String getprop(String name) {
		//sử dụng process getprop để truy xuất thuộc tính hệ thống
		// using process getprop to retrieve system property
		ProcessBuilder pb = new ProcessBuilder("/system/bin/getprop", name);
		pb.redirectErrorStream(true);
		Process p = null;
		InputStream is = null;
		try {
			p = pb.start();
			is = p.getInputStream();
			Scanner scan = new Scanner(is);
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
}
