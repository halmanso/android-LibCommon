package edu.purdue.libcommon.utils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class MapPosition {
	private GeoPoint mMapCenter;
	private int mLatSpan;
	private int mLonSpan;
	
	// Create an empty mapPosition
	public MapPosition() {
		mMapCenter = new GeoPoint(0,0);
		mLatSpan = -1;
		mLonSpan = -1;
	}
	
	// Create a mapPosition object
	public MapPosition(GeoPoint mapCenter, int latSpan, int lonSpan) {
		// Get our own local version of mapCenter
		mMapCenter = new GeoPoint(mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6());
		
		mLatSpan = latSpan;
		mLonSpan = lonSpan;
	}
	
	public GeoPoint getMapCenter() {
		return mMapCenter;
	}
	
	public void setMapCenter(GeoPoint mapCenter) {
		mMapCenter = new GeoPoint(mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6());
	}
	
	public int getLatSpan() {
		return mLatSpan;
	}
	
	public void setLatSpan(int latSpan) {
		mLatSpan = latSpan;
	}
	
	public int getLonSpan() {
		return mLonSpan;
	}
	
	public void setLonSpan(int lonSpan) {
		mLonSpan = lonSpan;
	}
	
	// From raw data determine if the the map "has moved"
	public boolean hasMoved(GeoPoint mapCenter, int latSpan, int lonSpan) {
		if(mMapCenter == null || !mMapCenter.equals(mapCenter) ||
			mLatSpan != latSpan || mLonSpan != lonSpan) {
			return true;
		}
		
		return false;
	}
	
	// From another MapPosition determine if map "has moved"
	public boolean hasMoved(MapPosition mapPosition2) {
		GeoPoint mapCenter2 = mapPosition2.getMapCenter();
		
		if(mapCenter2 == null || mMapCenter == null ||
		   !mMapCenter.equals(mapCenter2) ||
		   mLatSpan != mapPosition2.getLatSpan() ||
		   mLonSpan != mapPosition2.getLonSpan()) {
			return true;
		}
		
		return false;
	}
	
	// From a map view determine if the map "has moved"
	public boolean hasMoved(MapView mapView) {
		return hasMoved(mapView.getMapCenter(), mapView.getLatitudeSpan(), mapView.getLongitudeSpan());
	}
}
