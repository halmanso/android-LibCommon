package edu.purdue.libwaterapps.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class RockMoveView extends SurfaceView implements Runnable {
	private boolean running = false;
	private Thread thread = null;
	private SurfaceHolder surfaceHolder;
	
	public RockMoveView(Context context) {
		super(context);
		
		// Get the surface's holder
		surfaceHolder = getHolder();
		
		// Make sure the surface is top most so transparency will work
		setZOrderOnTop(true);
		
		// Set the surface format to Translucent to background view can shin through
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
	}
	
	public boolean isRunning() {
		return running;
	}
	
	// Start the thread that updates the drawing
	public void startDrawing() {
		
		thread = new Thread(this);
		
		running = true;
		thread.start();
	}
	
	// Stop the thread that is updating the drawing
	public void stopDrawing() {
		boolean isDead = false;
		
		running = false; 
		while(!isDead) {
			try {
				thread.join();
				isDead = true;
			} catch(InterruptedException e) {
				/* Do nothing */
			}
		}
		
		// Clear the polygon from the surface
		Canvas canvas = surfaceHolder.lockCanvas();
		//canvas.drawColor(R.color.rock_move_background);
		canvas.drawARGB(100, 255, 0, 0);
//		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	public void run() {
		while(running) {
			Canvas canvas = surfaceHolder.lockCanvas();
			
			if(canvas == null) {
				continue;
			}
			
			canvas.drawARGB(100, 255, 0, 0);
		//	canvas.drawColor(R.color.rock_move_background);
//			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			
			/*
			if(!polygon.isEmpty()) {
				Path path = new Path();
				
				path.moveTo(polygon.get(0).x, polygon.get(0).y);
				for(Point p : polygon) {
					path.lineTo(p.x, p.y);
					canvas.drawPoint(p.x, p.y, paintFixed);
				}
				
				canvas.drawPath(path, paintFixed);
				
				if(possNextPoint != null) {
					path = new Path();
					
					path.moveTo(polygon.getLast().x, polygon.getLast().y);
					
					synchronized(pointLock) {
						path.lineTo(possNextPoint.x, possNextPoint.y);
					}
					
					canvas.drawPath(path, paintTemp);
				}
			}
			*/
			
			surfaceHolder.unlockCanvasAndPost(canvas);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	/*
		boolean handledEvent = false;
		
		if(running) {
			Point p = new Point((int)event.getX(), (int)event.getY());
			
			switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if(polygon.isEmpty()) {
						polygon.addPoint(p);
					} else {
						updatePossNextPoint(p);
					}
					
					handledEvent = true;
				
				break;
				
				case MotionEvent.ACTION_MOVE:
					updatePossNextPoint(p);
					
					handledEvent = true;
				break;
				
				case MotionEvent.ACTION_UP:
					updatePossNextPoint(null);
					
					polygon.addPoint(p);
					
					handledEvent = true;
				break;
				
				case MotionEvent.ACTION_CANCEL:
					updatePossNextPoint(null);
					
					handledEvent = true;
				break;
			}
		}
		
		return handledEvent;
	*/
		return false; 
	}
}
