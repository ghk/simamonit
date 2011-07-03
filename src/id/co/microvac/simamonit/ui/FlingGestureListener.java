package id.co.microvac.simamonit.ui;

import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public abstract class FlingGestureListener  extends SimpleOnGestureListener {
	private static final int SWIPE_MIN_DISTANCE = 50;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 100;
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.v("fling", "on fling");
        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH){
			Log.v("fling", "max path failed");
            return false;
        }
        
		if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			Log.v("fling", "fling left");
			onFling(false);
			return true;
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			Log.v("fling", "fling right");
			onFling(true);
			return true;
		}
		Log.v("fling", "dif = "+(e2.getX() - e1.getX() )+" vel: "+velocityX);
		return false;
	}
	
	public abstract void onFling(boolean leftToRight);
}