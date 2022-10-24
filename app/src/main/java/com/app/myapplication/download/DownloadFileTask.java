package com.app.myapplication.download;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;


public class DownloadFileTask extends AsyncTask<Void, Void, String>  {

	private static final String LOG_TAG = "DownloadFileTask";

	private final static int BUFFER_SIZE = 1024 * 8;

	private Context mCtx;
	private RandomAccessFile outputStream;
	private RandomAccessFile outputStreamBasic;
	private InputStream inputStream = null;
	private InputStream inputStreamBasic = null;
	private URL mURL = null;
	private URL mfwBasicURL = null;	//nctmanh: 02112016 - for new ota flow
	private DownloadManager.DownloaderListener listener;
	public static final String REQUEST_METHOD = "GET";

	private long downloadSize;
	private long downloadSizeBasic;
	private long downloadSizeBoth;
	private long previousFileSize;
	private long previousFileSizeBasic;
	private long totalSizeDelta;
	private long totalSizeBasic;
	private long totalSize;
	private long downloadPercent;
	int responseCode;
	int responseCodeDelta;
	private HttpURLConnection httpConn = null;
	private HttpURLConnection httpConnBasic = null;

	private File tempFile;
	private String fileName = "";
	private VnptOtaState downloadState;
	
	//nctmanh: 02112016 - for new ota flow - @{
	private File tempfwBasicFile;
	private String fwBasicfileName = "";
	private boolean isDownloadedFwBasic = false;


	//Báo cáo Tiến độ Truy cập Ngẫu nhiên
	private final class ProgressReportingRandomAccessFile extends RandomAccessFile{
		public ProgressReportingRandomAccessFile(File file, String mode)
				throws FileNotFoundException {
			super(file, mode);
		}

		@Override
		public void write(byte[] buffer, int offset, int count) throws IOException {
			super.write(buffer, offset, count);
		}
	}


	//nctmanh: 02112016 - for new ota flow
	public DownloadFileTask(Context ctx, String folderPath,String urlFile, String fwBasicUrl, Boolean isDownloaded,
			VnptOtaState downloadState, DownloadManager.DownloaderListener listener) {

		VnptOtaUtils.LogDebug(LOG_TAG, "oncreate DownloadFileTask");

		mCtx = ctx;
		this.listener = listener;

		try {
			VnptOtaUtils.LogDebug(LOG_TAG, "urlFile=" + urlFile);
			// nếu urlfile không có dữ liệu thì lưu vào mUrl
			if(!TextUtils.isEmpty(urlFile))
				mURL = new URL(urlFile);
			else mURL = null;

			VnptOtaUtils.LogDebug(LOG_TAG, "fwBasicUrl=" + fwBasicUrl);

			if(!TextUtils.isEmpty(fwBasicUrl))
				mfwBasicURL = new URL(fwBasicUrl);
			else
				mfwBasicURL = null;

		} catch (Exception e) {

			VnptOtaUtils.LogError(LOG_TAG, "FORMED_URL_EXCEPTION " + e);

			if (listener != null)
				listener.downloadError(DownloadUtils.FORMED_URL_EXCEPTION);

			return;
		}

		File mFolder = new File(folderPath);
		//tạo folder 
		if (!mFolder.exists()) {
			if (mFolder.mkdir()) {

				VnptOtaUtils.LogDebug(LOG_TAG, "create folder is successful");

			} else {

				VnptOtaUtils.LogError(LOG_TAG, "create folder is not successful");

				if (listener != null)
					listener.downloadError(DownloadUtils.CREATE_FIRMARE_FOLDER_ERROR);
				return;
			}
			
		}
		//create folder for fw basic
		File mFwBasicFolder = new File(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION);
		if (!mFwBasicFolder.exists()) {
			if (mFwBasicFolder.mkdir()) {

				VnptOtaUtils.LogDebug(LOG_TAG, "create fw basicfolder is successful");

			} else {

				VnptOtaUtils.LogError(LOG_TAG, "create fw basic folder is not successful");

				if (listener != null)
					listener.downloadError(DownloadUtils.CREATE_FIRMARE_FOLDER_ERROR);
				return;
			}
			
		}

		if(mURL != null) {
			this.fileName = new File(mURL.getFile()).getName();		
			this.tempFile = new File(folderPath, this.fileName);	
		}
		//nctmanh: 02112016 - for new ota flow - @{
		if(mfwBasicURL!= null){
			this.fwBasicfileName= new File(mfwBasicURL.getFile()).getName(); 
			this.tempfwBasicFile = new File(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION, this.fwBasicfileName);
			this.isDownloadedFwBasic = isDownloaded;
		} else {
			this.isDownloadedFwBasic = true;
		}
		//@}
		
		this.downloadState = downloadState;
	}	

	// dowload firmware 
	@Override
	protected String doInBackground(Void... arg0) {

	    VnptOtaUtils.LogDebug(LOG_TAG, "run() DownloadFileTask");

		if(isDownloadedFwBasic){
			if(mURL != null)
				downloadFirmware();

			VnptOtaUtils.LogDebug(LOG_TAG, "run() Download fw delta Done");

		}else{
			if(mfwBasicURL != null)
				downloadFirmwareBasic();

			VnptOtaUtils.LogDebug(LOG_TAG, "run() Download fw basic Done");
		}
		return "";
	}

	@Override
    protected void onPreExecute() {  
        // Do things before downloading on UI Thread
    }

    // @Override
    // protected void onPostExecute(final Void result) {
    //     // Do things on UI thread after downloading, then execute your callback
    //     VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI download fw is Done!!!");
    // }
	
	// @Override
	// public void run() {
	// 	VnptOtaUtils.LogDebug(LOG_TAG, "run() DownloadFileTask");
	// 	if(isDownloadedFwBasic){
	// 		if(mURL != null)
	// 			downloadFirmware();
	// 		VnptOtaUtils.LogDebug(LOG_TAG, "run() Download fw delta Done");
	// 	}else{
	// 		if(mfwBasicURL != null)
	// 			downloadFirmwareBasic();
	// 		VnptOtaUtils.LogDebug(LOG_TAG, "run() Download fw basic Done");
	// 	}
	// }
	
	//nctmanh: 02112016 - for new ota flow - @{
	private void downloadFirmwareBasic() {
		try {
			try {

				if(!VnptOtaUtils.isNetworkConnected(mCtx)) {
					if (listener != null)
						listener.downloadError(DownloadUtils.NETWORK_NOT_CONNECT);
					return;
				}
				//Kết nối với URL HTTP đã cho và định cấu hình hết thời gian để tránh bị treo vô hạn.
				httpConnBasic = (HttpURLConnection) mfwBasicURL.openConnection();
				//10s
				httpConnBasic.setReadTimeout(DownloadUtils.REQUEST_TIMEOUT);
				httpConnBasic.setConnectTimeout(DownloadUtils.REQUEST_TIMEOUT);
				//get 
				httpConnBasic.setRequestMethod(REQUEST_METHOD);
				//~ httpConnBasic.setDoInput(true);
				//~ httpConnBasic.setDoOutput(true);
			} catch (IOException e) {

				VnptOtaUtils.LogError(LOG_TAG, "check network exception" + e.toString());

				if (listener != null)

					listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_OPEN_CONNECTION);

				return;
			}

			VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmwareBasic: downloadState: " + downloadState.getState());

			// nếu fwbasicFile tạm thời có tồn tại
			if (tempfwBasicFile.exists()) {

				VnptOtaUtils.LogDebug(LOG_TAG, "tempfwBasicFile exist");

				if ((downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_STOP) || 
						(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE) || 
						(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_COMPLETED) ||
						(downloadState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD) ||
						(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS)){

					//previous: trước 
					previousFileSizeBasic = tempfwBasicFile.length();	
					//nctmanh: 02112016 - for new ota flow
					VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmwareBasic: resume download");

					httpConnBasic.setRequestProperty("Range", "bytes="+ previousFileSizeBasic +"-");

					totalSizeBasic = tempfwBasicFile.length();	
					//nctmanh: 02112016 - for new ota flow
				} else {

					VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmwareBasic: delete file and start download");

					tempfwBasicFile.delete();
					previousFileSizeBasic = 0;
					totalSizeBasic = 0;
				}
			} else {

				previousFileSizeBasic = 0;
				totalSizeBasic = 0;
			}


			try {
				if(!VnptOtaUtils.isNetworkConnected(mCtx)) {
					if (listener != null)
						listener.downloadError(DownloadUtils.NETWORK_NOT_CONNECT);
					return;
				}

				//dowload 
				httpConnBasic.connect();

				responseCode = httpConnBasic.getResponseCode();

			} catch (IOException e) {

				VnptOtaUtils.LogDebug(LOG_TAG, "DOWNLOAD_ERROR_CONNECT_TO_RESOURCE: " + e.toString());

				if (listener != null)
					listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_CONNECT_TO_RESOURCE);
				return;
			}

			VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmwareBasic: Http Response: " + responseCode);

			//nếu file đã tồn tại thì xóa 
			if(tempfwBasicFile.exists() && (previousFileSizeBasic > 0) 
				&& (responseCode != HttpURLConnection.HTTP_PARTIAL)){

				VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmwareBasic: Server not support Partial Content. Remove exited File" );

				tempfwBasicFile.delete();
				previousFileSizeBasic = 0;
				totalSizeBasic = 0;
			}

			totalSizeBasic = httpConnBasic.getContentLength() + totalSizeBasic;

			if (responseCode == DownloadUtils.HTTP_REQUEST_RANGE_NOT_SATISFIABLE) {

				if(tempfwBasicFile.exists()) totalSizeBasic++;

				outOfRangeBasic();

			} else if ((responseCode == HttpURLConnection.HTTP_OK) 
				|| (responseCode == HttpURLConnection.HTTP_PARTIAL)){

				downloadFwBasicFile(httpConnBasic);

			} else {
				if (listener != null) listener.downloadError(responseCode);
			}
		}
		catch (Exception e) {

			VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmwareBasic: exception" + e.toString());

			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_UNKNOWN);
		}
	}
	
	private void downloadFwBasicFile(HttpURLConnection httpConn) {
		try {
			outputStreamBasic = new ProgressReportingRandomAccessFile(tempfwBasicFile, "rw");
			inputStreamBasic = httpConn.getInputStream();

		} catch (IOException e){
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_GET_INPUT_STREAM);
			return;
		} catch (Exception e) {
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_WRITE_OUTPUT_STREAM);
			return;
		}

		int bytesRead = -1;
		int progressDownFile = (int)previousFileSizeBasic;
		int bufferSize = BUFFER_SIZE;

		if(downloadState.getState() == VnptOtaState.STATE_DOWNLOADING 
			&& VnptOtaUtils.isForceUpdate())
			bufferSize = BUFFER_SIZE/8;

		byte[] buffer = new byte[bufferSize];

		VnptOtaUtils.LogDebug(LOG_TAG, "downloadFwBasicFile: new download seek:" + previousFileSizeBasic + " with bufferSize=" + bufferSize);

		try {
			outputStreamBasic.seek(previousFileSizeBasic);

			while ((bytesRead = inputStreamBasic.read(buffer)) != -1) {	
				// VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI download fw Basic update state  buffer: " + buffer);
				if ((downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE)){

					VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI cancel 1!");

					listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_USER_CANCEL);		
					break;
				}
				
				if ((downloadState.getState() != VnptOtaState.STATE_DOWNLOADING)
					&& (downloadState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD)
				){	
					VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI cancel 2!");

					break;
				}

				if(!VnptOtaUtils.isNetworkConnected(mCtx)) {

					listener.downloadError(DownloadUtils.NETWORK_NOT_CONNECT);

					break;
				}

				outputStreamBasic.write(buffer, 0, bytesRead);
				progressDownFile += bytesRead;
				publishProgressBasic(progressDownFile);

				//limit the bandwidth for force stop update: ~1Mb/s
				if(downloadState.getState() == VnptOtaState.STATE_DOWNLOADING && VnptOtaUtils.isForceUpdate()){
					//sleep 10ms
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// nothing
					}
				}
			}
		} catch (IOException e) {
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_IN_OUT_STREAM);			
			return;
		}	
		onPostExecuteFwBasic();
	}
	
	
	// tiến độ dowload fw basic theo thời gian 
	private void publishProgressBasic(int progress) {
		downloadSizeBasic = progress;
		downloadPercent = (downloadSizeBasic) * 100 / (totalSizeBasic);
		if (listener != null)
			listener.downloadProgess(downloadSizeBasic,downloadPercent);
	}

	private void onPostExecuteFwBasic() {
		try {
			if (outputStreamBasic != null)
				outputStreamBasic.close();
			if (inputStreamBasic != null)
				inputStreamBasic.close();			
			if (httpConnBasic != null) 
				httpConnBasic.disconnect();
		} catch (Exception e){}

		VnptOtaUtils.LogDebug(LOG_TAG, "onPostExecuteFwBasic: " + downloadState.getState());
		if (!(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE)) {
			if(isDownloadSuccessfullyBasic()) {
				if (listener != null)
					listener.downloadSuccessFileBasic(tempfwBasicFile);
				if(mURL != null)
					downloadFirmware();//START download delta after download basic completed
			}
		}
	}
	//@}
	
	//START download delta after download basic completed
	private void downloadFirmware() {
		try {
			try {
				if(!VnptOtaUtils.isNetworkConnected(mCtx)) {
					if (listener != null)
						listener.downloadError(DownloadUtils.NETWORK_NOT_CONNECT);
					return;
				}
				httpConn = (HttpURLConnection) mURL.openConnection();
				httpConn.setReadTimeout(DownloadUtils.REQUEST_TIMEOUT);
				httpConn.setConnectTimeout(DownloadUtils.REQUEST_TIMEOUT);
				httpConn.setRequestMethod(REQUEST_METHOD);
				//~ httpConn.setDoInput(true);
				//~ httpConn.setDoOutput(true);
			} catch (IOException e) {

				VnptOtaUtils.LogDebug(LOG_TAG, "check network exception" + e.toString());

				if (listener != null)
					listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_OPEN_CONNECTION);
				return;
			}

			VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmware: downloadState: " + downloadState.getState());
			
			if (tempFile.exists()) {

				VnptOtaUtils.LogDebug(LOG_TAG, "tempFile exist");

				if ((downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_STOP) || 
						(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE) || 
						(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_COMPLETED) ||
						(downloadState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD) ||
						(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS)){

					VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmware-resume download");	

					previousFileSize = tempFile.length();	//nctmanh: 02112016 - for new ota flow
					httpConn.setRequestProperty("Range", "bytes="+ previousFileSize +"-");
					totalSizeDelta = tempFile.length();	//nctmanh: 02112016 - for new ota flow
				} else {

					VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmware-delete file and start download");
					
					tempFile.delete();
					previousFileSize = 0;
					totalSizeDelta = 0;
				}
			} else {
				previousFileSize = 0;
				totalSizeDelta = 0;
			}

			try {
				if(!VnptOtaUtils.isNetworkConnected(mCtx)) {
					if (listener != null)
						listener.downloadError(DownloadUtils.NETWORK_NOT_CONNECT);
					return;
				}
				httpConn.connect();
				responseCode = httpConn.getResponseCode();
			} catch (IOException e) {

				VnptOtaUtils.LogDebug(LOG_TAG, "DOWNLOAD_ERROR_CONNECT_TO_RESOURCE: " + e.toString());
				
				if (listener != null)
					listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_CONNECT_TO_RESOURCE);
				return;
			}

			VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmware-Http Response: " + responseCode + " with method " + httpConn.getRequestMethod());
			
			if(tempFile.exists() && (previousFileSize > 0) 
				&& (responseCode != HttpURLConnection.HTTP_PARTIAL)){
				
				VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmware: Server not support Partial Content. Remove exited File" );
				
				tempFile.delete();
				previousFileSize = 0;
				totalSizeDelta = 0;
			}

			totalSizeDelta = httpConn.getContentLength() + totalSizeDelta;

			if (responseCode == DownloadUtils.HTTP_REQUEST_RANGE_NOT_SATISFIABLE) {
				if(tempFile.exists())
					totalSizeDelta++;
				outOfRanage();
			} else if ((responseCode == HttpURLConnection.HTTP_OK) 
				|| (responseCode == HttpURLConnection.HTTP_PARTIAL)){
				downloadFile(httpConn);
			} else {
				if (listener != null)
					listener.downloadError(responseCode);
			}
		}
		catch (Exception e) {

			VnptOtaUtils.LogDebug(LOG_TAG, "downloadFirmware: downloadFirmware exception " + e.toString());
			
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_UNKNOWN);
		}
	}

	private void downloadFile(HttpURLConnection httpConn) {
		try {

			outputStream = new ProgressReportingRandomAccessFile(tempFile, "rw");
			inputStream = httpConn.getInputStream();

		} catch (IOException e){
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_GET_INPUT_STREAM);
			return;
		} catch (Exception e) {
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_WRITE_OUTPUT_STREAM);
			return;
		}

		int bytesRead = -1;
		int progressDownFile = (int)previousFileSize;
		int bufferSize = BUFFER_SIZE;

		if(downloadState.getState() == VnptOtaState.STATE_DOWNLOADING 
			&& VnptOtaUtils.isForceUpdate())
			bufferSize = BUFFER_SIZE/8;
		byte[] buffer = new byte[bufferSize];

		VnptOtaUtils.LogDebug(LOG_TAG, "downloadFile: new download seek:" + previousFileSize + " with bufferSize=" + bufferSize);

		try {
			outputStream.seek(previousFileSize);
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				if (/*!isAlive() || */(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE)) {
					listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_USER_CANCEL);		
					break;
				}
				
				if ((downloadState.getState() != VnptOtaState.STATE_DOWNLOADING)
					&& (downloadState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD)
				){	
					break;
				}

				if(!VnptOtaUtils.isNetworkConnected(mCtx)) {
					listener.downloadError(DownloadUtils.NETWORK_NOT_CONNECT);	
					break;
				}
				outputStream.write(buffer, 0, bytesRead);
				progressDownFile += bytesRead;
				publishProgress(progressDownFile);
				//limit the bandwidth for force stop update: ~1Mb/s
				if(downloadState.getState() == VnptOtaState.STATE_DOWNLOADING && VnptOtaUtils.isForceUpdate()){
					//sleep 10ms
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// nothing
					}
				}
			}
		} catch (IOException e) {
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_IN_OUT_STREAM);			
			return;
		}		
		onPostExecuteFwDelta();
	}

		// tiến độ theo 100%
	private void publishProgress(int progress) {
			downloadSize = progress;
			downloadPercent = (downloadSize) * 100 / (totalSizeDelta);
			if (listener != null)
				listener.downloadProgess(downloadSize,downloadPercent);
	}


	//nctmanh: 02112016 - for new ota flow
	// nếu file tồn tại và length file = totaldelta file thì sử dụng, không thì xóa 
	private void outOfRanage(){
		if(tempFile.exists() && (tempFile.length() == totalSizeDelta)) {
			if (listener != null)
				listener.downloadSuccess(tempFile);
		} else {
			if(tempFile.exists()) {
				tempFile.delete();
			}
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_RANGE_NOT_SATISFIABLE);
		}
	}
	
	private void outOfRangeBasic(){
		if(tempfwBasicFile.exists() && (tempfwBasicFile.length() == totalSizeBasic)) {
			if (listener != null)
				listener.downloadSuccessFileBasic(tempfwBasicFile);
		} else {
			if(tempfwBasicFile.exists()) {
				tempfwBasicFile.delete();
			}
			if (listener != null)
				listener.downloadError(DownloadUtils.DOWNLOAD_ERROR_RANGE_NOT_SATISFIABLE);
		}
	}

	//hoàn thành post fw delta 
	private void onPostExecuteFwDelta() {
		try {
			if (outputStream != null)
				outputStream.close();
			if (inputStream != null)
				inputStream.close();			
			if (httpConn != null) 
				httpConn.disconnect();
		} catch (Exception e){}

		VnptOtaUtils.LogDebug(LOG_TAG, "onPostExecuteFwDelta: " + downloadState.getState());
		if (!(downloadState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE)
			&& !(downloadState.getState() == VnptOtaState.STATE_SHOW_UI_MANUAL)
			&& !(downloadState.getState() == VnptOtaState.STATE_MANUAL_QUERY)
		){
			if(isDownloadSuccessfully()) {
				if (listener != null)
					listener.downloadSuccess(tempFile);
			}
		}
	}

	private boolean isDownloadSuccessfully() {
		if(tempFile.exists() && (tempFile.length() == totalSizeDelta)) {
			return true;
		} 
		return false;
	}
	
	private boolean isDownloadSuccessfullyBasic() {
		if(tempfwBasicFile.exists() && (tempfwBasicFile.length() == totalSizeBasic)) {
			return true;
		} 
		return false;
	}
	
}
