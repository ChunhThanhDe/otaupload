package com.app.myapplication.settings;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class OtaSettingsProvider extends ContentProvider{

	private static final String PROVIDER_NAME = "com.vnptt.ota.settings.otasettingsprovider";
	private static final String URL = "content://" + PROVIDER_NAME + "/otasettings";
	private static final Uri CONTENT_URI = Uri.parse(URL);

	private static final int OTASETTINGS = 1;
	private static final int OTASETTINGS_ID = 2;

	private static final UriMatcher uriMatcher;
	private static HashMap<String, String> SIM_STATUS_PROJECTION_MAP;

	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "otasettings", OTASETTINGS);
		uriMatcher.addURI(PROVIDER_NAME, "otasettings/#", OTASETTINGS_ID);
	}

	public static abstract class SimStatusEntry implements BaseColumns {
		public static final String TABLE_NAME = "otasettings";
		public static final String COLUMN_NAME = "name";
		public static final String COLUMN_VALUE = "value";
	}

	/**
	 * Database specific constant declarations
	 */
	private SQLiteDatabase db;
	static final int DATABASE_VERSION = 1;
	static final String CREATE_DB_TABLE = 
			" CREATE TABLE " + SimStatusEntry.TABLE_NAME + " (" +
					SimStatusEntry._ID + " INTEGER PRIMARY KEY," + 
					SimStatusEntry.COLUMN_NAME + " TEXT," +
					SimStatusEntry.COLUMN_VALUE + " TEXT);";

	/**
	 * Helper class that actually creates and manages 
	 * the provider's underlying data repository.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context){
			super(context, SimStatusEntry.TABLE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(CREATE_DB_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " +  SimStatusEntry.TABLE_NAME);
			onCreate(db);
		}
	}


	@Override
	public boolean onCreate() {
		Context context = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		/**
		 * Create a write able database which will trigger its 
		 * creation if it doesn't already exist.
		 */
		db = dbHelper.getWritableDatabase();
		return (db == null)? false:true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(SimStatusEntry.TABLE_NAME);

		switch (uriMatcher.match(uri)) {
		case OTASETTINGS:
			qb.setProjectionMap(SIM_STATUS_PROJECTION_MAP);
			break;
		case OTASETTINGS_ID:
			qb.appendWhere(SimStatusEntry._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		if (sortOrder == null || sortOrder == ""){
	
			sortOrder = SimStatusEntry._ID;
		}
		Cursor c = qb.query(db,	projection,	selection, selectionArgs, 
				null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)){
		/**
		 * Get all student records 
		 */
		case OTASETTINGS:
			return "";
			/** 
			 * Get a particular student
			 */
		case OTASETTINGS_ID:
			return "";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		/**
		 * Add a new student record
		 */
		long rowID = db.insert(	SimStatusEntry.TABLE_NAME, "", values);
		/** 
		 * If record is added successfully
		 */
		if (rowID > 0)
		{
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to add a record into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)){
		case OTASETTINGS:
			count = db.delete(SimStatusEntry.TABLE_NAME, selection, selectionArgs);
			break;
		case OTASETTINGS_ID:
			String id = uri.getPathSegments().get(1);
			count = db.delete(SimStatusEntry.TABLE_NAME, SimStatusEntry._ID +  " = " + id + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
							selection + ')' : ""), selectionArgs);
			break;
		default: 
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)){
		case OTASETTINGS:
			count = db.update(SimStatusEntry.TABLE_NAME, values, 
					selection, selectionArgs);
			break;
		case OTASETTINGS_ID:
			count = db.update(SimStatusEntry.TABLE_NAME, values, SimStatusEntry._ID + 
					" = " + uri.getPathSegments().get(1) + 
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""), selectionArgs);
			break;
		default: 
			throw new IllegalArgumentException("Unknown URI " + uri );
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
