package edu.purdue.autogenics.libcommon.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import edu.purdue.autogenics.libcommon.provider.ObjectsProvider;

/*
 * Manages the SQLite database for the ObjectProvider
 * 
 */
public class ObjectsDB extends SQLiteOpenHelper {
	// A tool help keep dates formated correctly in the database
	private static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String db_name = "objects";
	private static final int version = 2;
	
	/* DB uses a singleton pattern */
	public ObjectsDB(Context context) {
		super(context, db_name, null, version);
		
		// We always store and use UTC time
		dateFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/*
	 * Called by Android when no SQLite database exists. This is 
	 * where the database should be created.
	 */
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + ObjectsProvider.Constants.TABLE + "(" +
				ObjectsProvider.Constants._ID + " INTEGER PRIMARY KEY," +
				ObjectsProvider.Constants.GROUP + " INTEGER," +
				ObjectsProvider.Constants.TYPE + " INTEGER," + 
				ObjectsProvider.Constants.LAT + " INTEGER," +
				ObjectsProvider.Constants.LON + " INTEGER," +
				ObjectsProvider.Constants.NOTES_ID + " INTEGER," + 
				ObjectsProvider.Constants.DELETED + " TEXT)");
	}
	
	/*
	 * Called by Android when a SQLite database exists but the current version
	 * is different.
	 * 
	 * Our action is the just delete the old DB and make the new (cloud should
	 * restore the data)
	 */
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("DB", "Updating FeildNotebookApp Database");
		db.execSQL("DROP TABLE IF EXISTS " + ObjectsProvider.Constants.TABLE);
		this.onCreate(db);
	}
	
	/*
	 * Takes in a date and returns it in a string format
	 */
	public static String dateFormat(Date date) {
		return ObjectsDB.dateFormater.format(date);
	}
	
	/*
	 * Takes in a string formated by dateFormat() and returns the
	 * original date.
	 */
	public static Date dateParse(String date) {
		Date d;
		
		try {
			d = ObjectsDB.dateFormater.parse(date);
		} catch (ParseException e) {
			d = new Date(0);
		}
		
		return d;
	}
}
