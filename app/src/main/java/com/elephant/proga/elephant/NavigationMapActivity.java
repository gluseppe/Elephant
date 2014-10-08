package com.elephant.proga.elephant;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import static android.graphics.Color.RED;

public class NavigationMapActivity extends FragmentActivity implements GoogleMap.OnCameraChangeListener, GoogleMap.OnMarkerClickListener {


    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Thread selfPositionThread;
    private Thread trafficPositionThread;
    private Thread predictioThread;
    private Marker me;
    private Hashtable<Integer,ArrayList<Particle>> particles;

    private final String ROOTSOURCE = "http://192.168.1.16:8080";
    private final String SELFSOURCE = ROOTSOURCE + "/traffic?item=myState";
    private final String TRAFFICSOURCE = ROOTSOURCE + "/traffic?item=traffic";
    private final String PREDICTIONSOURCE = ROOTSOURCE + "/prediction";
    private static final long SELFSLEEPINGTIME = 2000;
    private static final long TRAFFICSLEEPINGTIME = 2000;
    private Hashtable<String, Marker> traffic;
    private float autoZoomLevel = 12;
    private float userZoomLevel = -1;
    private float currentZoomLevel = autoZoomLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.me = null;
        this.trafficPositionThread = null;
        this.predictioThread = null;
        this.traffic = new Hashtable();

        setContentView(R.layout.activity_navigation_map);
        setUpMapIfNeeded();
        receivePosition();
        receiveTraffic();

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
            mMap.setOnCameraChangeListener(this);
            mMap.setOnMarkerClickListener(this);
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.selfPositionThread.interrupt();
        this.selfPositionThread = null;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setUpMapIfNeeded();
        receivePosition();
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


    public void animateMarker(final Marker marker, final LatLng toPosition, final float rotAngle,
                              final boolean hideMarker, final long duration) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = this.mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final float startAngle = marker.getRotation();
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);

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
                float intermediate_angle = t * rotAngle + (1 -t) * startAngle;

                 marker.setPosition(new LatLng(lat, lng));
                marker.setRotation(intermediate_angle);


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

    public void onTrafficUpdate(JSONObject jTraffic) {
        if (jTraffic == null)
        {
            Log.d("TRAFFIC UPDATE",String.format("JSONObject was null"));
        }
        else
        {
            Iterator<String> iter = jTraffic.keys();
            Marker current;
            while (iter.hasNext()) {
                String key = iter.next();
                Log.d("TRAFFIC UPDATE", String.format("flight id:%s",key));
                try {
                    JSONObject status = (JSONObject) jTraffic.get(key);
                    double lat = status.getDouble("lat");
                    double lon = status.getDouble("lon");
                    //double h = status.getDouble("h");

                    double vx = status.getDouble("vx");
                    double vy = status.getDouble("vy");

                    current = this.traffic.get(key);


                    if (current == null) {
                        Log.d("TRAFFIC UPDATE", String.format("flight id:%s was not in our hashtable",key));
                        current = this.mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lon))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.traffic_red_40))
                                .flat(true)
                                .title(key));
                        Log.d("TRAFFIC UPDATE", String.format("Adding %s, currently we have %d elements",key,this.traffic.size()));
                        this.traffic.put(key,current);
                        Log.d("TRAFFIC UPDATE", String.format("We now have %d elements in the hasthable",this.traffic.size()));

                    }


                    //float angle = getRotAngle(vx,vy);

                    this.animateMarker(current, new LatLng(lat,lon),getRotAngle(vx,vy),false,TRAFFICSLEEPINGTIME);

                } catch (JSONException e) {
                    // Something went wrong!
                    Log.e("JSONERROR", "SOMETHING WRONG WITH JSON");
                }
            }
        }


    }

    public void onPredictionReceived(JSONObject jPrediction) {
        Log.d("PREDICTION", "Hey, prediction received maybe");


    }

    private void removeOldRawPrediction() {
        if (this.particles == null) return;

                Enumeration<Integer> i = particles.keys();
                while (i.hasMoreElements()) {
                    ArrayList ft = (ArrayList) particles.get(i.nextElement());
                    Iterator j = ft.iterator();

                    while(j.hasNext()) {
                        Particle p = (Particle) j.next();
                        Circle c = (Circle) p.getRepresentation();
                        c.remove();
                        c = null;
                        j.remove();
                    }

                    //j.remove();
                }
    }


    public void onRawPredictionReceived(Hashtable<Integer, ArrayList<Particle>> particles) {
        //delete from map all of the other circles
        this.removeOldRawPrediction();


        this.particles = null;
        Enumeration<Integer> i = particles.keys();

        while (i.hasMoreElements()) {
            ArrayList timesparticles = (ArrayList) particles.get(i.nextElement());
            Iterator j = timesparticles.iterator();
            while(j.hasNext()) {

                Particle p = (Particle) j.next();
                Circle circle = mMap.addCircle(new CircleOptions()
                                .center(p.getPosition())
                                .radius(50)
                                .strokeColor(RED)
                );

                p.setRepresentation(circle);
                circle = null;
            }
        }

        this.particles = particles;
    }



    private float getRotAngle(double vx, double vy) {

        if (vx == 0.0 && vy == 0.0)
            return 0.0f;



        float rotAngle = (float) Math.acos(vy/(Math.sqrt(Math.pow(vx,2)+Math.pow(vy,2)))) * (float) (vx/Math.abs(vx));
        return rotAngle * (180/(float)Math.PI);

    }

    public void onSelfUpdate(JSONObject jSelf) {
        double lat;
        double lon;
        //double h = 0;
        double vx;
        double vy;

        if (jSelf == null) return;

        try {
            lat = jSelf.getDouble("lat");
            lon = jSelf.getDouble("lon");
            vx = jSelf.getDouble("vx");
            vy = jSelf.getDouble("vy");
            //h = jSelf.getDouble("h");

            /*double bx = 0.0;
            double by = 1.0;

            float rotAngle;
            rotAngle = (float) Math.acos(vy/(Math.sqrt(Math.pow(vx,2)+Math.pow(vy,2)))) * (float) (vx/Math.abs(vx));
            rotAngle = rotAngle * (180/(float)Math.PI);
            */

            //float rotAngle = getRotAngle(vx,vy);
            //Log.d("ANGLE",String.format("Vx:%f, Vx:%f Alfa is:%f",vx,vy,rotAngle));

            if(this.me == null) {
                this.me = this.mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lon))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.self_black_40))
                        .flat(true)
                        .title("SELF"));
            }
            else
            {
                animateMarker(this.me,new LatLng(lat,lon),getRotAngle(vx,vy),false,SELFSLEEPINGTIME);
            }

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat,lon))      // Sets the center of the map to the new position
                    .zoom(this.currentZoomLevel)                   // Sets the zoom
//                .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(60)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            this.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),2000,null);



        } catch (JSONException e) {
            e.printStackTrace();
            //da capire perchè potrebbe generare un'eccezione
            //e decidere se chiudere tutto o lasciar perdere
        }

    }


    private void receivePosition() {
        this.selfPositionThread = new Thread(new SelfStatusReceiver(this, this.SELFSOURCE, SELFSLEEPINGTIME));
        this.selfPositionThread.start();
    }

    private void receiveTraffic() {
        this.trafficPositionThread = new Thread(new TrafficReceiver(this, this.TRAFFICSOURCE, TRAFFICSLEEPINGTIME));
        this.trafficPositionThread.start();
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        this.currentZoomLevel = cameraPosition.zoom;

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        Log.d("MARKER",String.format("MARKER TOUCHED, HELLO I M %s",marker.getTitle()));
        PredictionReceiver pr = new PredictionReceiver(this,this.PREDICTIONSOURCE);
        pr.setPredictionParams(marker.getTitle(), 5, 10, true);
        this.predictioThread = new Thread(pr);
        this.predictioThread.start();
        return true;
    }
}
