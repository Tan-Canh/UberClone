package canh.tan.nguye.uberclone;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import canh.tan.nguye.uberclone.common.Common;
import canh.tan.nguye.uberclone.interfaces.IGoogleAPI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{


    //grant use to permission ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
    private static final int MY_PERMISSION_REQUEST_CODE = 1000;

    //result connect play service
    private static final int PLAY_SERVICE_RES_REQUEST = 1001;

    private static final int INTERVAL = 5000; //it is 5 secs
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;

    //
    DatabaseReference driveRef;
    GeoFire geoFire;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;

    Marker mMarker;

    MaterialAnimatedSwitch mSwitch;

    //car animation
    private List<LatLng> polyLineList;
    private Marker carMarker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition, currentPosition;
    private int index, next;
    private Button btnGo;
    private EditText editPlace;
    private String destination;
    private PolylineOptions polylineOptions, backPolyLineOptions;
    private Polyline backPolyLine, greyPolyLine;

    Runnable drawPathRunable = new Runnable() {
        @Override
        public void run() {
            if (index < polyLineList.size() - 1){
                index++;
                next = index + 1;
            }

            if (index < polyLineList.size() - 1){
                startPosition = polyLineList.get(index);
                endPosition = polyLineList.get(next);
            }

            ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v = animation.getAnimatedFraction();
                    lat = v * endPosition.latitude + (1 - v) * startPosition.latitude;
                    lng = v * endPosition.longitude + (1 - v) * startPosition.longitude;
                    LatLng newPos = new LatLng(lat, lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f, 0.5f);
                    carMarker.setRotation(getBearing(startPosition, newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(newPos)
                    .zoom(15.5f)
                    .build()));

                }
            });

            valueAnimator.start();
            handler.postDelayed(this, 3000);
        }
    };

    private float getBearing(LatLng startPosition, LatLng newPos) {
        double lat = (startPosition.latitude - endPosition.latitude);
        double lng = (startPosition.longitude - endPosition.longitude);

        if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude) {
            return (float) Math.toDegrees(Math.atan(lat / lng));
        } else {
            if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude) {
                return (float) ((90 - Math.toDegrees(Math.atan(lat / lng))) + 90);
            } else {
                if (startPosition.latitude >= endPosition.longitude && startPosition.longitude >= endPosition.longitude) {
                    return (float) (Math.toDegrees(Math.atan(lat / lng)) + 180);
                } else {
                    if (startPosition.latitude < endPosition.longitude && startPosition.longitude >= endPosition.longitude) {
                        return (float) ((90 - Math.toDegrees(Math.atan(lat / lng))) + 270);
                    }
                }
            }
        }

        return -1;
    }

    private IGoogleAPI services;

    SupportMapFragment mapFragment;


    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //init Data
        initData();

        //init view
        mSwitch = findViewById(R.id.location_switch);
        mSwitch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline){
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are online!", Snackbar.LENGTH_SHORT).show();
                }else {
                    mMarker.remove();
                    mMap.clear();
                    handler.removeCallbacks(drawPathRunable);
                    stopLocationUpdates();
                    Snackbar.make(mapFragment.getView(), "You are offline!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        btnGo = findViewById(R.id.btnGo);
        editPlace = findViewById(R.id.editPlace);

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destination = editPlace.getText().toString();
                destination = destination.replace(" ", "+"); //replace space to +

                Log.i("CANH-Destination", destination + "");
                getDirection();
            }
        });

        setupLocation();

        services = Common.getGoogleAPI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //Toast.makeText(this, "hello", Toast.LENGTH_SHORT).show();
                    if (checkService()){
                        buildGoogleApiClient();
                        createLocationRequest();

                        if (mSwitch.isChecked()){
                            displayLocation();
                        }
                    }
                }
                break;
        }
    }

    private void setupLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        }else {
            if (checkService()){
                buildGoogleApiClient();
                createLocationRequest();

                if (mSwitch.isChecked()){
                    displayLocation();
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private boolean checkService() {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (result != ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(result)){
                GooglePlayServicesUtil.getErrorDialog(result, this, PLAY_SERVICE_RES_REQUEST);
            }else {
                Toast.makeText(this, "This device not supported!", Toast.LENGTH_SHORT).show();
            }

            return false;
        }

        return true;
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null){
            if (mSwitch.isChecked()){
                final double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();

                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mMarker != null){
                            mMarker.remove();
                        }

                        mMarker = mMap.addMarker(new MarkerOptions()
                                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                .position(new LatLng(latitude, longitude))
                                .title("You"));

                        //Move map to location current
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));

                        //createRotateMarker(mMarker, -360);
                    }
                });
            }
        }else {
            Toast.makeText(this, "cannot be get location", Toast.LENGTH_SHORT).show();
        }
    }

    private void createRotateMarker(final Marker mMarker, final int i) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotate = mMarker.getRotation();
        final float duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rotate = t * i + (1 - t) * startRotate;
                mMarker.setRotation(-rotate > 180 ? rotate/2 : rotate);

                //Toast.makeText(WelcomeActivity.this, "t: " + t + " - Rotate: " + rotate, Toast.LENGTH_SHORT).show();

                if (t < 1.0){
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void initData() {
        driveRef = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire = new GeoFire(driveRef);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        /*//check time one
        displayLocation();
        startLocationUpdates();*/
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //check time two
        displayLocation();
    }

    public void getDirection() {
        currentPosition = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        String requestApi = null;
        try{

            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + destination + "&" +
                    "key=" + getResources().getString(R.string.google_direction_api);

            Log.i("CANH-RequestApi", requestApi);

            services.getPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++){
                            JSONObject routes = jsonArray.getJSONObject(i);
                            JSONObject poly = routes.getJSONObject("overview_polyline");
                            String polyLine = poly.getString("points");
                            polyLineList = decodePoly(polyLine);

                            //adjusting bounds
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (LatLng latLng : polyLineList){
                                builder.include(latLng);
                            }

                            LatLngBounds bounds = builder.build();
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                            mMap.animateCamera(cameraUpdate);

                            polylineOptions = new PolylineOptions();
                            polylineOptions.width(5);
                            polylineOptions.color(Color.GRAY);
                            polylineOptions.startCap(new SquareCap());
                            polylineOptions.endCap(new SquareCap());
                            polylineOptions.addAll(polyLineList);
                            polylineOptions.jointType(JointType.ROUND);
                            greyPolyLine = mMap.addPolyline(polylineOptions);

                            backPolyLineOptions = new PolylineOptions();
                            backPolyLineOptions.width(5);
                            backPolyLineOptions.color(Color.GRAY);
                            backPolyLineOptions.startCap(new SquareCap());
                            backPolyLineOptions.endCap(new SquareCap());
                            backPolyLineOptions.addAll(polyLineList);
                            backPolyLineOptions.jointType(JointType.ROUND);
                            backPolyLine = mMap.addPolyline(backPolyLineOptions);

                            mMap.addMarker(new MarkerOptions()
                            .position(polyLineList.get(polyLineList.size() - 1))
                            .title("pickup location!"));

                            //Animation
                            ValueAnimator polyAnimator =  ValueAnimator.ofInt(0, 100);
                            polyAnimator.setDuration(2000);
                            polyAnimator.setInterpolator(new LinearInterpolator());
                            polyAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    List<LatLng> points = greyPolyLine.getPoints();
                                    int percentValues = (int) animation.getAnimatedValue();
                                    int size = points.size();
                                    int newPoints = (int) (size * (percentValues/100.0f));
                                    List<LatLng> p = points.subList(0, newPoints);
                                    backPolyLine.setPoints(p);
                                }
                            });

                            polyAnimator.start();

                            carMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                            .flat(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                            handler = new Handler();
                            index = 1;
                            next = 1;

                            handler.postDelayed(drawPathRunable, 3000);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                    Toast.makeText(WelcomeActivity.this, t.getMessage() + "", Toast.LENGTH_SHORT).show();
                }
            });
        }catch (Exception e){

        }
    }

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}