package edu.purdue.libcommon.view.maps;

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

import com.example.libcommon.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import edu.purdue.libcommon.rock.Rock;
import edu.purdue.libcommon.utils.MapPosition;

public class RockMapOverlay extends Overlay {
	private MapView mMapView;
	private Bitmap mUp;
	private Bitmap mDown;
	private Bitmap mUpDown;
	private Bitmap mShadow;
	private Bitmap mSelected;
	private ArrayList<Rock> mRocks;
	private ArrayList<RockMapGroup> mRockGroups;
	private MapPosition mLastMapPosition;
	
	private int mSelectedRockId;
	private int mSelectedRockLat;
	private int mSelectedRockLon;
	
	private int mShowHide; 
	
	private int movePointerId;
	private boolean mMoveInAction = false;
	private boolean mMoveFreely = false;
	private boolean mDragging = false; 
	
	private Context mContext;
	private LocalBroadcastReceiver mLocalBroadcastReceiver;
	
	public static final int SHOW_ALL_ROCKS = 1;
	public static final int SHOW_NOT_PICKED_ROCKS = 2;
	public static final int SHOW_PICKED_ROCKS = 3;

	private static final int DRAG_DEAD_ZONE = 75;
	private static final int SHADOW_OFFSET_X = 10;
	private static final int SHADOW_OFFSET_Y = 10;
	
	public static final String ACTION_GROUP_SELECTED = "edu.purdue.libcommon.view.maps.GROUP_SELECTED";
	public static final String ACTION_ROCK_SELECTED = "edu.purdue.libcommon.view.maps.ROCK_SELECTED";
	public static final String ACTION_REVERT_MOVE = "edu.purdue.libcommon.view.maps.REVERT_MOVE";
	
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
		mSelectedRockId = -1;
		
		// Initialize the last map position
		mLastMapPosition = new MapPosition(); 
		
		// Initialize the default show hide
		mShowHide = SHOW_NOT_PICKED_ROCKS;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		// See if we have no groupings or if the map has moved and so we need to (re)filter and group rocks
		if(mRockGroups == null || mLastMapPosition.hasMoved(mapView)) {
			filterAndGroupRocks(mapView);
		}
		 
		// Print all of the rocks in the mRockGroups
		Point p = new Point();
		for(RockMapGroup group : mRockGroups) {
			mapView.getProjection().toPixels(new GeoPoint(group.getAvgLat(), group.getAvgLon()), p);
		
			drawGroup(group, p, canvas, shadow);
		}
		
		// Draw the selected rock last. It is never part of mRockGroups
		if(mSelectedRockId > 0) { 
			RockMapGroup group = new RockMapGroup(Rock.getRock(mContext, mSelectedRockId));
			mapView.getProjection().toPixels(new GeoPoint(mSelectedRockLat, mSelectedRockLon), p);
			drawGroup(group, p, canvas, shadow);
		}
	}
	
	// Helper function which knows the details of drawing a group 
	private void drawGroup(RockMapGroup group, Point p, Canvas canvas, boolean shadow) {
		if(shadow) {
			// Draw the shadow first so that its on the bottom
			canvas.drawBitmap(mShadow, p.x - mShadow.getWidth()/2 + SHADOW_OFFSET_X, p.y - mShadow.getHeight()/2 + SHADOW_OFFSET_Y, null);
		} else {
			// Draw the actual rock icons (or rock group icons)
			// If this rock is selected then draw the select bubble
			if(group.size() == 1) {
				Rock rock = group.getAll().get(0);
				if(rock != null && mSelectedRockId == rock.getId()) {
					canvas.drawBitmap(mSelected, p.x - mSelected.getWidth()/2, p.y - mSelected.getHeight()/2, null);
				}
			}
			
			// Determine what the make up of the group is
			Bitmap marker;
			switch(group.getGroupStatus()) {
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
				// This is not a tap, there was a drag motion
				mDragging = true;
				
				// See if we got a "move" for a finger we were tracking
				int idx = e.findPointerIndex(movePointerId);
				if(mMoveInAction && idx != -1) {
					
					// If we are in a move then just track the finger
					if(mMoveFreely) {
						GeoPoint gp = mapView.getProjection().fromPixels((int)e.getX(idx), (int)e.getY(idx));
						
						mSelectedRockLat = gp.getLatitudeE6();
						mSelectedRockLon = gp.getLongitudeE6();
						
						// Otherwise see if we should start a move yet
					} else {
						Point p = mapView.getProjection().toPixels(new GeoPoint(mSelectedRockLat, mSelectedRockLon), null);
						if(Math.hypot(p.x - e.getX(), p.y - e.getY()) > DRAG_DEAD_ZONE) {
							mMoveFreely = true;
						}
					}
					
					return true;
				}
			break;
			
			case MotionEvent.ACTION_DOWN:
				// Starting a new touch, not sure if its a drag yet or not
				mDragging = false;
				
				// Look for a rock which we pushed down on 
				Point p = new Point();
				RectF markerBoundary = new RectF(0, 0, mSelected.getWidth(), mSelected.getHeight());
	
				// First test to see if we reselected the selected rock
				if(mSelectedRockId > 0) {
					mapView.getProjection().toPixels(new GeoPoint(mSelectedRockLat, mSelectedRockLon), p);
					markerBoundary.offsetTo(p.x, p.y);
					markerBoundary.offset(-mDown.getWidth()/2, -mDown.getHeight()/2);
					
					if(markerBoundary.contains(e.getX(), e.getY())) {
						// Start tracking the finger 
						mMoveInAction = true;
						movePointerId = e.getPointerId(0);
						
						return true;
					}
				}
				
				// Otherwise see if we picked any of the rocks
				markerBoundary = new RectF(0, 0, mDown.getWidth(), mDown.getHeight());
				
				for(RockMapGroup group : mRockGroups) {
					mapView.getProjection().toPixels(new GeoPoint(group.getAvgLat(), group.getAvgLon()), p);
					
					markerBoundary.offsetTo(p.x, p.y);
					markerBoundary.offset(-mDown.getWidth()/2, -mDown.getHeight()/2);
					
					if(markerBoundary.contains(e.getX(), e.getY())) {
						// Save the rock which was selected before
						if(mSelectedRockId > 0) {
							// Save the current one 
							// First we need to commit to these new lat and lon
							Rock rock = Rock.getRock(mContext, mSelectedRockId);
							rock.setLat(mSelectedRockLat);
							rock.setLon(mSelectedRockLon);
							rock.save();
						}
						
						// Send out a group select broadcast if we selected one
						if(group.size() > 1) {
							
							// If we are at the highest zoom level then just select the top
							// on of the group so that you can at least work with it
							if(mapView.getZoomLevel() == 21) {
								Rock rock = group.getAll().get(0);
								if(rock != null) {
									setSelected(rock.getId());
								}
							} else {
								// Send a broadcast to who ever cares that the user selected a group
								Intent intent = new Intent(ACTION_GROUP_SELECTED);
								intent.putExtra("lat", group.getAvgLat());
								intent.putExtra("lon", group.getAvgLon());
								intent.putExtra("lat-span", group.getLatSpan());
								intent.putExtra("lon-span", group.getLonSpan());
								LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
								
								// We do not know what to select so we do not select anything
								setSelected(-1);
							}
						} else {
							// We selected a single rock so we select that
							Rock rock = group.getAll().get(0);
							if(rock != null) {
								setSelected(rock.getId());
							}	
							
							// We just selected the rock so act as if we are dragging it
							mDragging = true;
						}
						
						return true;
					}
				}
			break;
			
			case MotionEvent.ACTION_UP:
				// We pushed down somewhere else (without dragging) and we have a currently selected rock, cancel the selection
				if(mSelectedRockId > 0 && !mDragging) {
					Rock rock = Rock.getRock(mContext, mSelectedRockId);
					rock.setLat(mSelectedRockLat);
					rock.setLon(mSelectedRockLon);
					rock.save();
					
					setSelected(-1);
				}
				
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
		// Save the rock object
		mSelectedRockId = id;
		
		// Tell the world 
		Intent intent = new Intent(ACTION_ROCK_SELECTED);
		intent.putExtra("id", mSelectedRockId);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
		
		// Store the original location of the rock for later when rendering
		Rock rock = getSelected();
		if(rock != null) {
			mSelectedRockLat = rock.getLat();
			mSelectedRockLon = rock.getLon();
		}
		
		// Force a re-group
		mRockGroups = null;
		mMapView.postInvalidate();
	}
	
	// Return the currently selected rock
	public Rock getSelected() {
		return Rock.getRock(mContext, mSelectedRockId);
	}
	
	// Start listening to changes in rocks so we can stay up to date
	public void registerListeners() {
		if(mLocalBroadcastReceiver == null) {
			mLocalBroadcastReceiver = new LocalBroadcastReceiver();
			
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mLocalBroadcastReceiver, new IntentFilter(Rock.ACTION_ADDED));
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mLocalBroadcastReceiver, new IntentFilter(Rock.ACTION_UPDATED));
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mLocalBroadcastReceiver, new IntentFilter(Rock.ACTION_DELETED));
			LocalBroadcastManager.getInstance(mContext).registerReceiver(mLocalBroadcastReceiver, new IntentFilter(ACTION_REVERT_MOVE));
			
			// Refresh our rock list because we might have missed messages when it was off
			mRocks = Rock.getRocks(mContext);
			mRockGroups = null;
			mMapView.postInvalidate();
		}
	}
	
	// Stop listening to changes in rocks
	public void unregisterListeners() {
		if(mLocalBroadcastReceiver != null) {
			LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mLocalBroadcastReceiver);
			
			mLocalBroadcastReceiver = null;
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
			
			// Filter out rocks which are picked when only showing not picked rocks
			if(mShowHide == SHOW_NOT_PICKED_ROCKS && rock.isPicked()) {
				continue;
			}
			
			// Filter out rocks which are not picked when only showing picked rocks
			if(mShowHide == SHOW_PICKED_ROCKS && !rock.isPicked()) {
				continue;
			}
			
			// Make sure it is not the currently selected rock
			if(mSelectedRockId == rock.getId()) {
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
	private class LocalBroadcastReceiver extends BroadcastReceiver {
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
				
			} else if (intent.getAction() == ACTION_REVERT_MOVE) {
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
			
			// If we have both a new and old rock, then replace it
			if(old_rock.getId() > 0 && new_rock != null) {
				mRocks.set(mRocks.indexOf(old_rock), new_rock);
				result = true;
			}
			
			if(new_rock != null && new_rock.getId() == mSelectedRockId) {
				// do not display rocks which are not in view because of being picked up
				if(new_rock.isPicked() && mShowHide == SHOW_NOT_PICKED_ROCKS) {
					setSelected(-1);
				}
				
				// do not display rocks which are not in view because of not being picked up
				if(!new_rock.isPicked() && mShowHide == SHOW_PICKED_ROCKS) {
					setSelected(-1);
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
			if(old_rock.getId() == mSelectedRockId) {
				setSelected(-1);
			}
			
			// Make sure the deleted rock was in the list
			if(old_rock.getId() > 0) {
				mRocks.remove(old_rock);
				result = true;
			}
			
			// Make sure to re-group after adding a rock
			mRockGroups = null;
			mMapView.postInvalidate();
			
			return result;
		}
		
		// Revert the last move and if there is not one to revert cancel the current rock selection
		private boolean handleRevertMove(Context context) {
			Rock rock = getSelected();
			
			if(rock != null && (rock.getLat() != mSelectedRockLat || rock.getLon() != mSelectedRockLon)) {
				mSelectedRockLat = rock.getLat();
				mSelectedRockLon = rock.getLon();
			} else {
				setSelected(-1);
			}
			
			return true;
		}
	}
}
