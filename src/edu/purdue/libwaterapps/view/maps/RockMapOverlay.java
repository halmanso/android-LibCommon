package edu.purdue.libwaterapps.view.maps;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import edu.purdue.libwaterapps.rock.Rock;

public class RockMapOverlay extends ItemizedOverlay<OverlayItem> {
	private Context context;
	private ArrayList<Rock> rocks;
	private ArrayList<Rock> rocksToShow;
	private int selectedId;
	private MapView mapView;
	private int showHide;
	private RockBroadcastReceiver rockBroadcastReceiver;
	private GestureDetector gestureDetector = null;
	private RockMapOverlayGestureListener gestureListener = null;
	
	// Enums for defines of what rocks to show
	public static final int SHOW_ALL_ROCKS = 1;
	public static final int SHOW_NOT_PICKED_ROCKS = 2;
	public static final int SHOW_PICKED_ROCKS = 3;
	
	public RockMapOverlay(Context context) {
		super(null);
		
		this.context = context;
		
		// Get the list of rocks
		rocks = Rock.getRocks(context);

		selectedId = Rock.BLANK_ROCK_ID;
		
		// By default show just not picked rocks
		showHide = SHOW_NOT_PICKED_ROCKS;
		
		// Known work around to Android ArrayIndexOutOfBounds exception when
		// list is empty but added to a MapView
		setLastFocusedIndex(-1);
		populate();
	}
	
	
	/* This is called by Android after a call to populate. It is asking 
	 * for each OverlayItem individually to draw them */
	@Override
	protected OverlayItem createItem(int i) {
		return new RockOverlayItem(rocksToShow.get(i));
	}
	
	/* 
	 * This is called by Android to get the size of the overlay list
	 * so that it can safely call createItem() 
	 */
	@Override
	public int size() {
		// Build the list of rocks to show on the map. This is controlled by the showHide parameter
		if(showHide == SHOW_ALL_ROCKS) {
			rocksToShow = rocks;
		} else {
			rocksToShow = new ArrayList<Rock>();
			for(Rock rock : rocks) {
				switch(showHide) {
					case SHOW_PICKED_ROCKS:
						if(rock.isPicked()) {
							rocksToShow.add(rock);
						}
						
					break;
					
					default:
					case SHOW_NOT_PICKED_ROCKS:
						if(!rock.isPicked()) {
							rocksToShow.add(rock);
						}
						
					break;
				}
			}
		}
		
		return rocksToShow.size();
	}
	
	/* 
	 * Provides the index of the overlay item which was last touched. 
	 */
	@Override
	protected boolean onTap(int i) {
		setSelected(((RockOverlayItem)getItem(i)).getRock().getId());
		
		return true;
	}
	
	/*
	 * Get notified that the map was tapped
	 */
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		// Make sure a overlay item was selected
		if(!super.onTap(p, mapView)) {
			setSelected(Rock.BLANK_ROCK_ID);
		}
		
		return true;
	}
	
	/* 
	 * Stop the overlay from having shadows 
	 */
	@Override
	public void	draw(Canvas canvas, MapView mapView, boolean shadow) {
		// Store the mapView so we can force invalidates
		this.mapView = mapView;
		
		if(!shadow) {
			super.draw(canvas, mapView, shadow);
		}
	}
	
	/*
	 * A overlay item gained focus. Tell the world if they are listening
	 */
	@Override
	public void setFocus(OverlayItem item) {
		super.setFocus(item);
		
		Intent intent = new Intent(Rock.ACTION_SELECTED);
		intent.putExtra("id", selectedId);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	
	}

	/* 
	 * Provides the movement events so we can move rocks around 
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		// Make the gesture detector if it does not already exist
		if(gestureDetector == null) {
			gestureListener = new RockMapOverlayGestureListener(mapView);
			gestureDetector = new GestureDetector(mapView.getContext(), gestureListener);
			gestureDetector.setOnDoubleTapListener(gestureListener);
		}
		
		// Let the gesture detector handle the touch event
		boolean result = gestureDetector.onTouchEvent(event);
		
		// If event is not handled and it is a move action feed it back into the listener (gestureListener's don't react to simple MOVE's)
		if(!result && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
			result = gestureListener.onMove(event);
		}
		
		return result;
	}
	/*
	
	 * Will listen to changes in the rocks and keep the overlay update to date.
	 * If you listen to rock changes you must stopListeningToRockChanges() in the applications onPause()
	 */
	public void registerListeners() {
		if(rockBroadcastReceiver == null) {
			rockBroadcastReceiver = new RockBroadcastReceiver();
			
			LocalBroadcastManager.getInstance(context).registerReceiver(rockBroadcastReceiver, new IntentFilter(Rock.ACTION_ADDED));
			LocalBroadcastManager.getInstance(context).registerReceiver(rockBroadcastReceiver, new IntentFilter(Rock.ACTION_UPDATED));
			LocalBroadcastManager.getInstance(context).registerReceiver(rockBroadcastReceiver, new IntentFilter(Rock.ACTION_DELETED));
		}
	}
	
	/*
	 * Stop listening to changes in the rocks and updating the overlay automatically 
	 */
	public void unregisterListeners() {
		if(rockBroadcastReceiver != null) {
			LocalBroadcastManager.getInstance(context).unregisterReceiver(rockBroadcastReceiver);
			
			rockBroadcastReceiver = null;
		}
		
	}
	
	/*
	 * Change the current showHide setting
	 */
	public void setShowHide(int type) {
		// Validate change
		switch(type) {
			case SHOW_ALL_ROCKS:
			case SHOW_NOT_PICKED_ROCKS:
			case SHOW_PICKED_ROCKS:
				showHide = type;
			break;
			
			default:
				showHide = SHOW_ALL_ROCKS;
			break;
		}
		
		setLastFocusedIndex(-1);
		populate();
	}
	
	/*
	 * Return the current showHide setting
	 */
	public int getShowHide() {
		return showHide;
	}
		
	/*
	 * Return the selected selected rock 
	 */
	public Rock getSelected() {
		return findRockById(selectedId);
	}
	
	/*
	 * Return the OverlayItem for the selected rock 
	 */
	public OverlayItem getSelectedOverlayItem() {
		return getOverlayItem(getSelected());
	}
	
	/*
	 * Return the OverlayItem for the a given rock (if there is one)
	 */
	public OverlayItem getOverlayItem(Rock rock) {
		int i = rocksToShow.indexOf(rock);
		
		if(i >= 0) {
			return getItem(i);
		}
		
		return null;
	}
	
	/*
	 * Set the selected rock by index in it the rocks list.
	 * Set to Rock.BLACK_ROCK_ID to select no rock
	 */
	public void setSelected(int rockId) {
		// Search of the rock in the list
		int i = rocksToShow.indexOf(findRockById(rockId));
			
		if(i < 0) {
			selectedId = Rock.BLANK_ROCK_ID;
			setFocus(null);
			
		} else {
			selectedId = rockId;
			setFocus(getItem(i));
		}
	}
	
	/*
	 * Return a rock from its Rock ID
	 */
	private Rock findRockById(int rockId) {
		if(rockId >= 0 && rockId != Rock.BLANK_ROCK_ID) {
			for(Rock rock : rocks) {
				if(rock.getId() == rockId) {
					return rock;
				}
			}
		}
		
		return null;
	}
	
	
	/*
	 * A Listener class which listens for interaction with map to delegate moving overlays around
	 */
	private class RockMapOverlayGestureListener implements OnGestureListener, OnDoubleTapListener {
		
		RockMapOverlayGestureListener(MapView mapView) {
			super();
		}

		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}

		/*
		 * When a double tap is detected see if it hits the selected overlay, and if it does tell the interested world
		 */
		public boolean onDoubleTap(MotionEvent e) {
			final int x = (int)e.getX();
			final int y = (int)e.getY();
			
			// Get the selected overlay item
			OverlayItem currentOverlay = getSelectedOverlayItem();
			
			// Make sure we found an overlay item
			if(currentOverlay == null) {
				return false;
			}
			
			// Translate the overlay item's coordinates to screen x,y
			Point overlayPoint = new Point();
			mapView.getProjection().toPixels(currentOverlay.getPoint(), overlayPoint);
			
			// See if the double tap was on the marker or not
			if(hitTest(currentOverlay, currentOverlay.getMarker(OverlayItem.ITEM_STATE_SELECTED_MASK), 
						x-overlayPoint.x, y-overlayPoint.y)) {
				
				// Tell the interested world
				Intent intent = new Intent(Rock.ACTION_DOUBLE_TAP);
				intent.putExtra("id", getSelected().getId());
				LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				
				return true;
			}
			
			// Do Nothing
			return false;
		}
	
		/*
		 * When a long press is detected see if it hits an overlay, and if it does tell the interested world
		 */
		public void onLongPress(MotionEvent e) {
			final int x = (int)e.getX();
			final int y = (int)e.getY();
			
			// Look for a rock (in all of the rocks showing) that may have been long held
			for(Rock rock : rocksToShow) {
				OverlayItem overlay = getOverlayItem(rock);
				
				// Make sure this rock as a overlay item on the screen
				if(overlay == null) {
					continue;
				}
				
				// Translate the overlay item's coordinates to screen x,y
				Point overlayPoint = new Point();
				mapView.getProjection().toPixels(overlay.getPoint(), overlayPoint);
				
				// See if the double tap was on the marker or not
				if(hitTest(overlay, overlay.getMarker(OverlayItem.ITEM_STATE_SELECTED_MASK), 
							x-overlayPoint.x, y-overlayPoint.y)) {
					
					// Tell the interested world
					Intent intent = new Intent(Rock.ACTION_LONG_HOLD);
					intent.putExtra("id", rock.getId());
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
					
					return;
				}
			}
		}
	
		public boolean onMove(MotionEvent event) {
			
			return false;
		}
		
		public boolean onDown(MotionEvent e) {
			return false;
		}
		
		public boolean onScroll(MotionEvent startEvent, MotionEvent currentEvent, float dX, float dY) {
			// Do nothing
			return false;
		}
	
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// Do nothing
			return false;
		}
	
		public void onShowPress(MotionEvent e) {
			// Do nothing
		}
	
		public boolean onSingleTapUp(MotionEvent e) {
			// Do nothing
			return false;
		}

		public boolean onSingleTapConfirmed(MotionEvent e) {
			// Do Nothing
			return false;
		}
	}
		
	/*
	 * A class which handles translating a rock to a OverlayItem
	 */
	class RockOverlayItem extends OverlayItem {
		private Rock rock;
		
		public RockOverlayItem(Rock rock) {
			super(new GeoPoint(rock.getLat(), rock.getLon()), "Rock", "Rock");
			
			this.rock = rock;
		}

		/*
		 * Returns the specific marker which will be used to render on the map for this overlay item
		 */
		@Override  
		public Drawable getMarker(int stateBitset) {  
			Drawable marker;
			
			// Choose between a picked or non-picked picture
			marker = boundCenter(rock.getDrawable());
			
			// Determine if the picture should be selected or not
			if(selectedId == rock.getId()) {
				OverlayItem.setState(marker, OverlayItem.ITEM_STATE_SELECTED_MASK);
			} else {
				OverlayItem.setState(marker, 0);
			}
			
			return marker;
		}
		
		// Get the rock which this is an overlay item for
		public Rock getRock() {
			return rock;
		}
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
				
			}
			
			// Something weird happened so just lose the current list and
			// get a new one
			if(!result) {
				rocks = Rock.getRocks(context);
			}
			
			// Update the map view
			setLastFocusedIndex(-1);
			populate();
			mapView.postInvalidate();
		}
		
		// Add the new rock to the list
		private boolean handleAddRock(Context context, int rockId) {
			boolean result = false;
			
			Rock new_rock = Rock.getRock(context, rockId);
			
			// Make sure we really got a new rock
			if(new_rock != null) {
				rocks.add(new_rock);
				result = true;
			} 
				
			return result;
		}
		
		// Update the edited rock in the list
		private boolean handleUpdatedRock(Context context, int rockId) {
			boolean result = false;
			
			Rock old_rock = findRockById(rockId);
			Rock new_rock = Rock.getRock(context, rockId);
			
			// If we have both a new and old rock, then replace it
			if(old_rock != null && new_rock != null) {
				rocks.set(rocks.indexOf(old_rock), new_rock);
				result = true;
			}
			
			return result;
		}
		
		// Remove the deleted rock from the list
		private boolean handleDeletedRock(Context context, int rockId) {
			boolean result = false;
			
			Rock old_rock = findRockById(rockId);
			
			// Make sure the deleted rock was in the list
			if(old_rock != null) {
				rocks.remove(old_rock);
				result = true;
			}
			
			return result;
		}
	}
}
