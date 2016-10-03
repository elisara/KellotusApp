package com.example.elisarajaniemi.kellotusapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import static android.R.attr.tension;
import static android.R.attr.visible;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Elisa Rajaniemi on 26.9.2016.
 */
public class MyArrayAdapter extends ArrayAdapter<ResultItem> {

    ResultItem ri;

    public MyArrayAdapter(Context context, ArrayList<ResultItem> resultlist) {
        super(context, 0, resultlist);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ri = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.my_list_item, parent, false);
        }
        long shareDate;
        shareDate = (long)ri.date;
        Date d = new Date(shareDate *1000);
        SimpleDateFormat ft = new SimpleDateFormat("E,  d.MM  HH:mm");

        TextView tvName = (TextView) convertView.findViewById(R.id.nameview);
        TextView timeView = (TextView) convertView.findViewById(R.id.timeview);
       // TextView tvAngle = (TextView) convertView.findViewById(R.id.angle);
        TextView commentView = (TextView) convertView.findViewById(R.id.commentview);
        TextView tvDate = (TextView) convertView.findViewById(R.id.dateview);

        tvDate.setText(ft.format(d));
        tvName.setText(ri.name);
        timeView.setText(""+ri.kellotusTime+ "  (" + ri.time+"s)");
        //tvAngle.setText(""+ri.maxAngle);
        commentView.setText(ri.comment);

        return convertView;
    }


}
