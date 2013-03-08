package edu.purdue.autogenics.libcommon.rock;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.purdue.autogenics.libcommon.db.RockDB;
import edu.purdue.autogenics.libcommon.provider.RockProvider;

/* A class which knows everything about a given rock */
public class Rock {
	private int id = BLANK_ROCK_ID;
	private String trelloId;
	private int lat;
	private int lon;
	private int actualLat;
	private int actualLon;
	private boolean picked;
	private String comments;
	private String picture;
	private Date updateDate; 
	private Date trelloPullDate;
	private boolean deleted;
	private Context context;
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
		this.updateDate = new Date(0);
		this.updateDate = new Date(0);
		this.context = context;
	}

	public Rock(Context context, GeoPoint point, boolean picked) {
		this.lat = point.getLatitudeE6();
		this.actualLat = this.lat;
		this.lon = point.getLongitudeE6();
		this.actualLon = this.lon;
		this.picked = picked;
		this.deleted = false;
		this.context = context;
	}
	
	// Returns a single rock located by its id
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
	
	// Returns a single rock located by its trello_id
	public static Rock getRockByTrelloId(Context context, String id) {
		Log.d("RockApp", "getting rock by trelloid:" + id);
		Rock rock = null;
		String where = RockProvider.Constants.TRELLO_ID + " = '" + id + "'";
		
		Log.d("RockAPp", "where:" + where);
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI,
														   Rock.rockProjection, where,
														   null, "");
		
		if(cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			rock = Rock.translateCursorToRock(context, cursor);
			Log.d("RockApp", "Got it");
		} else {
			if(cursor == null){
				Log.d("RockApp", "Not found");
			} else {
				Log.d("Rockppp", "Duplicate ids:" + Integer.toString(cursor.getCount()));
			}
		}
		
		cursor.close();
				
		return rock;
	}
	
	// Returns all rocks in the database
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
		
		Log.d("Rock", "Save Occured, notify = " + notifyApplication);
		
		/* Set the update time to the current time */
		this.setUpdateDate(new Date());
		
		ContentValues vals = new ContentValues();
		vals.put(RockProvider.Constants.TRELLO_ID, this.getTrelloId());
		vals.put(RockProvider.Constants.LAT, this.getActualLat());
		vals.put(RockProvider.Constants.LON, this.getActualLon());
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
			Log.d("RockApp New ROCK", "RockApp New ROCK");
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
		
		//Send to trello if we are syncing
		Log.d("RockApp", "Lat:" + Integer.toString(this.getActualLat()));
		Log.d("RockApp", "Lng:" + Integer.toString(this.getActualLon()));
		
		Log.d("RockApp", "TrelloId:" + this.getTrelloId());
		//Send rock to trello
		Intent sendIntent = new Intent();
		Bundle extras = new Bundle();
		extras.putString("PushCard", "Nothing Matters");
		extras.putString("id", this.getTrelloId());
		
		String negLat = "";
		Integer actLat = this.getActualLat();
		if(this.getActualLat() < 0){
			negLat = "-";
			actLat = actLat * -1;
		}
		Integer bigLat = actLat / 1000000;
		Integer smallLat = actLat  - (bigLat * 1000000);
		
		String negLng = "";
		Integer actLng = this.getActualLon();
		if(this.getActualLon() < 0){
			negLng = "-";
			actLng = actLng * -1;
		}
		Integer bigLng = actLng / 1000000;
		Integer smallLng = actLng  - (bigLng * 1000000);
		
		String doneLat = Integer.toString(bigLat) + "." + Integer.toString(smallLat);
		String doneLng =  Integer.toString(bigLng) + "." + Integer.toString(smallLng);
		
		while(doneLat.length() < 8){
			doneLat = doneLat + "0";
		}
		while(doneLng.length() < 8){
			doneLng = doneLng + "0";
		}
		String newName = "Lat: " + negLat + doneLat + " Lng: "+ negLng + doneLng;
		String listId = this.isPicked() == true ? "2" : "1";
		extras.putString("name", newName);
		extras.putString("desc", this.getComments());
		extras.putString("list_id", listId);
		extras.putString("owner", "edu.purdue.autogenics.rockapp");
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.setPackage("edu.purdue.autogenics.trello");
		sendIntent.putExtras(extras);
		this.context.startService(sendIntent);
		
		/*final Context it = this.context;
		new Thread(new Runnable() {
			public void run() {
				//Sync
				Intent sendIntent2 = new Intent();
				Bundle extras2 = new Bundle();
				extras2.putString("Sync", "Nothing Matters");
				extras2.putString("owner", "edu.purdue.autogenics.rockapp");
				sendIntent2.setAction(Intent.ACTION_SEND);
				sendIntent2.setPackage("edu.purdue.autogenics.trello");
				sendIntent2.putExtras(extras2);
				it.startService(sendIntent2);
			}
		}).run();*/
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
		Log.d("DETELE","DETELE");
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
		rock.setActualLat(cursor.getInt(2));
		rock.setLon(cursor.getInt(3));
		rock.setActualLon(cursor.getInt(3));
		rock.setPicked(Boolean.parseBoolean(cursor.getString(4)));
		rock.setComments(cursor.getString(5));
		rock.setPicture(cursor.getString(6));
		rock.setUpdateDate(RockDB.dateParse(cursor.getString(7)));
		rock.setTrelloPullDate(RockDB.dateParse(cursor.getString(8)));
		rock.setDeleted(Boolean.parseBoolean(cursor.getString(9)));
		
		return rock;
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
	
	public void setActualLat(int lat) {
		this.actualLat = lat;
	}
	
	public int getActualLat() {
		return this.actualLat;
	}
	
	public int getLon() {
		return this.lon;
	}

	public void setLon(int lon) {
		this.lon = lon;
	}
	
	public void setActualLon(int lat) {
		this.actualLon = lon;
	}
	
	public int getActualLon() {
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