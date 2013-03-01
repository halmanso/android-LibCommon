package edu.purdue.libwaterapps.note;

import java.util.ArrayList;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import edu.purdue.libwaterapps.provider.NotesProvider;
import edu.purdue.libwaterapps.provider.RockProvider;

/* A class which knows everything about a given rock */
public class Note {
	private int id = BLANK_NOTE_ID;
	private int color;
	private String comments;
	private boolean deleted;
	private Context context;
	public static final String[] fieldProjection = {
		NotesProvider.Constants._ID,
		NotesProvider.Constants.COLOR,
		NotesProvider.Constants.COMMENTS,
		NotesProvider.Constants.DELETED 
	};
	
	public static final int BLANK_NOTE_ID = -1; 

	/*
	 * A way to make a dummy rock. A context needs to be set if going to interact with the content provider
	 */
	public Note() {
	}
	
	public Note(Context context) {
		this.context = context;
	}

	public Note(Context context, int color, String comments) {
		this.color = color;
		this.comments = comments;
		this.deleted = false;
		this.context = context;
	}
	
	public static Note getNote(Context context, int id) {
		Note rock = null;
		String where = NotesProvider.Constants._ID + " = ? " +
					"AND NOT " + NotesProvider.Constants.DELETED;
		String[] whereArgs = { Integer.toString(id) };
		
		Cursor cursor = context.getContentResolver().query(NotesProvider.Constants.CONTENT_URI,
														   Note.fieldProjection, where,
														   whereArgs, "");
		
		if(cursor != null && cursor.getCount() == 1) {
			cursor.moveToFirst();
			rock = Note.translateCursorToNote(context, cursor);
		}
		
		cursor.close();
				
		return rock;
	}
	
	public static ArrayList<Note> getNotes(Context context) { 
		ArrayList<Note> notes = new ArrayList<Note>();
		String where = "NOT " + NotesProvider.Constants.DELETED;
		String[] whereArgs = { };
		
		Cursor cursor = context.getContentResolver().query(NotesProvider.Constants.CONTENT_URI,
														   Note.fieldProjection, where, whereArgs, "");
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			notes.add(Note.translateCursorToNote(context, cursor));
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return notes;
	}
	
	/* 
	 * Update (or create) a rock in the DB.
	 * Default is to notify the application of the change
	 */
	public void save() {
		ContentValues vals = new ContentValues();
		vals.put(NotesProvider.Constants.COLOR, getColor());
		vals.put(NotesProvider.Constants.COMMENTS, this.getComments());
		vals.put(NotesProvider.Constants.DELETED, this.isDeleted());
		
		if(this.id < 0) {
			Uri uri = this.context.getContentResolver().insert(
					NotesProvider.Constants.CONTENT_URI,
					vals);
			
			this.setId((int)ContentUris.parseId(uri));
			
		} else {
			String where = RockProvider.Constants._ID + "=?";
			String[] whereArgs = {Integer.toString(this.id)};
			
			this.context.getContentResolver().update(
					NotesProvider.Constants.CONTENT_URI,
					vals, where, whereArgs);
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
	private static Note translateCursorToNote(Context context, Cursor cursor) {
		Note note = new Note(context);
		
		note.setId(Integer.parseInt(cursor.getString(0)));
		note.setColor(Integer.parseInt(cursor.getString(1)));
		note.setComments(cursor.getString(2));
		note.setDeleted(Boolean.parseBoolean(cursor.getString(3)));
		
		return note;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}