package edu.purdue.libwaterapps.rock;

import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.android.maps.GeoPoint;

import edu.purdue.libwaterapps.db.RockDB;
import edu.purdue.libwaterapps.provider.RockProvider;
import edu.purdue.libwaterapps.utils.NotifyArrayList;

/* A class which knows everything about a given rock */
public class Rock {
	private int id = -1;
	private String trelloId;
	private int lat;
	private int lon;
	private boolean picked;
	private String comments;
	private String picture;
	private Date updateDate; 
	private Date trelloPullDate;
	private Context context;
	public static String[] rockProjection = {
		RockProvider.Constants._ID,
		RockProvider.Constants.TRELLO_ID,
		RockProvider.Constants.LAT,
		RockProvider.Constants.LON,
		RockProvider.Constants.PICKED,
		RockProvider.Constants.COMMENTS,
		RockProvider.Constants.PICTURE,
		RockProvider.Constants.UPDATE_TIME,
		RockProvider.Constants.TRELLO_PULL_TIME
	};
	
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
		lat = point.getLatitudeE6();
		lon = point.getLongitudeE6();
		this.picked = picked;
		this.context = context;
	}
	
	public static Rock getRock(Context context, int id) {
		Rock rock = null;
		String where = RockProvider.Constants._ID + "=?";
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
	
	public static NotifyArrayList<Rock> getAllRocks(Context context) { 
		NotifyArrayList<Rock> rocks = new NotifyArrayList<Rock>();
		
		Cursor cursor = context.getContentResolver().query(RockProvider.Constants.CONTENT_URI, 
														   Rock.rockProjection, null, null, "");
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			rocks.add(Rock.translateCursorToRock(context, cursor));
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return rocks;
	}
	
	public static NotifyArrayList<Rock> getAllPickedRocks(Context context) { 
		NotifyArrayList<Rock> rocks = new NotifyArrayList<Rock>();
		String where = "picked = ?";
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
	
	public static NotifyArrayList<Rock> getAllNonPickedRocks(Context context) { 
		NotifyArrayList<Rock> rocks = new NotifyArrayList<Rock>();
		String where = "picked = ?";
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
	
	
	
	/* Create a rock in the database */
	public void save() {
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
		
		if(this.id < 0) {
			Uri uri = this.context.getContentResolver().insert(
					RockProvider.Constants.CONTENT_URI,
					vals);
			
			this.setId((int)ContentUris.parseId(uri));
		} else {
			String where = RockProvider.Constants._ID + "=?";
			String[] whereArgs = {Integer.toString(this.id)};
			
			this.context.getContentResolver().update(
					RockProvider.Constants.CONTENT_URI,
					vals, where, whereArgs);
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
	
}