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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Date;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class KellotusFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ServiceConnection {

    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    protected static final String TAG = "MainActivity";


    private Accelerometer accModule;
    private TextView textView, textView2;
    private EditText editName,editComment;
    private ImageView can;
    private Button readyBtn, submitBtn;
    private InputMethodManager inputManager;

    private String message, rawData, name, comment;
    private int REFRESH_RATE, kellotustime, maxAngle;
    private double d, dbtime, timeResult;
    private long startTime, endTime, elapsedTime;
    private boolean kellotettu, loppu, aloitettu, resultDone, showEditFields;

    private MetaWearBoard mwBoard = null;
    private FragmentSettings settings;
    private StringBuilder strBuild;
    private GoogleApiClient gac;
    private Location loc;
    private DecimalFormatSymbols df;
    private ResultsDBHelper rdbh;
    private AddressResultReceiver mResultReceiver;

    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    protected String mAddressOutput;
    protected boolean mAddressRequested;


    public KellotusFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        updateValuesFromBundle(savedInstanceState);
        Activity owner = getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings = (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
        /**
         * Instantiate variables
         */
        mAddressRequested = false;
        mAddressOutput = "";
        kellotettu = false;
        loppu = false;
        aloitettu = false;
        resultDone = false;
        showEditFields = false;
        maxAngle = 0;


        REFRESH_RATE = 100;
        d = 0;
        strBuild = new StringBuilder();

        rdbh = new ResultsDBHelper(getContext());

        View view = inflater.inflate(R.layout.kellotus_layout, container, false);
        /**
         * Instantiate layout elements
         */
        textView = (TextView) view.findViewById(R.id.timeview);
        textView2 = (TextView) view.findViewById(R.id.angleview);
        readyBtn = (Button) view.findViewById(R.id.readyBtn);
        submitBtn = (Button) view.findViewById(R.id.submitBtn);
        editName = (EditText) view.findViewById(R.id.edit_name);
        editComment = (EditText) view.findViewById(R.id.edit_comment);
        can = (ImageView) view.findViewById(R.id.canView);
        inputManager = (InputMethodManager)
                owner.getSystemService(Context.INPUT_METHOD_SERVICE);

        editName.setVisibility(View.GONE);
        editComment.setVisibility(View.GONE);
        submitBtn.setVisibility(View.GONE);
        can.setImageResource(R.drawable.can);

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
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, loc);
        getActivity().startService(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
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
        loc = LocationServices.FusedLocationApi.getLastLocation(gac);
        if (loc != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(getContext(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }
            if (mAddressRequested) {
                startIntentService();
            }
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
        mwBoard = ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice());
        ready();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() {
    }

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

        readyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Start drinking when ready");
                can.setImageResource(R.drawable.orange_can_open);
                if (gac.isConnected() && loc != null) {
                    startIntentService();
                }

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


                                        mHandler2.removeCallbacks(angle);
                                        mHandler2.postDelayed(angle, 0);

                                        if (d >= 40 && aloitettu == false && kellotettu == false) {
                                            aloitettu = true;
                                            startTime = System.currentTimeMillis();
                                            mHandler.removeCallbacks(startTimer);
                                            mHandler.postDelayed(startTimer, 0);


                                        }
                                        if (d > 120) {
                                            kellotettu = true;
                                        }
                                        if (d < 45 && kellotettu == true && loppu == false) {
                                            loppu = true;
                                            endTime = System.currentTimeMillis();
                                            //Log.i("Timestamp end ", endTime.toString());
                                            Log.i("Result ", String.valueOf(timeResult));
                                            showEditFields = true;


                                        }
                                        if (kellotettu == true && aloitettu == true && loppu == true) {

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
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);
                name = "Anonymous";
                comment = "";
                if (!editName.getText().toString().isEmpty()) name = editName.getText().toString();
                if (!editComment.getText().toString().isEmpty()) comment = editComment.getText().toString();

                //count kellotustime


                int date = (int) (new Date().getTime()/1000);
                rdbh.insertResults(mAddressOutput, dbtime, kellotustime, date, comment, name, loc.getLatitude(), loc.getLongitude(), maxAngle);
                showToast("Saved successfully");
                editName.setText("");
                editComment.setText("");

                mAddressRequested = true;

                kellotettu = false;
                loppu = false;
                aloitettu = false;
                showEditFields = false;
                textView.setText("");
                textView2.setText("");
                editName.setVisibility(View.GONE);
                editComment.setVisibility(View.GONE);
                readyBtn.setVisibility(View.VISIBLE);
                submitBtn.setVisibility(View.GONE);


            }
        });
    }


    private final Runnable startTimer = new Runnable() {
        @Override
        public void run() {

            elapsedTime = (System.currentTimeMillis() - startTime);
            mHandler.postDelayed(this, REFRESH_RATE);
            dbtime = elapsedTime / 1000.0;

            kellotustime = 1;
            double valiLuku = (dbtime-28.9)/3.7;
            System.out.println("DBTIME" +dbtime);
            if ((dbtime-0.6) >= 2.5 && (dbtime-0.6)<5)kellotustime = 2;
            else if((dbtime-0.6) >= 5 && (dbtime-0.6)< 8) kellotustime = 3;
            else if((dbtime-0.6) >= 8 && (dbtime-0.6) < 11) kellotustime = 4;
            else if((dbtime-0.6) >= 11 && (dbtime-0.6) < 14.3) kellotustime = 5;
            else if((dbtime-0.6) >= 14.3 && (dbtime-0.6) < 17.6) kellotustime = 6;
            else if((dbtime-0.6) >= 17.6 && (dbtime-0.6) < 21.1) kellotustime = 7;
            else if((dbtime-0.6) >= 21.1 && (dbtime-0.6) < 24.6) kellotustime = 8;
            else if((dbtime-0.6) >= 24.6 && (dbtime-0.6) < 28.9) kellotustime = 9;
            else if ((dbtime-28.9) > 0) kellotustime = (int) valiLuku + 10;

            textView.setText(""+kellotustime);
            textView2.setText("" + elapsedTime / 1000.0+"s");
        }
    };
    private final Runnable angle = new Runnable() {
        @Override
        public void run() {
            mHandler2.postDelayed(this, REFRESH_RATE);

            Long l = Math.round(d);
            int i = Integer.valueOf(l.intValue());
            if (i < 0) i = 0;
            else if (i > 180) i = 180;
            //textView2.setText("" + i);
            can.setRotation(i);
            if (showEditFields) {
                editName.setVisibility(View.VISIBLE);
                editComment.setVisibility(View.VISIBLE);
                readyBtn.setVisibility(View.GONE);
                submitBtn.setVisibility(View.VISIBLE);
            }
            if(i > maxAngle){
                maxAngle = i;
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
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();
            System.out.println("RESULTCODE:::::" + resultCode);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                //showToast(getString(R.string.address_found));
            }
            if (resultCode == 1) {
                mAddressOutput = "Kellotettu";
            }
            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
        }
    }


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

