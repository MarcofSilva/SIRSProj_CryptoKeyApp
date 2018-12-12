package a46.cryptokey;
import android.util.Log;

import java.util.Date;
import java.nio.ByteBuffer;

public class AuthorizationHandler {

    public byte[] addTimestamp(byte[] msg){
        ByteBuffer byteBuffer = ByteBuffer.allocate(8  + msg.length);
        byteBuffer.put(msg);

        byteBuffer.putLong(new Date().getTime());

        return byteBuffer.array();
    }

    public byte[] addSessionNumber(byte[] msg, long sessionnr) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8  + msg.length);
        byteBuffer.put(msg);

        byteBuffer.putLong(sessionnr);

        return byteBuffer.array();
    }

    //Returns null if invalid
    public byte[] validateTimestamp(byte[] msgtimestamp) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(msgtimestamp);
        byte[] msg = new byte[msgtimestamp.length-8];

        byteBuffer.get(msg);

        long timestamp = byteBuffer.getLong();
        long currentDate = new Date().getTime();

        //intervalo de 1seg pa tras 1 seg pa frente
        if( !(timestamp > currentDate-10000 && timestamp < currentDate+10000)){ //10000 milsecs is a bit to much, but it was necessary because the pc and the android used to testing were not synchronized
            Log.d("PC_Message", "Error validating timestamps");
            Log.d("PC_Message", "Timestamp received (ms) = " + timestamp + " -- current tipmestamp (ms)= " + currentDate);
            return null;
        }
        return msg;
    }

    public byte[] validateSessionNumber(byte[] msgSessionNumber, long sessionnr) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(msgSessionNumber);
        byte[] msg = new byte[msgSessionNumber.length-8];

        byteBuffer.get(msg);

        long sessionNumber = byteBuffer.getLong();

        if(sessionNumber != sessionnr ){
            Log.d("PC_Message", "Error validating session number");
            Log.d("PC_Message", "Received SessionNumber = " + sessionNumber + " -- Current SessionNumber = " + sessionnr);
            return null;
        }
        return msg;
    }

    public byte[] addTimestampAndSessionNumber(byte[] msg, long sessionnr) {
        return addSessionNumber(addTimestamp(msg),sessionnr);
    }

    public byte[] validateTimestampAndSessionNumber(byte[] msgtimesession, long sessionnr) {
        byte[] msgtime;
        byte[] msg;

        if((msgtime = validateSessionNumber(msgtimesession, sessionnr)) != null){
            if((msg = validateTimestamp(msgtime)) != null){
                return msg;
            }
        }
        return null;
    }
}
