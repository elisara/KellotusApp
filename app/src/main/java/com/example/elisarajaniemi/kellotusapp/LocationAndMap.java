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
import android.support.design.widget.Snackbar;
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class LocationAndMap extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,ServiceConnection {

    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    protected static final String TAG = "MainActivity";

    private MetaWearBoard mwBoard= null;
    private FragmentSettings settings;
    private Accelerometer accModule;
    TextView textView;
    private String message;
    private int REFRESH_RATE;
    private double d;
    private long startTime;
    private long endTime;
    private long elapsedTime;
    private double timeResult;
    private boolean kellotettu;
    private boolean loppu;
    private boolean aloitettu;

    private MapView mapView;
    private GoogleMap map;
    private GoogleApiClient gac;
    private Location loc;
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    private DecimalFormatSymbols df;

    public LocationAndMap(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        REFRESH_RATE = 100;
        kellotettu = false;
        loppu = false;
        aloitettu = false;
        d = 0;



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
        View view = inflater.inflate(R.layout.map_layout, container, false);
        textView = (TextView) view.findViewById(R.id.timeview);

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

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10);
        map.animateCamera(cameraUpdate);
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
            System.out.println(String.format("%s: %f", mLongitudeLabel, loc.getLongitude()));
        } else {
            System.out.println("No location detected");
        }
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
            Snackbar.make(getActivity().findViewById(R.id.device_setup_fragment), e.getMessage(),
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.readybtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accModule.routeData().fromAxes().stream("acc_stream").commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(final RouteManager result) {
                                result.subscribe("acc_stream", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message msg) {
                                        df = new DecimalFormatSymbols();
                                        df.setDecimalSeparator('.');
                                        message = msg.getData(CartesianFloat.class).toString().substring(1, 6);

                                            d = Double.parseDouble(message);
                                            d = (d + 1.03) * 90;

                                        //Log.i("tutorial", msg.getData(CartesianFloat.class).toString());


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

                                        }
                                        if(kellotettu == true && aloitettu == true && loppu == true){
                                            kellotettu = false;
                                            loppu = false;
                                            aloitettu = false;
                                            //mChronometer.stop();
                                            //textView.append("Result: " + timeResult + "\n");
                                            mHandler.removeCallbacks(startTimer);

                                        }

                                    }
                                });
                                accModule.enableAxisSampling();
                                accModule.start();
                            }
                        });
            }
        });
        /**
        view.findViewById(R.id.acc_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accModule.stop();
                accModule.disableAxisSampling();
                mwBoard.removeRoutes();
            }
        });
         */
    }

    /** private final Runnable mUpdateUITimerTask = new Runnable() {
    @Override
    public void run() {
    //textView.setText("Timer: " + mChronometer.start());
    mChronometer.setBase(SystemClock.elapsedRealtime());
    mChronometer.start();
    }
    };
     */
    private final Runnable startTimer = new Runnable() {
        @Override
        public void run() {

            elapsedTime = (System.currentTimeMillis() - startTime);
            timeResult = (endTime - startTime) / 1000.0;
            mHandler.postDelayed(this, REFRESH_RATE);
            textView.setText("Time: " + elapsedTime / 1000.0);

        }
    };

    private final Handler mHandler = new Handler();

}