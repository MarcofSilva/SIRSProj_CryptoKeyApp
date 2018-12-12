package a46.cryptokey;

import android.util.Log;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
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

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //TODO deal with exception
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            //TODO deal with exception
        }
        return digest;
    }

    public byte[] addMAC(byte[] msg, SecretKey key){
        ByteBuffer byteBuffer = ByteBuffer.allocate(msg.length + 256);
        byteBuffer.put(msg);
        byteBuffer.put(getMAC(msg,key));

        return byteBuffer.array();
    }

    //Returns null if invalid and msg without the mac in the end
    public byte[] validateMAC(byte[] messageMac, SecretKey key) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageMac);
        byte[] IVandEncryptedMsg = new byte[messageMac.length - 256];
        byte[] mac = new byte[256];

        byteBuffer.get(IVandEncryptedMsg);
        byteBuffer.get(mac);

        byte[] calculatedMac = getMAC( IVandEncryptedMsg, key );
        if(!Arrays.equals(mac, calculatedMac)) {
            Log.d("PC_Message", "received = " + mac + " -- calculated = " + calculatedMac);
            return null;
        }
        //return parte inicial da msg sem o mac
        return IVandEncryptedMsg;
    }

}
