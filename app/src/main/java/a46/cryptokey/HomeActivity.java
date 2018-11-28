package a46.cryptokey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
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

public class HomeActivity extends AppCompatActivity {

    private static final String SECRET_KEY_FILENAME = "SecretKey";

    private static final int NUMBER_OF_DIGITS_IN_OTP = 6; //n belongs to [0, 9] (One time password size)
    private static final int TIME_RANGE_PASSWORD = 30; //For how long is a one time password valid until a new gets

    private KeyManager keyManager;
    private TOTP totp;
    private Thread otpUpdaterThread;
    private TextView otpTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //TODO delete
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        keyManager = new KeyManager();
        totp = new TOTP(readSecretKey(SECRET_KEY_FILENAME), NUMBER_OF_DIGITS_IN_OTP, TIME_RANGE_PASSWORD);
        otpTextView = findViewById(R.id.otp_txtView);

        //TODO se houver tempo ver melhor a espera por novo codigo
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
                StoreSecretKey(SECRET_KEY_FILENAME, secretKey);
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
                        StoreSecretKey(SECRET_KEY_FILENAME, secretKey);
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


    private void StoreSecretKey(String filename, String secret) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE); //MODE_PRIVATE make file private to the app
            fileOutputStream.write(secret.getBytes());
            fileOutputStream.close();

            Toast.makeText(HomeActivity.this,"New SecretKey stored", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //TODO Deal with exception in a proper way
        } catch (IOException e) {
            e.printStackTrace();
            //TODO Deal with exception in a proper way
        }
        //Update secretKey in TOTP generator
        totp.set_secretKey(secret);
    }

    private String readSecretKey(String filename) {
        String secretKey_string = null;

        try {
            FileInputStream fileInputStream = openFileInput(filename);
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

}
