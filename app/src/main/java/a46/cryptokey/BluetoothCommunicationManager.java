package a46.cryptokey;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.UUID;

public class BluetoothCommunicationManager extends Thread {
    private static final String UUID_STRING = "1a86d886-8382-4103-a0d2-98e61ce4d50c";
    private static final String APP_NAME = "CryptoKey";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mmServerSocket;
    private BluetoothSocket mSocket;

    private SecurityManager securityManager = SecurityManager.getInstance();

    public BluetoothCommunicationManager(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
        try {
            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, UUID.fromString(UUID_STRING));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(mSocket == null) {
            // Keep listening until exception occurs or a mSocket is returned
            try {
                Log.d("PC_Message", "Abrindo ServerSocket");//TODO
                mSocket = mmServerSocket.accept();
                Log.d("PC_Message", "Conectou");//TODO
            } catch (IOException e) {
                return;
            }
            // If a connection was accepted
            if (mSocket != null) {
                // Do work to manage the connection (in a separate thread?) TODO
                manageConnectedSocket(mSocket);
                mSocket = null;
            }
        }
    }

    //TODO sera preciso ir para outra thread?
    private void manageConnectedSocket(BluetoothSocket socket) {
        // Get the input and output streams, using temp objects because
        // member streams are final
        InputStream inStream;
        OutputStream outStream;
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch (IOException e) {
            return;
        }
        //TODO
        byte[] buffer = new byte[2048];  // buffer store for the stream
        int numBytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        try {
            // Read from the InputStream
            numBytes = inStream.read(buffer); //TODO fazer logica tal como no PC app
            byte[] received = new byte[numBytes];
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            byteBuffer.get(received, 0, numBytes);
            Log.d("PC_Message","received: " +  securityManager.byteArrayToHexString(received)); //TODO
            //TODO verify input as to know what is being asked and to authenticate and validate the integrity of the input
            if(securityManager.validateMessageReceived(received)) {
                //TODO send the answer
                byte[] msgToSend = securityManager.prepareMessageToSend();
                Log.d("PC_Message", "before sending " + securityManager.byteArrayToHexString(msgToSend));
                outStream.write(msgToSend);
                Log.d("PC_Message", "msgSent: " + securityManager.byteArrayToHexString(msgToSend));
            }
            else {
                Log.d("PC_Message","Message received not valid!!");
            }
            inStream.close();
            outStream.close();
            socket.close();
        } catch (IOException e) {
    } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Will cancel the listening mSocket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
            if(mSocket.isConnected()) {
                mSocket.close();
            }
        } catch (IOException e) { }
    }
}