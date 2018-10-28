package com.trianguloy.watchlaterall;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Launcher activity, with little explanation and settings
 */
public class MainActivity extends Activity {

    //--------------- used classes ---------------//
    private Preferences preferences;

    /**
     * Starts activity
     * @param savedInstanceState passed to super
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize classes
        preferences = new Preferences(this);

        //initialize views
        setContentView(R.layout.activity_main);

        //default selection
        CheckBox view_chk_defaultSelection = findViewById(R.id.chk_defaultSelection);
        view_chk_defaultSelection.setChecked(preferences.getDefaultSelection());
        view_chk_defaultSelection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                preferences.setDefaultSelection(compoundButton.isChecked());
            }
        });

        //open links
        Switch view_swt_openLinks = findViewById(R.id.swt_openLinks);
        view_swt_openLinks.setChecked(getOpenLinks());
        view_swt_openLinks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setOpenLinks(compoundButton.isChecked());
            }
        });

        //minimum videos
        final SeekBar view_skBr_autoAdd = findViewById(R.id.skBr_autoAdd);
        final TextView view_txt_autoAdd = findViewById(R.id.txt_autoAdd);
        view_skBr_autoAdd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    preferences.setAutoAdd(progress);
                }
                view_txt_autoAdd.setText( progress>0 ? progress + " videos" : "Disable");
            }
        });
        view_skBr_autoAdd.post(new Runnable() {
            @Override
            public void run() {
                view_skBr_autoAdd.setMax(5);
                view_skBr_autoAdd.setProgress(preferences.getAutoAdd());
            }
        });

        //blacklist
        TextView view_edTxt_blackList = findViewById(R.id.edTxt_blackList);
        view_edTxt_blackList.setText(preferences.getBlackList());
        view_edTxt_blackList.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                preferences.setBlackList(editable.toString());
            }
        });

        //example text
        final EditText example = findViewById(R.id.txt_example);
        example.setKeyListener(null);
        example.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                example.selectAll();
            }
        });

    }



    /**
     * Button clicked
     * @param view wich button
     */
    public void onClick(View view){
        Intent intent;
        switch (view.getId()){
            case R.id.btn_resetAccount:
                //reset account button, unselect account
                boolean deleted = preferences.getAccountName() != null;
                preferences.setAccountName(null);
                Toast.makeText(this, deleted? R.string.toast_accountUnselected : R.string.toast_accountUnselectedNone, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_privacyPolicy:
                //privacy policy button, open page
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://triangularapps.blogspot.com/p/add-all-to-watch-later-privacy-policy.html"));
                startActivity(intent);
                break;
            case R.id.btn_mainPage:
                //main page button, open page
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://triangularapps.blogspot.com/p/add-all-to-watch-later.html"));
                startActivity(intent);
                break;
            case R.id.swt_openLinks:
                setOpenLinks(((Switch) view).isChecked());
                break;
            case R.id.btn_share:
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.edTxt_exampleVideo));
                startActivity(Intent.createChooser(intent,getString(R.string.chsr_choose)));
        }
    }

    ////////////////// Open links ///////////////////

    private void setOpenLinks(boolean newState) {
        getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(),OpenLinkActivity.class.getName()),
                newState ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private boolean getOpenLinks(){
        return getPackageManager().getComponentEnabledSetting(new ComponentName(getPackageName(),OpenLinkActivity.class.getName())) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

}
