package a46.cryptokey;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

//Thread that hold the bluetooth server for communication with the PC App
public class EncryptionKeyHolder extends Thread {

    private static final String APP_NAME = "CryptoKey";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;

    public EncryptionKeyHolder(Context context) {
        bluetoothManager = context.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled())
                bluetoothAdapter.enable();
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, UUID.randomUUID());
            } catch (IOException e) {
                e.printStackTrace();
                //TODO handle exception
            }
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    @Override
    public void run() {
        BluetoothSocket bluetoothSocket;

        try {
            bluetoothSocket = bluetoothServerSocket.accept();
            bluetoothServerSocket.close();
            String a = null;
        } catch (IOException e) {
            e.printStackTrace();
            //TODO handle exception
        }
        super.run();

    }
}
