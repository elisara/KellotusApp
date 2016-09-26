package com.example.elisarajaniemi.kellotusapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class KellotusFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,ServiceConnection {

    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    protected static final String TAG = "MainActivity";

    private MetaWearBoard mwBoard= null;
    private FragmentSettings settings;
    private Accelerometer accModule;
    TextView textView;
    TextView textView2;
    TextView textView3;
    private String message;
    private String rawData;
    private int REFRESH_RATE;
    private double d;
    private long startTime;
    private long endTime;
    private long elapsedTime;
    private double timeResult;
    private boolean kellotettu;
    private boolean loppu;
    private boolean aloitettu;
    private StringBuilder strBuild;
    private String kellotusData;

    private MapView mapView;
    private GoogleMap map;
    private GoogleApiClient gac;
    private Location loc;
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    private DecimalFormatSymbols df;
    private ArrayList resultList;
    private ResultsDBHelper rdbh;
    private boolean resultDone;
    private double dbtime;
    private double kellotustime;



    public KellotusFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        REFRESH_RATE = 100;
        kellotettu = false;
        loppu = false;
        aloitettu = false;
        d = 0;
        strBuild = new StringBuilder();
        kellotusData = "";
        resultDone = false;

        rdbh = new ResultsDBHelper(getContext());


        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings= (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.kellotus_layout, container, false);
        textView = (TextView) view.findViewById(R.id.timeview);
        textView2 = (TextView) view.findViewById(R.id.angleview);
        textView3 = (TextView) view.findViewById(R.id.dataview);

        mapView = (MapView) view.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setMyLocationEnabled(true);

        try {
            MapsInitializer.initialize(this.getActivity());
        } catch (Exception e) {
            e.printStackTrace();
        }

        buildGoogleApiClient();
        return view;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


    public void onStart() {
        gac.connect();
        super.onStart();
    }

    public void onStop() {
        gac.disconnect();
        super.onStop();
    }

    protected synchronized void buildGoogleApiClient() {
        gac = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        loc = LocationServices.FusedLocationApi.getLastLocation(gac);
        if (loc != null) {
            System.out.println(String.format("%s: %f", mLatitudeLabel, loc.getLatitude()));
            System.out.println("Latitude: " + loc.getLatitude());
            System.out.println(String.format("%s: %f", mLongitudeLabel, loc.getLongitude()));
            System.out.println("Longitude: " + loc.getLongitude());
        } else {
            System.out.println("No location detected");
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 13);
        map.animateCamera(cameraUpdate);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        gac.connect();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice());
        ready();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }

    /**
     * Called when the mwBoard field is ready to be used
     */
    public void ready() {
        try {
            accModule = mwBoard.getModule(Accelerometer.class);
            // Set the output data rate to 25Hz or closet valid value
            accModule.setOutputDataRate(4.f);
        } catch (UnsupportedModuleException e) {
            //Snackbar.make(getActivity().findViewById(R.id.device_setup_fragment), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.readybtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultDone = false;
                kellotettu = false;
                loppu = false;
                aloitettu = false;
                textView.setText("");
                accModule.routeData().fromAxes().stream("acc_stream").commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(final RouteManager result) {
                        result.subscribe("acc_stream", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                df = new DecimalFormatSymbols();
                                df.setDecimalSeparator('.');
                                rawData = msg.getData(CartesianFloat.class).toString();
                                message = rawData.substring(1, 6);
                                d = Double.parseDouble(message);
                                d = (d + 1.03) * 90;

                                //Log.i("tutorial", msg.getData(CartesianFloat.class).toString());
                                mHandler2.removeCallbacks(angle);
                                mHandler2.postDelayed(angle, 0);
                                strBuild.append(rawData + "\n");
                                if(d >= 50 && aloitettu == false && kellotettu == false){
                                    aloitettu = true;
                                    startTime = System.currentTimeMillis();
                                    //Log.i("Timestamp start ", startTime.toString());
                                    //mHandler.post(mUpdateUITimerTask);
                                    mHandler.removeCallbacks(startTimer);
                                    mHandler.postDelayed(startTimer, 0);

                                }
                                if(d > 100){
                                    kellotettu = true;
                                }
                                if(d < 90 && kellotettu == true && loppu == false){
                                    loppu = true;
                                    endTime = System.currentTimeMillis();
                                    //Log.i("Timestamp end ", endTime.toString());
                                    Log.i("Result ", String.valueOf(timeResult));
                                    kellotusData = strBuild.toString();
                                }
                                if(kellotettu == true && aloitettu == true && loppu == true){
                                    mHandler.removeCallbacks(startTimer);
                                    int date = (int)endTime % Integer.MAX_VALUE;
                                    if (resultDone == false) {
                                        rdbh.insertResults("uusosote", dbtime, kellotustime, date, "comment", "name");
                                        resultDone = true;
                                    }
                                }

                            }
                        });
                        accModule.enableAxisSampling();
                        accModule.start();
                    }
                });
            }
        });

    }


    private final Runnable startTimer = new Runnable() {
        @Override
        public void run() {

            elapsedTime = (System.currentTimeMillis() - startTime);
            //timeResult = (endTime - startTime) / 1000.0;
            mHandler.postDelayed(this, REFRESH_RATE);
            dbtime = elapsedTime/1000.0;
            kellotustime = elapsedTime/1000;
            textView.setText("" + elapsedTime / 1000.0);



        }
    };
    private final Runnable angle = new Runnable() {
        @Override
        public void run() {
            mHandler2.postDelayed(this, REFRESH_RATE);

            Long l = Math.round(d);
            int i = Integer.valueOf(l.intValue());
            if (i<0) i=0;
            else if (i>180) i=180;
            textView2.setText("" + i);
            textView3.setText(kellotusData);




        }
    };

    private final Handler mHandler = new Handler();
    private final Handler mHandler2 = new Handler();
}