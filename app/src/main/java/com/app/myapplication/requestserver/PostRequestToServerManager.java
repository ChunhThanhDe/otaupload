package com.app.myapplication.requestserver;

import com.vnptt.ota.common.VnptOtaUtils;

import android.content.Context;
import android.os.AsyncTask;

public class PostRequestToServerManager {

	private static final String LOG_TAG = "PostRequestToServerManager";

	private Context context;
	private String url;
	private String authen;
	private String preAuthenString = null;
	private String suffAuthenString = null;
	private String jsonData;
	private PostRequestToServerListerner listener;
	private PostRequestToServerTask requestTask;
	private String authoHeader;

	public PostRequestToServerManager(Context context, PostRequestToServerListerner listerner) {
		this.context = context;
		this.listener = listerner;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void addAuthen(String authen) {
		this.authen = authen;
	}

	public void addAuthen(String authen, String preAuthenString, String suffAuthenString) {
		this.authen = authen;
		this.preAuthenString = preAuthenString;
		this.suffAuthenString = suffAuthenString;
	}

	public void addData(String jsonData) {
		this.jsonData = jsonData;
	}

	public void start() {

		if (authen != null) {

			String mUsername = authen;
			String mPassword;

			if ((preAuthenString != null) && (suffAuthenString == null)) {
				mPassword = preAuthenString + mUsername;
			} else if ((preAuthenString == null) && (suffAuthenString != null)) {
				mPassword = mUsername + suffAuthenString;
			} else if ((preAuthenString != null) && (suffAuthenString != null)) {
				mPassword = preAuthenString + mUsername + suffAuthenString;
			} else {
				mPassword = mUsername;
			}

			mPassword = PostRequestToServerUtils.getSHA1(mPassword).substring(0, 16);
			authoHeader = PostRequestToServerUtils.authHeaderGenerator(mUsername, mPassword);

			VnptOtaUtils.LogDebug(LOG_TAG, "user: " + mUsername + "; pass: " + mPassword);
		}
		
		requestTask = new PostRequestToServerTask(context, url, authoHeader, jsonData, listener);
		requestTask.execute();
	}
}
