package com.app.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.app.myapplication.common.VnptOtaState;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.main.VnptOtaHelperMgr;
import com.app.myapplication.main.VnptOtaHelperMgrUtils;
import com.app.myapplication.ui.VnptOtaUI;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class UpdateFwUIActivity extends Activity implements OnClickListener, Handler.Callback {
	private final static String LOG_TAG ="UpdateFwUIActivity";
	private Button btCheckFw;
	private TextView tvDevice, tvCurrentVersion, tvUpdatedDate;
	private static final String ROM_RELEASE_ID = "ro.build.display.id";
	private static final String DEVICE_NAME = "ro.product.device";
	private static final String ROM_RELEASE_DATE = "ro.build.date.utc";
	private boolean isConnectSuccess;
	private VnptOtaHelperMgr otaMgr;
	private ProgressDialog mCirDialog;
	private Handler handler = new Handler(this);
	private int mErrorCode = 0;
	private VnptOtaState otaState = null;
	private class MSG_TYPE {
		public final static int TYPE_AUTO_QUERY = 1;
		public final static int TYPE_USER_QUERY = 2;
		public final static int TYPE_CONFIGURE = 3;
		public final static int TYPE_RESPONSE_SUCCESS = 4;
		public final static int TYPE_REPONSE_ERROR = 5;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_update_fw_ui);
		
		addControl();
		otaMgr = new VnptOtaHelperMgr(getApplicationContext(), otaCallback);
		otaState = VnptOtaState.getInstance(getApplicationContext());
		isConnectSuccess= otaMgr.bindService();
		if(otaState.getState() != VnptOtaState.STATE_DOWNLOAD_INPROGRESS && otaState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD) {
			otaState.setState(VnptOtaState.STATE_SHOW_UI_MANUAL);
		}
		mErrorCode = 0;
	}
	
	private VnptOtaHelperMgr.VnptOtaHelperMgrCallback otaCallback = new VnptOtaHelperMgr.VnptOtaHelperMgrCallback() {
		@Override
		public void haveNewVersion(String firmwareVersion, String firmwareName,
				String firmwareDate) {
			if (mCirDialog.isShowing())
				mCirDialog.dismiss();
			btCheckFw.setEnabled(true);
		}
		@Override
		public void errorCode(int response) {
			Message mUser = Message.obtain(handler, MSG_TYPE.TYPE_REPONSE_ERROR, 
					MSG_TYPE.TYPE_USER_QUERY, response);
			mErrorCode = response;
			if (mCirDialog.isShowing())
				mCirDialog.dismiss();
			btCheckFw.setEnabled(true);
			mUser.sendToTarget();
		}
	};
	
	private void addControl(){
		tvDevice = (TextView) findViewById(R.id.device);
		tvCurrentVersion = (TextView) findViewById(R.id.currentversion);
		tvUpdatedDate = (TextView) findViewById(R.id.tvdateupdate);
		
		tvDevice.setText(getDeviceName());
		tvCurrentVersion.setText(getCurRomVersion());
		tvUpdatedDate.setText(getCurRomDate());
		
		btCheckFw = (Button) findViewById(R.id.btnupdatefw);
		btCheckFw.setOnClickListener(this);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// KHAICUCAI modify to download by Intend
		if(otaState.getState() != VnptOtaState.STATE_DOWNLOAD_INPROGRESS && otaState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD){
			otaState.setState(VnptOtaState.STATE_IDLE);
		}

		if (isConnectSuccess) {
			otaMgr.releaseService();
		}
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnupdatefw:
			xuLyCheckNewFW();
			break;

		default:
			break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// otaState.setState(VnptOtaState.STATE_SHOW_UI_MANUAL);
		// KHAICUCAI modify to download by Intend
		if(otaState.getState() != VnptOtaState.STATE_DOWNLOAD_INPROGRESS 
			&& otaState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD){

			otaState.setState(VnptOtaState.STATE_SHOW_UI_MANUAL);
		}
	}
	
	private void xuLyCheckNewFW() {
		if(isConnectSuccess) {
			// otaState.setState(VnptOtaState.STATE_QUERYING_MANUAL);
			// KHAICUCAI modify to download by Intend
			if (otaState.getState() == VnptOtaState.STATE_DOWNLOAD_INPROGRESS || otaState.getState() == VnptOtaState.STATE_MANUAL_DOWNLOAD) {
				if (VnptOtaUtils.isNetworkConnected(getApplicationContext())) {

					VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI enter to VnptOtaUI not query new fw if otaState is STATE_DOWNLOAD_INPROGRESS or STATE_MANUAL_DOWNLOAD");
					
					Intent mIntent = new Intent(getApplicationContext(), VnptOtaUI.class);
					mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(mIntent);	
				} else {

					VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI query new fw before enter to VnptOtaUI");

					otaState.setState(VnptOtaState.STATE_QUERYING_MANUAL);

					otaMgr.userQueryNewVersion();
					//~ btCheckFw.setEnabled(false);

					mCirDialog = new ProgressDialog(this);
					mCirDialog.setMessage(this.getResources().getString(R.string.circle_dialog_wait));
					mCirDialog.show();
				}
			} else {

				VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI query new fw before enter to VnptOtaUI");
				
				otaState.setState(VnptOtaState.STATE_QUERYING_MANUAL);

				otaMgr.userQueryNewVersion();
				//~ btCheckFw.setEnabled(false);

				mCirDialog = new ProgressDialog(this);
				mCirDialog.setMessage(this.getResources().getString(R.string.circle_dialog_wait));
				mCirDialog.show();
			}

			// if(otaState.getState() != VnptOtaState.STATE_DOWNLOAD_INPROGRESS && otaState.getState() != VnptOtaState.STATE_MANUAL_DOWNLOAD){
			// 	VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI query new fw before enter to VnptOtaUI");
			// 	otaState.setState(VnptOtaState.STATE_QUERYING_MANUAL);

			// 	otaMgr.userQueryNewVersion();
			// 	//~ btCheckFw.setEnabled(false);

			// 	mCirDialog = new ProgressDialog(this);
			// 	mCirDialog.setMessage(this.getResources().getString(R.string.circle_dialog_wait));
			// 	mCirDialog.show();
			// } else {
			// 	VnptOtaUtils.LogDebug(LOG_TAG, "KHAICUCAI enter to VnptOtaUI not query new fw if otaState is STATE_DOWNLOAD_INPROGRESS or STATE_MANUAL_DOWNLOAD");
			// 	Intent mIntent = new Intent(getApplicationContext(), VnptOtaUI.class);
			// 	mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// 	startActivity(mIntent);	
			// }
		} else {
			VnptOtaUtils.LogError(LOG_TAG, "Can not check FW.");
			mErrorCode = -1;
			alarmNoFirmware();
		}
	}
	public static String getCurRomVersion() {
		return getprop(ROM_RELEASE_ID);
	}
	
	public static String getDeviceName() {
		return getprop(DEVICE_NAME);
	}
	
	public static String getCurRomDate() {

		long val = Long.parseLong(getprop(ROM_RELEASE_DATE));
		Date date=new Date(val*1000);
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(date);
	}
	
	private static String getprop(String name) {
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
	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_TYPE.TYPE_USER_QUERY:
				if (mCirDialog.isShowing())
					mCirDialog.dismiss();
				btCheckFw.setEnabled(true);
				break;
			case MSG_TYPE.TYPE_REPONSE_ERROR:
				int type = (Integer) msg.arg1;
				int response = (Integer) msg.arg2;
				if (mCirDialog.isShowing())
					mCirDialog.dismiss();
				btCheckFw.setEnabled(true);
				if ((response == VnptOtaHelperMgrUtils.NETWORK_NOT_CONNECT) ||
						(response == VnptOtaHelperMgrUtils.NETWORK_NOT_CONNECT2)) {
					alarmNoNetwork();
				} else {
					alarmNoFirmware();
				}
				break;
			default:
				break;
		}
		return false;
	}
	private void alarmNoFirmware () {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String alarmString = getResources().getString(R.string.dialog_no_firmware) + " (" + Integer.toString(mErrorCode) + ")!";
		builder.setMessage(alarmString)
		.setTitle(R.string.dialog_warning_title)
		.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).create().show();			
	}
	private String convertErrCodeToString (Context context, int errCode) {
		try {
			String errorCodeString = context.getResources().getString(R.string.unknown_error);
			switch (errCode) {
			case VnptOtaHelperMgrUtils.FORMED_URL_EXCEPTION:

				break;
			case VnptOtaHelperMgrUtils.CREATE_FIRMARE_FOLDER_ERROR:
				errorCodeString = context.getResources().getString(R.string.download_error_create_folder);
				break;
			case VnptOtaHelperMgrUtils.NETWORK_NOT_CONNECT:
				errorCodeString = context.getResources().getString(R.string.download_error_network_not_connected);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_RANGE_NOT_SATISFIABLE:
				errorCodeString = context.getResources().getString(R.string.download_error_range_not_satisfiable);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_OPEN_CONNECTION:
				errorCodeString = context.getResources().getString(R.string.download_error_open_connection);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_CONNECT_TO_RESOURCE:
				errorCodeString = context.getResources().getString(R.string.download_error_connect_to_resource);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_GET_INPUT_STREAM:
				errorCodeString = context.getResources().getString(R.string.download_error_file_stream);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_WRITE_OUTPUT_STREAM:
				errorCodeString = context.getResources().getString(R.string.download_error_file_stream);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_IN_OUT_STREAM:
				errorCodeString = context.getResources().getString(R.string.download_error_file_stream);
				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_UNKNOWN:

				break;
			case VnptOtaHelperMgrUtils.DOWNLOAD_ERROR_USER_CANCEL:

				break;
			case VnptOtaHelperMgrUtils.TOTAL_SIZE_NOT_MATCH:
				errorCodeString = context.getResources().getString(R.string.download_error_size_not_match);
				break;
			case VnptOtaHelperMgrUtils.NETWORK_NOT_CONNECT2:
				errorCodeString = context.getResources().getString(R.string.download_error_network_not_connected);
				break;
			case VnptOtaHelperMgrUtils.ERROR_CLIENT_PROTOCOL:
				errorCodeString = context.getResources().getString(R.string.get_info_error_client_protocol);
				break;
			case VnptOtaHelperMgrUtils.ERROR_PARSER_XML:
				errorCodeString = context.getResources().getString(R.string.get_info_error_parser_xml);
				break;
			case VnptOtaHelperMgrUtils.UNKNOWN_ERROR:

				break;
			case VnptOtaHelperMgrUtils.ERROR_GET_XML:
				errorCodeString = context.getResources().getString(R.string.get_info_error_no_xml);
				break;
			case VnptOtaHelperMgrUtils.ERROR_PARSER_FIRMWARE_INFO:
				errorCodeString = context.getResources().getString(R.string.get_info_error_get_rom_info);
				break;
			case VnptOtaHelperMgrUtils.NO_FIRMWARE_UPDATE:
				errorCodeString = context.getResources().getString(R.string.get_info_error_no_firmware);
			default:

				break;
			}
			return errorCodeString;
		} catch (Exception e) {
			return "Unknow";
		}
	}
	private void alarmNoNetwork() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.dialog_no_internet)
		.setTitle(R.string.dialog_warning_title)
		.setNegativeButton(R.string.dialog_btn_settings, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if("nttv_smb".equals(SystemProperties.get("ro.product.name")) || SystemProperties.getBoolean( "ro.build.simplesetting", false)) {
					try {
						Intent mIntent = new Intent();
						mIntent.setComponent(new ComponentName("vn.vnpttech.smartboxsettings", "vn.vnpttech.smartboxsettings.MainActivity"));
						mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(mIntent);
					} catch (Exception ex){
						VnptOtaUtils.LogError(LOG_TAG, "Can not open simple settings: \n" + ex.toString());
					}
				} else {
					startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
				}
			}
		})
		.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).create().show();			
	}
}
