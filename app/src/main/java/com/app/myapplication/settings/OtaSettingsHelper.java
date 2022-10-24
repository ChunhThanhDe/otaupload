package com.app.myapplication.settings;

import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class OtaSettingsHelper {
	
	public static final String OTA_SETTINGS_PROVIDER_URI = "content://com.vnptt.ota.settings.otasettingsprovider/otasettings";

	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_VALUE = "value";
	private static final String COLUMN_ID = "_id";

	public static final String OTA_SETTINGS = "ota_settings";

	public static boolean putString(ContentResolver resolver, String name, String value) {
		return putValueToDb(resolver, name, value);
	}

	public static boolean putBoolean(ContentResolver resolver, String name, boolean value) {
		String tempValue = String.valueOf(value);
		return putValueToDb(resolver, name, tempValue);
	}

	public static boolean putInt(ContentResolver resolver, String name, int value){
		String tempValue = String.valueOf(value);
		return putValueToDb(resolver, name, tempValue);
	}

	public static String getString(ContentResolver resolver, String name, String defValue) {
		return getValueToDb(resolver, name, defValue);
	}
	
	public static int getInt(ContentResolver resolver, String name, int defValue) {
		String tempValue = String.valueOf(defValue);
		try { 
			return Integer.valueOf(getValueToDb(resolver, name, tempValue));
		} catch (NumberFormatException e){
			return defValue;
		}
		
	}
	
	public static boolean getBoolean(ContentResolver resolver, String name, boolean defValue) {
		String tempValue = String.valueOf(defValue);
		try { 
			return Boolean.valueOf(getValueToDb(resolver, name, tempValue));
		} catch (NumberFormatException e){
			return defValue;
		}
	}

	public static HashMap<String, String> getAllValue(ContentResolver resolver) {
		HashMap<String, String> returnValue = new HashMap<String, String>();
		Uri simStateUri = Uri.parse(OTA_SETTINGS_PROVIDER_URI);
		Cursor cursor = null;
		try {
			cursor = resolver.query(simStateUri, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						returnValue.put(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)), 
								cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE)));
					}while (cursor.moveToNext());
				}
			} 
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return returnValue;
	}

	private static String getValueToDb(ContentResolver resolver, String name, String defValue) {
		Uri simStateUri = Uri.parse(OTA_SETTINGS_PROVIDER_URI);
		String selection = COLUMN_NAME + "=?";
		Cursor cursor = null;
		try {
			cursor = resolver.query(simStateUri, null, selection, new String[]{name}, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE));
				}
			} 
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return defValue;
	}

	// sửa giá trị lên database
	// ContentResolver cung cấp ContentProvider phù hợp trong số tất cả các ContentProvider sử dụng URI.
	private static boolean putValueToDb(ContentResolver resolver, String name, String value) {
		Uri simStateUri = Uri.parse(OTA_SETTINGS_PROVIDER_URI);
		String selection = COLUMN_NAME + "=?";
		// CURSOR là một tập hợp kết quả truy vấn (các hàng), với CURSOR ta có thể duyệt 
		// qua từng hàng kết quả để thi hành những tác vụ phức tạp.
		// Ở một thời điểm, CURSOR có thể truy cập bởi một con trỏ đến một hàng của nó, 
		// bạn chỉ thể dịch chuyển con trỏ từ dòng này sang dòng khác.
		Cursor cursor = null;
		try {
			cursor = resolver.query(simStateUri, new String[]{COLUMN_ID, COLUMN_VALUE}, selection, new String[]{name}, null);
			// modToFirst di chuyển con trở cursor lên dòng đầu tiên (row)
			if (cursor == null || !cursor.moveToFirst()) {
				// ContentValues Lớp này được sử dụng để lưu trữ một tập hợp các giá trị mà ContentResolver có thể xử lý.
				ContentValues values = new ContentValues();
				// add value
				values.put(COLUMN_NAME, name);
				values.put(COLUMN_VALUE, value);
				// thành công trả về true
				if (!resolver.insert(simStateUri, values).equals(null)) {
					return true;
				}
			} else {
				// getColumnIndexOrThrow trả về giá trọ hoặc throws IllegalArgumentException nếu 
				// colum không tồn tại 	
				int comlumId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
				String oldValue = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE));
				// nếu giá trị cũ không giống giá trị mới
				if (!oldValue.equalsIgnoreCase(value)) {
					// thêm id vào cuối đường dần 
					Uri rowUri = ContentUris.withAppendedId(simStateUri, comlumId);
					ContentValues values = new ContentValues(1);
					values.put(COLUMN_VALUE, value);
					if (resolver.update(rowUri, values, null, null) > 0) {
						return true;
					}
				}	
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				// Phương thức này đóng con trỏ, đặt lại tất cả kết quả và đảm bảo rằng đối tượng 
				// con trỏ không có tham chiếu đến đối tượng kết nối ban đầu của nó.
				cursor.close();
			}
		}
		return false;
	}
}
