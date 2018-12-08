package a46.cryptokey;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.util.Set;
import java.util.UUID;

public class BluetoothClientManager extends Thread {
    private static final String UUID_STRING = "1a86d886-8382-4103-a0d2-98e61ce4d50c";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;

    public BluetoothClientManager(BluetoothAdapter bluetoothAdapter, BluetoothDevice device) {
        mBluetoothAdapter = bluetoothAdapter;
        mmDevice = device;
        createSocket();
    }

    // Set the new device
    public void setDevice(BluetoothDevice device) {
        mmDevice = device;
    }

    public void resetSocket() {
        mmSocket = null;
    }

    // Get a BluetoothSocket to connect with the given BluetoothDevice
    public void createSocket() {
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
        } catch (IOException e) {

        }
    }
//TODO funcionar desligando e ligando o bluetooth, mas ainda nao funciona afastando simplesmente o movel e aproximando (acho eu)
    @Override
    public void run() {
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                //TODO ignore?
            }
            return;
        }
        // Do work to manage the connection
        manageConnectedSocket(mmSocket);
        return;
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    //TODO sera preciso ir para outra thread
    private void manageConnectedSocket(BluetoothSocket socket) {
        // Get the input and output streams, using temp objects because
        // member streams are final
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch (IOException e) {
            return;
        }


        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = inStream.read(buffer);
                //TODO for testing
                outStream.write("Nem pensar".getBytes());
            } catch (IOException e) {
                break;
            }

            Log.d("PC_Message", new String(buffer));
        }
    }
}