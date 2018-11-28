package a46.cryptokey;
//TODO pensar qual a melhor distribuicao por packages
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

//RFC4226
class HOTP {

    private KeyManager _keyManager;
    private static final String MAC_ALGORITHM = "HmacSHA1"; //HmacSHA1 returns a 160bit (20bytes) message digest
    private int _numberDigitsOTP;

    public HOTP(int numberDigitsOTP) {
        _numberDigitsOTP = numberDigitsOTP;
        _keyManager = KeyManager.getInstance();
    }

    protected String generateOTP(long counter) {
        SecretKey secretKey = _keyManager.getSecretKey(MAC_ALGORITHM);
        if (secretKey == null) {
            return ""; //The UI activity will interpret this as no key was already created
        }

        byte[] counterByte = ByteBuffer.allocate(8).putLong(counter).array(); //Long to byte array

        byte[] hmacHash = hmacHash(secretKey, counterByte);

        // This piece of code is used in TOTP for truncate de hash value of 20 bytes generated by de hmacSHA-1
        // This truncated value will be the final one time password
        int offset = hmacHash[hmacHash.length-1] & 0xf;
        int truncatedHash = (hmacHash[offset++] & 0x7f) << 24 |
                            (hmacHash[offset++] & 0xff) << 16 |
                            (hmacHash[offset++] & 0xff) << 8 |
                            (hmacHash[offset] & 0xff);
        int otp = (truncatedHash % (int)(Math.pow(10, _numberDigitsOTP)));

        String finalOTP = Integer.toString(otp);
        //Get to the desired size in those rare cases where the modulo had given us one digit less
        while(finalOTP.length() < _numberDigitsOTP) {
            finalOTP = "0" + finalOTP;
        }

        return finalOTP;
    }

    private byte[] hmacHash(SecretKey secretKey, byte[] counter) {
        byte[] digest = null;

        Mac mac;
        try {
            mac = Mac.getInstance(MAC_ALGORITHM);

            mac.init(secretKey);

            digest = mac.doFinal(counter);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //TODO deal with exception
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            //TODO deal with exception
        }

        return digest;
    }
}
