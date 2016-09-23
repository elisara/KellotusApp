package com.example.elisarajaniemi.kellotusapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

public class MainActivity extends AppCompatActivity  implements BleScannerFragment.ScannerCommunicationBus, ServiceConnection {
    public static final int REQUEST_START_APP= 1;

    private MetaWearBleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;
    Devices devices;
    Results results;
    LocationAndMap lam;
    Button mapButton, resultsButton, scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);

        devices = new Devices();
        results = new Results();
        lam = new LocationAndMap();

        //getSupportFragmentManager().beginTransaction().add(R.id.frag_container, lam).commit();

        mapButton = (Button) findViewById(R.id.mapbtn);
        mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frag_container, lam).commit();
            }
        });
        resultsButton = (Button) findViewById(R.id.resultbtn);
        resultsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frag_container, results).commit();
            }
        });
        scanButton = (Button) findViewById(R.id.scanbtn);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frag_container, devices).commit();
            }
        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_START_APP:
                ((BleScannerFragment) getFragmentManager().findFragmentById(R.id.scanner_fragment)).startBleScan();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public UUID[] getFilterServiceUuids() {
        return new UUID[] {MetaWearBoard.METAWEAR_SERVICE_UUID};
    }

    @Override
    public long getScanDuration() {
        return 10000L;
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device) {
        mwBoard= serviceBinder.getMetaWearBoard(device);

        final ProgressDialog connectDialog = new ProgressDialog(this);
        connectDialog.setTitle(getString(R.string.title_connecting));
        connectDialog.setMessage(getString(R.string.message_wait));
        connectDialog.setCancelable(false);
        connectDialog.setCanceledOnTouchOutside(false);
        connectDialog.setIndeterminate(true);
        connectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mwBoard.disconnect();
            }
        });
        connectDialog.show();

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                connectDialog.dismiss();

                Intent navActivityIntent = new Intent(MainActivity.this, DeviceSetupActivity.class);
                navActivityIntent.putExtra(DeviceSetupActivity.EXTRA_BT_DEVICE, device);
                startActivityForResult(navActivityIntent, REQUEST_START_APP);
            }

            @Override
            public void disconnected() {
                mwBoard.connect();
            }

            @Override
            public void failure(int status, Throwable error) {
                mwBoard.connect();
            }
        });
        mwBoard.connect();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (MetaWearBleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        //MenuInflater inflater = getMenuInflater();
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_disconnect) {

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
