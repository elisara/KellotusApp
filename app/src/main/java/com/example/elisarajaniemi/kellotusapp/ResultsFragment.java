package com.example.elisarajaniemi.kellotusapp;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */

public class ResultsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemSelectedListener {

    protected static final String TAG = "ResultFragment";

    private ArrayList resultlist;
    private ResultsDBHelper rdbh;
    private MyArrayAdapter adapter;
    private MapView mapView;
    private GoogleMap map;
    private LatLng latLng;
    private GoogleApiClient gac;
    private Location loc;

    private TextView averageView;
    private ListView listview;
    private TextView countView;
    private Spinner spinner;

    private int count, i;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.results_layout, container, false);

        //create map
        mapView = (MapView) view.findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setMyLocationEnabled(true);
        try {
            MapsInitializer.initialize(this.getActivity());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //create drop-down menu
        spinner = (Spinner) view.findViewById(R.id.sort);
        spinner.setOnItemSelectedListener(this);
        addItemsOnSpinner();

        //populate listview with results
        listview = (ListView) view.findViewById(R.id.resultview);
        rdbh = new ResultsDBHelper(getContext());
        resultlist = rdbh.getResults();
        adapter = new MyArrayAdapter(getContext(),rdbh.getResults());
        listview.setAdapter(adapter);

        //Find locations on map when clicking results
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long rowId) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(rdbh.getResults().get(position).latitude, rdbh.getResults().get(position).longitude), 17);
                map.animateCamera(cameraUpdate);
            }

        });

        //Share results
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long rowId) {
                Intent sendIntent = new Intent();
                Object obj = listview.getAdapter().getItem(position);
                ResultItem ri = (ResultItem)obj;
                sendIntent.setAction(Intent.ACTION_SEND);
                long shareDate;
                shareDate = (long)ri.date;
                Date d = new Date(shareDate *1000);
                SimpleDateFormat ft = new SimpleDateFormat("dd.MM.   HH:mm");



                if (ri.comment.length() > 0)sendIntent.putExtra(Intent.EXTRA_TEXT, ri.name + " "+ ri.kellotusTime + " (" + ri.time + "s)\n" + ri.comment + "\n" + ri.address);
                else sendIntent.putExtra(Intent.EXTRA_TEXT, ri.name + " "+ ri.kellotusTime + " (" + ri.time + "s)\n" + ri.address);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;
            }
        });

        //Kellotus count
        count = resultlist.size();
        countView = (TextView) view.findViewById(R.id.times);
        countView.setText("KELLOTETTU: "+count+" times");

        //Average
        averageView = (TextView) view.findViewById(R.id.average);
        averageView.setText("AVERAGE:        "+(Math.round(rdbh.getAverage()*100.0) / 100.0) +"s");


        // Add markers to map
        i = 0;
        try {
            while (i < rdbh.getResults().size() - 1) {
                latLng = new LatLng(rdbh.getResults().get(i).latitude, rdbh.getResults().get(i).longitude);
                System.out.println("Latitude & Longitude: " + latLng);
                Marker mapMarker = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(rdbh.getResults().get(i).address)
                        .snippet(rdbh.getResults().get(i).name +" " +rdbh.getResults().get(i).kellotusTime+" ("+rdbh.getResults().get(i).time +"s)"));
                i++;
            }

        }catch (Exception e){
            System.out.println(e);
        }

        buildGoogleApiClient();
        System.out.println("GAC DONE");

        return view;
    }

    public void addItemsOnSpinner() {
        List<String> list = new ArrayList<String>();
        list.add("DATE");
        list.add("RESULT  ");
        list.add("NAME");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        System.out.println("added items to spinner");
    }

    //selectiong items from drop-down menu
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String value = parent.getItemAtPosition(position).toString();
        //Toast.makeText(parent.getContext(), "OnItemSelectedListener : " + parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
        if(value.contains("DATE")){
            System.out.println("SORT: DATE");
            resultlist = rdbh.getResults();
            adapter = new MyArrayAdapter(getContext(),resultlist);
            listview.setAdapter(adapter);
        }
        else if(value.contains("RESULT")){
            System.out.println("SORT: RESULT");
            resultlist = rdbh.getResultsByTime();
            adapter = new MyArrayAdapter(getContext(),resultlist);
            listview.setAdapter(adapter);

        }
        else if(value.contains("NAME")){
            System.out.println("SORT: NAME");
            resultlist = rdbh.getResultsByName();
            adapter = new MyArrayAdapter(getContext(),resultlist);
            listview.setAdapter(adapter);

        }
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
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
        super.onResume();
        mapView.onResume();

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
        if (loc != null) {
            System.out.println("Latitude: " + loc.getLatitude());
            System.out.println("Longitude: " + loc.getLongitude());
        }
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 13);
        map.animateCamera(cameraUpdate);
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");

        gac.connect();
    }

}

