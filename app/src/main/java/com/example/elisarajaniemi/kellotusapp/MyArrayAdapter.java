package com.example.elisarajaniemi.kellotusapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Elisa Rajaniemi on 26.9.2016.
 */
public class MyArrayAdapter extends ArrayAdapter<ResultItem> {

    public MyArrayAdapter(Context context, ArrayList<ResultItem> resultlist) {
        super(context, 0, resultlist);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        ResultItem ri = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.my_list_item, parent, false);
        }
        // Lookup view for data population
        TextView tvAddress = (TextView) convertView.findViewById(R.id.addressview);
        TextView tvName = (TextView) convertView.findViewById(R.id.nameview);
        TextView timeView = (TextView) convertView.findViewById(R.id.timeview);
        TextView kellotusView = (TextView) convertView.findViewById(R.id.kellotustimeview);
        // Populate the data into the template view using the data object
        tvAddress.setText(ri.address);
        tvName.setText(ri.name);
        timeView.setText(""+ri.time);
        kellotusView.setText(""+ri.kellotusTime);
        // Return the completed view to render on screen
        return convertView;
    }




}
