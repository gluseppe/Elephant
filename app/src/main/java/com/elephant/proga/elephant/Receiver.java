package com.elephant.proga.elephant;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by gluse on 26/09/14.
 */
public class Receiver implements Runnable {

    private NavigationMapActivity activity;
    private String selfposition;

    public Receiver(NavigationMapActivity activity) {
        this.activity = activity;
    }


    private String responseContentToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    @Override
    public void run() {


        HttpResponse response;
        HttpGet req;
        HttpClient client;

        while(true) {
            //ask traffic
            Log.d("RECEIVER",String.format("ASKING FOR DATA ON A WORKER THREAD %s",Thread.currentThread().getName()));
            client = new DefaultHttpClient();
            req = new HttpGet("http://192.168.1.31:8080/traffic?item=myState");


            try {
                response = client.execute(req);
                selfposition = responseContentToString(response.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();
            }


            this.activity.runOnUiThread(new Runnable() {
                public void run() {
                    activity.selfUpdated(selfposition);

                }
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }


        }

    }
}
