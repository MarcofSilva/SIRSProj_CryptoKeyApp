package a46.cryptokey;

import android.util.Log;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MACHandler {
    private static final String MAC_ALGORITHM = "HmacSHA256";

    public byte[] getMAC(byte[] message, SecretKey key){
        byte[] digest = null;
        Mac mac;

        try {
            mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(key);
            digest = mac.doFinal(message);
            Log.d("PC_Message", "maccalculated = " + digest);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return digest;
    }

    public byte[] addMAC(byte[] msg, SecretKey key){
        byte[] mac = getMAC(msg,key);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32  + msg.length);
        byteBuffer.put(msg);
        byteBuffer.put(mac);

        return byteBuffer.array();
    }

    //Returns null if invalid and msg without the mac in the end
    public byte[] validateMAC(byte[] messageMac, SecretKey key) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageMac);
        byte[] IVandEncryptedMsg = new byte[messageMac.length - 32];
        byte[] mac = new byte[32];

        byteBuffer.get(IVandEncryptedMsg);
        byteBuffer.get(mac);

        byte[] calculatedMac = getMAC( IVandEncryptedMsg, key );
        if(!Arrays.equals(mac, calculatedMac)) {
            Log.d("PC_Message", "Error, MAC validation failed. Corrupted Message");
            Log.d("PC_Message", "MAC received = " + SecurityManager.getInstance().byteArrayToHexString(mac) + " -- calculated = " + SecurityManager.getInstance().byteArrayToHexString(calculatedMac));
            return null;
        }
        //return parte inicial da msg sem o mac
        Log.d("PC_Message", "MAC validated");
        return IVandEncryptedMsg;
    }

}
