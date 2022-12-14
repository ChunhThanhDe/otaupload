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

	// s???a gi?? tr??? l??n database
	// ContentResolver cung c???p ContentProvider ph?? h???p trong s??? t???t c??? c??c ContentProvider s??? d???ng URI.
	private static boolean putValueToDb(ContentResolver resolver, String name, String value) {
		Uri simStateUri = Uri.parse(OTA_SETTINGS_PROVIDER_URI);
		String selection = COLUMN_NAME + "=?";
		// CURSOR l?? m???t t???p h???p k???t qu??? truy v???n (c??c h??ng), v???i CURSOR ta c?? th??? duy???t 
		// qua t???ng h??ng k???t qu??? ????? thi h??nh nh???ng t??c v??? ph???c t???p.
		// ??? m???t th???i ??i???m, CURSOR c?? th??? truy c???p b???i m???t con tr??? ?????n m???t h??ng c???a n??, 
		// b???n ch??? th??? d???ch chuy???n con tr??? t??? d??ng n??y sang d??ng kh??c.
		Cursor cursor = null;
		try {
			cursor = resolver.query(simStateUri, new String[]{COLUMN_ID, COLUMN_VALUE}, selection, new String[]{name}, null);
			// modToFirst di chuy???n con tr??? cursor l??n d??ng ?????u ti??n (row)
			if (cursor == null || !cursor.moveToFirst()) {
				// ContentValues L???p n??y ???????c s??? d???ng ????? l??u tr??? m???t t???p h???p c??c gi?? tr??? m?? ContentResolver c?? th??? x??? l??.
				ContentValues values = new ContentValues();
				// add value
				values.put(COLUMN_NAME, name);
				values.put(COLUMN_VALUE, value);
				// th??nh c??ng tr??? v??? true
				if (!resolver.insert(simStateUri, values).equals(null)) {
					return true;
				}
			} else {
				// getColumnIndexOrThrow tr??? v??? gi?? tr??? ho???c throws IllegalArgumentException n???u 
				// colum kh??ng t???n t???i 	
				int comlumId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
				String oldValue = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE));
				// n???u gi?? tr??? c?? kh??ng gi???ng gi?? tr??? m???i
				if (!oldValue.equalsIgnoreCase(value)) {
					// th??m id v??o cu???i ???????ng d???n 
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
				// Ph????ng th???c n??y ????ng con tr???, ?????t l???i t???t c??? k???t qu??? v?? ?????m b???o r???ng ?????i t?????ng 
				// con tr??? kh??ng c?? tham chi???u ?????n ?????i t?????ng k???t n???i ban ?????u c???a n??.
				cursor.close();
			}
		}
		return false;
	}
}
