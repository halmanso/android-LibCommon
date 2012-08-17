package edu.purdue.libwaterapps.note;

import java.util.ArrayList;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.purdue.libwaterapps.provider.NotesProvider;
import edu.purdue.libwaterapps.provider.ObjectsProvider;

/* A class which knows everything about a given rock */
public class Object {
	private int id = BLANK_NOTE_ID;
	private int group;
	private int type;
	private ArrayList<GeoPoint> points;
	private int notes_id;
	private boolean deleted;
	private Context context;
	public static final String[] fieldProjection = {
		ObjectsProvider.Constants._ID,
		ObjectsProvider.Constants.GROUP,
		ObjectsProvider.Constants.TYPE,
		ObjectsProvider.Constants.LAT,
		ObjectsProvider.Constants.LON,
		ObjectsProvider.Constants.NOTES_ID,
		ObjectsProvider.Constants.DELETED
	};
	
	public static final int BLANK_NOTE_ID = -1; 
	
	public static final int TYPE_POINT = 1;
	public static final int TYPE_LINE = 2;
	public static final int TYPE_POLYGON = 3;
	
	/*
	 * A way to make a dummy rock. A context needs to be set if going to interact with the content provider
	 */
	public Object() {
	}
	
	public Object(Context context) {
		this.context = context;
	}

	public Object(Context context, int group,  int type, ArrayList<GeoPoint> points, int notes_id) {
		this.group = group; 
		this.type = type;
		this.points = points;
		this.notes_id = notes_id;
		this.deleted = false;
		this.context = context;
		
		Log.d("GroupID", "group = " + group);
	}
	
	public static Object getObject(Context context, int id) {
		Object object = null;
		String where = ObjectsProvider.Constants._ID + " = ? " +
					"AND NOT " + NotesProvider.Constants.DELETED;
		String[] whereArgs = { Integer.toString(id) };
		
		Cursor cursor = context.getContentResolver().query(ObjectsProvider.Constants.CONTENT_URI,
														   Object.fieldProjection, where,
														   whereArgs, "");
		
		if(cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			object = Object.translateCursorToNote(context, cursor);
		}
		
		cursor.close();
				
		return object;
	}
	
	public static Object getObjectByGroup(Context context, int group) { 
		Object object = new Object();
		String where = ObjectsProvider.Constants.GROUP + " = ? " + 
					   " AND NOT " + ObjectsProvider.Constants.DELETED;
		String[] whereArgs = { Integer.toString(group) };
		
		Cursor cursor = context.getContentResolver().query(ObjectsProvider.Constants.CONTENT_URI,
														   Object.fieldProjection, where, whereArgs, "");
		cursor.moveToFirst();
		if(!cursor.isAfterLast()) {
			object = Object.translateCursorToNote(context, cursor);
			
			cursor.close();
			
			return object;
		} else {
			return null;
		}
	}
	
	public static int getNewGroupId(Context context) {
		Cursor cursor = context.getContentResolver().query(ObjectsProvider.Constants.CONTENT_URI, 
				new String[] {"MAX(groupnum) as max_group"}, null, null, null);
		
		cursor.moveToFirst();
		if(!cursor.isAfterLast()) {
			return cursor.getInt(0) + 1;
		}
		
		return -1;
	}
	
	/* 
	 * Update (or create) a rock in the DB.
	 * Default is to notify the application of the change
	 */
	public void save() {
		for(GeoPoint point : points) {
			ContentValues vals = new ContentValues();
			
			vals.put(ObjectsProvider.Constants.GROUP, getGroup());
			vals.put(ObjectsProvider.Constants.TYPE, getType());
			vals.put(ObjectsProvider.Constants.LAT, point.getLatitudeE6());
			vals.put(ObjectsProvider.Constants.LON, point.getLongitudeE6());
			vals.put(ObjectsProvider.Constants.NOTES_ID, getNotes_id());
			vals.put(ObjectsProvider.Constants.DELETED, isDeleted());
			
			if(this.id < 0) {
				Uri uri = this.context.getContentResolver().insert(
						ObjectsProvider.Constants.CONTENT_URI,
						vals);
				
				this.setId((int)ContentUris.parseId(uri));
				
			} else {
				String where = ObjectsProvider.Constants._ID + "=?";
				String[] whereArgs = {Integer.toString(this.id)};
				
				this.context.getContentResolver().update(
						ObjectsProvider.Constants.CONTENT_URI,
						vals, where, whereArgs);
			}
		}
	}
	
	/* 
	 * Mark a rock deleted in the DB.
	 * Default is to notify the application of the change
	 */
	public void delete() {
		// The rock has not yet been saved so there is nothing to delete
		if(getId() < 0) {
			return;
		}
		
		// Mark deleted
		setDeleted(true);
		
		// We will send our own notify
		save();
	}
	
	/*
	 * Internal method which can translate the result of the DB request (a Cursor object)
	 * into our custom Rock object for consumption in the rest of the application
	 */
	private static Object translateCursorToNote(Context context, Cursor cursor) {
		Object note = new Object(context);
		
		note.setId(Integer.parseInt(cursor.getString(0)));
		note.setGroup(Integer.parseInt(cursor.getString(1)));
		note.setType(Integer.parseInt(cursor.getString(2)));
		note.setNotes_id(Integer.parseInt(cursor.getString(5)));
		note.setDeleted(Boolean.parseBoolean(cursor.getString(6)));
		
		ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();
		while(!cursor.isAfterLast()) {
			GeoPoint p = new GeoPoint(Integer.parseInt(cursor.getString(3)), Integer.parseInt(cursor.getString(4)));
			
			points.add(p);
			
			cursor.moveToNext();
		}
		
		note.setPoints(points);
		
		return note;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public ArrayList<GeoPoint> getPoints() {
		return points;
	}

	public void setPoints(ArrayList<GeoPoint> points) {
		this.points = points;
	}

	public int getNotes_id() {
		return notes_id;
	}

	public void setNotes_id(int notes_id) {
		this.notes_id = notes_id;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
}