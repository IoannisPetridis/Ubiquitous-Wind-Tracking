package es.hol.ubiquitous.windtrack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.weatherflow.windmeter.sensor_sdk.entities.HeadsetState;
import com.weatherflow.windmeter.sensor_sdk.sdk.AnemometerObservation;
import com.weatherflow.windmeter.sensor_sdk.sdk.HeadphonesReceiver;
import com.weatherflow.windmeter.sensor_sdk.sdk.IHeadphonesStateListener;
import com.weatherflow.windmeter.sensor_sdk.sdk.WFConfig;
import com.weatherflow.windmeter.sensor_sdk.sdk.WFSensor;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class MeasurementActivity extends ActionBarActivity implements SensorEventListener, IHeadphonesStateListener,WFSensor.OnValueChangedListener {
    private final static String HEADSET_ACTION = "android.intent.action.HEADSET_PLUG";

    private TextView mSpeed,comment,loading,firstname,surname;

    private HeadphonesReceiver mHeadphonesReceiver;
    private float count = 1.0f;
    private float currentDegree = 0.0f;
    private float accuracy;
    private double speed_count = 0.0;
    double longitude,latitude,altitude,velocity;
    private String email,timestamp,deviceid;
    private SensorManager mSensorManager;
    private HttpClient client;
    private HttpGet request;
    Button send_data;
    DialogFragment additionalinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        checkLocationServices();
        checkConnection(getApplicationContext());
        if (!checkMeterConnected()) {
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
        }

        send_data = (Button)findViewById(R.id.send_data);
        send_data.setVisibility(View.INVISIBLE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        Intent intent = getIntent();
        longitude = intent.getDoubleExtra("LONGITUDE",0);
        latitude = intent.getDoubleExtra("LATITUDE",0);
        altitude = intent.getDoubleExtra("ALTITUDE",0);
        accuracy = intent.getFloatExtra("ACCURACY", 0.0f);
        email = intent.getStringExtra("EMAIL");
        deviceid = intent.getStringExtra("DEVICEID");

        mSpeed = (TextView) findViewById(R.id.speed);
        loading = (TextView) findViewById(R.id.loading);
        firstname = (TextView) findViewById(R.id.firstname);
        surname = (TextView) findViewById(R.id.surname);
        comment = (TextView) findViewById(R.id.comment);

        mSpeed.setVisibility(View.INVISIBLE);
        firstname.setVisibility(View.INVISIBLE);
        surname.setVisibility(View.INVISIBLE);
        comment.setVisibility(View.INVISIBLE);


        mHeadphonesReceiver = new HeadphonesReceiver(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        WFSensor.getInstance(MeasurementActivity.this).setOnValueChangedListener(MeasurementActivity.this);

        //We modify the Thread Policy in order to execute HTTPGet request in our main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }


    //Checks whether Wi-Fi or mobile data are available
    public void checkConnection(Context context) {
        //Get Connectivity Manager
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wi-Fi or data is not enabled");
            builder.setMessage("Please enable Wi-Fi/data usage");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show mobile data settings when the user presses OK
                    Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                    startActivity(intent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    //Checks whether Location Services and GPS are enabled
    public void checkLocationServices() {
        // Get Location Manager and check for GPS & Network location services
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location services are not active");
            builder.setMessage("Please enable location services and GPS");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user presses OK
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    //Checks if the wind anemometer is connected
    public boolean checkMeterConnected() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (!audio.isWiredHeadsetOn()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wind anemometer is not plugged in");
            builder.setMessage("Please plug in the anemometer");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public void onStart() {
        if (!checkMeterConnected()) {
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
        }
        super.onStart();
        registerReceiver(mHeadphonesReceiver, new IntentFilter(HEADSET_ACTION));
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
        WFConfig.getAnoConfig(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loading.setText("Measuring...please wait!");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WFSensor.getInstance(this).onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WFSensor.getInstance(this).onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        HeadsetState state = new HeadsetState();
        state.setPluggedIn(false);
        onHeadphonesStateChanged(state);
        //unregisterReceiver(mHeadphonesReceiver);
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onHeadphonesStateChanged(HeadsetState headsetState) {
        WFSensor.getInstance(this).onHeadphonesStateChanged(headsetState);
    }

    @Override
    public void onValueChanged(final AnemometerObservation anemometerObservation) {
        if (count ==10.0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    velocity =  speed_count / count;
                    mSpeed.setText("" + velocity);
                    mSpeed.setVisibility(View.VISIBLE);
                    loading.setText("Wind velocity is approximately:");
                    SimpleDateFormat gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    timestamp = gmtDateFormat.format(new Date());
                    firstname.setVisibility(View.VISIBLE);
                    surname.setVisibility(View.VISIBLE);
                    comment.setVisibility(View.VISIBLE);
                    send_data.setVisibility(View.VISIBLE);
                    additionalinfo = new CreateAdditionalInfoDialog();
                    additionalinfo.show(getFragmentManager(),"additionalinfo");
                    unregisterReceiver(mHeadphonesReceiver);
                }
            });
        }
        speed_count+=anemometerObservation.getWindSpeed();
        count++;
    }

    public void buttonOnClickSendData(View v) {
        if (velocity==0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wrong data");
            builder.setMessage("Please try measuring again");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(intent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
        else {
            try {
                client = new DefaultHttpClient();
                request = new HttpGet();
                URI website = new URI("http://ubiquitous.hol.es/Scripts/receive_data.php?velocity=" + velocity + "&direction=" + currentDegree + "&longitude=" + longitude + "&latitude=" + latitude + "&altitude=" + altitude + "&device_id=" + deviceid + "&timestamp=" + timestamp.replaceAll("\\s", "%20") + "&accuracy=" + accuracy + "&email=" + email + "&firstname=" +firstname.getText().toString().replaceAll("\\s", "%20")+ "&surname=" +surname.getText().toString().replaceAll("\\s", "%20")+ "&comment=" +comment.getText().toString().replaceAll("\\s", "%20"));
                request.setURI(website);
                HttpResponse response = client.execute(request);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = in.readLine();

                if (!line.equals("All good here")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Wrong data");
                    builder.setMessage("Please try measuring again");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Show location settings when the user acknowledges the alert dialog
                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);
                        }
                    });
                    Dialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Data sent");
                    builder.setMessage("Thank you for your cooperation :)");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Show location settings when the user acknowledges the alert dialog
                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);
                        }
                    });
                    Dialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
                }
            } catch (Exception e) {
                    e.printStackTrace();
              }
        }
    }

    @Override
    public void onError(String s) {
        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
    }

    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[2]);
        currentDegree = -degree;
     }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

}