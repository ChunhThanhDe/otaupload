package com.app.myapplication.getfirmwareinfo;

import java.io.File;
import java.net.URL;

import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.app.myapplication.common.RecoveryUtil;
import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.requestserver.PostRequestToServerListerner;
import com.app.myapplication.requestserver.PostRequestToServerManager;

public class FirmwareInfoManager extends AsyncTask<Void, Void, FirmwareInfo> {

	private final static String LOG_TAG = "FirmwareInfoManager";

	private final static String PRE_AUTHO_STRING = "S2Lowcost";
	private final static String SUFF_AUTHO_STRING = "permission.network.mobile";

	private FirmwareInfoListerner listener = null;
	private Context context = null;
	private PostRequestToServerManager requestToServerMgr;
	FirmwareInfo rom = null;
	//nctmanh: 23092015 - them luong xu ly ma loi tra ve -@{
	public static final int OK_SUCCESSFULLY = 0;
	public static final int FIRMWARE_DOWNLOADED = 11;
	//@}
	private VnptOtaState otaState = null;
	
	public FirmwareInfoManager(Context ctx) {
		this(ctx,null);
	}

	public FirmwareInfoManager(Context ctx, FirmwareInfoListerner listener) {
		this.context = ctx;
		this.listener = listener;
		requestToServerMgr = new PostRequestToServerManager(context, mListener);
		otaState = VnptOtaState.getInstance(context);
	}

	@Override
	protected FirmwareInfo doInBackground(Void... notused) {
		if (!VnptOtaUtils.isNetworkConnected(context)) {
			listener.onError(FirmwareInfoUtils.NETWORK_NOT_CONNECT);
			return null;
		}
		try {
			startRequest();
			if (rom != null)
				return rom;
		} catch (Exception e) {
			listener.onError(FirmwareInfoUtils.UNKNOWN_ERROR);
		}
		return null;
	}

	public void startRequest() {

		VnptOtaUtils.LogDebug(LOG_TAG, "get update xml link: " + VnptOtaUtils.getUpdateXmlLink(context));

		//lấy link tải firmware trong database
		requestToServerMgr.setUrl(VnptOtaUtils.getUpdateXmlLink(context));
		//nctmanh: 05122015 - modified for vnpt ota client smb v2 -@{
		//get macUser 
		requestToServerMgr.addAuthen(VnptOtaUtils.getDeviceId(context), 
				PRE_AUTHO_STRING , SUFF_AUTHO_STRING);
		//@}
		//this.jsonData = jsonData;
		requestToServerMgr.addData(getRequestTypeString());
		requestToServerMgr.start();
	}

	// create json toString  
	private String getRequestTypeString() {
		try {
			JSONObject jsonObject = new JSONObject();
			String device = (android.os.Build.MODEL).replaceAll(" ", "");
			String version = android.os.Build.DISPLAY;
			//support fw version of smartbox v2
			if (version != null && version.lastIndexOf("_V") > 0) {
				device = version.substring(0, version.lastIndexOf("_"));
				version = version.substring(version.lastIndexOf("V") + 1);
			}
			jsonObject.put("request", "update_firmware");
			jsonObject.put("device", device);
			jsonObject.put("version", version);

			VnptOtaUtils.LogDebug(LOG_TAG, "json request: " + jsonObject.toString());

			return jsonObject.toString();

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void onPostExecute(FirmwareInfo result) {
		if (listener != null) {
			if (result != null) {

				VnptOtaUtils.LogDebug(LOG_TAG, "firmware info: " + "\n" +
						"firmware name: " + result.getFirmwareName() + "\n" +
						"version: " + result.getFirmwareVersion() + "\n" +
						"md5: " + result.getFirmwareMd5() + "\n" +
						"size: " + result.getFirmwareSize() + "\n" +
						"release date: " + result.getFirmwareDate() + "\n" +
						"url: " + result.getFirmwareUrl() + "\n" +
						"release note: " + result.getFirmwareDescription() + "\n"
						);

				if (FirmwareInfoUtils.isFirmwareAvailable(result.getFirmwareVersion())) {
					listener.haveFirmwareUpdate(result);
				} else {
					listener.onError(FirmwareInfoUtils.NO_FIRMWARE_UPDATE);
				}
			} 
		}
	}

	private PostRequestToServerListerner mListener = new PostRequestToServerListerner() {

		@Override
		public void ResponseFailed(int errorCode) {
			//truong hop khong ket noi duoc toi server thi mac dinh coi nhu may da update
			listener.onError(FirmwareInfoUtils.NO_FIRMWARE_UPDATE);	//nctmanh: 23092015 - them luong xu ly ma loi tra ve
		}

		@Override
		public void ResponseSuccess(JSONObject jsonResponse) {
			//get code reponse, version, release date and device name
			String version;
			String releaseDate;
			String device;
			String descRom;
			try {
				int codeResponse = jsonResponse.getInt("code");
				if (codeResponse == OK_SUCCESSFULLY) {	//nctmanh: 23092015 - them luong xu ly ma loi tra ve
					if(jsonResponse.has("device"))
						device = jsonResponse.getString("device");	
					else
						device = jsonResponse.getString("devie");

					String deviceCorrect = (android.os.Build.MODEL).replaceAll(" ", "");
					String buildDisplay = android.os.Build.DISPLAY;

					//support fw version of smartbox v2
					if (buildDisplay != null && buildDisplay.lastIndexOf("_V") > 0) {
						deviceCorrect = buildDisplay.substring(0, buildDisplay.lastIndexOf("_"));
					}
					if(!device.equals(deviceCorrect)){
						listener.onError(FirmwareInfoUtils.DEVICE_NOT_MATCH);
						return;
					}

					version = jsonResponse.getString("version");
					releaseDate = jsonResponse.getString("releaseDate");
					releaseDate = releaseDate.replace("\\/", "/");
					descRom = jsonResponse.getString("releaseNote");
				}

				else if (codeResponse == FIRMWARE_DOWNLOADED) {	//nctmanh: 23092015 - them luong xu ly ma loi tra ve
					listener.onError(FirmwareInfoUtils.NO_FIRMWARE_UPDATE);
					return;
				}
				else {	//nctmanh: 23092015 - them luong xu ly ma loi tra ve
					listener.onError(FirmwareInfoUtils.NO_FIRMWARE_UPDATE);	//De tranh thong bao "unknow" khi khong ket noi duoc toi server
					return;
				}
		
			} catch (Exception e) {
				VnptOtaUtils.LogError(LOG_TAG, "json exception: " + e);
				listener.onError(FirmwareInfoUtils.ERROR_PARSER_XML);		//nctmanh: 23092015 - them luong xu ly ma loi tra ve
				return;
			}
			
			//get FW basic information {
			String strFwBasicMd5=null;
			int fwBasicSize = 0;
			String strFwBasic_url=null;
			String firmwareFileName=null;

			try{
					strFwBasicMd5 = jsonResponse.getString("md5");
					fwBasicSize = jsonResponse.getInt("size");
					strFwBasic_url = jsonResponse.getString("url");
					strFwBasic_url = strFwBasic_url.replace("\\/", "/");

					URL mURL = new URL(strFwBasic_url);
					firmwareFileName = new File(mURL.getFile()).getName();

			} catch (Exception e) {

				VnptOtaUtils.LogError(LOG_TAG, "Can not FW Basic: " + e);

				strFwBasic_url = null;
			}

			//not need FW Basic
			// if fwBasicSize < 100 bytes -> fake FW
			if(fwBasicSize < 100 || TextUtils.isEmpty(strFwBasicMd5))
				strFwBasic_url = null;
			
			//nctmanh: 02112016 - for new ota flow - FW delta @{
			String strFwDelta_url=null;
			String strFwDeltaMd5=null;
			int fwDeltaSize=0;

			String fwDeltaFileName = null;
			try {
					if(jsonResponse.has("delta_url"))
						strFwDelta_url = jsonResponse.getString("delta_url");
					else
						strFwDelta_url = jsonResponse.getString("basic_url");

					strFwDelta_url = strFwDelta_url.replace("\\/", "/");

					URL mFwDeltaURL = new URL(strFwDelta_url);
					fwDeltaFileName = new File(mFwDeltaURL.getFile()).getName();

					if(jsonResponse.has("delta_md5"))
						strFwDeltaMd5 = jsonResponse.getString("delta_md5");
					else
						strFwDeltaMd5 = jsonResponse.getString("basic_md5");

					if(jsonResponse.has("delta_fw_size"))
						fwDeltaSize = jsonResponse.getInt("delta_fw_size");
					else
						fwDeltaSize = jsonResponse.getInt("basic_fw_size");

			} catch (Exception e) {

				VnptOtaUtils.LogError(LOG_TAG, "Can not get Delta FW: " + e);

				strFwDelta_url = null;
			}
			//Not need delta FW
			// if fwDeltaSize < 100 bytes -> fake FW
			if(fwDeltaSize < 100 || TextUtils.isEmpty(strFwDeltaMd5))
				strFwDelta_url = null;
			
			try {
				//{ checking existed basic FW
				if (!TextUtils.isEmpty(firmwareFileName)){
					
					File existedFWBasic = new File(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION, firmwareFileName);
					if(!existedFWBasic.exists() || (existedFWBasic.length() > fwBasicSize) || TextUtils.isEmpty(strFwBasic_url) ||
						((existedFWBasic.length() == fwBasicSize) &&
							(!VnptOtaUtils.getMd5sum(existedFWBasic).equals(strFwBasicMd5) || !RecoveryUtil.verifyPackage(existedFWBasic))
						)
					){
						VnptOtaUtils.LogError(LOG_TAG,"Old Basic file is not valid or not existed. Removing...");
						File dir = new File(VnptOtaUtils.FIRMWARE_BASIC_FILE_LOCATION); 
						if (dir.isDirectory()) {
							File[] children = dir.listFiles();
							for (int i = 0; i < children.length; i++) {
								children[i].delete();
							}
						}
					}
				}
				if(TextUtils.isEmpty(strFwBasic_url) && TextUtils.isEmpty(strFwDelta_url)){
					VnptOtaUtils.LogError(LOG_TAG, "Can not found FW Basic and Delta");
					listener.onError(FirmwareInfoUtils.ERROR_PARSER_XML);
					rom = null;
				} else {
					rom = new FirmwareInfo(firmwareFileName, version,
									strFwDelta_url, null, strFwDeltaMd5, fwDeltaSize, //delta
									VnptOtaUtils.convetStringToDate2(releaseDate),descRom, 
									strFwBasic_url, strFwBasicMd5, fwBasicSize //FW basic
									);
					
					VnptOtaUtils.LogDebug(LOG_TAG, "resoponse: " + "\n" +
							"device: " + device + "\n" +
							"version: " + version + "\n" +
							"md5: " + strFwBasicMd5 + "\n" +
							"size: " + fwBasicSize + "\n" +
							"release date: " + releaseDate + "\n" +
							"url: " + strFwBasic_url + "\n" +
							"release note: " + descRom + "\n" +
							"delta_url: " + strFwDelta_url + "\n" +
							"fw delta file name: " + fwDeltaFileName + "\n" +
							"fw delta size: " + fwDeltaSize + "\n"
							);
				}
			} catch (Exception e) {
				VnptOtaUtils.LogError(LOG_TAG, "Create New FW Info Exception: " + e);
				listener.onError(FirmwareInfoUtils.ERROR_PARSER_XML);		//nctmanh: 23092015 - them luong xu ly ma loi tra ve
			}
		}

	};

	public interface FirmwareInfoListerner {
		void onError(int err);
		void haveFirmwareUpdate(FirmwareInfo info);
	}

}
