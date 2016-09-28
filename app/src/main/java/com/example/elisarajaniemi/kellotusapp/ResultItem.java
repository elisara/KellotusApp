package com.example.elisarajaniemi.kellotusapp;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Elisa Rajaniemi on 26.9.2016.
 */
public class ResultItem {

    public String address;
    public double time;
    public double kellotusTime;
    public String comment;
    public String name;
    public int date;
    public double latitude;
    public double longitude;


    public ResultItem(String address, double time, double kellotusTime, String comment, String name, int date, double latitude, double longitude){
        this.address = address;
        this.time = time;
        this.kellotusTime = kellotusTime;
        this.comment = comment;
        this.name = name;
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
    }



}
