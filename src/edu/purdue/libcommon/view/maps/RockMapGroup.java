package edu.purdue.libcommon.view.maps;

import java.util.ArrayList;
import java.util.Iterator;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import edu.purdue.libcommon.rock.Rock;

public class RockMapGroup {
	ArrayList<Rock> mRocks;
	
	public static final int ROCK_MAP_GROUP_UP = 1;
	public static final int ROCK_MAP_GROUP_DOWN = 2;
	public static final int ROCK_MAP_GROUP_BOTH = 3;
	
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
		
		if(mRocks.size() > 1) {
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
		
		if(mRocks.size() > 1) {
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
	public int getGroupStatus() {
		Rock last = mRocks.get(0);
		
		if(last == null) {
			return ROCK_MAP_GROUP_UP;
		}
		
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
	
	// STATIC HELPER FUNCTION
	// Returns an array list of RockMapGroup group from the given list of rocks
	public static ArrayList<RockMapGroup> groupRocks(final ArrayList<Rock> rocks, Rect rockBound, Projection proj) {
		ArrayList<RockMapGroup> rockGroups = new ArrayList<RockMapGroup>();
		
		// Container class for rock, point pairs
		class RockPoint {
			Rock rock;
			Point point;
		}
		
		// An internal list of rocks and points for grouping
		ArrayList<RockPoint> items = new ArrayList<RockPoint>();
		for(Rock rock : rocks) {
			RockPoint rp = new RockPoint();
			
			rp.rock = rock;
			rp.point = proj.toPixels(new GeoPoint(rock.getLat(), rock.getLon()), null);
			
			items.add(rp);
		}
		
		// A copy of the bounds
		Rect bound1 = new Rect(rockBound);
		
		// Group rocks 
		while(!items.isEmpty()) {
			RockPoint rp = items.remove(0);
			
			// Create a new group for this top
			RockMapGroup group = new RockMapGroup(rp.rock);
			
			// Position rock bounds
			bound1.offsetTo(rp.point.x, rp.point.y);
		
			// Loop through remaining rocks
			Iterator<RockPoint> itr = items.iterator();
			while(itr.hasNext()) {
				RockPoint rp2 = itr.next();
				
				// Make a bounds for this rock
				Rect bound2 = new Rect(rockBound);
				bound2.offsetTo(rp2.point.x, rp2.point.y);
				
				// If the intersect
				if(Rect.intersects(bound1, bound2)) {
					group.add(rp2.rock);
					
					itr.remove();
				}
			}
			
			// Add the group to the list
			rockGroups.add(group);
		}
		
		return rockGroups;
	}
}
