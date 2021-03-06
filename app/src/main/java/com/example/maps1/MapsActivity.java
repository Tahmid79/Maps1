package com.example.maps1;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.maps1.R.layout.*  ;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback    {

    public GoogleMap mMap;

    public int REQUEST_CHECK_SETTINGS = 1 ;

    Location mCurrentLocation ;
    boolean  requestingLocationUpdates ;

    Button cancel_btn  ,  save_btn  , stop_btn;

    ArrayList<LatLng> cord =  new ArrayList<>() ;


    LocationService mService ;
    public boolean mBound = false ;
    public BroadcastReceiver receiver ;

    private static  final String TAG = "MyService" ;
    TextView distmet ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Data.loc = new ArrayList<>();
        Data.cord = new ArrayList<>() ;

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        receiver = new Receiver() ;

        IntentFilter filter = new IntentFilter() ;
        filter.addAction(LocationService.ACTION_BROADCAST);
        filter.addAction(LocationService.EXTRA_LOCATION);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver ,
               filter);

        distmet =  (TextView)findViewById(R.id.distMet) ;

        cancel_btn = (Button)findViewById(R.id.cancel_btn);
        save_btn = (Button)findViewById(R.id.save_btn) ;
        stop_btn = (Button)findViewById(R.id.stop_btn) ;

        cancel_btn.setVisibility(View.GONE) ;
        save_btn.setVisibility(View.GONE);
        stop_btn.setEnabled(false);

    }

    public void stopTracking(View view){
        mService.stopLocationUpdates();

        cancel_btn.setVisibility(View.VISIBLE) ;
        save_btn.setVisibility(View.VISIBLE);

    }

    public void startTracking(View view){
        clearMap();
        cancel_btn.setVisibility(View.GONE) ;
        save_btn.setVisibility(View.GONE);
        mService.startLocationUpdates();
    }

    public void saveTrack(View view){

        ContentValues values = new ContentValues() ;
        //saves the track record in the database

        values.put("duration" , Data.duration);
        values.put("date" ,  Data.date) ;
        values.put("distance" ,  Data.total_distance) ;

        getContentResolver().insert(TrackProvider.CONTENT_URI ,  values) ;
        clearMap();

        cancel_btn.setVisibility(View.GONE) ;
        save_btn.setVisibility(View.GONE);
        stop_btn.setEnabled(false);

    }

    public void cancel(View view){
        clearMap();         //cancels the current track of the user and does not save it

        cancel_btn.setVisibility(View.GONE) ;
        save_btn.setVisibility(View.GONE);
        stop_btn.setEnabled(false);
    }

    public void showTrackRecords(View view){
        Intent intent = new Intent(this ,  TracksActivity.class) ;
        startActivity(intent) ; //goes to a new activity to show all the tracks

    }

    public void clearMap(){

        mMap.clear();
        Data.total_distance = 0 ;
        Data.loc = new ArrayList<>() ;      //clears the map of markers
        Data.cord = new ArrayList<>() ;
        distmet.setText("Not Running");

    }

    public class Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //receives the broadcasts from the location service periodically
            //and updates the UI after receiving the location updates

            //Location lrecent = intent.getParcelableExtra(LocationService.EXTRA_LOCATION) ;
            //LatLng recent =  new LatLng(lrecent.getLatitude()  ,lrecent.getLongitude() ) ;
            LatLng recent =  Data.cord.get(Data.cord.size()-1) ;
            //cord.add(recent) ;

            PolylineOptions options = new PolylineOptions();
            options.addAll(Data.cord);
            options.width(8);
            options.color(Color.GREEN);


            mMap.addMarker(new MarkerOptions().position(recent).title("Current Location") ) ;
            mMap.addPolyline(options);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(recent));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(20), 2000, null);

            String reading  = Float.toString(Data.total_distance) + " metres "
                    + Double.toString(Data.duration) + " seconds" ;

            distmet.setText(reading);
            stop_btn.setEnabled(true);
        }

    }


    @Override
    protected void onStart() {
        super.onStart();

        Intent service =  new Intent(this, LocationService.class) ;
        bindService(service , mServiceConnection , Context.BIND_AUTO_CREATE ) ;
        //bind service
        //if service is in foreground mode it will stop foreground mode and will be a bound service
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mBound){

            unbindService(mServiceConnection);
            mBound  = false ;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();


        //this.registerReceiver(receiver , new IntentFilter( LocationService.ACTION_BROADCAST)) ;
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver ,
                new IntentFilter( LocationService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        //this.unregisterReceiver(receiver) ;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
      NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      manager.cancelAll(); //destroys the notifications
        super.onDestroy();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney , 4));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(5)  , 2000  ,null);
    }


    public void requestLocation(){


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mService.locationRequest) ;

        SettingsClient client  = LocationServices.getSettingsClient(this) ;

        Task<LocationSettingsResponse> task  = client.checkLocationSettings(builder.build()) ;

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                //mService.startLocationUpdates();

            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if ( e instanceof ResolvableApiException){

                    try{
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e ;

                        resolvableApiException.startResolutionForResult(MapsActivity.this ,
                                REQUEST_CHECK_SETTINGS);
                        //requests location permissions if not granted


                    }catch (IntentSender.SendIntentException sendEx){

                    }

                }

            }
        }) ;



    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CHECK_SETTINGS ) {
            //do if permission granted after prompt

            //mService.startLocationUpdates();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CHECK_SETTINGS && resultCode==RESULT_OK ) {
            //do if permission granted after prompt

            //mService.startLocationUpdates();
        }

    }



    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.MyBinder binder = (LocationService.MyBinder)service ;
            mService = binder.getBoundService();
            mBound = true ;
            requestLocation();
            //mService.startLocationUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null ;
            mBound = false ;


        }
    };




}





