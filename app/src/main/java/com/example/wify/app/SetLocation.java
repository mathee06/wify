package com.example.wify.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class SetLocation extends ActionBarActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private String TAG = ((Object) this).getClass().getSimpleName();
    private GoogleMap mMap;
    private Circle geoFence;
    private CircleOptions circleOptions;
    private LatLng currCoords;

    private TextView rssiA;
    private TextView txtConnectionStatus;
    private TextView txtLastKnownLoc;
    private EditText etLocationInterval;
    private TextView txtLocationRequest;

    private LocationClient locationclient;
    private LocationRequest locationrequest;
    private Intent mIntentService;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_location);
        rssiA = (TextView) findViewById(R.id.rssiA);
        txtConnectionStatus = (TextView) findViewById(R.id.txtConnectionStatus);
        txtLastKnownLoc = (TextView) findViewById(R.id.txtLastKnownLoc);
        etLocationInterval = (EditText) findViewById(R.id.etLocationInterval);
        txtLocationRequest = (TextView) findViewById(R.id.txtLocationRequest);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();


        NumberPicker numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(2000);
        numberPicker.setMinValue(15);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                geoFence.setRadius(newVal);
            }
        });


        mIntentService = new Intent(this,LocationService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, 0);

        int resp =GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resp == ConnectionResult.SUCCESS){
            locationclient = new LocationClient(this,this,this);
            locationclient.connect();
        }
        else {
            Toast.makeText(this, "Google Play Service Error " + resp, Toast.LENGTH_LONG).show();

        }
    }
    public void onClick(View v){
        if (v.getId() == R.id.getRSSI) {
            int wifiStrength = getSignalStrength(this);
            Log.i("FUCK", Integer.toString(wifiStrength));
            rssiA.setText(Integer.toString(wifiStrength));
        }
        if(v.getId() == R.id.btnLastLoc){
            if(locationclient!=null && locationclient.isConnected()){
                Location loc =locationclient.getLastLocation();
                Log.i(TAG, "Last Known Location :" + loc.getLatitude() + "," + loc.getLongitude());
                txtLastKnownLoc.setText(loc.getLatitude() + "," + loc.getLongitude());
                currCoords = new LatLng(loc.getLatitude(), loc.getLongitude());
                mMap.addMarker(new MarkerOptions().position(currCoords));
                circleOptions = new CircleOptions()
                        .center(currCoords)
                        .fillColor(0xffff0000)
                        .radius(10);
                geoFence = mMap.addCircle(circleOptions);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currCoords,15), 2000, null);
            }
        }
        if(v.getId() == R.id.btnStartRequest){
            if(locationclient!=null && locationclient.isConnected()){

                if(((Button)v).getText().equals("Start")){
                    locationrequest = LocationRequest.create();
                    locationrequest.setInterval(Long.parseLong(etLocationInterval.getText().toString()));
                    locationclient.requestLocationUpdates(locationrequest, this);
                    Location loc =locationclient.getLastLocation();
                    currCoords = new LatLng(loc.getLatitude(), loc.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currCoords));
                    circleOptions = new CircleOptions()
                            .center(currCoords)
                            .fillColor(0xffff0000)
                            .radius(10);
                    geoFence = mMap.addCircle(circleOptions);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currCoords,15), 2000, null);
                    ((Button) v).setText("Stop");
                }
                else{
                    locationclient.removeLocationUpdates(this);
                    ((Button) v).setText("Start");
                }
            }
        }
        if(v.getId() == R.id.btnRequestLocationIntent){
            if(((Button)v).getText().equals("Start")){

                locationrequest = LocationRequest.create();
                locationrequest.setInterval(100);
                locationclient.requestLocationUpdates(locationrequest, mPendingIntent);

                ((Button) v).setText("Stop");
            }
            else{
                locationclient.removeLocationUpdates(mPendingIntent);
                ((Button) v).setText("Start");
            }
        }
    }

    public static int getSignalStrength(Context c) {
        WifiManager wifiManager = (WifiManager) c.getSystemService(WIFI_SERVICE);
        return wifiManager.getConnectionInfo().getRssi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationclient!=null)
            locationclient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "onConnected");
        txtConnectionStatus.setText("Connection Status : Connected");

    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected");
        txtConnectionStatus.setText("Connection Status : Disconnected");

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "onConnectionFailed");
        txtConnectionStatus.setText("Connection Status : Fail");

    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){
            Log.i(TAG, "Location Request :" + location.getLatitude() + "," + location.getLongitude());
            txtLocationRequest.setText(location.getLatitude() + "," + location.getLongitude());
        }

    }
}