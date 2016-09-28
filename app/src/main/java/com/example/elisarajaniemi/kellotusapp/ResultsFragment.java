package com.example.elisarajaniemi.kellotusapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class ResultsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    protected static final String TAG = "ResultFragment";

    private ListView listview;
    private ArrayList resultlist;
    private ResultsDBHelper rdbh;
    private MyArrayAdapter adapter;
    private MapView mapView;
    private GoogleMap map;
    private TextView countView;
    private int count;
    private TextView averageView;
    private int i;
    private LatLng latLng;

    private GoogleApiClient gac;
    private Location loc;
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.results_layout, container, false);


        mapView = (MapView) view.findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setMyLocationEnabled(true);

        i = 0;

        try {
            MapsInitializer.initialize(this.getActivity());
        } catch (Exception e) {
            e.printStackTrace();
        }


        listview = (ListView) view.findViewById(R.id.resultview);
        rdbh = new ResultsDBHelper(getContext());
        resultlist = rdbh.getResults();
        adapter = new MyArrayAdapter(getContext(),resultlist);
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long rowId) {
                // TODO Auto-generated method stub
                System.out.println("Position: "+position+ " Row ID: "+rowId);
                Object obj = listview.getAdapter().getItem(position);
                ResultItem ri = (ResultItem)obj;
                System.out.println("Time: " + ri.time);

            }

        });

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long rowId) {
                Intent sendIntent = new Intent();
                Object obj = listview.getAdapter().getItem(position);
                ResultItem ri = (ResultItem)obj;
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, ri.name + "\nAika: " + ri.time + "\nKellotusaika: "+ ri.kellotusTime +"\n" + ri.comment + "\n" + ri.address);
                sendIntent.setType("text/plain");
                //sendIntent.setPackage("com.whatsapp");
                startActivity(sendIntent);
                return true;
            }
        });

        count = resultlist.size();
        countView = (TextView) view.findViewById(R.id.times);
        countView.setText("KELLOTETTU: "+count+" times");

        averageView = (TextView) view.findViewById(R.id.average);
        averageView.setText("AVERAGE:        "+(Math.round(rdbh.getAverage()*100.0) / 100.0) +"s");


        // Add markers to map
        try {
            while (i < rdbh.getResults().size() - 1) {
                latLng = new LatLng(rdbh.getResults().get(i).latitude, rdbh.getResults().get(i).longitude);
                System.out.println("Latitude & Longitude: " + latLng);
                Marker mapMarker = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(rdbh.getResults().get(i).address)
                        .snippet(Double.toString(rdbh.getResults().get(i).kellotusTime)));
                i++;

            }
        }catch (Exception e){
            System.out.println(e);
        }

        buildGoogleApiClient();
        return view;
    }

    protected synchronized void buildGoogleApiClient() {
        gac = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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

    @Override
    public void onConnected(Bundle connectionHint) {
          loc = LocationServices.FusedLocationApi.getLastLocation(gac);
        if(loc !=null) {
            System.out.println(String.format("%s: %f", mLatitudeLabel, loc.getLatitude()));
            System.out.println("Latitude: " + loc.getLatitude());
            System.out.println(String.format("%s: %f", mLongitudeLabel, loc.getLongitude()));
            System.out.println("Longitude: " + loc.getLongitude());
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



}