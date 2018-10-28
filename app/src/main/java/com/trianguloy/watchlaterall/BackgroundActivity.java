package com.trianguloy.watchlaterall;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTubeScopes;

import java.util.Arrays;

/**
 * Activity that runs when sending an intent to this app
 */
public class BackgroundActivity extends Activity {

    //--------- Used classes ------------//
    private GoogleAccountCredential mCredential;
    private YoutubeHandler youtubeHandler;
    private Preferences preferences;
    private PermissionsManager permissionManager;

    // ------------ final static variables -----------//
    private static final int REQUEST_ACCOUNT_PICKER = 1000;
            static final int REQUEST_AUTHORIZATION = 1001;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
            static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE };


    /**
     * Initialization of activity. Initializes used clases and starts the process
     * @param savedInstanceState used in super only
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize used classes
        preferences = new Preferences(this);
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        youtubeHandler = new YoutubeHandler(this, mCredential);
        permissionManager = new PermissionsManager(this);

        //start process
        runCommands();

    }


    /**
     * Starts the process after checking things:
     * 1) checks if play services are available
     *      If not tries to make the user install them
     * 2) checks if there is a selected account name on the api
     *      If not tries to load from preferences, otherwise asks the user to choose one
     * 3) checks if the device is online
     *      If not shows toast and finishes
     * All ok -> starts next phase
     */
    private void runCommands() {
        if (! isGooglePlayServicesAvailable()) {
            //no google play services, make the user acquire them
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            //play services ok BUT no account selected on the api, try to get it
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //play services ok && account selected BUT No internet, inform and exit
            Toast.makeText(this, R.string.toast_noConnection, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            //play services ok && account selected && internet, start the next phase
            youtubeHandler.startTask(getIntent());
        }
    }

    /**
     * Tries to load the selected account from preferences.
     * If nothing saved ask the user to choose one
     */
    private void chooseAccount() {
        String accountName = preferences.getAccountName();
        if (accountName != null) {
            //saved account, set it on the api and retry
            mCredential.setSelectedAccountName(accountName);
            runCommands();
        } else if (!permissionManager.hasPermission()) {
            //no account saved BUT no accounts permission, Request the GET_ACCOUNTS permission via a user dialog
            permissionManager.requestPermission();
        } else {
            //no account saved && account permission, Start a dialog from which the user can choose an account
            startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        }
    }

    /**
     * Returned to the activity after an external user input
     * @param requestCode identifies the external user input
     * @param resultCode if it went well or not
     * @param data result of the user input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                //after make the user get the play services
                if (resultCode != RESULT_OK) {
                    //result not ok, toast and exit
                    Toast.makeText(this, R.string.toast_playServicesNeeded, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    //result ok, retry
                    runCommands();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                //after selecting account
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    //all correct, get account selected
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        //account correct, save and retry
                        preferences.setAccountName(accountName);
                        mCredential.setSelectedAccountName(accountName);
                        runCommands();
                        break;
                    }
                }
                //account not selected or invalid, inform and exit
                Toast.makeText(this, "No account selected, canceled", Toast.LENGTH_SHORT).show();
                finish();

                break;
            case REQUEST_AUTHORIZATION:
                //after recovering from error in the youtubeHandler class
                if (resultCode == RESULT_OK) {
                    //ok, retry
                    runCommands();
                }
                break;
        }
    }

    /**
     * After requesting permissions
     * @param requestCode passed to api
     * @param permissions passed to api
     * @param grantResults passed to api
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( permissionManager.continueOnRequestPermissionsResult(requestCode, permissions, grantResults) ){
            //everything is correct, retry choosing account
            chooseAccount();
        } else {
            //error somewhere, inform and exit
            Toast.makeText(this, R.string.toast_permissionNotGranted, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    //---------------- Utilities ---------------//


    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null ) return false;
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability.getInstance()
            .getErrorDialog(this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES)
            .show();
    }

}