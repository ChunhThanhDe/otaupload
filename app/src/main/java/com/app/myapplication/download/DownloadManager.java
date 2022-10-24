package com.app.myapplication.download;

import android.content.Context;

import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;

import java.io.File;

public class DownloadManager {

	DownloaderListener listener;
	private DownloadFileTask dlTask;
	private VnptOtaState dlState;
	private Context context;
	private String filePath;
	private String url;
	private String fwBasicUrl;	//nctmanh: 02112016 - for new ota flow
	private boolean isDownloaded;	//nctmanh: 02112016 - for new ota flow
	
	//nctmanh: 02112016 - for new ota flow 
	public DownloadManager(Context context, String filePath,String url, String fwBasicUrl, boolean isDownloadedFwBasic) { 
		dlState = VnptOtaState.getInstance(context);
		this.context = context;
		this.filePath = filePath;
		this.url = url;
		this.fwBasicUrl = fwBasicUrl;	//nctmanh: 02112016 - for new ota flow
		this.isDownloaded = isDownloadedFwBasic;
	}

	public void addListener(DownloaderListener listener) {
		this.listener = listener;
	}

	public void startDownload() {

	VnptOtaUtils.LogDebug("DownloadManager", "KHAICUCAI: startDownload!");

		//url: link cua fw delta
		//fwbasicUrl: link cua basic

		dlTask = new DownloadFileTask(context, filePath, url, fwBasicUrl, isDownloaded, dlState, listener);	

		dlTask.execute();
		
	}

	public void resumeDownload() {

		VnptOtaUtils.LogDebug("DownloadManager", "KHAICUCAI: resumeDownload!");

		if (dlState.getState() == VnptOtaState.STATE_DOWNLOAD_PAUSE) { 
			dlTask = new DownloadFileTask(context, filePath, url, fwBasicUrl, isDownloaded, dlState, listener);
			dlTask.execute();
		}
	}

	public void pauseDownload() {

		VnptOtaUtils.LogDebug("DownloadManager", "KHAICUCAI: pauseDownload!");

		try {
			dlTask.cancel(true);
		} catch (Exception e){}
	}

	public void cancelDownload() {

		VnptOtaUtils.LogDebug("DownloadManager", "KHAICUCAI: cancelDownload!");

		try {
			if (dlState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS
				|| dlState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD
			){ 
				dlTask.cancel(true);
			}
		} catch (Exception e){}
	}

	//xóa loại bỏ 
	public void disposeResoure() {

		VnptOtaUtils.LogDebug("DownloadManager", "KHAICUCAI: disposeResoure!");

		try {
			//nctmanh: 02112016 - for new ota flow
			File dir = new File(VnptOtaUtils.FIRMWARE_FILE_LOCATION); 
			if (dir.isDirectory()) {
				File[] children = dir.listFiles();	//nctmanh: 02112016 - for new ota flow
				for (int i = 0; i < children.length; i++) {
					//new File(dir, children[i]).delete();
					children[i].delete();
				}
			}
			//@}
			// if (dlTask.isAlive()) {
				dlTask.cancel(true);
			// }
		} catch (Exception e) {}
	}

	public interface DownloaderListener {
		public void downloadProgess(long downloadingSize, long progress);
		public void downloadSuccess(File tempFile);
		public void downloadError(int errorCode);
		public void downloadSuccessFileBasic(File tempFileBasic);
	}
}









