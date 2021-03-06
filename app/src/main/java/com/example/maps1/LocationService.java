package com.example.maps1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class LocationService extends Service {

    public IBinder binder = new MyBinder();

    public FusedLocationProviderClient   fusedLocationClient ;
    public LocationCallback locationCallback ;
    public LocationRequest locationRequest ;


    ArrayList<LatLng> cord =  new ArrayList<>() ;
    Location mCurrentLocation  ;

    public String CHANNEL_ID = "channel1" ;
    public int NTF_ID = 12 ;

    //intent filters for broadcast
    public static String ACTION_BROADCAST = "com.example.maps1.broadcast"  ;
    public static String EXTRA_LOCATION =   "com.example.maps1.broadcast.location" ;

    public NotificationManager mNotificationManager ;



    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE) ;

        createNotificationChannel();
        createLocationRequest();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) ;

        createLocCallBack();


    }

    public void createLocCallBack(){
        locationCallback =  new LocationCallback(){

            @Override
            public void onLocationResult(LocationResult locationResult) {
                             super.onLocationResult(locationResult);

                if (locationResult == null) {
                    return;
                }


                Location recent = locationResult.getLastLocation() ;
                mCurrentLocation = recent  ;

                Intent broadcast = new Intent(LocationService.ACTION_BROADCAST);
                broadcast.putExtra(LocationService.EXTRA_LOCATION , mCurrentLocation) ;

                Data.cord.add(new LatLng(recent.getLatitude() ,  recent.getLongitude()) ) ;
                Data.loc.add(recent) ;

                distanceCalc();


                long t1 = Data.loc.get(0).getElapsedRealtimeNanos() ;
                long t2 = Data.loc.get(Data.loc.size()-1).getElapsedRealtimeNanos() ;
                double time =  (t2 - t1)/1e9 ;

                time = Math.round(time);
                Data.duration = time ;

                //Calendar cal = Calendar.getInstance();
                //String dt =  cal.getTime().toString();

               if(Data.cord.size() == 1) {
                  // Date date = Calendar.getInstance().getTime();
                   //SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                   //String formattedDate = df.format(date);
                    String formattedDate = getDateTime() ;
                   //Toast.makeText(getApplicationContext(), formattedDate, Toast.LENGTH_LONG).show();
                   Data.date = formattedDate ;
               }

                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast) ;

               mNotificationManager.notify(NTF_ID ,  getNotification());

            }
        };

    }

    public void distanceCalc(){

        int len = Data.cord.size() ;

        float [] results  = new float[3] ;

        float total_distance = 0 ;

        for (int i =0 ; i< len-1 ; i++){

            LatLng fst = Data.cord.get(i);
            LatLng snd = Data.cord.get(i+1) ;

            Location.distanceBetween(
                    fst.latitude , fst.longitude,
                    snd.latitude , snd.longitude ,
                    results

            );

            total_distance = total_distance+results[0] ;

        }

        total_distance = Math.round(total_distance) ;
        Data.total_distance = total_distance ;

    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

   public Notification getNotification(){

        Intent intent = new Intent(this , MapsActivity.class) ;
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP) ;


                NotificationCompat.Builder builder =
               new NotificationCompat.Builder(this, CHANNEL_ID) ;

       builder.setContentTitle("Fitness Tracker") ;
       //builder.setContentText("") ;
       builder.setSmallIcon(R.drawable.ic_launcher_foreground) ;
      // builder.setLargeIcon(BitmapFactory.decodeResource(getResources() , R.drawable.msc));
       //builder.setTicker() ;
       builder.setOngoing(true) ;
       builder.setPriority(Notification.PRIORITY_HIGH) ;

       PendingIntent pendingIntent = PendingIntent.getActivity(this.getBaseContext(), 0,intent, PendingIntent.FLAG_CANCEL_CURRENT ) ;
       builder.setContentIntent(pendingIntent) ;


       // Set the Channel ID for Android O.
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           builder.setChannelId(CHANNEL_ID); // Channel ID
       }

       return builder.build();

   }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        return START_NOT_STICKY;
    }


    protected void createLocationRequest(){

        locationRequest = LocationRequest.create() ;
        locationRequest.setInterval(2000)  ;
        locationRequest.setFastestInterval(500) ;
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) ;


    }

    public void startLocationUpdates(){

        Intent serviceStart = new Intent(getApplicationContext()  , LocationService.class) ;
        startService(serviceStart) ;

        fusedLocationClient.requestLocationUpdates(locationRequest ,
                locationCallback , Looper.getMainLooper() ) ;
    }

    public void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback) ;

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        return binder;
    }

    public class MyBinder extends Binder{

        LocationService getBoundService(){
            return LocationService.this  ;
        }


    }

    @Override
    public boolean onUnbind(Intent intent) {

        startForeground(NTF_ID ,  getNotification());
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        super.onRebind(intent);
    }



    public void createNotificationChannel(){



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }


    }


}
