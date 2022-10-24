package com.app.myapplication.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.app.myapplication.common.VnptOtaUtils;


public class VnptOtaHelperMgr {

	private static final String LOG_TAG = "VnptOtaHelperMgr";

	private VnptOtaHelperMgrCallback callback;
	private Context context;
	private ServiceManagementConnection connection;
	private IServiceManagement service;

	public VnptOtaHelperMgr(Context context, VnptOtaHelperMgrCallback callback) {
		this.context = context;
		this.callback = callback;
	}

	public boolean bindService() {
		connection = new ServiceManagementConnection();
		Intent intent = new Intent("com.vnptt.ota.service.ServiceManagement");
		intent.setPackage("com.vnptt.ota");
		return context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	public void releaseService() {
		context.unbindService(connection);
		connection = null;
	}

	public void autoQueryNewVersion() {
		try {
			service.autoQueryNewVersion(iCallback);
		} catch (RemoteException e) {
			callback.errorCode(VnptOtaHelperMgrUtils.ERROR_BIND_SERVICE_FAILD);
		}
	}

	public void userQueryNewVersion() {
		try {
			if (service != null) {
				VnptOtaUtils.LogDebug(LOG_TAG, "userQueryNewVersion()");
				service.userQueryNewVersion(iCallback);
			} else {
				callback.errorCode(VnptOtaHelperMgrUtils.ERROR_BIND_SERVICE_FAILD);
			}
		} catch (RemoteException e) {
			callback.errorCode(VnptOtaHelperMgrUtils.ERROR_BIND_SERVICE_FAILD);
		}
	}

	public void configureOtaSetting(int configure) {

	}

	IResponseQueryNewVersion.Stub iCallback = new IResponseQueryNewVersion.Stub() {
		@Override
		public void haveNewVersion(String firmwareVersion, String firmwareName,
				String firmwareDate) throws RemoteException {
			callback.haveNewVersion(firmwareVersion, firmwareName, firmwareDate);
		}
		@Override
		public void ErrorVersion(int response) throws RemoteException {
			callback.errorCode(response);
		}
	};

	class ServiceManagementConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName name, IBinder boundService) {
			service = IServiceManagement.Stub.asInterface((IBinder) boundService);
		}

		public void onServiceDisconnected(ComponentName name) {
			if (service != null)
				service = null;
		}
	}

	public interface VnptOtaHelperMgrCallback{
		public void haveNewVersion(String firmwareVersion, String firmwareName, String firmwareDate);
		public void errorCode(int response);
	}
}
