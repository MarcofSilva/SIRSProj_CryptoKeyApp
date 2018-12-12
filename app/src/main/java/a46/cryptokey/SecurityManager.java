package a46.cryptokey;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityManager {

    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final String AES_ALGORITHM = "AES";

    private String _secretKey; //TODO Not here and not deal with string but with the real key
                                //TODO usar a chave para encriptacao da mensagem a enviar e para gerar o mac, provavelmente e mais seguro a partir desta chave obter duas independentes para cada caso with HKDF
    private KeyPair _keyPair; //TODO provavelmente todas as chaves que tou a guardar aqui por agr deverao ir, encriptadas por uma chave na keystore, para um ficheiro

    private long sessionNumber;



    private final KeyPairGenerator _keyPairGen;

    private AuthorizationHandler authHandler;
    private MACHandler macHandler;

    private static class SingletonHolder {
        private static final SecurityManager instance = new SecurityManager();
    }

    private SecurityManager() {
        _secretKey = null;
        authHandler = new AuthorizationHandler();
        macHandler = new MACHandler();

        sessionNumber = 0;
        //temporary variable used because _keyPairGen is final
        KeyPairGenerator tmpKeyPairGen = null;
        SecureRandom secureRandom = null;
        try {
            tmpKeyPairGen = KeyPairGenerator.getInstance("RSA");
            secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //TODO
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        _keyPairGen = tmpKeyPairGen;
        _keyPairGen.initialize(2048, secureRandom); //key size
    }

    public static synchronized SecurityManager getInstance() {
        return SingletonHolder.instance;
    }

    public void setSecretKey(String secretKey) {
        _secretKey = secretKey;
    }

    public SecretKey getSecretKey(String algorithm) {
        if(_secretKey == null) {
            return null;
        }
        byte[] secretKey = hexStringToBytes(_secretKey);
        return new SecretKeySpec(secretKey, algorithm);
    }

    public byte[] generateSecret(int numBytes) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[numBytes];
        secureRandom.nextBytes(key);
        return key;
    }

    public PublicKey generatePairOfKeys() {
        _keyPair = _keyPairGen.generateKeyPair();
        return _keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        if(_keyPair == null) {
            return null;
        }
        return _keyPair.getPrivate();
    }

    //Prepare the message to send with all the security procedures necessary for the context
    public byte[] prepareMessageToSend() throws Exception {
        ByteBuffer byteBuffer;
        byte[] msg;
        //Get private key needed for decryption
        PrivateKey privateKeyForDecryption = getPrivateKey();
        byte[] publicKeyForEncryptionEncoded = generatePairOfKeys().getEncoded();
        if(privateKeyForDecryption != null) {
            byte[] privateKeyForDecryptionEncoded = privateKeyForDecryption.getEncoded();

            byteBuffer = ByteBuffer.allocate(8 + privateKeyForDecryptionEncoded.length + publicKeyForEncryptionEncoded.length);
            byteBuffer.putInt(privateKeyForDecryptionEncoded.length);
            byteBuffer.putInt(publicKeyForEncryptionEncoded.length);
            byteBuffer.put(privateKeyForDecryptionEncoded);
            byteBuffer.put(publicKeyForEncryptionEncoded);
            msg = byteBuffer.array();
        }
        else {
            byteBuffer = ByteBuffer.allocate(8 + publicKeyForEncryptionEncoded.length);
            byteBuffer.putInt(0); //No private key in first send
            byteBuffer.putInt(publicKeyForEncryptionEncoded.length);
            byteBuffer.put(publicKeyForEncryptionEncoded);
            msg = byteBuffer.array();
        }
        //Apply the security procedures
        //TODO

        //Encrypt the MAC
        byte[] content = authHandler.addTimestampAndSessionNumber(msg, sessionNumber);
        byte[] IVandEncryptedMsg = encrypt(content, getSecretKey("AES"));
        byte[] secureMsg = macHandler.addMAC(IVandEncryptedMsg, getSecretKey(MAC_ALGORITHM));
        return secureMsg;
    }

    //Returns true if message is valid and false otherwise
    public boolean validateMessageReceived(byte[] input) throws Exception {
        byte[] IVandEncrypted;
        byte[] decrypted;
        byte[] msg;
        if((IVandEncrypted = macHandler.validateMAC(input, getSecretKey(MAC_ALGORITHM))) != null) {
            if((decrypted = decrypt(IVandEncrypted, getSecretKey("AES"))) != null) {
                if((msg = authHandler.validateTimestampAndSessionNumber(decrypted, ++sessionNumber)) != null) { //Incremented the session number
                    //All security requirements validated
                    return true;
                }
            }
        }
        //Reject message and connection
        return false;
    }

    public byte[] encrypt(byte[] array, SecretKey secretKey) throws Exception {
        // Generate IV.
        int ivSize = 16;
        byte[] iv = generateSecret(ivSize);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Encrypt.
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encrypted = cipher.doFinal(array);

        // Concatenate IV and encrypted part.
        byte[] IVandEncryptedMsg = new byte[ivSize + encrypted.length];
        System.arraycopy(iv, 0, IVandEncryptedMsg, 0, ivSize);
        System.arraycopy(encrypted, 0, IVandEncryptedMsg, ivSize, encrypted.length);

        return IVandEncryptedMsg;
    }

    public byte[] decrypt(byte[] IVandEncryptedMsg, SecretKey secretKey) throws Exception {
        int ivSize = 16;
        int keySize = 16;

        // Extract IV.
        byte[] iv = new byte[ivSize];
        System.arraycopy(IVandEncryptedMsg, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted part.
        int encryptedSize = IVandEncryptedMsg.length - ivSize;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(IVandEncryptedMsg, ivSize, encryptedBytes, 0, encryptedSize);

        // Decrypt.
        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

        return decrypted;
    }


    //Base64 Encoder and Decoder don't work for Android with API < 26 and the android used for testing with the lowest API version has API 23
    //As so the hexStringToBytes and byteArrayToHexString help us to convert byte arrays to strings and vice versa
    public byte[] hexStringToBytes(String hexInputString){
        byte[] byteArray = new byte[hexInputString.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexInputString.substring(2*i, 2*i+2), 16);
        }
        return byteArray;
    }

    public String byteArrayToHexString(byte[] byteArray) {
        StringBuffer buffer = new StringBuffer();

        for(int i =0; i < byteArray.length; i++){
            String hex = Integer.toHexString(0xff & byteArray[i]);

            if(hex.length() == 1)
                buffer.append("0");

            buffer.append(hex);
        }
        return buffer.toString();
    }
}
