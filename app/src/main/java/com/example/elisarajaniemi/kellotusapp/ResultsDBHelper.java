package com.example.elisarajaniemi.kellotusapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Elisa Rajaniemi on 26.9.2016.
 */
public class ResultsDBHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "resultsdb";
    static int DATABASE_VERSION = 1;
    private ResultItem ri;

    public ResultsDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS results(id INTEGER PRIMARY KEY AUTOINCREMENT, address VARCHAR, " +
                "time DOUBLE, kellotustime INT, date INT, comment VARCHAR, name VARCHAR, latitude DOUBLE, longitude DOUBLE, maxangle INT);");
        //values.clear();

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS results");
        onCreate(db);
    }

    public boolean insertResults (String address, double time, int kellotustime, int date, String comment, String name, double latitude, double longitude, int maxAngle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", address);
        contentValues.put("time", time);
        contentValues.put("kellotustime", kellotustime);
        contentValues.put("date", date);
        contentValues.put("comment", comment);
        contentValues.put("name", name);
        contentValues.put("latitude", latitude);
        contentValues.put("longitude", longitude);
        contentValues.put("maxangle", maxAngle);

        db.insert("results", null, contentValues);
        return true;
    }


    public ArrayList<ResultItem> getResults() {
        ArrayList<ResultItem> resultList = new ArrayList<ResultItem>();
        resultList.clear();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c =  db.rawQuery( "select * from results", null );
        c.moveToFirst();

        while(c.isAfterLast() == false){

            ri = new ResultItem(c.getString(c.getColumnIndex("address")), c.getDouble(c.getColumnIndex("time")),
                    c.getInt(c.getColumnIndex("kellotustime")),c.getString(c.getColumnIndex("comment")),
                    c.getString(c.getColumnIndex("name")),c.getInt(c.getColumnIndex("date")),c.getDouble(c.getColumnIndex("latitude")),
                    c.getDouble(c.getColumnIndex("longitude")),c.getInt(c.getColumnIndex("maxangle")));

            resultList.add(0,ri);
            c.moveToNext();
        }
        return resultList;
    }

    public ArrayList<ResultItem> getResultsByName(){
        ArrayList<ResultItem> resultByNameList = new ArrayList<>();
        resultByNameList.clear();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("select * from results order by name", null);
        c.moveToFirst();

        while(c.isAfterLast() == false){
            ri = new ResultItem(c.getString(c.getColumnIndex("address")), c.getDouble(c.getColumnIndex("time")),
                    c.getInt(c.getColumnIndex("kellotustime")),c.getString(c.getColumnIndex("comment")),
                    c.getString(c.getColumnIndex("name")),c.getInt(c.getColumnIndex("date")),c.getDouble(c.getColumnIndex("latitude")),
                    c.getDouble(c.getColumnIndex("longitude")),c.getInt(c.getColumnIndex("maxangle")));
            resultByNameList.add(ri);
            c.moveToNext();
        }
        return resultByNameList;
    }

    public ArrayList<ResultItem> getResultsByTime(){
        ArrayList<ResultItem> resultByTimeList = new ArrayList<>();
        resultByTimeList.clear();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("select * from results order by time", null);
        c.moveToFirst();

        while(c.isAfterLast() == false){
            ri = new ResultItem(c.getString(c.getColumnIndex("address")), c.getDouble(c.getColumnIndex("time")),
                    c.getInt(c.getColumnIndex("kellotustime")),c.getString(c.getColumnIndex("comment")),
                    c.getString(c.getColumnIndex("name")),c.getInt(c.getColumnIndex("date")),c.getDouble(c.getColumnIndex("latitude")),
                    c.getDouble(c.getColumnIndex("longitude")),c.getInt(c.getColumnIndex("maxangle")));
            resultByTimeList.add(ri);
            c.moveToNext();
        }
        return resultByTimeList;
    }

    public double getAverage(){
        double time = 0;
        double average = 0;
        int i = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c =  db.rawQuery( "select * from results", null );
        c.moveToFirst();

        while(c.isAfterLast() == false){
            time = time + c.getDouble(c.getColumnIndex("time"));
            i++;
            c.moveToNext();
        }

        average = time/i;
        return average;

    }


}
