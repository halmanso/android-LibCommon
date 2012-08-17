package edu.purdue.libwaterapps.rock;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.libwaterapps.R;
import com.google.android.maps.GeoPoint;

import edu.purdue.libwaterapps.db.RockDB;
import edu.purdue.libwaterapps.provider.RockProvider;

/* A class which knows everything about a given rock */
public class Rock {
	private int id = BLANK_ROCK_ID;
	private String trelloId;
	private int lat;
	private int lon;
	private boolean picked;
	private String comments;
	private String picture;
	private Date updateDate; 
	private Date trelloPullDate;
	private boolean deleted;
	private Context context;
	private Drawable drawable;
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
	
	public static final int BLANK_ROCK_ID = -1; 
	
	public static final String ACTION_ADDED = "edu.purdue.libwaterapps.rock.ADDED";
	public static final String ACTION_UPDATED = "edu.purdue.libwaterapps.rock.UPDATED";
	public static final String ACTION_DELETED = "edu.purdue.libwaterapps.rock.DELETED";
	public static final String ACTION_SELECTED = "edu.purdue.libwaterapps.rock.SELECTED";
	public static final String ACTION_MOVE = "edu.purdue.libwaterapps.rock.MOVE";
	public static final String ACTION_MOVE_DONE = "edu.purdue.libwaterapps.rock.MOVE_DONE";
	public static final String ACTION_DOUBLE_TAP = "edu.purdue.libwaterapps.rock.DOUBLE_TAP";
	public static final String ACTION_LONG_HOLD = "edu.purdue.libwaterapps.rock.LONG_HOLD";
	
	public static final String IMAGE_PATH = Environment.getExternalStorageDirectory() + "/edu.purdue.rockapp/images";
	public static final String IMAGE_FILENAME_PATTERN = "rock_%d.png";
	
	/*
	 * A way to make a dummy rock. A context needs to be set if going to interact with the content provider
	 */
	public Rock() {
		updateDrawable();
	}
	
	public Rock(Context context) {
		this.updateDate = new Date(0);
		this.updateDate = new Date(0);
		this.context = context;
		updateDrawable();
	}

	public Rock(Context context, GeoPoint point, boolean picked) {
		this.lat = point.getLatitudeE6();
		this.lon = point.getLongitudeE6();
		this.picked = picked;
		this.deleted = false;
		this.context = context;
		updateDrawable();
	}
	
	public static Rock getRock(Context context, int id) {
		Rock rock = null;
		String where = RockProvider.Constants._ID + " = ? " +
					"AND NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { Integer.toString(id) };
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where,
														   whereArgs, "");
		
		if(cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			rock = Rock.translateCursorToRock(context, cursor);
		}
		
		cursor.close();
				
		return rock;
	}
	
	public static ArrayList<Rock> getRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		String where = "NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { };
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where, whereArgs, "");
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			rocks.add(Rock.translateCursorToRock(context, cursor));
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return rocks;
	}
	
	public static ArrayList<Rock> getPickedRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		String where = RockProvider.Constants.PICKED + " = ? AND NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { "true" };
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI, 
														   Rock.rockProjection, where, whereArgs, "");
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			rocks.add(Rock.translateCursorToRock(context, cursor));
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return rocks;
	}
	
	public static ArrayList<Rock> getNotPickedRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		String where = RockProvider.Constants.PICKED + " = ? AND NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { "false" };
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI, 
														   Rock.rockProjection, where, whereArgs, "");
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			rocks.add(Rock.translateCursorToRock(context, cursor));
			cursor.moveToNext();
		}
		
		cursor.close();
		
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
		Intent actionIntent;
		
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
		
		if(this.id < 0) {
			Uri uri = this.context.getContentResolver().insert(
					RockProvider.Constants.CONTENT_URI,
					vals);
			
			this.setId((int)ContentUris.parseId(uri));
			
			actionIntent = new Intent(Rock.ACTION_ADDED);
			
		} else {
			String where = RockProvider.Constants._ID + "=?";
			String[] whereArgs = {Integer.toString(this.id)};
			
			this.context.getContentResolver().update(
					RockProvider.Constants.CONTENT_URI,
					vals, where, whereArgs);
			
			actionIntent = new Intent(Rock.ACTION_UPDATED);
		}
		
		// Send a broadcast that a rock was added/updated
		if(notifyApplication) {
			actionIntent.putExtra("id", getId());
			LocalBroadcastManager.getInstance(context).sendBroadcast(actionIntent);
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
			Intent actionIntent = new Intent(Rock.ACTION_DELETED);
			actionIntent.putExtra("id", getId());
			LocalBroadcastManager.getInstance(context).sendBroadcast(actionIntent);
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
	
	private void updateDrawable() {
		if(picked) {
			drawable = context.getResources().getDrawable(R.drawable.rock_picked);
		} else {
			drawable = context.getResources().getDrawable(R.drawable.rock_not_picked);
		}
	}
	
	public Drawable getDrawable() {
		return drawable;
	}
	
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTrelloId() {
		return this.trelloId;
	}

	public void setTrelloId(String trelloId) {
		this.trelloId = trelloId;
	}

	public int getLat() {
		return this.lat;
	}

	public void setLat(int lat) {
		this.lat = lat;
	}

	public int getLon() {
		return this.lon;
	}

	public void setLon(int lon) {
		this.lon = lon;
	}

	public boolean isPicked() {
		return this.picked;
	}

	public void setPicked(boolean picked) {
		this.picked = picked;
		updateDrawable();
	}

	public String getComments() {
		return this.comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getPicture() {
		return this.picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}
	
	public void deletePicture() {
		if(picture != null) {
			File pic = new File(picture);
			pic.delete();
			
			picture = null;
			save();
		}
	}

	public Date getUpdateDate() {
		return this.updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public Date getTrelloPullDate() {
		return this.trelloPullDate;
	}

	public void setTrelloPullDate(Date trelloPullDate) {
		this.trelloPullDate = trelloPullDate;
	}
	
	public Context getContext() {
		return this.context;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	
	public boolean getDeleted() {
		return this.deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}