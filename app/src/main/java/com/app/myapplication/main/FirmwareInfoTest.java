package com.app.myapplication.main;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import com.app.myapplication.R;
import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.getfirmwareinfo.FirmwareInfo;
import com.app.myapplication.getfirmwareinfo.FirmwareInfoManager;

public class FirmwareInfoTest extends Activity implements Handler.Callback{

	private class MSG_TYPE {
		public final static int HAVE_FIRMWARE_UPDATE = 1;
		public final static int ERROR_FIRMWARE_ERROR = 2;
	}

	private TextView tvFirmwareInfo;
	private FirmwareInfoManager fmrInfo;
	Handler mHanlder = new Handler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.firmware_info);
		tvFirmwareInfo = (TextView) findViewById(R.id.tv_firmware_info);
		fmrInfo = new FirmwareInfoManager(this, listener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		fmrInfo.execute();
	}

	private FirmwareInfoManager.FirmwareInfoListerner listener = new FirmwareInfoManager.FirmwareInfoListerner(){
		@Override
		public void onError(int err) {
			Message m = Message.obtain(mHanlder, MSG_TYPE.ERROR_FIRMWARE_ERROR, Integer.valueOf(err));
			m.sendToTarget();
		}

		@Override
		public void haveFirmwareUpdate(FirmwareInfo info) {
			Message m = Message.obtain(mHanlder, MSG_TYPE.HAVE_FIRMWARE_UPDATE, info);
			m.sendToTarget();
		}
	};

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_TYPE.ERROR_FIRMWARE_ERROR:
			Integer errorCode = (Integer) msg.obj;
			Toast.makeText(getApplicationContext(), String.valueOf(errorCode), 
					Toast.LENGTH_SHORT).show();
			break;
		case MSG_TYPE.HAVE_FIRMWARE_UPDATE:
			FirmwareInfo info = (FirmwareInfo) msg.obj;
			String romInfo = "Rom Version: " + info.getFirmwareVersion() + "\n" +
					"Date Release: " + VnptOtaUtils.convetDateToString(info.getFirmwareDate()) + "\n" +
					"Rom Md5: " + info.getFirmwareMd5() + "\n" +
					"Rom size: " + info.getFirmwareSize() + "\n" +
					"url: " + info.getFirmwareUrl() + "\n" +
					"What new: " + info.getFirmwareDescription();
			tvFirmwareInfo.setText(romInfo);
			break;
		default:
			break;
		}
		return false;
	}
}




























