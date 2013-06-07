package edu.purdue.autogenics.libcommon.rock;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import edu.purdue.autogenics.libcommon.R;
import edu.purdue.autogenics.libcommon.db.RockDB;
import edu.purdue.autogenics.libcommon.provider.RockProvider;

/* A class which knows everything about a given rock */
public class Rock {

	private int id = BLANK_ROCK_ID;
	private String trelloId;
	private double lat;
	private double lon;
	private double actualLat;
	private double actualLon;
	
	private boolean picked;
	private String comments;
	private String picture;
	private boolean deleted;
	private boolean changed = true; 
	private Date changedDate = new Date(0);
	
	private Context context;
	public static final String[] rockProjection = {
		RockProvider.Constants._ID,
		RockProvider.Constants.TRELLO_ID,
		RockProvider.Constants.LAT,
		RockProvider.Constants.LON,
		RockProvider.Constants.PICKED,
		RockProvider.Constants.COMMENTS,
		RockProvider.Constants.PICTURE,
		RockProvider.Constants.HAS_CHANGED,
		RockProvider.Constants.DATE_CHANGED,
		RockProvider.Constants.DELETED
	};
	
	public static final int BLANK_ROCK_ID = -1; 
	
	public static final String ACTION_ADDED = "edu.purdue.autogenics.libcommon.rock.ADDED";
	public static final String ACTION_UPDATED = "edu.purdue.autogenics.libcommon.rock.UPDATED";
	public static final String ACTION_DELETED = "edu.purdue.autogenics.libcommon.rock.DELETED";
	public static final String ACTION_SELECTED = "edu.purdue.autogenics.libcommon.rock.SELECTED";
	public static final String ACTION_REVERT_MOVE = "edu.purdue.autogenics.libcommon.rock.REVERT_MOVE";
	public static final String ACTION_MOVE = "edu.purdue.autogenics.libcommon.rock.MOVE";
	public static final String ACTION_MOVE_DONE = "edu.purdue.autogenics.libcommon.rock.MOVE_DONE";
	public static final String ACTION_DOUBLE_TAP = "edu.purdue.autogenics.libcommon.rock.DOUBLE_TAP";
	public static final String ACTION_LONG_HOLD = "edu.purdue.autogenics.libcommon.rock.LONG_HOLD";
	
	public static final String IMAGE_PATH = Environment.getExternalStorageDirectory() + "/edu.purdue.rockapp/images";
	public static final String IMAGE_FILENAME_PATTERN = "rock_%d.png";
	
	/*
	 * A way to make a dummy rock. A context needs to be set if going to interact with the content provider
	 */
	public Rock() {
	}
	
	public Rock(Context context) {
		this.context = context;
	}

	public Rock(Context context, double latitude, double longitude, boolean picked) {
		this.lat = latitude;
		this.actualLat = this.lat;
		this.lon = longitude;
		this.actualLon = this.lon;
		this.picked = picked;
		this.deleted = false;
		this.context = context;
	}
	
	// Returns a single rock located by its id
	public static Rock getRock(Context context, int id) {
		Rock rock = null;
		String where = RockProvider.Constants._ID + " = ?";
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
	
	// Returns a single rock located by its trello_id
	public static Rock getRockByTrelloId(Context context, String id) {
		Rock rock = null;
		String where = RockProvider.Constants.TRELLO_ID + " = '" + id + "'";
				
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where,
														   null, "");
		
		if(cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			rock = Rock.translateCursorToRock(context, cursor);
		}
		cursor.close();
				
		return rock;
	}
	
	// Returns all rocks in the database that aren't deleted
	public static ArrayList<Rock> getRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		String where = "NOT " + RockProvider.Constants.DELETED;
		String[] whereArgs = { };
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where, whereArgs, "");
		if(cursor == null){
			Log.e("LibCommon", "Null cursor in getRocks");
		} else {
			cursor.moveToFirst();
			while(!cursor.isAfterLast()) {
				rocks.add(Rock.translateCursorToRock(context, cursor));
				
				cursor.moveToNext();
			}
			
			cursor.close();
		}
		
		return rocks;
	}
	
	// Returns all rocks in the database
	public static ArrayList<Rock> getAllRocks(Context context) { 
		ArrayList<Rock> rocks = new ArrayList<Rock>();
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, null, null, "");
		if(cursor == null){
			Log.e("LibCommon", "Null cursor in getAllRocks");
		} else {
			cursor.moveToFirst();
			while(!cursor.isAfterLast()) {
				rocks.add(Rock.translateCursorToRock(context, cursor));
				
				cursor.moveToNext();
			}
			
			cursor.close();
		}
		
		return rocks;
	}
	
	// Returns all of the "picked" rocks in the database
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
	
	// Returns all of the "not-picked" rocks in the database
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
		
		//Log.d("Rock", "Save Occured, notify = " + notifyApplication);
		
		ContentValues vals = new ContentValues();
		vals.put(RockProvider.Constants.TRELLO_ID, this.getTrelloId());
		vals.put(RockProvider.Constants.LAT, this.getLat());
		vals.put(RockProvider.Constants.LON, this.getLon());
		vals.put(RockProvider.Constants.PICKED, Boolean.toString(this.isPicked()));
		vals.put(RockProvider.Constants.COMMENTS, this.getComments());
		vals.put(RockProvider.Constants.PICTURE, this.getPicture());
		
		int intChanged = 0;
		if(this.getChanged() == true){
			intChanged = 1;
		}
		
		vals.put(RockProvider.Constants.HAS_CHANGED, intChanged);
		vals.put(RockProvider.Constants.DATE_CHANGED, this.getChangedDateString());
		
		int intDeleted = 0;
		if(this.getDeleted() == true){
			intDeleted = 1;
		}
		vals.put(RockProvider.Constants.DELETED, intDeleted);
		
		if(this.id < 0) {
			Uri uri = this.context.getContentResolver().insert(RockProvider.Constants.CONTENT_URI,vals);
			this.setId((int)ContentUris.parseId(uri));
			Log.d("RockApp New ROCK", "RockApp New ROCK");
			actionIntent = new Intent(Rock.ACTION_ADDED);
		} else {
			String where = RockProvider.Constants._ID + "=?";
			String[] whereArgs = {Integer.toString(this.id)};
			this.context.getContentResolver().update(RockProvider.Constants.CONTENT_URI, vals, where, whereArgs);
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
		Log.d("DELETE","DELETE");
		
		// Mark deleted
		this.setDeleted(true);
		
		//Mark changed
		this.setChanged(true);
		
		//Set changed date
		this.setChangedDate(new Date());
		
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
		rock.setLat(cursor.getDouble(2));
		rock.setActualLat(cursor.getDouble(2));
		rock.setLon(cursor.getDouble(3));
		rock.setActualLon(cursor.getDouble(3));
		rock.setPicked(Boolean.parseBoolean(cursor.getString(4)));
		rock.setComments(cursor.getString(5));
		rock.setPicture(cursor.getString(6));
		int intChanged = cursor.getInt(7);
		if(intChanged == 1){
			rock.setChanged(true);
		} else {
			rock.setChanged(false);
		}
		rock.setChangedDate(RockDB.stringToDate(cursor.getString(8)));
		int intDeleted = cursor.getInt(9);
		if(intDeleted == 1){
			rock.setDeleted(true);
		} else {
			rock.setDeleted(false);
		}		
		return rock;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(actualLat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(actualLon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((comments == null) ? 0 : comments.hashCode());
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + (deleted ? 1231 : 1237);
		result = prime * result + id;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (picked ? 1231 : 1237);
		result = prime * result + ((picture == null) ? 0 : picture.hashCode());
		result = prime * result
				+ ((trelloId == null) ? 0 : trelloId.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rock other = (Rock) obj;
		if (Double.doubleToLongBits(actualLat) != Double
				.doubleToLongBits(other.actualLat))
			return false;
		if (Double.doubleToLongBits(actualLon) != Double
				.doubleToLongBits(other.actualLon))
			return false;
		if (comments == null) {
			if (other.comments != null)
				return false;
		} else if (!comments.equals(other.comments))
			return false;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (deleted != other.deleted)
			return false;
		if (id != other.id)
			return false;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
			return false;
		if (picked != other.picked)
			return false;
		if (picture == null) {
			if (other.picture != null)
				return false;
		} else if (!picture.equals(other.picture))
			return false;
		if (trelloId == null) {
			if (other.trelloId != null)
				return false;
		} else if (!trelloId.equals(other.trelloId))
			return false;
		return true;
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

	public double getLat() {
		return this.lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setActualLat(double lat) {
		this.actualLat = lat;
	}
	
	public double getActualLat() {
		return this.actualLat;
	}
	
	public double getLon() {
		return this.lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public void setActualLon(double lat) {
		this.actualLon = lon;
	}
	
	public double getActualLon() {
		return this.actualLon;
	}
	
	public boolean isPicked() {
		return this.picked;
	}

	public void setPicked(boolean picked) {
		this.picked = picked;
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
	
	public boolean getChanged(){
		return this.changed;
	}
	
	public void setChanged(boolean changed){
		this.changed = changed;
	}
	
	public String getChangedDateString(){
		return RockDB.dateToString(this.changedDate);
	}
	
	public Date getChangedDate(){
		return this.changedDate;
	}
	
	public void setChangedDate(Date changed){
		this.changedDate = changed;
	}
}