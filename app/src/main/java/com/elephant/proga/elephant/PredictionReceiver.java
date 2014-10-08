package com.elephant.proga.elephant;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Created by gluse on 06/10/14.
 */
public class PredictionReceiver extends Receiver {

    //source contiene http://127.0.0.1:8080/prediction

    private ArrayList flights;
    private String flight;
    private boolean rawPrediction;
    private Hashtable<Integer,ArrayList<Particle>> particles;
    private int dt;
    private int nsteps;

    public PredictionReceiver(Activity activity, String source) {
        super(activity, source, -1);
    }

    public boolean setFlights(ArrayList flightsList) {

        this.flights = flightsList;
        //this.source = this.source + buildRequestString(flightsList);
        this.rawPrediction = false;
        return true;

    }


    public boolean setPredictionParams(String flight, int dt, int nsteps, boolean rawPrediction) {
        this.flight = flight;
        this.rawPrediction = rawPrediction;
        this.source = this.source + buildRequestString(flight, dt, nsteps, rawPrediction);
        this.dt = dt;
        this.nsteps = nsteps;
        return true;
    }

    private String buildRequestString(String flight, int dt, int nsteps, boolean rawPrediction) {
        String dt_string = String.valueOf(dt);
        String nsteps_string = String.valueOf(nsteps);

        return "?" + "flight_id="+ flight + "&" + "deltaT=" + dt_string + "&" + "nsteps=" + nsteps_string + "&" + "raw=" + rawPrediction;
    }


    @Override
    public void run() {

        Log.d("PREDICTION RECEIVER", String.format("ASKING PREDICTION FOR FLIGHT "));
        JSONObject jtimes = null;
        JSONArray jtimesraw = null;
        JSONArray jparticles = null;

        this.content = GET();
        if (this.content != null) {
            if (this.rawPrediction)
            {
                JSONObject jpred = this.toJSON(content);
                Iterator<String> ids = jpred.keys();
                particles = new Hashtable<Integer, ArrayList<Particle>>();
                while(ids.hasNext()) {
                    try {
                        jtimesraw = jpred.getJSONArray(ids.next());
                        for (int i=0; i<jtimesraw.length();i++)
                        {
                            int futureTime = i*this.dt;
                            jparticles = jtimesraw.getJSONArray(i);
                            ArrayList<Particle> particlesInFutureTime = new ArrayList<Particle>();

                            for (int j=0; j<jparticles.length(); j++) {
                                Particle particle = new Particle(new LatLng(jparticles.getJSONArray(j).getDouble(1), jparticles.getJSONArray(j).getDouble(0)),futureTime);
                                particlesInFutureTime.add(particle);

                                particles.put(Integer.valueOf(futureTime), particlesInFutureTime);

                            }
                            particlesInFutureTime = null;
                        }

                        this.activity.runOnUiThread(new Runnable() {
                            public void run() {
                                activity.onRawPredictionReceived(particles);
                            }
                        });



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

        }
        else
            Log.d("RECEIVER", String.format("THERE ARE PROBLEMS RECEIVING DATA PLEASE CHECK CONNECTIONS AND IP"));


        /**
         *             jcontent = this.toJSON(content);
         Iterator<String> ids = jcontent.keys();
         while (ids.hasNext()) {
         try {
         jtimes = (JSONObject) jcontent.get(ids.next());
         } catch (JSONException e) {
         e.printStackTrace();
         }

         Iterator times = jtimes.keys();
         while(times.hasNext()) {

         }

         }

         this.activity.runOnUiThread(new Runnable() {
         public void run() {
         activity.onPredictionReceived(jcontent);
         }
         });

         */


    }
}
