package com.elephant.proga.elephant;

import android.app.Activity;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by gluse on 03/10/14.
 */
public class Receiver implements Runnable {

    protected NavigationMapActivity activity;
    protected String source;
    protected String content;
    protected JSONObject jcontent;
    protected long sleepingTime;

    public Receiver(Activity activity, String source, long sleepingTime) {
        this.activity = (NavigationMapActivity) activity;
        this.source = source;
        this.sleepingTime = sleepingTime;
    }


    private String responseContentToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    protected String GET() {
        HttpResponse response;
        HttpGet req;
        HttpClient client;

        client = new DefaultHttpClient();
        req = new HttpGet(this.source);


        try {
            response = client.execute(req);
            return responseContentToString(response.getEntity().getContent());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Override this method to specialize your conversion
    protected JSONObject toJSON(String string) {

        try {
            return new JSONObject(string);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
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
                        activity.onSelfUpdate(jcontent);
                    }
                });
            }
            else
                Log.d("RECEIVER", String.format("THERE ARE PROBLEMS RECEIVING DATA PLEASE CHECK CONNECTIONS AND IP"));




            try {
                Thread.sleep(sleepingTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }


        }
    }
}
