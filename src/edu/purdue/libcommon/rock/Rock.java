package edu.purdue.libcommon.rock;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.purdue.libcommon.db.RockDB;
import edu.purdue.libcommon.provider.RockProvider;

/* A class which knows everything about a given rock */
public class Rock {
	private int mId = -1;
	private String mTrelloId;
	private int mLat;
	private int mLon;
	private boolean mPicked;
	private String mComments;
	private String mPicture;
	private Date mUpdateDate; 
	private Date mTrelloPullDate;
	private boolean mDeleted;
	private Context mContext;
	public static final String[] rockProjection = {
		RockProvider.Constants._ID,
		RockProvider.Constants.TRELLO_ID,
		RockProvider.Constants.LAT,
		RockProvider.Constants.LON,
		RockProvider.Constants.PICKED,
		RockProvider.Constants.COMMENTS,
		RockProvider.Constants.PICTURE,
		RockProvider.Constants.UPDATE_TIME,
		RockProvider.Constants.TRELLO_PULL_TIME,
		RockProvider.Constants.DELETED
	};
	
	public static final String ACTION_ADDED = "edu.purdue.libcommon.rock.ADDED";
	public static final String ACTION_UPDATED = "edu.purdue.libcommon.rock.UPDATED";
	public static final String ACTION_DELETED = "edu.purdue.libcommon.rock.DELETED";
	
	public static final String IMAGE_PATH = Environment.getExternalStorageDirectory() + "/edu.purdue.rockapp/images";
	public static final String IMAGE_FILENAME_PATTERN = "rock_%d.png";
	
	/*
	 * A way to make a dummy rock. A context needs to be set if going to interact with the content provider
	 */
	public Rock() {
	}
	
	/*
	 * Make a model for a branch new rock
	 */
	public Rock(Context context) {
		this.mUpdateDate = new Date(0);
		this.mUpdateDate = new Date(0);
		this.mContext = context;
	}

	/*
	 * Create model for an existing rock
	 */
	public Rock(Context context, GeoPoint point, boolean picked) {
		this.mLat = point.getLatitudeE6();
		this.mLon = point.getLongitudeE6();
		this.mPicked = picked;
		this.mDeleted = false;
		this.mContext = context;
	}
	
	/*
	 *  Returns a single rock located by its id
	 */
	public static Rock getRock(Context context, int id) {
		Rock rock = null;
		
		// Build query for a rock ID, do not allow one to get a deleted rock
		String where = RockProvider.Constants._ID + " = ? " +
					"AND NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { Integer.toString(id) };
		
		// Query the DB
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where,
														   whereArgs, "");

		// If we got a result then translate it to a rock object
		if(cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			rock = Rock.translateCursorToRock(context, cursor);
		}
		
		// Clean up the cursor 
		cursor.close();
				
		// Return the rock or null
		return rock;
	}
	
	/*
	 *  Returns all rocks in the database
	 */
	public static ArrayList<Rock> getRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		
		// Build the query to get all rocks not deleted
		String where = "NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { };
		
		// Query the DB
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where, whereArgs, "");
		
		// Move the cursor to the first element (just in case)
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			// Translate DB returns to rocks
			rocks.add(Rock.translateCursorToRock(context, cursor));
			
			cursor.moveToNext();
		}
		
		// Clean up the query
		cursor.close();
		
		// Return what we found
		return rocks;
	}
	
	// Returns all of the "picked" rocks in the database
	public static ArrayList<Rock> getPickedRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		
		// Build the query to get all not deleted picked rocks
		String where = RockProvider.Constants.PICKED + " = ? AND NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { "true" };
		
		// Query the DB
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI, 
														   Rock.rockProjection, where, whereArgs, "");
		
		// Move the cursor to the first element (just in case)
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			// Translate DB returns to rocks
			rocks.add(Rock.translateCursorToRock(context, cursor));
			cursor.moveToNext();
		}
		
		// Clean up query
		cursor.close();
		
		// Return what was found
		return rocks;
	}
	
	// Returns all of the "not-picked" rocks in the database
	public static ArrayList<Rock> getNotPickedRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		
		// Build the query to get all not deleted not-picked rocks
		String where = RockProvider.Constants.PICKED + " = ? AND NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { "false" };
		
		// Query the DB
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI, 
														   Rock.rockProjection, where, whereArgs, "");
	
		// Move the cursor to the first element (just in case)
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			// Translate DB returns to rocks
			rocks.add(Rock.translateCursorToRock(context, cursor));
			cursor.moveToNext();
		}
		
		// CLean up query
		cursor.close();
		
		// Return what was found
		return rocks;
	}
	
	/* 
	 * Update (or create) a rock in the DB.
	 * Default is to notify the application of the change
	 */
	public void save() {
		save(true);
	}
	
	/*
	 * Update (or create) a rock in the DB.
	 * Allows caller to determine if the application show be notified of the change
	 */
	public void save(boolean notifyApplication) {
		Intent intent = new Intent();
		
		Log.d("Rock", "Save Occured, notify = " + notifyApplication);
		
		/* Set the update time to the current time */
		this.setUpdateDate(new Date());
		
		ContentValues vals = new ContentValues();
		vals.put(RockProvider.Constants.TRELLO_ID, "");
		vals.put(RockProvider.Constants.LAT, this.getLat());
		vals.put(RockProvider.Constants.LON, this.getLon());
		vals.put(RockProvider.Constants.PICKED, Boolean.toString(this.isPicked()));
		vals.put(RockProvider.Constants.COMMENTS, this.getComments());
		vals.put(RockProvider.Constants.PICTURE, this.getPicture());
		vals.put(RockProvider.Constants.UPDATE_TIME, RockDB.dateFormat(this.getUpdateDate()));
		vals.put(RockProvider.Constants.TRELLO_PULL_TIME, "");
		vals.put(RockProvider.Constants.DELETED, this.getDeleted());
		
		// Determine if we should "insert" or "update"
		if(this.mId < 0) {
			Uri uri = this.mContext.getContentResolver().insert(
					RockProvider.Constants.CONTENT_URI,
					vals);
			
			this.setId((int)ContentUris.parseId(uri));
			
			intent.setAction(Rock.ACTION_ADDED);
			
		} else {
			String where = RockProvider.Constants._ID + "=?";
			String[] whereArgs = {Integer.toString(this.mId)};
			
			this.mContext.getContentResolver().update(
					RockProvider.Constants.CONTENT_URI,
					vals, where, whereArgs);
			
			intent.setAction(Rock.ACTION_UPDATED);
		}
		
		// Send a broadcast that a rock was added/updated
		if(notifyApplication) {
			intent.putExtra("id", getId());
			LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
		}
	}
	
	/* 
	 * Mark a rock deleted in the DB.
	 * Default is to notify the application of the change
	 */
	public void delete() {
		delete(true);
	}
	
	/*
	 * Mark a rock deleted in the DB.
	 * Allows caller to determine if the application show be notified of the change
	 */
	public void delete(boolean notifyApplication) {
		// The rock has not yet been saved so there is nothing to delete
		if(getId() < 0) {
			return;
		}
		
		// Mark deleted
		setDeleted(true);
		
		// We will send our own notify
		save(false);
		
		if(notifyApplication) {
			Intent intent = new Intent(Rock.ACTION_DELETED);
			intent.putExtra("id", getId());
			LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
		}
	}
	
	/*
	 * Internal method which can translate the result of the DB request (a Cursor object)
	 * into our custom Rock object for consumption in the rest of the application
	 */
	private static Rock translateCursorToRock(Context context, Cursor cursor) {
		Rock rock = new Rock(context);
		
		rock.setId(Integer.parseInt(cursor.getString(0)));
		rock.setTrelloId(cursor.getString(1));
		rock.setLat(cursor.getInt(2));
		rock.setLon(cursor.getInt(3));
		rock.setPicked(Boolean.parseBoolean(cursor.getString(4)));
		rock.setComments(cursor.getString(5));
		rock.setPicture(cursor.getString(6));
		rock.setUpdateDate(RockDB.dateParse(cursor.getString(7)));
		rock.setTrelloPullDate(RockDB.dateParse(cursor.getString(8)));
		rock.setDeleted(Boolean.parseBoolean(cursor.getString(9)));
		
		return rock;
	}
	
	public int getId() {
		return this.mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public String getTrelloId() {
		return this.mTrelloId;
	}

	public void setTrelloId(String trelloId) {
		this.mTrelloId = trelloId;
	}

	public int getLat() {
		return this.mLat;
	}

	public void setLat(int lat) {
		this.mLat = lat;
	}
	
	public int getLon() {
		return this.mLon;
	}

	public void setLon(int lon) {
		this.mLon = lon;
	}
	
	public boolean isPicked() {
		return this.mPicked;
	}

	public void setPicked(boolean picked) {
		this.mPicked = picked;
	}

	public String getComments() {
		return this.mComments;
	}

	public void setComments(String comments) {
		this.mComments = comments;
	}

	public String getPicture() {
		return this.mPicture;
	}

	public void setPicture(String picture) {
		this.mPicture = picture;
	}
	
	public void deletePicture() {
		deletePicture(true);
	}
	
	public void deletePicture(boolean notifyApplication) {
		if(mPicture != null) {
			File pic = new File(mPicture);
			pic.delete();
			
			mPicture = null;
			save(notifyApplication);
		}
	}

	public Date getUpdateDate() {
		return this.mUpdateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.mUpdateDate = updateDate;
	}

	public Date getTrelloPullDate() {
		return this.mTrelloPullDate;
	}

	public void setTrelloPullDate(Date trelloPullDate) {
		this.mTrelloPullDate = trelloPullDate;
	}
	
	public Context getContext() {
		return this.mContext;
	}
	
	public boolean getDeleted() {
		return this.mDeleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.mDeleted = deleted;
	}
}