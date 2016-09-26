package com.example.elisarajaniemi.kellotusapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class ResultsFragment extends Fragment {

    private ListView listview;
    private ArrayList resultlist;
    private ResultsDBHelper rdbh;
    private MyArrayAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.results_layout, container, false);
        listview = (ListView) view.findViewById(R.id.resultview);
        rdbh = new ResultsDBHelper(getContext());
        resultlist = rdbh.getResults();
        adapter = new MyArrayAdapter(getContext(),resultlist);

        //ArrayAdapter<ResultItem> arrayAdapter = new ArrayAdapter<ResultItem>(getActivity().getApplicationContext(), R.layout.my_list_item,R.id.addressview, resultlist);
        listview.setAdapter(adapter);

        return view;
    }



}
