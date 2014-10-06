package com.elephant.proga.elephant;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by gluse on 06/10/14.
 */
public class PredictionReceiver extends Receiver {

    //source contiene http://127.0.0.1:8080/prediction

    private ArrayList flights;
    private String flight;

    public PredictionReceiver(Activity activity, String source) {
        super(activity, source, -1);
    }

    public boolean setFlights(ArrayList flightsList) {

        this.flights = flightsList;
        //this.source = this.source + buildRequestString(flightsList);
        return true;

    }


    public boolean setFlight(String flight) {
        this.flight = flight;
        this.source = this.source + buildRequestString(flight);
        return true;
    }

    private String buildRequestString(String flight) {
        int dt = 10;
        String dt_string = String.valueOf(dt);

        int nsteps = 5;
        String nsteps_string = String.valueOf(nsteps);

        return "?" + "flight_id="+ flight + "&" + "deltaT=" + dt_string + "&" + "nsteps=" + nsteps_string;
    }


    @Override
    public void run() {

        Log.d("PREDICTION RECEIVER", String.format("ASKING PREDICTION FOR FLIGHT "));
        this.content = GET();
        if (this.content != null) {
            jcontent = this.toJSON(content);
            this.activity.runOnUiThread(new Runnable() {
                public void run() {
                    activity.onPredictionReceived(jcontent);
                }
            });
        }
        else
            Log.d("RECEIVER", String.format("THERE ARE PROBLEMS RECEIVING DATA PLEASE CHECK CONNECTIONS AND IP"));


    }
}
