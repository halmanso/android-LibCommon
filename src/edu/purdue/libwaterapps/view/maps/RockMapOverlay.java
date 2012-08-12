package edu.purdue.libwaterapps.view.maps;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import edu.purdue.libwaterapps.rock.Rock;
import edu.purdue.libwaterapps.utils.NotifyArrayList;

public class RockMapOverlay extends ItemizedOverlay<OverlayItem> {
	private NotifyArrayList<Rock> rocks;
	private Drawable pickedMarker;
	private Drawable notPickedMarker;
	private Rock current;
	private GestureDetector gestureDetector = null;
	private RockMapOverlayGestureListener gestureListener = null;
	
	public RockMapOverlay(NotifyArrayList<Rock> rocks, Drawable notPickedMarker, Drawable pickedMarker) {
		super(notPickedMarker);
		
		// Make markers positioned at the image center
		this.notPickedMarker = boundCenter(notPickedMarker);
		this.pickedMarker = boundCenter(pickedMarker);
		
		this.rocks = rocks;
		// Update the overlay on rock changes
		this.rocks.addOnListChangeListener(new OnListChangeListener());
		
		// Start with no current rock
		this.current = new Rock();
		
		// Known work around to Android ArrayIndexOutOfBounds exception when
		// list is empty but added to a MapView
		this.populate();
	}
	
	
	/* This is called by Android after a call to populate. It is asking 
	 * for each OverlayItem individually to draw them */
	@Override
	protected OverlayItem createItem(int i) {
		return new RockOverlayItem(rocks.get(i));
	}
	
	/* This is called by Android to get the size of the overlay list
	 * so that it can safely call createItem() */
	@Override
	public int size() {
		return rocks.size();
	}
	
	/* Provides the index of the overlay item which was last touched. */
	@Override
	protected boolean onTap(int i) {
		this.current = rocks.get(i);
		
		return true;
	}
	
	/* Stop the overlay from having shadows */
	@Override
	public void	draw(Canvas canvas, MapView mapView, boolean shadow) {
		if(!shadow) {
			super.draw(canvas, mapView, shadow);
		}
	}
	
	/*
	 * Return the current selected rock 
	 */
	public Rock getCurrent() {
		return this.current;
	}
	
	/*
	 * Return the OverlayItem for the current selected rock 
	 */
	public OverlayItem getCurrentOverlayItem() {
		int i = rocks.indexOf(this.current);
		
		if(i >= 0) {
			return getItem(i);
		}
		
		return null; 
	}
	
	/*
	 * Set the current selected rock by index in it the rocks list
	 */
	public void setCurrent(int i) {
		if(i >= 0 || i < rocks.size()) {
			this.current = rocks.get(i); 
		} else {
			this.current = new Rock();
		}
		
		// Make this rock the new focus
		updateFocus();
	}
	
	/*
	 * Set the current rock by rock object
	 */
	public void setCurrent(Rock rock) {
		if(rock != null) {
			Rock internalRock = findRockById(rock.getId());
		
			if(internalRock != null) {
				this.current = internalRock;
			} else {
				this.current = new Rock();
			}
		} else {
			this.current = new Rock();
		}
		
		// Make this rock the new focus
		updateFocus(); 
	}
	
	/*
	 * Return a rock from its Rock ID
	 */
	private Rock findRockById(int id) {
		for(Rock rock : rocks) {
			if(rock.getId() == id) {
				return rock;
			}
		}
		
		return null;
	}
	
	/*
	 * Updates the focused overlay to the current selected rock
	 */
	private void updateFocus() {
		int i = rocks.indexOf(this.current);
		
		if(i < 0) {
			setFocus(null);
		} else {
			setFocus(getItem(i));
		}
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
	 * A Listener class which listens for interaction with map to delegate moving overlays around
	 */
	private class RockMapOverlayGestureListener implements OnGestureListener, OnDoubleTapListener {
		MapView mapView;
		boolean inDrag;
		
		RockMapOverlayGestureListener(MapView mapView) {
			super();
			this.mapView = mapView;
			inDrag = false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			
			return true;
			
			/*
			final int x=(int)e.getX();
			final int y=(int)e.getY();
			
			OverlayItem currentOverlay = getCurrentOverlayItem();
			
			switch(e.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					if(currentOverlay != null) {
						Point overlayPoint = new Point();
						mapView.getProjection().toPixels(currentOverlay.getPoint(), overlayPoint);
				
						if(hitTest(currentOverlay, currentOverlay.getMarker(OverlayItem.ITEM_STATE_SELECTED_MASK), 
								x-overlayPoint.x, y-overlayPoint.y)) {
							inDrag = true;
							FrameLayout fl = new FrameLayout(mapView.getContext());
							fl.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
							fl.setForegroundGravity(0x77);
							fl.setForeground(new ColorDrawable(R.color.rock_move_background));
							mapView.addView(fl);
							
							return true;
						}
					}
				break;
				
				case MotionEvent.ACTION_MOVE:
					if(inDrag) {
						
						GeoPoint pos = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
						
						Rock current = getCurrent();
						current.setLat(pos.getLatitudeE6());
						current.setLon(pos.getLongitudeE6());
						current.save();
						
						// TODO: Remove after adding callback to rocks to update the list on rock saves
						setLastFocusedIndex(-1);
						populate();
						
						return true;
					}
				break;
				
				case MotionEvent.ACTION_UP:
					inDrag = false;
					return true;
			}
			
			return false;
			*/
		}
	
		/*
		 * When a long press is detected, check to see if it hits the currently selected rock and if it does are the drag process
		 */
		@Override
		public void onLongPress(MotionEvent e) {
		}
	
		/*
		 * When a MOVE action is fed in, check to see if we are in drag mode and if we are then move the current overlay 
		 */
		public boolean onMove(MotionEvent event) {
			
			return false;
		}
		
		/*
		 * If a Down action is observed then we are done with a drag event is there is one so end it
		 */
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}
		
		@Override
		public boolean onScroll(MotionEvent startEvent, MotionEvent currentEvent, float dX, float dY) {
			// Do nothing
			return false;
		}
	
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// Do nothing
			return false;
		}
	
		@Override
		public void onShowPress(MotionEvent e) {
			// Do nothing
		}
	
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// Do nothing
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// Do Nothing
			return false;
		}

		@Override
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
			if(rock.isPicked()) {
				marker = pickedMarker;
			} else {
				marker = notPickedMarker;
			}
			
			// Determine if the picture should be selected or not (current or not)
			if(current.getId() == rock.getId()) {
				OverlayItem.setState(marker, OverlayItem.ITEM_STATE_SELECTED_MASK);
			} else {
				OverlayItem.setState(marker, 0);
			}
			
			return marker;
		}
	}
	
	/*
	 * A class which listens to changes in the rock list and updates the overlay
	 */
	private class OnListChangeListener implements NotifyArrayList.OnListChangeListener<Rock> {
		@Override
		public void onAdd(Rock rock) {
			// Known work around to Android ArrayIndexOutOfBounds exception when
			// list is empty but added to a MapView
			RockMapOverlay.this.setLastFocusedIndex(-1);
			
			// Update the display after changing the list
			RockMapOverlay.this.populate();
		}

		@Override
		public void onRemove(Rock rock) {
			// Known work around to Android ArrayIndexOutOfBounds exception when
			// list is empty but added to a MapView
			RockMapOverlay.this.setLastFocusedIndex(-1);
			
			// Update the display after changing the list
			RockMapOverlay.this.populate();
		}
	}
}
