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

import java.util.ArrayList;

/**
 * Created by Elisa Rajaniemi on 22.9.2016.
 */
public class ResultsFragment extends Fragment {

    private ListView listview;
    private ArrayList resultlist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.results_layout, container, false);
        listview = (ListView) view.findViewById(R.id.resultview);
        resultlist = new ArrayList<String>();

        resultlist.add("result");
        resultlist.add("result");
        resultlist.add("result");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.my_list_item,R.id.list_textview, resultlist);
        listview.setAdapter(arrayAdapter);

        return view;
    }
}
