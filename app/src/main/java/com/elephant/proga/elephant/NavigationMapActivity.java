package com.elephant.proga.elephant;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class NavigationMapActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Thread selfPositionThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.selfPositionThread = null;
        setContentView(R.layout.activity_navigation_map);
        setUpMapIfNeeded();
        receivePosition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    public void selfUpdated(String s) {

        Log.d("NAVIGATION_MAP_ACTIVITY",String.format("%s threadname:%s",s,Thread.currentThread().getName()));
        JSONObject jobj;
        double lat = 0, lon = 0, h = 0;
        try {
            jobj = new JSONObject(s);
            lat = jobj.getDouble("lat");
            lon = jobj.getDouble("lon");
            h = jobj.getDouble("h");
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.d("NAVIGATION_MAP_ACTIVITY",String.format("lat:%f lon:%f h:%f",lat,lon,h));


        this.mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("SELF"));

        CameraUpdate camUpdate = CameraUpdateFactory.newLatLng(new LatLng(lat,lon));
        this.mMap.moveCamera(camUpdate);







    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    private void receivePosition() {
        this.selfPositionThread = new Thread(new Receiver(this));
        this.selfPositionThread.start();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
}
