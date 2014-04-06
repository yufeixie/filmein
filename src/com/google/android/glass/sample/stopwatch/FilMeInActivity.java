package com.google.android.glass.sample.stopwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;


// The "main" activity...
public class FilMeInActivity extends Activity
{
    // For tap event
    private GestureDetector mGestureDetector;

    // Service to handle liveCard publishing, etc...
    private boolean mIsBound = false;
    private FilMeInService subtitleCardService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("filmein", "onServiceConnected() called.");
            subtitleCardService = ((FilMeInService.LocalBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.d("filmein", "onServiceDisconnected() called.");
            subtitleCardService = null;
            finish();
            
        }
    };
    private void doBindService()
    {
        bindService(new Intent(this, FilMeInService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    private void doUnbindService() {
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }
    private void doStartService()
    {
        startService(new Intent(this, FilMeInService.class));
    }
    private void doStopService()
    {
        stopService(new Intent(this, FilMeInService.class));
    }


    @Override
    protected void onDestroy()
    {
        doUnbindService();
        // doStopService();   // TBD: When do we call Stop service???
        super.onDestroy();  
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
        Log.d("filmein", "onCreate() called.");

        //setContentView(R.layout.card_chronometer);

        // For gesture handling.
        mGestureDetector = createGestureDetector(this);
        

        // bind does not work. We need to call start() explilicitly...
        doBindService();
        //doStartService();
        // TBD: We need to call doStopService() when user "closes" the app....
        // ...

    }


    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d("filmein", "onResume() called.");

    }



    // TBD:
    // Just use context menu instead of gesture ???
    // ...

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null) {
        	Log.d("filmein", "motion not null");
            return mGestureDetector.onMotionEvent(event);
        }
        Log.d("filmein", "motion NULL");
        return false;
    }

    private GestureDetector createGestureDetector(Context context)
    {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gesture
        Log.d("filmein", "In create function");
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
            	Log.d("filmein", "ure3");
                if (gesture == Gesture.TAP) {
                    handleGestureTap();
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    handleGestureTwoTap();
                    return true;
                }
                return false;
            }
        });
        Log.d("filmein", "end of create function");
        return gestureDetector;
    }

    private void handleGestureTap()
    {
        Log.d("filmein", "handleGestureTap() called.");
        if (subtitleCardService != null) {
        	Log.d("filmein", "before pause");
        	subtitleCardService.pause();
        }
        //doStartService();
        //finish(); 
    }

    private void handleGestureTwoTap()
    {
        Log.d("filmein", "handleGestureTwoTap() called.");
        doUnbindService();
        subtitleCardService.onServiceStop();
        subtitleCardService = null;
        finish();
    }


}
