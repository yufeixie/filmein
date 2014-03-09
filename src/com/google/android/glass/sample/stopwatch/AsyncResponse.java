package com.google.android.glass.sample.stopwatch;

import org.json.JSONObject;

public interface AsyncResponse {
    void processFinish(JSONObject output);
}
