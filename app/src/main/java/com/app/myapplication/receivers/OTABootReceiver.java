package com.app.myapplication.receivers;

import com.app.myapplication.common.VnptOtaUtils;
import com.app.myapplication.service.VnptOtaService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// BroadcastReceiver dùng để nhận biết được tất cả những sự thay đổi của hê thống 
// như lắng nghe tin nhắn đến, hành động rút - cắm sạc, hay hành động bật tắt mạng,...
public class OTABootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			VnptOtaUtils.LogError("OTABootReceiver", "Receive BOOT COMPLETE action");
			Intent startServiceIntent = new Intent(context,
					VnptOtaService.class);
			context.startService(startServiceIntent);
		}
	}
}
