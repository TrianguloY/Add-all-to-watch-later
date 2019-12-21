package com.trianguloy.watchlaterall;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Class that handles the getAccounts Permissions of the app
 */

class PermissionsManager {

    //original activity
    private Activity activity;


    /**
     * Constructor
     * @param activity the original activity to run activity-related commands
     */
    PermissionsManager(BackgroundActivity activity) {
        this.activity = activity;
    }


    /**
     * If the app have the account permission
     * @return true if granted and can be used, false otherwise
     */
    boolean hasPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || activity.checkSelfPermission(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request permissions to the user.
     * Always shows the explanation (the user have the right to know why) and then asks for it
     */
    void requestPermission(){
        if ( hasPermission()){
            //check to avoid requesting if already have
            return;
        }

        //shows the reason to the user, then asks
        new AlertDialog.Builder(activity)
                .setTitle("Permission needed")
                .setMessage(R.string.permission_contacts)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        activity.requestPermissions( new String[]{Manifest.permission.GET_ACCOUNTS}, BackgroundActivity.REQUEST_PERMISSION_GET_ACCOUNTS);
                    }
                })
                .show();
    }

    /**
     * Checks if the user accepted the permission or not
     * @param requestCode code of the request, should be BackgroundActivity.REQUEST_PERMISSION_GET_ACCOUNTS
     * @param permissions should be only the GET_ACCOUNTS (checks first)
     * @param grantResults should be GRANTED
     * @return true if the user accepted, false otherwise
     */
    boolean continueOnRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return requestCode == BackgroundActivity.REQUEST_PERMISSION_GET_ACCOUNTS
                && Manifest.permission.GET_ACCOUNTS.equals(permissions[0])
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ;
    }

}
