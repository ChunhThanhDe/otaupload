package com.app.myapplication.requestserver;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;
import android.util.Log;

public class PostRequestToServerUtils {
	private static final String TAG = "PostRequestToServerUtils";
	private static final boolean DEBUG = false;

	public final static String RESPONSE_TAG = "tag";
	public final static String RESPONSE_STATUS = "status";
	public final static String RESPONSE_ID = "id";

	public static final int ERROR_AUTHO_HEADER_NULL = 91;
	public static final int ERROR_UNSUPPORTED_ENCODING = 92; //encoding url is failed
	public static final int ERROR_CLIENT_PROTOCOL = 93;
	public static final int ERROR_GET_CONTENT_HTTP = 94;
	public static final int ERROR_UNKNOW = 95;
	public static final int ERROR_CONVERT_CONTENT_TO_STRING = 96;
	public static final int ERROR_PARSING_TO_JSON = 97; 
	public static final int ERROR_NO_CONTENT_TAG_RESPONSE = 98;
	public static final int ERROR_TAG_NOT_MATCH = 99;
	
	
	// Generate SHA1 key
	public static String getSHA1(String str) {

		MessageDigest digest = null;
		byte[] input = null;

		try {
			digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			input = digest.digest(str.getBytes("UTF-8"));

		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return convertToHex(input);
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	//Generator auth Header 
	public static String authHeaderGenerator(String username, String password) {
		try {
			return ("Basic " + Base64.encodeToString((username+":"+password).getBytes("UTF-8"),Base64.DEFAULT)).replace("\n", "");

		} catch (UnsupportedEncodingException exception) {
			return null;
		}
	}
}
