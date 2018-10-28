package com.trianguloy.watchlaterall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Class that opens the backgroundActivity when clicking a youtube link
 */
public class OpenLinkActivity extends Activity {
    private static final String EXTRA_NAME = "ADD_ALL_TO_WATCH_LATER";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Intent openIntent;

        if (intent!=null) {
            if (EXTRA_NAME.equals(intent.getStringExtra(EXTRA_NAME)) || !Utilities.wasLaunchedByDefault(this) ) {
                //Open the intent
                openIntent = new Intent(this, BackgroundActivity.class);
                if(intent.getData()!=null) {
                    openIntent.putExtra(Intent.EXTRA_TEXT, intent.getData().toString());
                }
                startActivity(openIntent);
            } else {
                //This was launched by default, ask the user again
                openIntent = new Intent(intent);
                openIntent.setComponent(null);
                openIntent.putExtra(EXTRA_NAME, EXTRA_NAME);
                openIntent.setFlags(openIntent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(openIntent, getString(R.string.chsr_choose)));
                Log.d("choose",openIntent.toUri(0));
            }
        }
        finish();
    }
}
