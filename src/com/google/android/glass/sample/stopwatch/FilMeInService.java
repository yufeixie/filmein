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

import org.json.JSONArray;
import org.json.JSONException;
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
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;


/**
 * Service owning the LiveCard living in the timeline.
 */
public class FilMeInService extends Service implements AsyncResponse {

    private static final String TAG = "StopwatchService";
    private static final String LIVE_CARD_TAG = "stopwatch";

    //private ChronometerDrawer mCallback;

    private TimelineManager mTimelineManager;
    private LiveCard liveCard;
    
    //private GestureDetector gc;

    private Timer heartBeat = null;
    private int i = 0;
    private boolean paused = false;
    private JSONObject jsonObj;
    private boolean isBlank = true;
    private long previousEnd = -1;
    final Handler handler = new Handler();
    private TimerTask nextTask; 
    private long taskStartTime;
    private long delay;
    private String currentText;
    
    
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
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	 ArrayList<String> voiceResults = intent.getExtras().getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
    	 publishCard(this);
         Log.d("speech + n", voiceResults.toString());

         new ASyncGetData(this).execute(voiceResults.toArray(new String[voiceResults.size()]));
        
        return START_STICKY;
    }
    
    //Result of ASyncTask, this is called when it's finished
    @Override
	public void processFinish(JSONObject output) {
		// TODO Auto-generated method stub
    	if(output == null)
    	{
    		Log.d("filmein", "errrrrrr");
    	}
		jsonObj = output;
		//publishCard(this);
		//pausedCard(this);
		if (jsonObj != null) {
			onServiceStart();
		}
	}
    
    private class ASyncGetData extends AsyncTask<String, AsyncResponse, JSONObject> {
    	
    	public AsyncResponse delegate = null;
    	
    	public ASyncGetData(AsyncResponse delegate) {
    		this.delegate = delegate;
    	}

		@Override
		protected JSONObject doInBackground(String... params) {
			try {
				String movie = "";
				for (String s : params) {
					movie += s.replace(" ", "%20");
				}
				//movie = "duck";
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
					Log.d("filmein", response.toString());
					JSONObject jsonObj = new JSONObject(response.toString());
					return jsonObj;
				} else {
					updateCardText("No subtitles currently exist for:\r\n" + movie);
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
    	Log.d("filmein", "destroying service");
        if (liveCard != null && liveCard.isPublished()) {
            Log.d("filmein", "Unpublishing LiveCard");
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
        Log.d("filmein", "onServiceStart() called.");
        Intent gestureIntent = new Intent(getBaseContext(), FilMeInActivity.class);
        gestureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(gestureIntent);
        if(heartBeat == null) {
            heartBeat = new Timer();
        }
        setHeartBeat();
        updateCard(FilMeInService.this);
        

        return true;
    }

    private boolean onServicePause()
    {
        Log.d("filmein", "onServicePause() called.");
        return true;
    }
    private boolean onServiceResume()
    {
        Log.d("filmein", "onServiceResume() called.");
        return true;
    }

    public boolean onServiceStop()
    {
        Log.d("filmein", "onServiceStop() called.");

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
        stopSelf();

        return true;
    }




    // For live cards...

    private void publishCard(Context context)
    {
        //Log.d("publishCard() called.", "publishCard() called.");
        // if (liveCard == null || !liveCard.isPublished()) {
        if (liveCard == null) {
            TimelineManager tm = TimelineManager.from(context);
            liveCard = tm.createLiveCard(LIVE_CARD_TAG);
//             // liveCard.setNonSilent(false);       // Initially keep it silent ???
//             liveCard.setNonSilent(true);      // for testing, it's more convenient. Bring the card to front.
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_chronometer);
            remoteViews.setTextViewText(R.id.subtitle_target, "");
            liveCard.setViews(remoteViews);
            Intent intent = new Intent(context, FilMeInActivity.class);
            liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));
            liveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            // Card is already published.
            return;
        }
    }
    
    private void pausedCard(Context context) {
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.card_chronometer);
        
        remoteViews.setTextViewText(R.id.subtitle_target, "this is a dummy");
        liveCard.setViews(remoteViews);
    }
    
    // This will be called by the "HeartBeat".
    private void updateCard(Context context)
    {
        Log.d("filmein", "updateCard() called.");
        // if (liveCard == null || !liveCard.isPublished()) {
        if (liveCard == null) {
        	//Log.d("publish card when livecard is null", "publish card when livecard is null");
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
        	String content = "kkkk";
        	try {
        		JSONArray subtitles = jsonObj.getJSONArray(("subtitles"));
                JSONObject j = subtitles.getJSONObject(i);
                if(isBlank)
                {
                	content = "";
                }	
                else
                {
                	content = j.getString("text");
                	updateText();
                }
                	
        	} catch(Exception e) {
        		Log.e("err", e.toString());
        	}
        	updateCardText(content);
            currentText = content;
        }
    }

    private void unpublishCard(Context context)
    {
        Log.d("filmein", "unpublishCard() called.");
        if (liveCard != null) {
            liveCard.unpublish();
            liveCard = null;
        }
    }

    private TimerTask getNextTimerTask() {
    	nextTask = new TimerTask() {
            @Override
            public void run() {
            	
                handler.post(new Runnable() {
                    public void run() {
                        try {
                        	//Log.e("timer updated", "timer updated");
                            
                            setHeartBeat();
                            updateCard(FilMeInService.this);
                            
                        } catch (Exception e) {
                            Log.e("filmein", "Failed to run the task." + e);
                        }
                    }

    				
                });
            }
        };
        return nextTask;
    }

    private void setHeartBeat()
    {
        

        try {
        	JSONArray subtitles = jsonObj.getJSONArray(("subtitles"));
        	if(i >= subtitles.length())
        	{
        		onServiceStop();
        	}
            JSONObject j = subtitles.getJSONObject(i);
            long start = j.getLong("start");
            long endTime = j.getLong("endTime");
            if(!isBlank)
            {
            	if(start - previousEnd < 10) {
            		delay = endTime - start;

            		heartBeat.schedule(getNextTimerTask(), delay);
            		taskStartTime = System.currentTimeMillis();
            		previousEnd = endTime;
            	}
            	else {
            		isBlank = true;
                	//Log.d("not blank", "" + (start - previousEnd));
            		delay = start - previousEnd;
                	heartBeat.schedule(getNextTimerTask(), delay);
                	taskStartTime = System.currentTimeMillis();
                	
                	previousEnd = endTime;
            	}
            }
            else {
            	//Log.d("isBlank","" + (endTime - start));
            	delay = endTime - start;
            	heartBeat.schedule(getNextTimerTask(), endTime - start);
            	taskStartTime = System.currentTimeMillis();
            	previousEnd = endTime;
            	isBlank = false;
            }
        } catch (Exception e)
        {
        	Log.e("filmein", e.toString());
        }
        
    }

    private void updateText() {
    	Log.d("filmein", "" + i);
    	i++;
	}



	public void pause() {
		// TODO Auto-generated method stub
			if (paused) {
				heartBeat = new Timer();
				Log.d("filmein", "resuming: " + Long.toString(delay));
				heartBeat.schedule(getNextTimerTask(), delay);
				taskStartTime = System.currentTimeMillis();
				updateCardText(currentText);
			} else {
				long currentTime = System.currentTimeMillis();
				long establishedTime = currentTime - taskStartTime;
				delay -= establishedTime;
				Log.d("filmein", "pausing: " + Long.toString(delay));
				heartBeat.cancel();
				updateCardText("[PAUSED]\r\n" + currentText);
			}
			
			
			paused = !paused;			
	}

	private void updateCardText(String text) {
		RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.card_chronometer);
		remoteViews.setTextViewText(R.id.subtitle_target, text);
		liveCard.setViews(remoteViews);
	}
	

}
