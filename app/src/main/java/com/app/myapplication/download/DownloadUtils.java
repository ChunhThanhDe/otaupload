package com.app.myapplication.download;

public class DownloadUtils {

	//Configure download
	public static final int REQUEST_TIMEOUT = 10000;//10s
	//end - Configure download
	
	
	//Response code
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
	
	//end - Response code
	
}
