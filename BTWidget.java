package com.dannybuonocore.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * @author Danny Buonocore
 * @file BTWidget.java
 * @description A wrapper class for an Android AppWidget to provide basic
 * Bluetooth functionality. Your AppWidgetProvider should extend this class.
 */
public class BTWidget extends AppWidgetProvider {

    private static final String TAG = "BTWidget";

    /* The time (in ms) between each packet request check */
    private static final int DELAY = 10;

    /* The size of the input buffer */
    private static final int BUFFER_SIZE = 1024;

    /* The current status of the Bluetooth connection */
    static final byte STATUS_DISCONNECTED = 0x00;
    static final byte STATUS_CONNECTING = 0x01;
    static final byte STATUS_CONNECTED = 0x02;

    /* initialize the current status to disconnected */
    static byte status = STATUS_DISCONNECTED;

    /* Universal transmission control characters */
    static final byte NUL = 0x00;       // null terminator
    static final byte SOH = 0x01;       // start of header
    static final byte STX = 0x02;       // start of text
    static final byte ETX = 0x03;       // end of text
    static final byte EOT = 0x04;       // end of transmission

    /* The MAC address of the slave device */
    protected String macAddress = "10:14:06:26:04:87";

    /* Bluetooth socket data streams */
    protected OutputStream outputStream;
    protected InputStream inputStream;

    /* The AsyncTask used for connecting and pairing to a device */
    private ConnectTask task;

    /* The AsyncTask used for polling the input stream */
    private ReadTask readTask;

    /* The Bluetooth socket used to communicate with the slave device */
    private BluetoothSocket socket;

    // ************************************************************************
    // Write
    // ************************************************************************

    /**
     * Writes a string of data to the connected device.
     * Writes 0x00 if the supplied string is null.
     *
     * @param s The string of data (or null for 0x00) to write.
     * @throws IOException If the output stream fails.
     */
    protected void write(String s) throws IOException {
        if (s == null) outputStream.write(NUL);
        else outputStream.write(s.getBytes());
    }

    /**
     * Writes a single byte to the connected device.
     *
     * @param b The byte to write.
     * @throws IOException If the output stream fails.
     */
    protected void write(byte b) throws IOException {
        outputStream.write(b);
    }

    /**
     * Writes a string of data to the connected device in packets
     * of the specified length. Can be used in conjunction with reading
     * to send data too large for the target device's input buffer.
     * Write 0x00 if the supplied string is null.
     *
     * @param s   The string of data (or null for 0x00) to write.
     * @param len The maximum packet length.
     * @throws IOException If the output stream fails.
     */
    protected void writePackets(String s, int len) throws IOException {

        if (s == null) outputStream.write(NUL);

        else if (len > s.length())
            write(s);

        else {

            while (s.length() > 0) {

                // get a chunk of the data and send it
                outputStream.write(s.substring(0, len).getBytes());
                s = s.substring(len);

                // wait for receiver to request the next packet
                try {
                    while (!ReadyToSendNext())
                        Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    Log.e(TAG + ".writePackets()", "Thread has been interrupted.");
                }
            }

        }

    }

    /**
     * Writes the transmission control character indicating
     * the start of a transmission header.
     *
     * @throws IOException If the output stream fails.
     */
    protected void beginTransmission() throws IOException {
        outputStream.write(SOH);
    }

    /**
     * Writes the transmission control character indicating
     * the beginning of a block of text.
     *
     * @throws IOException If the output stream fails.
     */
    protected void beginText() throws IOException {
        outputStream.write(STX);
    }

    /**
     * Writes the transmission control character indicating
     * the end of a block of text.
     *
     * @throws IOException If the output stream fails.
     */
    protected void endText() throws IOException {
        outputStream.write(ETX);
    }

    /**
     * Writes the transmission control character indicating
     * the end of a transmission.
     *
     * @throws IOException If the output stream fails.
     */
    protected void endTransmission() throws IOException {
        outputStream.write(EOT);
    }

    /**
     * Returns true if the slave device is ready to receive the next packet.
     * This method is used by writePackets() to send large volumes of data
     * in smaller chunks.
     *
     * @return True if slave device is ready, false otherwise.
     */
    protected boolean ReadyToSendNext() {

        // TODO: read from slave until request is received
        throw new UnsupportedOperationException("Implement ReadyToSendNext()");

    }

    // ************************************************************************
    // Enabling and Disabling
    // ************************************************************************

    protected void enableBT() {
        BluetoothAdapter.getDefaultAdapter().enable();
    }

    protected void disableBT() {
        BluetoothAdapter.getDefaultAdapter().disable();
    }

    protected void enableRead() {
        if (readTask != null)
            readTask.setReading(true);
    }

    protected void disabledRead() {
        if (readTask != null)
            readTask.setReading(false);
    }

    // ************************************************************************
    // Connecting and Pairing
    // ************************************************************************

    /**
     * Creates and starts the AsyncTask to connect to the slave device.
     * This is the first method that should be called in the Bluetooth process.
     * With the Bluetooth-Admin permission in the Android Manifest, this method
     * is able to enable Bluetooth and pair to a device without prompting
     * the user for confirmation. While Google does not consider this a best
     * practice, it is sometimes desirable for a smoother user experience.
     */
    protected void beginConnectionTask() {
        task = new ConnectTask(this);
        task.execute();
        status = STATUS_CONNECTING;
    }

    /**
     * Runs on the ConnectTask thread to enable Bluetooth in the background,
     * connect, and pair to the slave device. This method blocks the thread
     * until the connection is successful or an exception is thrown.
     *
     * @throws IOException If the socket could not be opened.
     */
    public void connect() throws IOException {

        try {

            // get a handle to the default bluetooth adapter
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            // ensure that the device is bluetooth enabled
            if (adapter != null) {

                // enable the bluetooth adapter (if not done so already)
                if (!adapter.isEnabled())
                    BluetoothAdapter.getDefaultAdapter().enable();

                // get the device from it's mac address
                BluetoothDevice device = adapter.getRemoteDevice(macAddress);

                // connect to the device
                Log.i(TAG, "Connecting to device: " + device.toString());
                pairDevice(device);

                // get the universally unique identification number
                UUID uuid = device.getUuids()[0].getUuid();

                // open a socket and connect to the device
                socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();

                // get handles to the socket's input and output streams
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                Log.i(TAG, "Connection successful!");

            }
        } catch (IOException e) {

            Log.e(TAG + ".connect()", "Connection error.");
            status = STATUS_DISCONNECTED;
            throw e;

        }
    }

    /**
     * Runs on the ConnectTask thread to pair with the specified
     * device in the background.
     *
     * @param device The Bluetooth device to pair with.
     */

    private void pairDevice(BluetoothDevice device) {

        try {

            Log.i(TAG + ".pairDevice()", "start pairing...");

            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

            Log.i(TAG + ".pairDevice()", "pairing finished.");

        } catch (Exception e) {

            Log.e("pairDevice()", "Error pairing\n" + e.getMessage());

        }

    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    /**
     * Called by ConnectTask once a successful connected has been made.
     */
    public void displayConnected() {

        // begin polling for input
        readTask = new ReadTask(this);
        readTask.execute();

        status = STATUS_CONNECTED;

    }

    // ************************************************************************
    // getters
    // ************************************************************************

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

}
