/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.glass.sample.stopwatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;


/**
 * Service owning the LiveCard living in the timeline.
 */
public class FilMeInService extends Service implements AsyncResponse {

    private static final String TAG = "StopwatchService";
    private static final String LIVE_CARD_TAG = "stopwatch";

    private ChronometerDrawer mCallback;

    private TimelineManager mTimelineManager;
    private LiveCard liveCard;

    private Timer heartBeat = null;
    private int i = 0;
    
    public class LocalBinder extends Binder {
        public FilMeInService getService() {
            return FilMeInService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
    
    @Override
    public void onCreate() {
        super.onCreate();
        mTimelineManager = TimelineManager.from(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	 ArrayList<String> voiceResults = intent.getExtras().getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
         
         Log.d("speech + n", voiceResults.toString());
         
         new ASyncGetData().execute(voiceResults.toArray(new String[voiceResults.size()]));
        onServiceStart();
        return START_STICKY;
    }
    
    //Result of ASyncTask, this is called when it's finished
    @Override
	public void processFinish(JSONObject output) {
		// TODO Auto-generated method stub
		
	}
    
    private class ASyncGetData extends AsyncTask<String, Void, JSONObject> {
    	
    	public AsyncResponse delegate = null;

		@Override
		protected JSONObject doInBackground(String... params) {
			try {
				String movie = "";
				for (String s : params) {
					movie += s.replace(" ", "%20");
				}
				String url = "http://acompany.herokuapp.com/api/getFilm/" + movie;
				 
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		 
				// optional default is GET
				con.setRequestMethod("GET");
		 
				//add request header
		 
				int responseCode = con.getResponseCode();
				System.out.println("\nSending 'GET' request to URL : " + url);
				System.out.println("Response Code : " + responseCode);
				
				if (responseCode == 200) {
		 
					BufferedReader in = new BufferedReader(
					        new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();
			 
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
			 
					//print result
					Log.d("movie", response.toString());
					JSONObject jsonObj = new JSONObject(response.toString());
					return jsonObj;
				} else {
					return null;
				}
		 
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
			
		}
		
		@Override
		   protected void onPostExecute(JSONObject result) {
		      delegate.processFinish(result);
		   }
    	
    }
    

    @Override
    public void onDestroy() {
        if (liveCard != null && liveCard.isPublished()) {
            Log.d(TAG, "Unpublishing LiveCard");
            if (mCallback != null) {
                liveCard.getSurfaceHolder().removeCallback(mCallback);
            }
            liveCard.unpublish();
            liveCard = null;
        }
        if (heartBeat != null) {
        	heartBeat.cancel();
        }
        super.onDestroy();
    }
    
    private boolean onServiceStart()
    {
        Log.d("onServiceStart() called.", "onServiceStart() called.");
        publishCard(this);
        if(heartBeat == null) {
            heartBeat = new Timer();
        }
        startHeartBeat(3000);

        return true;
    }

    private boolean onServicePause()
    {
        Log.d("onServicePause() called.", "onServicePause() called.");
        return true;
    }
    private boolean onServiceResume()
    {
        Log.d("onServiceResume() called.", "onServiceResume() called.");
        return true;
    }

    private boolean onServiceStop()
    {
        Log.d("onServiceStop() called.", "onServiceStop() called.");

        // TBD:
        // Unpublish livecard here
        // .....
        unpublishCard(this);
        // ...

        // Stop the heart beat.
        // ???
        // onServiceStop() is called when the service is destroyed.... ??? Need to check
        if(heartBeat != null) {
            heartBeat.cancel();
        }
        // ...

        return true;
    }




    // For live cards...

    private void publishCard(Context context)
    {
        Log.d("publishCard() called.", "publishCard() called.");
        // if (liveCard == null || !liveCard.isPublished()) {
        if (liveCard == null) {
            TimelineManager tm = TimelineManager.from(context);
            liveCard = tm.createLiveCard(LIVE_CARD_TAG);
//             // liveCard.setNonSilent(false);       // Initially keep it silent ???
//             liveCard.setNonSilent(true);      // for testing, it's more convenient. Bring the card to front.
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_chronometer);
            liveCard.setViews(remoteViews);
            Intent intent = new Intent(context, FilMeInActivity.class);
            liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));
            liveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            // Card is already published.
            return;
        }
    }
    // This will be called by the "HeartBeat".
    private void updateCard(Context context)
    {
        Log.d("updateCard() called.", "updateCard() called.");
        // if (liveCard == null || !liveCard.isPublished()) {
        if (liveCard == null) {
        	Log.d("publish card when livecard is null", "publish card when livecard is null");
            // Use the default content.
            publishCard(context);
        } else {
            // Card is already published.

            // ????
            // Without this (if use "republish" below),
            // we will end up with multiple live cards....
            // ...


            // getLiveCard() seems to always publish a new card
            //       contrary to my expectation based on the method name (sort of a creator/factory method).
            // That means, we should not call getLiveCard() again once the card has been published.
//            TimelineManager tm = TimelineManager.from(context);
//            liveCard = tm.createLiveCard(cardId);
//            liveCard.setNonSilent(true);       // Bring it to front.
            // TBD: The reference to remoteViews can be kept in this service as well....
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_chronometer);
            String content = "kkkk";
            
            content = "Updated: " + i;
            // ...

            remoteViews.setTextViewText(R.id.subtitle_target, content);
            liveCard.setViews(remoteViews);
        }
    }

    private void unpublishCard(Context context)
    {
        Log.d("unpublishCard() called.", "unpublishCard() called.");
        if (liveCard != null) {
            liveCard.unpublish();
            liveCard = null;
        }
    }


    private void startHeartBeat(int nextSubTime)
    {
        final Handler handler = new Handler();
        TimerTask liveCardUpdateTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                        	Log.e("timer updated", "timer updated");
                        	updateText();
                            updateCard(FilMeInService.this);
                        } catch (Exception e) {
                            Log.e("Failed to run the task.", "Failed to run the task." + e);
                        }
                    }

					
                });
            }
        };
        heartBeat.scheduleAtFixedRate(liveCardUpdateTask, 0,nextSubTime);
    }

    private void updateText() {
    	i++;
	}
}
