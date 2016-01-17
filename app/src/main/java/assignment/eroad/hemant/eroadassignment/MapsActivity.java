package assignment.eroad.hemant.eroadassignment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private static final String EROAD_OFFICE_ADDRESS = "260 Oteha Valley Rd, Albany, Auckland";

    private static final String SIMPLE_DATE_FORMAT = "hh:mm aa";
    private static final int EARTH_RADIUS_KM = 6371;
    private static final int FLIGHT_SPEED_KMH = 800;
    private static final int CAR_SPEED_KMH = 80;
    private static final int WALK_SPEED_KMH = 15;

    private TextView mLatLongText;
    private TextView mTimeZoneText;
    private TextView mCurrentUTCText;
    private TextView mCurrentLocalTimeText;
    private TextView mDistanceFromERoads;
    private TextView mJournerTime;

    private Bitmap mMarkerBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_hybrid:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;

            case R.id.action_normal:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;

            case R.id.action_satellite:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;

            case R.id.action_terrain:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
        }

        return true;
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        double[] locationCoordinates = getlocation();

        View v = getLayoutInflater().inflate(R.layout.map_bubble_layout, null);
        mLatLongText = (TextView)v.findViewById(R.id.tv_latlong);
        mTimeZoneText = (TextView)v.findViewById(R.id.tv_timezone);
        mCurrentUTCText = (TextView)v.findViewById(R.id.tv_currentutc);
        mCurrentLocalTimeText = (TextView)v.findViewById(R.id.tv_currentlocal);
        mDistanceFromERoads = (TextView)v.findViewById(R.id.tv_distance);
        mJournerTime = (TextView)v.findViewById(R.id.tv_journeyTime);

        mLatLongText.setText("Lat: "+locationCoordinates[0]+"; Long: "+locationCoordinates[1]);
        mTimeZoneText.setText("Location Timezone: "+TimeZone.getDefault().getID());
        mCurrentUTCText.setText("UTC Time: "+getCurrentUTCTime());
        mCurrentLocalTimeText.setText("Local Time: "+getCurrentLocalTime());
        DecimalFormat df = new DecimalFormat("#.##");
        double distanceInKm = getDistanceInKm();
        mDistanceFromERoads.setText("Distance: "+df.format(distanceInKm)+" Km");
        mJournerTime.setText("Flight Time: "+df.format(distanceInKm/FLIGHT_SPEED_KMH)+" hrs\nCar Time: "+df.format(distanceInKm/CAR_SPEED_KMH)+" hrs\nWalking Time: "+df.format(distanceInKm/WALK_SPEED_KMH)+" hrs");

        mMarkerBitmap = getBitmapFromLayout(v);


        mMap.addMarker(new MarkerOptions().position(new LatLng(locationCoordinates[0], locationCoordinates[1])).title("Marker").icon(BitmapDescriptorFactory.fromBitmap(mMarkerBitmap)));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }


    /**
     * Find the current location to place the marker
     * @return
     */
    private double[] getlocation() {
        Location l = getUserLocationInstance();
        double[] gps = new double[2];

        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }
        return gps;
    }


    private Location getUserLocationInstance() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        Location l = null;
        for (int i = 0; i < providers.size(); i++) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null)
                break;
        }
        return l;
    }


    private Bitmap getBitmapFromLayout(View v) {
        if (v.getMeasuredHeight() <= 0) {
            v.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            v.draw(c);
            return b;
        }else {
            Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
            v.draw(c);
            return b;
        }
    }

    private String getCurrentUTCTime() {
        DateFormat df = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("gmt"));
        return df.format(new Date());
    }


    private String getCurrentLocalTime() {
        Date dt = new GregorianCalendar().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        return sdf.format(dt);
    }


    private double getDistanceInKm() {
        Location l1 = getUserLocationInstance();
        double latitude1 = l1.getLatitude();
        double longitude1 = l1.getLongitude();

        Address l2 = getLocationFromAddress();
        double latitude2 = l2.getLatitude();
        double longitude2 = l2.getLongitude();

        return getDistance(latitude1, longitude1, latitude2, longitude2);
    }


    private Address getLocationFromAddress() {
        Geocoder coder = new Geocoder(this);
        List<Address> address = null;
        LatLng p1 = null;

            try {
                address = coder.getFromLocationName(EROAD_OFFICE_ADDRESS, 5);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
        return location;

    }



    //GeoCircle Distance
    private double getDistance(double lat1, double long1, double lat2, double long2) {
        lat1 = Math.toRadians(lat1);
        long1 = Math.toRadians(long1);
        lat2 = Math.toRadians(lat2);
        long2 = Math.toRadians(long2);

        double absDiffLong = long1 - long2;

        double absDifference = lat1 - long2;
        //find central angle
        double centralAngle = Math.acos(Math.sin(lat1)*Math.sin(lat2)+Math.cos(lat1)*Math.cos(lat2)*Math.cos(absDifference));
        return EARTH_RADIUS_KM * centralAngle;
    }

}
