package edu.purdue.libwaterapps.view.maps;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.example.libwaterapps.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import edu.purdue.libwaterapps.rock.Rock;

public class RockOverlay extends Overlay {
	Drawable mRockDrawable;
	Bitmap mRockSelected;
	Bitmap mRockUnSelected;
	Context mContext;

	public RockOverlay(Context context, Drawable rockDrawable) {
		mRockDrawable = rockDrawable;
		mContext = context;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		Bitmap shadowBitmap = null;
		// Disable shadows for now
		if(shadow) {
			Drawable shadowDrawable = mContext.getResources().getDrawable(R.drawable.rock_shadow);
			shadowBitmap = Bitmap.createBitmap(shadowDrawable.getIntrinsicWidth(), shadowDrawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas rockCanvas = new Canvas(shadowBitmap);
			shadowDrawable.setBounds(0, 0, rockCanvas.getWidth(), rockCanvas.getHeight());
			shadowDrawable.draw(rockCanvas);
		} else {
			//int[] state = {android.R.attr.state_selected};
			//mRockDrawable.setState(state);
			
			mRockSelected = Bitmap.createBitmap(mRockDrawable.getIntrinsicWidth(), mRockDrawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas rockCanvas = new Canvas(mRockSelected);
			mRockDrawable.setBounds(0, 0, rockCanvas.getWidth(), rockCanvas.getHeight());
			mRockDrawable.draw(rockCanvas);
		}
		
		ArrayList<Rock> rocks = Rock.getNotPickedRocks(mContext);
		
		Point p = new Point();
		for(Rock rock : rocks) {
			mapView.getProjection().toPixels(new GeoPoint(rock.getLat(), rock.getLon()), p);
			
			if(shadow) {
				canvas.drawBitmap(shadowBitmap, p.x - shadowBitmap.getWidth()/2 + 14, p.y - shadowBitmap.getHeight()/2 - 4, null);
			} else {
				canvas.drawBitmap(mRockSelected, p.x - mRockSelected.getWidth()/2, p.y - mRockSelected.getHeight()/2, null);
				
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(Color.WHITE);
				paint.setTextAlign(Paint.Align.CENTER);
				paint.setTextSize(20);
				paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
				paint.setTextSkewX(-0.2f);
				canvas.drawText("3", p.x-1, p.y+3, paint);
			}
		}
	}
	
	public void setFocus(Rock rock) {
		
	}
	
	public int getShowHide() {
		return 1;
	}
	
	public void setShowHide(int i) {
		
	}
	
	public void setSelected(int id) {
		
	}
	
	public Rock getSelected() {
		return new Rock();
	}
	
	public void registerListeners() {
		
	}
	
	public void unregisterListeners() {
		
	}

}
