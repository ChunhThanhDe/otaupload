package com.app.myapplication.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.IPackageDataObserver;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;

import com.app.myapplication.R;
import com.app.myapplication.common.VnptOtaUtils;


//chế độ chờ
public class StandbyActivity extends Activity{

	final static String LOG_TAG = "StandbyActivity";
	ProgressDialog mProgressDialogWaiting = null;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //set cờ vào
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.force_update);

		mProgressDialogWaiting = new ProgressDialog(this/*, R.style.MyTheme*/);
		mProgressDialogWaiting.setMessage(getResources().getString(R.string.wait_for_booting_subsystem));
		mProgressDialogWaiting.setCancelable(false);
		mProgressDialogWaiting.show();
		
		/*ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		PackageManager mPackageManager = (PackageManager) getPackageManager();
		List<ActivityManager.RecentTaskInfo> recentTasks = mActivityManager.getRecentTasks(100,0);
		for(ActivityManager.RecentTaskInfo taskInfo : recentTasks){
		   Intent baseIntent = taskInfo.baseIntent;
		   ResolveInfo resolveInfo = mPackageManager.resolveActivity(baseIntent, 0);
		   try{
			   if(!resolveInfo.activityInfo.packageName.equals("com.vnptt.ota")){     //kill task except vnptOTA
				   mActivityManager.removeTask(taskInfo.persistentId, ActivityManager.REMOVE_TASK_KILL_PROCESS);
				}
		   }catch(Exception e){
				Log.d(LOG_TAG,"ERROR : " + e);
				e.printStackTrace();
		   }
		}*/
    }
 	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		VnptOtaUtils.LogDebug(LOG_TAG, "onDestroy...");
		if(mProgressDialogWaiting != null && mProgressDialogWaiting.isShowing())
			mProgressDialogWaiting.dismiss();
	}
	
}
