package edu.purdue.autogenics.libcommon.view.maps;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;

import com.example.libwaterapps.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import edu.purdue.autogenics.libcommon.rock.Rock;

public class RockMapOverlay extends Overlay {
	private MapView mMapView;
	private Rock mSelectedRock;
	private Bitmap mUp;
	private Bitmap mDown;
	private Bitmap mUpDown;
	private Bitmap mShadow;
	private Bitmap mSelected;
	private ArrayList<Rock> mRocks;
	private ArrayList<RockMapGroup> mRockGroups;
	private GeoPoint mLastMapCenter;
	private int mLastLonSpan;
	private int mLastLatSpan;
	
	private int mShowHide; 
	
	private int movePointerId;
	private boolean mMoveInAction = false;
	private boolean mMoveFreely = false;
	
	private Context mContext;
	private RockBroadcastReceiver mRockBroadcastReceiver;
	
	public static final int SHOW_ALL_ROCKS = 1;
	public static final int SHOW_NOT_PICKED_ROCKS = 2;
	public static final int SHOW_PICKED_ROCKS = 3;

	private static final int DRAG_DEAD_ZONE = 50;
	
	/*
	 * Create a Overlay for a Google Map which knows how to display 
	 * rocks over the map.
	 * 
	 * The class will listen for notifications of rock changes, display
	 * setting, etc
	 * 
	 * The class will emit notifications when rocks are selected and will
	 * zoom the map to show grouped rocks if enabled
	 */
	public RockMapOverlay(Context context, MapView mapView) {
		Drawable drawable;
		Canvas canvas;
		
		// Save the context
		mContext = context;
		// Save the MapView
		mMapView = mapView;
		
		// Get the images needed for drawing the rock
		drawable = context.getResources().getDrawable(R.drawable.rock_up);
		mUp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		canvas = new Canvas(mUp);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		
		drawable = context.getResources().getDrawable(R.drawable.rock_down);
		mDown = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		canvas = new Canvas(mDown);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		
		drawable = context.getResources().getDrawable(R.drawable.rock_up_down);
		mUpDown = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		canvas = new Canvas(mUpDown);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		
		drawable = context.getResources().getDrawable(R.drawable.rock_shadow);
		mShadow = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		canvas = new Canvas(mShadow);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		
		drawable = context.getResources().getDrawable(R.drawable.rock_selected);
		mSelected = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		canvas = new Canvas(mSelected);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		
		// Create a list to hold the rock groups
		mRockGroups = new ArrayList<RockMapGroup>();
		
		// Get a list of all the rocks
		mRocks = Rock.getRocks(context);
		
		// Start with no selected rock
		mSelectedRock = new Rock();
		
		// Initialize the last map position
		mLastMapCenter = new GeoPoint(0,0);
		mLastLonSpan = -1;
		mLastLatSpan = -1;
		
		// Initialize the default show hide
		mShowHide = SHOW_NOT_PICKED_ROCKS;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		// See if we have no groupings or if the map has moved and so we need to (re)filter and group rocks
		if(mRockGroups == null || mapView.getLatitudeSpan() != mLastLatSpan || mapView.getLongitudeSpan() != mLastLonSpan ||
				!mapView.getMapCenter().equals(mLastMapCenter)) {
			filterAndGroupRocks(mapView);
		}
		 
		// Print all of the rocks in the mRockGroups
		Point p = new Point();
		for(RockMapGroup group : mRockGroups) {
			mapView.getProjection().toPixels(new GeoPoint(group.getAvgLat(), group.getAvgLon()), p);
		
			drawGroup(group, p, canvas, shadow);
		}
		
		// Draw the selected rock last. It is never part of mRockGroups
		if(mSelectedRock.getId() != Rock.BLANK_ROCK_ID) { 
			RockMapGroup group = new RockMapGroup(mSelectedRock);
			mapView.getProjection().toPixels(new GeoPoint(group.getAvgLat(), group.getAvgLon()), p);
			drawGroup(group, p, canvas, shadow);
		}
		
	}
	
	// Helper function which knows the details of drawing a group 
	private void drawGroup(RockMapGroup group, Point p, Canvas canvas, boolean shadow) {
		if(shadow) {
			// Draw the shadow first so that its on the bottom
			canvas.drawBitmap(mShadow, p.x - mShadow.getWidth()/2 + 10, p.y - mShadow.getHeight()/2+10, null);
		} else {
			// Draw the actual rock icons (or rock group icons)
			// If this rock is selected then draw the select bubble
			if(mSelectedRock.getId() == group.getAll().get(0).getId()) {
				canvas.drawBitmap(mSelected, p.x - mSelected.getWidth()/2, p.y - mSelected.getHeight()/2, null);
			}
			
			// Determine what the make up of the group is
			Bitmap marker;
			switch(group.getGroupPicked()) {
				case RockMapGroup.ROCK_MAP_GROUP_BOTH:
					marker = mUpDown;
				break;
				
				case RockMapGroup.ROCK_MAP_GROUP_UP:
					marker = mUp;
				break;
				
				default:
				case RockMapGroup.ROCK_MAP_GROUP_DOWN:
					marker = mDown;
				break;
			}
			
			// Draw the rock icon
			canvas.drawBitmap(marker, p.x - marker.getWidth()/2, p.y - marker.getHeight()/2, null);
	
			// If this is a group of size greater then one then type the number on the marker
			if(group.size() > 1) {
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(Color.BLACK);
				paint.setTextAlign(Paint.Align.CENTER);
				paint.setTextSize(20);
				paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
				paint.setTextSkewX(-0.2f);
				canvas.drawText(Integer.toString(group.size()), p.x+1, p.y+5, paint);
				
				paint.setColor(Color.WHITE);
				paint.setTextSize(20);
				canvas.drawText(Integer.toString(group.size()), p.x-1, p.y+3, paint);
			}
		}
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		// Make sure the rock groups are the most up to date
		if(mRockGroups == null) {
			filterAndGroupRocks(mapView);
		}
		
		switch(e.getAction()) {
			case MotionEvent.ACTION_MOVE:
				// See if we got a "move" for a finger we were tracking
				int idx = e.findPointerIndex(movePointerId);
				if(mMoveInAction && idx != -1) {
					
					// If we are in a move then just track the finger
					if(mMoveFreely) {
						GeoPoint gp = mapView.getProjection().fromPixels((int)e.getX(idx), (int)e.getY(idx));
						
						mSelectedRock.setLat(gp.getLatitudeE6());
						mSelectedRock.setLon(gp.getLongitudeE6());
						
						// Otherwise see if we should start a move yet
					} else {
						Point p = mapView.getProjection().toPixels(new GeoPoint(mSelectedRock.getLat(), mSelectedRock.getLon()), null);
						if(Math.hypot(p.x - e.getX(), p.y - e.getY()) > DRAG_DEAD_ZONE) {
							mMoveFreely = true;
						}
					}
					
					return true;
				}
				
				// Stop moving the map if there is a "drag" when selecting a rock
				if(!mMoveInAction && mSelectedRock.getId() != Rock.BLANK_ROCK_ID) {
					return true;
				}
			break;
			
			case MotionEvent.ACTION_DOWN:
				// Look for a rock which we pushed down on 
				Point p = new Point();
				RectF markerBoundary = new RectF(0, 0, mSelected.getWidth(), mSelected.getHeight());
	
				// First test to see if we reselected the selected rock
				if(mSelectedRock.getId() != Rock.BLANK_ROCK_ID) {
					mapView.getProjection().toPixels(new GeoPoint(mSelectedRock.getLat(), mSelectedRock.getLon()), p);
					markerBoundary.offsetTo(p.x, p.y);
					markerBoundary.offset(-mDown.getWidth()/2, -mDown.getHeight()/2);
					
					if(markerBoundary.contains(e.getX(), e.getY())) {
						// Start tracking the finger 
						mMoveInAction = true;
						movePointerId = e.getPointerId(0);
						
						return true;
					}
				}
				
				markerBoundary = new RectF(0, 0, mDown.getWidth(), mDown.getHeight());
				
				for(RockMapGroup group : mRockGroups) {
					mapView.getProjection().toPixels(new GeoPoint(group.getAvgLat(), group.getAvgLon()), p);
					
					markerBoundary.offsetTo(p.x, p.y);
					markerBoundary.offset(-mDown.getWidth()/2, -mDown.getHeight()/2);
					
					if(markerBoundary.contains(e.getX(), e.getY())) {
						if(mSelectedRock.getId() != Rock.BLANK_ROCK_ID) {
							// Save the current one 
							// First we need to commit to these new lat and lon
							mSelectedRock.setActualLat(mSelectedRock.getLat());
							mSelectedRock.setActualLon(mSelectedRock.getLon());
							mSelectedRock.save();
						}
						
						// Send out a group select broadcast if we selected one
						if(group.size() > 1) {
							
							// If we are at the highest zoom level then just select the top
							// on of the group so that you can at least work with it
							if(mapView.getZoomLevel() == 21) {
								setSelected(group.getAll().get(0));
							} else {
								// Send a broadcast to who ever cares that the user selected a group
								Intent intent = new Intent(RockMapGroup.ACTION_GROUP_SELECTED);
								intent.putExtra("lat", group.getAvgLat());
								intent.putExtra("lon", group.getAvgLon());
								intent.putExtra("lat-span", group.getLatSpan());
								intent.putExtra("lon-span", group.getLonSpan());
								LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
								
								// We do not know what to select so we do not select anything
								setSelected(new Rock());
							}
						} else {
							// We selected a single rock so we select that
							setSelected(group.getAll().get(0));
						}
						
						return true;
					}
				}
				
				// We push down somewhere else and we have a currently selected rock, de-select it
				if(mSelectedRock.getId() != Rock.BLANK_ROCK_ID) {
					mSelectedRock.setActualLat(mSelectedRock.getLat());
					mSelectedRock.setActualLon(mSelectedRock.getLon());
					mSelectedRock.save();
					
					setSelected(new Rock());
				}
			break;
			
			case MotionEvent.ACTION_UP:
				// No matter what lifting your finger ends any current moves
				mMoveInAction = false;
				mMoveFreely = false;
				
				return true;
		}
		
		return false;
	}
	
	public int getShowHide() {
		return mShowHide;
	}
	
	public void setShowHide(int type) {
		// Validate change
		switch(type) {
			case SHOW_ALL_ROCKS:
			case SHOW_NOT_PICKED_ROCKS:
			case SHOW_PICKED_ROCKS:
				mShowHide = type;
			break;
			
			default:
				mShowHide = SHOW_ALL_ROCKS;
			break;
		}
		
		mRockGroups = null;
		mMapView.postInvalidate();
	}
	
	// Mark a rock as "selected"
	public void setSelected(int id) {
		
		// Find and save the rock object, defaults to nothing selected if id is not found
		setSelected(findRockById(id));
	}
	
	public void setSelected(Rock rock) { 
		// Save the rock object
		mSelectedRock = rock;
		
		// Tell the world 
		Intent intent = new Intent(Rock.ACTION_SELECTED);
		intent.putExtra("id", mSelectedRock.getId());
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
		
		// Put this rock at the end of the list so the next selection (if its a group which is too close
		// even for the lowest zoom level) gets a new rock
		mRocks.remove(rock);
		mRocks.add(rock);
		
		// Force a re-group
		mRockGroups = null;
		mMapView.postInvalidate();
	}
	
	// Return the currently selected rock
	public Rock getSelected() {
		return mSelectedRock;
	}
	
	// Start listening to changes in rocks so we can stay up to date
	public void registerListeners() {
		if(mRockBroadcastReceiver == null) {
			mRockBroadcastReceiver = new RockBroadcastReceiver();
			
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mRockBroadcastReceiver, new IntentFilter(Rock.ACTION_ADDED));
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mRockBroadcastReceiver, new IntentFilter(Rock.ACTION_UPDATED));
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mRockBroadcastReceiver, new IntentFilter(Rock.ACTION_DELETED));
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mRockBroadcastReceiver, new IntentFilter(Rock.ACTION_REVERT_MOVE));
			
			// Refresh our rock list because we might have missed messages when it was off
			mRocks = Rock.getRocks(mContext);
			mRockGroups = null;
			mMapView.postInvalidate();
		}
	}
	
	// Stop listening to changes in rocks
	public void unregisterListeners() {
		if(mRockBroadcastReceiver != null) {
			LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mRockBroadcastReceiver);
			
			mRockBroadcastReceiver = null;
		}
	}
	
	// Helper function to search through mRocks to get a specific one by id
	private Rock findRockById(int id) {
		for(Rock rock : mRocks) {
			if(rock.getId() == id) {
				return rock;
			}
		}
		
		return new Rock();
	}
	
	// Helper function which knows how to filter out rocks that wont display and then groups them
	private void filterAndGroupRocks(MapView mapView) {
		ArrayList<Rock> pendingRocks = new ArrayList<Rock>();
		
		// From the center point and spans determine the coordinate of the bottom right 
		GeoPoint c = mapView.getMapCenter();
		GeoPoint br_gp = new GeoPoint(c.getLatitudeE6() - mapView.getLatitudeSpan()/2,
								   c.getLongitudeE6() + mapView.getLongitudeSpan()/2);
		// Get the projection from coordinate to screen pixels
		Projection proj = mapView.getProjection();
		
		// Get the screen pixels of bottom right, top-left = (0,0) by definition
		Point br = proj.toPixels(br_gp, null);
		
		// Make a rectangle which represents the screen
		Rect screen = new Rect(0, 0, br.x, br.y);
		
		// Find all of the rocks on the screen
		// A point for the rock position
		Point p = new Point();
		for(Rock rock : mRocks) {
			// Project the rock to screen coordinates
			proj.toPixels(new GeoPoint(rock.getLat(), rock.getLon()), p);
			
			// Make sure it is on the screen
			if(!screen.contains(p.x, p.y)) {
				continue;
			}
			
			// Make sure it is not the currently selected rock
			if(mSelectedRock.getId() == rock.getId()) {
				continue;
			}
			
			// Filter out rocks which are picked when only showing not picked rocks
			if(mShowHide == SHOW_NOT_PICKED_ROCKS && rock.isPicked()) {
				continue;
			}
			
			// Filter out rocks which are not picked when only showing picked rocks
			if(mShowHide == SHOW_PICKED_ROCKS && !rock.isPicked()) {
				continue;
			}
			
			// This rock meets all the conditions to be group. It will be rendered on the current view
			pendingRocks.add(rock);
		}
		
		// Get a new grouping of filtered rocks
		Rect markerBound = new Rect(0, 0, mDown.getWidth(), mDown.getHeight());
		mRockGroups = RockMapGroup.groupRocks(pendingRocks, markerBound, proj);
	}
	
	// Used to listen in on changes to rocks
	private class RockBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			boolean result = false;
			
			// Handle if a rock was added
			if(intent.getAction() == Rock.ACTION_ADDED) {
				result = handleAddRock(context, intent.getExtras().getInt("id"));
				
			} else if(intent.getAction() == Rock.ACTION_UPDATED) {
				result = handleUpdatedRock(context, intent.getExtras().getInt("id"));
				
			} else if (intent.getAction() == Rock.ACTION_DELETED) {
				result = handleDeletedRock(context, intent.getExtras().getInt("id"));
				
			} else if (intent.getAction() == Rock.ACTION_REVERT_MOVE) {
				result = handleRevertMove(context);
			}
			
			// Something weird happened so just lose the current list and
			// get a new one
			if(!result) {
				mRocks = Rock.getRocks(context);
			}
			
			// Make sure to re-group rocks
			mRockGroups = null;
			mMapView.postInvalidate();
		}
		
		// Add the new rock to the list
		private boolean handleAddRock(Context context, int rockId) {
			boolean result = false;
			
			Rock new_rock = Rock.getRock(context, rockId);
			
			// Make sure we really got a new rock
			if(new_rock != null) {
				mRocks.add(new_rock);
				result = true;
			} 
			
			// Make sure to re-group after adding a rock
			mRockGroups = null;
			mMapView.postInvalidate();
			
			// Select new rocks by default
			setSelected(new_rock.getId());
			
			return result;
		}
		
		// Update the edited rock in the list
		private boolean handleUpdatedRock(Context context, int rockId) {
			boolean result = false;
			
			Rock old_rock = findRockById(rockId);
			Rock new_rock = Rock.getRock(context, rockId);
			
			// Update the mSelectedRock if the selected rock was the updated one
			if(mSelectedRock.getId() == rockId) {
				mSelectedRock = new_rock;
			}
			
			// If we have both a new and old rock, then replace it
			if(old_rock.getId() != Rock.BLANK_ROCK_ID && new_rock != null) {
				mRocks.set(mRocks.indexOf(old_rock), new_rock);
				result = true;
			}
			
			if(new_rock != null) {
				// do not display rocks which are not in view because of being picked up
				if(new_rock.isPicked() && mShowHide == SHOW_NOT_PICKED_ROCKS) {
					setSelected(new Rock());
				}
				
				// do not display rocks which are not in view because of not being picked up
				if(!new_rock.isPicked() && mShowHide == SHOW_PICKED_ROCKS) {
					setSelected(new Rock());
				}
			}
			
			// Make sure to re-group after adding a rock
			mRockGroups = null;
			mMapView.postInvalidate();
			
			return result;
		}
		
		// Remove the deleted rock from the list
		private boolean handleDeletedRock(Context context, int rockId) {
			boolean result = false;
			
			Rock old_rock = findRockById(rockId);
			
			// See if the rocked that was deleted is the currently selected rock
			if(old_rock.getId() == mSelectedRock.getId()) {
				setSelected(new Rock());
			}
			
			// Make sure the deleted rock was in the list
			if(old_rock.getId() != Rock.BLANK_ROCK_ID) {
				mRocks.remove(old_rock);
				result = true;
			}
			
			// Make sure to re-group after adding a rock
			mRockGroups = null;
			mMapView.postInvalidate();
			
			return result;
		}
		
		// Revert the last move and if there is not one to revert de select the rock
		private boolean handleRevertMove(Context context) {
			if(mSelectedRock.getId() == Rock.BLANK_ROCK_ID) {
				setSelected(new Rock());
			} else {
				if(mSelectedRock.getActualLat() != mSelectedRock.getLat() ||
					mSelectedRock.getActualLon() != mSelectedRock.getLon()) {
					mSelectedRock.setLat(mSelectedRock.getActualLat());
					mSelectedRock.setLon(mSelectedRock.getActualLon());
				} else {
					setSelected(new Rock());
				}
			}
			
			return true;
		}
	}
}
