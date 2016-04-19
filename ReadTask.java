package com.dannybuonocore.bluetooth;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

import javax.sql.RowSetReader;

/**
 * @author Danny Buonocore
 * @file ReadTask.java
 * @description This task polls in the background to read incoming serial data.
 */
public class ReadTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "ReadTask";

    /* The time (in ms) between each input reading */
    private static final int DELAY = 10;

    /* References the widget that created this task */
    private BTWidget widget;

    /* Stores incoming data stream */
    private int buffer;

    /**
     * If true, available bytes in the input stream will be read
     * into the buffer. Otherwise, the input stream will continue to
     * receive incoming data, but will not be read in until set back to true.
     */
    private boolean reading;

    public ReadTask(BTWidget widget) {
        super();
        this.widget = widget;

        Log.i(TAG, "Read Task created");
    }

    @Override
    protected Void doInBackground(Void... params) {

        Log.i(TAG, "Read Task running...");

        // start reading and initialize the buffer
        setReading(true);
        buffer = -1;

        try {

            while (true) {

                // only read if enabled
                if (!reading) {
                    Thread.sleep(DELAY);
                    continue;
                }

                // wait until a byte has been read
                while (buffer == -1) {
                    Thread.sleep(DELAY);
                    buffer = widget.getInputStream().read();
                }

                // TODO: do something with the data
                Log.d(TAG, "Byte Read: " + buffer);

            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading data.");
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread has been interrupted.");
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {

        // TODO: include code to be run when input is no longer accepted here
        Log.i(TAG, "Read Task has stopped.");

    }

    public void setReading(boolean b) {
        reading = b;
    }

    public boolean getReading() {
        return reading;
    }

}