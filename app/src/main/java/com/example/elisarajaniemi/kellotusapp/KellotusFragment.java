package com.example.elisarajaniemi.kellotusapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button sendBtn;

   // private MapView mapView;
    //private GoogleMap map;
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
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    protected String mAddressOutput;
    protected boolean mAddressRequested;
    private AddressResultReceiver mResultReceiver;
    private EditText editName;
    private EditText editComment;
    private Boolean showEditFields;



    public KellotusFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAddressRequested = false;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        showEditFields = false;

        //for counting
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
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.kellotus_layout, container, false);

        textView = (TextView) view.findViewById(R.id.timeview);
        textView2 = (TextView) view.findViewById(R.id.angleview);

        sendBtn = (Button) view.findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, rdbh.getResults().get(0).address.toString() + " " + Double.toString(rdbh.getResults().get(0).kellotusTime));
                sendIntent.setType("text/plain");
                //sendIntent.setPackage("com.whatsapp");
                startActivity(sendIntent);
            }
        });

        editName = (EditText) view.findViewById(R.id.edit_name) ;
        editComment = (EditText) view.findViewById(R.id.edit_comment) ;
        editName.setVisibility(View.GONE);
        editComment.setVisibility(View.GONE);

        /**
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
        */
        mResultReceiver = new AddressResultReceiver(new Handler());

        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = false;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        return view;
    }

    /**
     * Updates fields based on data stored in the bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
                System.out.println("Adress not found!!");
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                System.out.println("Bundle Update: " + savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY));
                //displayAddressOutput();
            }
        }
    }

    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(getContext(), FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, loc);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        getActivity().startService(intent);
    }

    /**
    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }
     */

    @Override
    public void onDestroy() {
        super.onDestroy();
       // mapView.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
    }
/**
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

*/
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
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(getContext(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }
            // It is possible that the user presses the button to get the address before the
            // GoogleApiClient object successfully connects. In such a case, mAddressRequested
            // is set to true, but no attempt is made to fetch the address (see
            // fetchAddressButtonHandler()) . Instead, we start the intent service here if the
            // user has requested an address, since we now have a connection to GoogleApiClient.
            if (mAddressRequested) {
                startIntentService();
            }
            System.out.println(String.format("%s: %f", mLatitudeLabel, loc.getLatitude()));
            System.out.println("Latitude: " + loc.getLatitude());
            System.out.println(String.format("%s: %f", mLongitudeLabel, loc.getLongitude()));
            System.out.println("Longitude: " + loc.getLongitude());
        } else {
            System.out.println("No location detected");
        }

        /**
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 13);
        map.animateCamera(cameraUpdate);
         */
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
                if (gac.isConnected() && loc != null) {
                    startIntentService();
                }
                mAddressRequested = true;
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
                                    showEditFields = true;
                                }
                                if(kellotettu == true && aloitettu == true && loppu == true){
                                    mHandler.removeCallbacks(startTimer);
                                    int date = (int)endTime % Integer.MAX_VALUE;
                                    if (resultDone == false) {
                                        rdbh.insertResults(mAddressOutput, dbtime, kellotustime, date, "comment", "name", loc.getLatitude(), loc.getLongitude());
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
            //displayAddressOutput();
            System.out.println("OSOITE: " + mAddressOutput);



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

            if(showEditFields) {
                editName.setVisibility(View.VISIBLE);
                editComment.setVisibility(View.VISIBLE);
            }


        }
    };

    private final Handler mHandler = new Handler();
    private final Handler mHandler2 = new Handler();


    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();
            System.out.println("RESULTCODE:::::" +resultCode);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }
            if(resultCode ==1){
                mAddressOutput = "Kellotettu";
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
        }
    }

    /**protected void displayAddressOutput() {
        textView3.setText(mAddressOutput);

    }*/

    protected void showToast(String text) {
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

}
