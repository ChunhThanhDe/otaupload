package com.app.myapplication.requestserver;

import org.json.JSONObject;


public interface PostRequestToServerListerner {
	public void ResponseSuccess(JSONObject result);
	public void ResponseFailed(int errorCode);
}
