package a46.cryptokey;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.util.UUID;

//Thread that hold the bluetooth server for communication with the PC App
public class BluetoothCommunicationManager extends Thread {
    private static final String APP_NAME = "CryptoKey";
    private static final String UUID_STRING = "1a86d886-8382-4103-a0d2-98e61ce4d50c";

    private final BluetoothServerSocket mServerSocket;

    public BluetoothCommunicationManager(BluetoothAdapter bluetoothAdapter) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, UUID.fromString(UUID_STRING));
        } catch (IOException e) { }
        mServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            if(socket == null) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        continue;
                    }
                    // Do work to manage the connection
                    manageConnectedSocket(socket);
                }
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mServerSocket.close();
        } catch (IOException e) { }
    }

    private void manageConnectedSocket(BluetoothSocket bluetoothSocket) {
        //TODO
    }
}