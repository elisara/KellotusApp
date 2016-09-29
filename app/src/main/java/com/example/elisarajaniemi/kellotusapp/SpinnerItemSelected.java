/**package com.example.elisarajaniemi.kellotusapp;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

/**
 * Created by Elisa Rajaniemi on 29.9.2016.


public class SpinnerItemSelected implements AdapterView.OnItemSelectedListener {
    public String value;

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        value = parent.getItemAtPosition(position).toString();
        //Toast.makeText(parent.getContext(), "OnItemSelectedListener : " + parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
        if(value.contains("DATE")){

        }
        else if(value.contains("RESULT")){

        }
        else if(value.contains("NAME")){

        }

    }

    public String sortResults(){

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

}
*/