package a46.cryptokey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    private static final String SECRET_KEY_FILENAME = "SecretKey";

    //These two must be in sync with the ones in the pc app
    private static final int NUMBER_OF_DIGITS_IN_OTP = 6; //n belongs to [0, 9] (One time password size)
    private static final int TIME_RANGE_PASSWORD = 20; //For how long is a one time password valid until a new gets

    private EncryptionKeyHolder bluetoothConnectionHandler;
    private KeyManager keyManager;
    private TOTP totp;

    private Thread otpUpdaterThread;
    private TextView otpTextView;
    private ProgressBar totpProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //TODO delete
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        keyManager = KeyManager.getInstance();
        keyManager.setSecretKey(readSecretKey());
        totp = new TOTP(NUMBER_OF_DIGITS_IN_OTP, TIME_RANGE_PASSWORD);
        otpTextView = findViewById(R.id.otp_txtView);
        totpProgressBar = findViewById(R.id.totp_progressBar);

        //Thread that keep on calculating the otp and update the corresponding text view when it changes
        otpUpdaterThread = new Thread() {
            @Override
            public void run() {
                String old_otp = "";
                String new_otp;
                while(isAlive()) {

                    if((new_otp = totp.generateOTP()) != old_otp) {
                        final String finalNew_otp = new_otp;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                otpTextView.setText(finalNew_otp);
                                updateTOTPProgressBar();
                            }
                        });
                        old_otp = new_otp;
                    }
                    try {
                        Thread.sleep(TIME_RANGE_PASSWORD / 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        //TODO deal with exception in the best way
                    }

                }
            }
        };
        otpUpdaterThread.start();

        bluetoothConnectionHandler = new EncryptionKeyHolder(this);
        if(bluetoothConnectionHandler.getBluetoothAdapter() != null)
            bluetoothConnectionHandler.start();
        else
            Toast.makeText(this, "This device don't support bluetooth", Toast.LENGTH_LONG).show();

    }



    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you really want to leave?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HomeActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    public void scan_QRCode(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("");
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    //Method for getting result of IntentIntegrator that offers the QRCode reader
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(scanResult != null) {
            String secretKey = scanResult.getContents();
            if(secretKey != null && !secretKey.equals("")) {
                StoreSecretKey(secretKey);
                updateTOTPOnScreen();
                Toast.makeText(HomeActivity.this,"New SecretKey stored", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, secretKey, Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
        }
    }


    public void inputSecretKey(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        builder.setTitle("Insert Secret Key:")
                .setView(input)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO check input before just accepting it
                        String secretKey = input.getText().toString();
                        StoreSecretKey(secretKey);
                        updateTOTPOnScreen();
                        Toast.makeText(HomeActivity.this,"New SecretKey stored", Toast.LENGTH_SHORT).show();
                        Toast.makeText(HomeActivity.this, secretKey, Toast.LENGTH_LONG).show();

                        }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();

                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void StoreSecretKey(String secret) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(SECRET_KEY_FILENAME, Context.MODE_PRIVATE); //MODE_PRIVATE make file private to the app
            fileOutputStream.write(secret.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //TODO Deal with exception in a proper way
        } catch (IOException e) {
            e.printStackTrace();
            //TODO Deal with exception in a proper way
        }
        //Update secretKey
        keyManager.setSecretKey(secret);
    }

    private String readSecretKey() {
        String secretKey_string = null;

        try {
            FileInputStream fileInputStream = openFileInput(SECRET_KEY_FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            secretKey_string = bufferedReader.readLine();

            fileInputStream.close();
        } catch (FileNotFoundException e) {
            return null; //No secret key in storage for the moment
        } catch (IOException e) {
            e.printStackTrace();
            //TODO Deal with exception in a proper way
        }

        return secretKey_string;
    }

    private void updateTOTPOnScreen() {
        otpTextView.setText(totp.generateOTP());
        updateTOTPProgressBar();
    }

    private void updateTOTPProgressBar() {
        totpProgressBar.setVisibility(View.VISIBLE);

        Date dt = new Date();
        float curSec = (float)dt.getSeconds();
        int progress;
        if(curSec > TIME_RANGE_PASSWORD)
            curSec = curSec % TIME_RANGE_PASSWORD;
        progress = (int)(100 - ((curSec/TIME_RANGE_PASSWORD)*100));

        totpProgressBar.setProgress(progress);
    }
}
