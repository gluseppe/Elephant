package com.elephant.proga.elephant;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by gluse on 06/10/14.
 */
public class TrafficMarkerListener implements GoogleMap.OnMarkerClickListener {

    public TrafficMarkerListener() {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
