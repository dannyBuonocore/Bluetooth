package com.dannybuonocore.bluetooth;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.os.AsyncTask;
import android.util.Log;

/**
 * @author Danny Buonocore
 * @name ConnectTask.java
 * @description This task waits for Bluetooth to be enabled, then
 * continuously attempts to connect to the specified device.
 */
public class ConnectTask extends AsyncTask<Void, Void, Void> {

    private static String TAG = "ConnectTask";

    /* References the widget that created this task */
    private BTWidget main;

    /* The time (in ms) between each connection attempt */
    private static final int DELAY = 10;

    public ConnectTask(BTWidget main) {
        super();
        this.main = main;
    }

    @Override
    protected Void doInBackground(Void... params) {

        // get a handle to the default bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        try {

            // ensure the adapter is enabled (handled by BTWidget)
            while (!adapter.isEnabled())
                Thread.sleep(DELAY);

            // attempt to connect
            try {
                main.connect();
                while (!main.isConnected()) ;
            } catch (IOException e) {
                Log.e(TAG, "Connection Error.");
            }

        } catch (InterruptedException e) {
            Log.e(TAG, "Thread has been interrupted.");
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {

        // notify widget of successful connection
        main.displayConnected();

        // TODO: include code to be run after task completes here
        Log.i(TAG, "Connect Task complete.");

    }

}