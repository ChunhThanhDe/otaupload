package com.app.myapplication.requestserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.Context;

import com.app.myapplication.common.VnptOtaUtils;

public class PostRequestToServerTask {

	private static final String LOG_TAG = "PostRequestToServerTask";
	//private Context context;
	private PostRequestToServerListerner listerner;
	private String url;
	private String autho;
	private String jsonData;
	private HttpURLConnection httpConn;
	private int responseCode;
	private int errorCode;
	private InputStream inputStream = null;

	public PostRequestToServerTask(Context context, String url, 
			String autho, String jsondata, PostRequestToServerListerner listener) {
		//this.context = context;
		this.url = url;
		this.autho = autho;
		this.jsonData = jsondata;
		this.listerner = listener;
	}

	// lệnh thi hành 
	public void execute() {
		JSONObject jObj = null;

		VnptOtaUtils.LogDebug(LOG_TAG, "execute()");

		try {
			URL myURL = new URL(url);
			httpConn = (HttpURLConnection)myURL.openConnection();

			VnptOtaUtils.LogDebug(LOG_TAG, "autho: " + autho);

			if (autho == null) {
				errorCode = PostRequestToServerUtils.ERROR_AUTHO_HEADER_NULL;
				return;
			}

			httpConn.setRequestProperty ("Authorization", autho);
			httpConn.setRequestProperty("Content-Type","application/json");
			httpConn.setRequestMethod("POST");
			httpConn.setReadTimeout(10000);
			httpConn.setConnectTimeout(10000);
			httpConn.setDoInput(true);

			VnptOtaUtils.LogDebug(LOG_TAG, "jsonData: " + jsonData);

			try {
				if (jsonData != null) {
					OutputStream os = httpConn.getOutputStream();	//gay treo request khi server khong ton tai
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
					writer.write(jsonData);
					writer.flush();
					writer.close();
					os.close();
				}
				httpConn.connect();
				responseCode = httpConn.getResponseCode();
			} catch (Exception e) {
				// TODO: handle exception

				VnptOtaUtils.LogDebug(LOG_TAG, "Exception: " + e);

				responseCode = HttpURLConnection.HTTP_UNAVAILABLE;
			}
			
			VnptOtaUtils.LogDebug(LOG_TAG, "response: " + responseCode);

			if (responseCode == HttpURLConnection.HTTP_OK) {
				inputStream = httpConn.getInputStream();
				String json;
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,  "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line = null;

					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}

					json = sb.toString();
					inputStream.close();	
				} catch (Exception e) {
					errorCode = PostRequestToServerUtils.ERROR_CONVERT_CONTENT_TO_STRING;
					return;
				}

				try {
					jObj = new JSONObject(json);
				} catch (JSONException e) {
					errorCode = PostRequestToServerUtils.ERROR_PARSING_TO_JSON;
					return;
				}
			
			} else{	//nctmanh: 23092015 - xu ly truong hop ma tra ve khac 200 OK, don gian bao unknow. truong hop loi server
				errorCode = PostRequestToServerUtils.ERROR_UNKNOW;
				VnptOtaUtils.LogDebug(LOG_TAG, "errorCode : " + errorCode + " with responseCode: " + responseCode);
			}
			//inputStream.close();
		} catch (UnsupportedEncodingException e) {
			errorCode = PostRequestToServerUtils.ERROR_UNSUPPORTED_ENCODING;
			return;
		} catch (ClientProtocolException e) {
			errorCode = PostRequestToServerUtils.ERROR_CLIENT_PROTOCOL;
			return;
		} catch (IOException e) {
			errorCode = PostRequestToServerUtils.ERROR_GET_CONTENT_HTTP;
			return;
		} catch (Exception e) {
			errorCode = PostRequestToServerUtils.ERROR_UNKNOW;
			return;
		} finally {
			if (httpConn != null) {
				httpConn.disconnect();
			}	
		}
		
		VnptOtaUtils.LogError(LOG_TAG, "errorCode : " + errorCode);

		VnptOtaUtils.LogError(LOG_TAG, "json Obj : " + String.valueOf(jObj));
		
		if (jObj != null) {			
			listerner.ResponseSuccess(jObj);
		} else {
			listerner.ResponseFailed(errorCode);
		}
	}
}






