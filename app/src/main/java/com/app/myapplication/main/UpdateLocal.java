package com.app.myapplication.main;

import java.lang.reflect.Method;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Build;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.app.myapplication.R;
import com.app.myapplication.common.RecoveryUtil;

public class UpdateLocal extends Activity{

	protected static final String TAG = "UpdateLocal";

	private SimpleAdapter mAdapter;
	private ListView mListLocal;
	List<Map<String, Object>> list;
	Map<String, Object> map;
	final String recovery_path = Environment.getExternalStorageDirectory().getPath();
	final File usb_path = new File("/storage");
	private List<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
	private static String disk_path = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.local_main);

		mListLocal = (ListView) findViewById(R.id.listLocal);
		list  = new ArrayList<Map<String, Object>>();
		initData();

		mAdapter = newListAdapter();
        mListLocal.setAdapter(mAdapter);
        mListLocal.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Map<String, Object> item = (Map<String, Object>)arg0.getItemAtPosition(arg2);
				Log.d(TAG,"item name = "+item.get("item_name"));
				Log.d(TAG,"item path = "+item.get("item_path"));
				Log.d(TAG,"Last id = "+mListLocal.getCount()+" postion = "+arg2);
				if(arg2 == (mListLocal.getCount() - 1)){
					finish();
				}else{
					String recoveryPath = (String) item.get("item_path");
					Log.d(TAG,"recovery_path = "+recoveryPath); 
					installFileDialog(UpdateLocal.this, recoveryPath);
				}
			}
        });
        getListDataUsb(usb_path);
        if(list.size() > 0){
        	mData.clear();
        	mData.addAll(list);
        	mData.addAll(getListDataBack(UpdateLocal.this, "Back"));
        }
	}

    public static Object getDiskInfo(String filePath, Context ctx){
        StorageManager mStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
        Class<?> volumeInfoC = null;
        Class<?> deskInfoC = null;
        Method getvolume = null;
        Method getDisk = null;
        Method isMount = null;
        Method getPath = null;
        Method getType = null;
        List<?> mVolumes = null;
        try {
            volumeInfoC = Class.forName("android.os.storage.VolumeInfo");
            deskInfoC = Class.forName("android.os.storage.DiskInfo");
            getvolume = StorageManager.class.getMethod("getVolumes");
            mVolumes = (List<?>)getvolume.invoke(mStorageManager);//mStorageManager.getVolumes();
            isMount = volumeInfoC.getMethod("isMountedReadable");
            getDisk = volumeInfoC.getMethod("getDisk");
            getPath = volumeInfoC.getMethod("getPath");
            getType = volumeInfoC.getMethod("getType");
            for (Object vol : mVolumes) {
                if (vol != null && (boolean)isMount.invoke(vol) && ((int)getType.invoke(vol) == 0)) {
                    Object info = getDisk.invoke(vol);
                    disk_path = ((File)getPath.invoke(vol)).getAbsolutePath();
                    Log.d(TAG, "getDiskInfo: " + disk_path);
                    if ( info != null && filePath.contains(disk_path) ) {
                        Log.d(TAG, "getDiskInfo path.getName():" + disk_path);
                        return info;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
	/* create command for recovery mode
	 * format: 
	 *   --update_package=path - verify install an OTA package file
	 *   --wipe_data - erase user data (and cache) base on keeplist file, then reboot
	 * 	 --wipe_all_data - erase user data (and cache), then reboot
	 *   --wipe_cache - wipe cache (but not user data), then reboot
	 */
    private static String createVnptScript(boolean wipe_data, boolean wipe_cache, boolean wipe_all_data, String full_path, Context ctx) {
        
        Log.d(TAG, "create recovery command from path=" + full_path);

        String res = "";
        String short_path = "";
        if (full_path.lastIndexOf("/") < 0) {
            Toast.makeText(ctx, ctx.getString(R.string.file_not_exist), 2000)
                .show();
            return res;
        }

        Class<?> deskInfoClass = null;
        Method isSd = null;
        Method isUsb = null;
        Object info = getDiskInfo(full_path, ctx);

        if (info == null) {
            return "";
        } else {
            try {
                deskInfoClass = Class.forName("android.os.storage.DiskInfo");
                isSd = deskInfoClass.getMethod("isSd");
                isUsb = deskInfoClass.getMethod("isUsb");
                String mount_point = "";
                if ((boolean)isSd.invoke(info)) {
                    mount_point += "/sdcard";
                } else if ((boolean)isUsb.invoke(info)) {
                    mount_point += "/udisk";
                } else {
                    mount_point += "/cache";
                }

                short_path = full_path.substring(disk_path.length());
                res += "--update_package=" + mount_point + short_path;
                res += "\n--locale=" + Locale.getDefault().toString();
                if (wipe_data) res += "\n--wipe_data";
                if (wipe_cache) res += "\n--wipe_cache";

                Log.d(TAG, "recovery command is: " + res);

                return res;
            } catch (Exception ex) {
                ex.printStackTrace();
                return "";
            }
        }
    }
	
	protected static void installFileDialog(final Context ctx, final String file) {
		Resources r = ctx.getResources();
		String[] installOpts = r.getStringArray(R.array.install_options);
		
		final boolean[] selectedOpts = new boolean[installOpts.length];
		selectedOpts[0] = true;
		
		Log.d(TAG,"Installing " + file);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
		alert.setCancelable(false);
		alert.setTitle(R.string.alert_install_title);
		 
		//hiện thị một danh sách lựa chọn 
		alert.setMultiChoiceItems(installOpts, selectedOpts, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				selectedOpts[which] = isChecked;
			}
		});
		alert.setPositiveButton(R.string.alert_install, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
				alert.setTitle(R.string.alert_install_title);
				alert.setMessage(R.string.alert_install_message);
				alert.setPositiveButton(R.string.alert_install, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							Log.d(TAG,"Recovery setting");
							Log.d(TAG, "OTA file name " + file);
							boolean wipe_data = false;
							boolean wipe_cache = false;
							boolean wipe_all_data = false;
							if (selectedOpts[1]) {
								wipe_data = true;
							}
							if (selectedOpts[0]) {
								wipe_cache = true;
							}
							String recovery_cmd = createVnptScript(wipe_data, wipe_cache, wipe_all_data, file, ctx);
							if(recovery_cmd != "") {
								RecoveryUtil.addCommandFile(recovery_cmd);

								Log.d(TAG,"reboot to recovery mode");

								PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
								// set reboot_mode: recovery => factory_reset; update, normal_boot, charging
								pm.reboot("recovery");
							} else 
								Log.d(TAG, "recovery command is invalid...");
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				alert.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alert.create().show();
			}
		});
		alert.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		Log.d(TAG,"Creating alert dialog");

		alert.create().show();

	}
	
	private void getListDataUsb(File directory) {
    	final File[] files = directory.listFiles();
		String path;
		String path_ext;

    	if ( files != null ) {
        	for ( File file : files ) {
        	    if ( file != null ) {
					path = file.getAbsolutePath();
             	   if ( file.isDirectory() ) {  // it is a folder...
             		  getListDataUsb(file);
             	   }
             	   else {  // it is a file...
						path_ext = path.substring(path.lastIndexOf(".") + 1);
						if(path_ext.equals("zip") || path_ext.equals("ZIP")
						){
							map = new HashMap<String, Object>();
	    			    	map.put("item_name", file.getName());
	    			    	map.put("item_path", file.getAbsolutePath());
	    			    	map.put("item_recovery_path", recovery_path + "/" + path.substring(path.indexOf("/",13) + 1));
	    			    	
	    	        		String date = new SimpleDateFormat("yyyy/MM/dd HH:mm")
	    	        			.format(new Date(file.lastModified()));    			    	
	    			    	map.put("item_date", date);
	    			    	  	
	    			    	list.add(map);
						}						
             	   }
            	}
        	}
    	}
	}
	
	private void initData(){
		mData.clear();
		mData.addAll(getListDataBack(UpdateLocal.this, "Back"));
	}

	public List<Map<String, Object>> getListDataBack(Context context, String backStr) {
		
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	Map<String, Object> map;
    	
    	map = new HashMap<String, Object>();
    	map.put("item_name", backStr);    			    	
    	map.put("item_path", "");     			    	
    	map.put("item_date", "");    
    	
    	list.add(map);      	
    	return list;
	}
	
	private SimpleAdapter newListAdapter() {
		return new SimpleAdapter(UpdateLocal.this, 
				mData, 
				R.layout.list_item, 
				new String[]{
					"item_name",
					"item_path",
					"item_date",
				}, 
				new int[]{
					R.id.item_name,
					R.id.item_path,
					R.id.item_date,
				}); 
	} 
}
