package es.hol.ubiquitous.windtrack;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;


public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener{

    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int REQUEST_CODE_EMAIL = 1;
    private static long MINTIME=4000;
    private static float MINDISTANCE=0.5f;

    private boolean mResolvingError = false; // Bool to track whether the app is already resolving an error
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    double longitude,latitude,altitude;
    private float accuracy;
    private String email,deviceid,best_provider;
    Button info,about_us;
    private DialogFragment infodialog,welcomedialog,aboutdialog;



    private LocationManager locationManager;
    private Location location;
    private Criteria criteria;




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = (Button)findViewById(R.id.button3);
        about_us = (Button)findViewById(R.id.button2);

        infodialog = new CreateInfoDialog();
        aboutdialog = new CreateAboutDialog();
        welcomedialog = new CreateWelcomeDialog();
        welcomedialog.show(getFragmentManager(),"about");

        checkLocationServices();
        checkConnection(getApplicationContext());


        Context context = getApplicationContext();
        locationManager=(LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        criteria=new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
        best_provider = locationManager.getBestProvider(criteria, true);
        location = new Location(best_provider);
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        accuracy = location.getAccuracy();
        altitude = location.getAltitude();
        locationManager.requestLocationUpdates(best_provider,MINTIME,MINDISTANCE,this);



        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        buildGoogleApiClient();
        deviceid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        try {
            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
            startActivityForResult(intent, REQUEST_CODE_EMAIL);
        } catch (ActivityNotFoundException e) {
            //Do nothing
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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

    public boolean checkLocationSet() {
        if (longitude ==0.0 || latitude == 0.0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Detecting location...");
            builder.setMessage("Please wait while your location is detected");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            locationManager.requestLocationUpdates(1000,0.5f, criteria, this, null);
            return false;
        }
        else {
            return true;
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

    public void onConnected(Bundle connectionHint) {
       /* Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            this.longitude = mLastLocation.getLongitude();
            this.latitude = mLastLocation.getLatitude();
            this.accuracy = mLastLocation.getAccuracy();
            //info.setText(String.valueOf(longitude));
            //about_us.setText(String.valueOf(latitude));
         } */
    }

    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            //showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
        else if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            this.email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }


    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    protected void onStop() {
        //mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void buttonOnClick(View v) {

        checkLocationServices();
        checkConnection(getApplicationContext());

        if (checkMeterConnected() && checkLocationSet()) {
            Intent intent = new Intent(this, MeasurementActivity.class);
            intent.putExtra("LONGITUDE", longitude);
            intent.putExtra("LATITUDE", latitude);
            intent.putExtra("ALTITUDE",altitude);
            intent.putExtra("EMAIL", email);
            intent.putExtra("ACCURACY", accuracy);
            intent.putExtra("DEVICEID", deviceid);
            startActivity(intent);
        }
    }

    public void buttonOnClickInfo(View v) {

        infodialog.show(getFragmentManager(),"info");

    }

    public void buttonOnClickAbout(View v) {

        aboutdialog.show(getFragmentManager(),"about");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        accuracy = location.getAccuracy();
        altitude = location.getAltitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}