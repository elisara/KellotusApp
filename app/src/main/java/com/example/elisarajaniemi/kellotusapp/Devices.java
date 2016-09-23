package com.example.elisarajaniemi.kellotusapp;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class Devices extends Fragment{

    private ListView listView;
    private ArrayList devicelist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.devices_layout, container, false);
        listView = (ListView)view.findViewById(R.id.devicelist);
        devicelist = new ArrayList<String>();

        devicelist.add("device");
        devicelist.add("device");
        devicelist.add("device");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.my_list_item,R.id.list_textview, devicelist);
        listView.setAdapter(arrayAdapter);

        return view;
    }
}