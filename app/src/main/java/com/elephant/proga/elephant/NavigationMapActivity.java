package com.elephant.proga.elephant;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class NavigationMapActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Thread selfPositionThread;
    private Marker me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.me = null;
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


    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = this.mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 1000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    public void selfUpdated(String s) {

        Log.d("NAVIGATION_MAP_ACTIVITY",String.format("%s threadname:%s",s,Thread.currentThread().getName()));
        JSONObject jobj;
        double lat = 0, lon = 0, h = 0, vx = 0, vy = 0;

        try {
            jobj = new JSONObject(s);
            lat = jobj.getDouble("lat");
            lon = jobj.getDouble("lon");
            vx = jobj.getDouble("vx");
            vy = jobj.getDouble("vy");
            h = jobj.getDouble("h");
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.d("NAVIGATION_MAP_ACTIVITY",String.format("lat:%f lon:%f h:%f",lat,lon,h));


        double bx = 1.0;
        double by = 0;

        double num = vx*bx + vy*by;
        double nv = Math.sqrt(Math.pow(vx,2)+Math.pow(vy,2));
        double nb = Math.sqrt(Math.pow(bx,2)+Math.pow(by,2));
        double den = nv*nb;

        double alfa = Math.acos(num/den);

        if(this.me == null) {
            this.me = this.mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.self_black_40))
                    .flat(true)
                    .rotation((float) alfa)
                    .title("SELF"));
        }
        else
        {
            animateMarker(this.me,new LatLng(lat,lon),false);
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat,lon))      // Sets the center of the map to the new position
                .zoom(15)                   // Sets the zoom
//                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        this.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),1000,null);



        //CameraUpdate camUpdate = CameraUpdateFactory.newLatLng(new LatLng(lat,lon));

        //this.mMap.animateCamera(camUpdate, 500, null);







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
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
}
