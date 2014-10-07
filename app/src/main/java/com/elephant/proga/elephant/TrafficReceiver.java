package com.elephant.proga.elephant;

import android.app.Activity;
import android.util.Log;

/**
 * Created by gluse on 03/10/14.
 */
public class TrafficReceiver extends Receiver {

    public TrafficReceiver(Activity activity, String source, long sleepingTime) {
        super(activity, source, sleepingTime);
    }


    @Override
    public void run() {


        while(!Thread.interrupted()) {


            this.content = this.GET();
            Log.d("RECEIVER", String.format("ASKING CONTENT"));
            if (this.content != null) {
                jcontent = this.toJSON(content);
                this.activity.runOnUiThread(new Runnable() {
                    public void run() {
                        activity.onTrafficUpdate(jcontent);
                    }
                });
            }
            else
                Log.d("TRAFFIC RECEIVER", String.format("THERE ARE PROBLEMS RECEIVING DATA PLEASE CHECK CONNECTIONS AND IP"));





            try {
                Thread.sleep(sleepingTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }


        }
    }



}
