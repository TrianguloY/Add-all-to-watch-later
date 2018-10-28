package com.trianguloy.watchlaterall;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Class to store and retrieve preferences
 * Sort of a wrapper of SharedPreferences
 */

class Preferences {

    //the android SharedPreferences
    private static final String PREF_NAME = "pref";
    private SharedPreferences preferences;
    /**
     * Constructor, initializates the preferences
     * @param context the context from where load the SharedPreferences class
     */
    Preferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }



    /**
     * Default selection: if true videos will be selected by default
     */
    private static final String KEY_DEFAULTSELECTION = "defaultSelection";
    private static final boolean DEFAULT_DEFAULTSELECTION = false;
    /**
     * Getter for the default selection setting
     * @return true if default selected is on, false otherwise
     */
    boolean getDefaultSelection(){
        return preferences.getBoolean(KEY_DEFAULTSELECTION,DEFAULT_DEFAULTSELECTION);
    }

    /**
     * Setter for the default selection
     * @param defaultSelection true if videos should be selected by default, false otherwise
     */
    void setDefaultSelection(boolean defaultSelection){
        preferences.edit().putBoolean(KEY_DEFAULTSELECTION,defaultSelection).apply();
    }




    /**
     * Account name: selected account name
     */
    private static final String KEY_ACCOUNTNAME = "accountName";
    private static final String DEFAULT_ACCOUNTNAME = null;

    /**
     * Getter for the account name
     * @return the selected account name, null if nothing selected
     */
    String getAccountName(){
        return preferences.getString(KEY_ACCOUNTNAME,DEFAULT_ACCOUNTNAME);
    }

    /**
     * Setter for the account name
     * @param accountName the account name
     */
    void setAccountName(String accountName){
        preferences.edit().putString(KEY_ACCOUNTNAME,accountName).apply();
    }



    /**
     * Autosave videos: automatically save if less than n videos
     */
    private static final String KEY_AUTOADD = "autoSave";
    private static final int DEFAULT_AUTOADD = 0;

    int getAutoAdd(){
        return preferences.getInt(KEY_AUTOADD, DEFAULT_AUTOADD);
    }

    void setAutoAdd(int autoAdd){
        preferences.edit().putInt(KEY_AUTOADD, autoAdd).apply();
    }


    /**
     * Blacklist
     */
    private static final String KEY_BLACKLIST = "blacklist";
    private static final String DEFAULT_BLACKLIST = "";

    String getBlackList(){
        return preferences.getString(KEY_BLACKLIST, DEFAULT_BLACKLIST);
    }

    void setBlackList(String blackList){
        preferences.edit().putString(KEY_BLACKLIST, blackList).apply();
    }


}
