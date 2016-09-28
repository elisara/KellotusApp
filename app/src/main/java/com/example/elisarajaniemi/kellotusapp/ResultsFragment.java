package com.example.elisarajaniemi.kellotusapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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
public class ResultsFragment extends Fragment {

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




        if(rdbh.getResults().size() != 0) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(rdbh.getResults().get(0).latitude, rdbh.getResults().get(0).longitude), 13);
            map.animateCamera(cameraUpdate);
        }
        else{
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 13);
            map.animateCamera(cameraUpdate);

        }

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

        Marker mapMarker = map.addMarker(new MarkerOptions()
                .position(new LatLng(10, 10))
                .title("Muumimaailma"));

        Marker mapMarker2 = map.addMarker(new MarkerOptions()
                .position(new LatLng(10.01, 10.01))
                .title("Muumimaailma 2"));

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

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }



}