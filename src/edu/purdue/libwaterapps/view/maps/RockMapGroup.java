package edu.purdue.libwaterapps.view.maps;

import java.util.ArrayList;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import edu.purdue.libwaterapps.rock.Rock;

public class RockMapGroup {
	ArrayList<Rock> mRocks;
	
	public static final int ROCK_MAP_GROUP_UP = 1;
	public static final int ROCK_MAP_GROUP_DOWN = 2;
	public static final int ROCK_MAP_GROUP_BOTH = 3;
	
	public static final String ACTION_GROUP_SELECTED = "edu.purdue.libwaterapps.rock.GROUP_SELECTED";
	
	// Prepare the rock group 
	public RockMapGroup(Rock rock) {
		mRocks = new ArrayList<Rock>();
		
		// Add first rock
		mRocks.add(rock);
	}
	
	// Add a rock to the group
	public void add(Rock rock) {
		mRocks.add(rock);
	}
	
	// Get all the rocks in the group
	public ArrayList<Rock> getAll() {
		return mRocks;
	}
	
	// Found how many rocks are in this rock group
	public int size() {
		return mRocks.size();
	}
	
	// Get the groups average latitude
	public int getAvgLat() {
		int lat = 0;
		for(Rock rock : mRocks) {
			lat += rock.getLat();
		}
		
		lat /= mRocks.size();
		
		return lat;
	}
	
	// Get the groups average longitude
	public int getAvgLon() {
		int lon = 0;
		for(Rock rock : mRocks) {
			lon += rock.getLon();
		}
		
		lon /= mRocks.size();
		
		return lon;
	}
	
	// Find the latitude that this rock group spans
	public int getLatSpan() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int span = 0;
		int lat;
		
		if(mRocks.size() > 0) {
			for(Rock rock : mRocks) {
				lat = rock.getLat();
				
				min = Math.min(min, lat);
				max = Math.max(max, lat);
			}
			
			// Determine span
			span = max-min;
		}

		return span;
	}
	
	// Find the the longitude that this rock group spans
	public int getLonSpan() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int span = 0;
		int lon;
		
		if(mRocks.size() > 0) {
			for(Rock rock : mRocks) {
				lon = rock.getLon();
				
				min = Math.min(min, lon);
				max = Math.max(max, lon);
			}
			
			// Determine span
			span = max-min;
		}
		
		return span;
	}
	
	// Determine if the group is all up, down, or a mixed of both
	public int getGroupPicked() {
		Rock last = mRocks.get(0);
		
		// If a type differs then we have mixed
		for(Rock rock : mRocks) {
			if(last.isPicked() != rock.isPicked()) {
				return ROCK_MAP_GROUP_BOTH;
			}
		}
		
		// We only get here if there is only one type so just return the
		// type of any of them
		if(last.isPicked()) {
			return ROCK_MAP_GROUP_UP;
		} else {
			return ROCK_MAP_GROUP_DOWN;
		}
	}
	
	// Returns an array list of RockMapGroup group from the given list of rocks
	// NOTE: The given list of rocks is EMPTIED. You will lose the list given to this function
	// so make a copy of it before passing if needed later
	public static ArrayList<RockMapGroup> groupRocks(ArrayList<Rock> rocks, Rect rockBound, Projection proj) {
		ArrayList<RockMapGroup> rockGroups = new ArrayList<RockMapGroup>();
		
		// Group rocks 
		while(!rocks.isEmpty()) {
			// Grab the leader rock 
			Rock topRock = rocks.remove(0);
			// Make the bounds for the top rock and position it 
			Rect topBounds = new Rect(rockBound);
			Point topP = proj.toPixels(new GeoPoint(topRock.getLat(), topRock.getLon()), null);
			topBounds.offsetTo(topP.x, topP.y);
			
			// Create a new group for this top
			RockMapGroup group = new RockMapGroup(topRock);
			
			for(Rock rock : rocks) {
				// Get the bounds for this rock
				Rect otherBounds = new Rect(topBounds);
				Point otherP = proj.toPixels(new GeoPoint(rock.getLat(), rock.getLon()), null);
				otherBounds.offsetTo(otherP.x, otherP.y);
				
				// If the intersect
				if(Rect.intersects(topBounds, otherBounds)) {
					group.add(rock);
				}
			}
			
			// Add the group to the list
			rockGroups.add(group);
			
			// Remove all the rocks which we just grouped from the pending list
			rocks.removeAll(group.getAll());
		}
		
		return rockGroups;
	}
}
