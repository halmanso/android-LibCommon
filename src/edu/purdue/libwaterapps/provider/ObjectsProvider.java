package edu.purdue.libwaterapps.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import edu.purdue.libwaterapps.db.ObjectsDB;

/*
 * The provider which manages the "field"
 * 
 * This should not be used directly but rather
 * accessed via the LibWaterApps Object class.
 */
public class ObjectsProvider extends ContentProvider {
	
	private ObjectsDB db;
	private static final UriMatcher MATCHER;
	private static final int OBJECTS = 1;
	private static final int OBJECT_ID = 2;

	/* 
	 * These are the constants which identifies the provider and its data
	 */
	public static final class Constants implements BaseColumns {
		public static final String AUTHORITY = "edu.purdue.libwaterapps.objectsProvider";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/objects");
		public static final String TABLE="objects";
		public static final String DEFAULT_SORT_ORDER="_id";
		public static final String NOTES_ID = "notes_id";
		public static final String GROUP = "groupnum";
		public static final String TYPE = "type";
		public static final String LAT = "lat";
		public static final String LON = "lon";
		public static final String DELETED = "deleted";
	}
	
	/*
	 * Builds the matcher which defines the request URI which will be answered
	 */
	static {
		MATCHER=new UriMatcher(UriMatcher.NO_MATCH);
		MATCHER.addURI("edu.purdue.libwaterapps.objectsProvider", "objects", OBJECTS);
		MATCHER.addURI("edu.prudue.libwaterapps.objectsProvider", "objects/#", OBJECT_ID);
	}
	
	/*
	 * Called by Android when the content provider is first created
	 * Here we should get the resources we need.
	 * This should be fast
	 */
	@Override
	public boolean onCreate() {
		/* Get hold of the field DB
		 * This is fast because FieldDB will hold off working with the DB until it is used
		 */
		db=new ObjectsDB(getContext());
		
		return ((db == null) ? false : true);
	}
	
	/*
	 * Used to determine the type of response which will be returned for a given request
	 */
	@Override
	public String getType(Uri url) {
		String type;
		switch(MATCHER.match(url)) {
			case OBJECTS:
				type = "vnd.libwaterapps.cursor.dir/constant";
			break;
			
			default:
				type = "vnd.libwaterapps.cursor.item/constant";
			break;
		}
		
		return type;
	}
	
	/*
	 * Used to get a field from the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public Cursor query(Uri url, String[] projection, String selection,
						String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		qb.setTables(Constants.TABLE);
		
		String orderBy;
		
		if(TextUtils.isEmpty(sort)) {
			orderBy = Constants.DEFAULT_SORT_ORDER;
		} else {
			orderBy=sort;
		}
		
		Cursor c = qb.query(db.getReadableDatabase(), projection, selection, selectionArgs,
				null, null, orderBy);
	
		return c;
	}
	
	/*
	 * Used to add a field to the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		long rowID = db.getWritableDatabase().insert(Constants.TABLE, Constants._ID, initialValues);
		
		// If we got a rowID, get the URI and tell the world
		if(rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Constants.CONTENT_URI, rowID);
			
			getContext().getContentResolver().notifyChange(uri, null);
			
			return uri;
		}
		
		throw new SQLException("Failed to insert row into " + url);
		
	}
	
	/*
	 * Used to update a filed in the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
		int count=db.getWritableDatabase().update(Constants.TABLE, values, where, whereArgs);
		
		// Tell the world of the update
		getContext().getContentResolver().notifyChange(url, null);
		
		return count;
	}
	
	/*
	 * Used to delete a field from the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		int count=db.getWritableDatabase().delete(Constants.TABLE, where, whereArgs);
		
		// Tell the world of the delete
		getContext().getContentResolver().notifyChange(url, null);
		
		return count;
	}
	
}