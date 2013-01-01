package edu.purdue.libcommon.provider;

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
import edu.purdue.libcommon.db.RockDB;

/*
 * The provider which manages the "rocks"
 * 
 * This should not be used directly but rather
 * accessed via the LibCommon Rock class.
 */
public class RockProvider extends ContentProvider {
	
	private RockDB mDb;
	private static final UriMatcher MATCHER;
	private static final int ROCKS = 1;
	private static final int ROCKS_ID = 2;

	/* 
	 * These are the constants which identifies the provider and its data
	 */
	public static final class Constants implements BaseColumns {
		public static final String AUTHORITY = "edu.purdue.libcommon.provider";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/rocks");
		public static final String TABLE="rocks";
		public static final String DEFAULT_SORT_ORDER="_id";
		public static final String TRELLO_ID = "trello_id";
		public static final String LAT = "lat";
		public static final String LON = "lon";
		public static final String PICKED = "picked";
		public static final String COMMENTS = "comments";
		public static final String PICTURE = "picture";
		public static final String UPDATE_TIME = "update_time";
		public static final String TRELLO_PULL_TIME = "trello_pull_time";
		public static final String DELETED = "deleted";
	}
	
	/*
	 * Builds the matcher which defines the request URI which will be answered
	 */
	static {
		MATCHER=new UriMatcher(UriMatcher.NO_MATCH);
		MATCHER.addURI("edu.purdue.libcommon.RockProvider", "rocks", ROCKS);
		MATCHER.addURI("edu.prudue.libcommon.RockProvider", "rocks/#", ROCKS_ID);
	}
	
	/*
	 * Called by Android when the content provider is first created
	 * Here we should get the resources we need.
	 * This should be fast
	 */
	@Override
	public boolean onCreate() {
		/* Get hold of the rock DB
		 * This is fast because RockDB will hold off working with the DB until it is used
		 */
		mDb=new RockDB(getContext());
		
		return ((mDb == null) ? false : true);
	}
	
	/*
	 * Used to determine the type of response which will be returned for a given request
	 */
	@Override
	public String getType(Uri url) {
		String type;
		switch(MATCHER.match(url)) {
			case ROCKS:
				type = "vnd.libcommon.cursor.dir/constant";
			break;
			
			default:
				type = "vnd.libcommon.cursor.item/constant";
			break;
		}
		
		return type;
	}
	
	/*
	 * Used to get a rock from the provider. See ContentProvider in android docs for usage
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
		
		Cursor c = qb.query(mDb.getReadableDatabase(), projection, selection, selectionArgs,
				null, null, orderBy);
	
		return c;
	}
	
	/*
	 * Used to add a rock to the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		long rowID = mDb.getWritableDatabase().insert(Constants.TABLE, Constants._ID, initialValues);
		
		// If we got a rowID, get the URI and tell the world
		if(rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Constants.CONTENT_URI, rowID);
			
			getContext().getContentResolver().notifyChange(uri, null);
			
			return uri;
		}
		
		throw new SQLException("Failed to insert row into " + url);
		
	}
	
	/*
	 * Used to update a rock in the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
		int count=mDb.getWritableDatabase().update(Constants.TABLE, values, where, whereArgs);
		
		// Tell the world of the update
		getContext().getContentResolver().notifyChange(url, null);
		
		return count;
	}
	
	/*
	 * Used to delete a rock from the provider. See ContentProvider in android docs for usage
	 */
	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		int count=mDb.getWritableDatabase().delete(Constants.TABLE, where, whereArgs);
		
		// Tell the world of the delete
		getContext().getContentResolver().notifyChange(url, null);
		
		return count;
	}
	
}