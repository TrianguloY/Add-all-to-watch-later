package com.trianguloy.watchlaterall;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that holds some utilities not specific to any other class
 * I love this class
 */

class Utilities {

    /**
     * Returns the text of the intent, if any
     * @param intent intent to check
     * @return text of the intent, null if none
     */
    static String getTextFromIntent(Intent intent) {
        return intent.getStringExtra(Intent.EXTRA_TEXT);
    }


    //from https://stackoverflow.com/a/31940028
    private static Pattern pattern_youtubeLinks = Pattern.compile("(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/\\S+/|(?:v|e(?:mbed)?)/|\\S*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})");

    /**
     * Returns the ids of the videos found by extracting youtube urls from the given text
     * @param HTMLPage the string where to search
     * @return a list with all the different ids found
     */
    static List<String> getIdsFromText(String HTMLPage){
        Matcher pageMatcher = pattern_youtubeLinks.matcher(HTMLPage);
        ArrayList<String> links = new ArrayList<>();
        while(pageMatcher.find()){
            //foreach link found, extract id
            String id = pageMatcher.group(1);
            if(!links.contains(id)){
                //if not already present, add to the list
                links.add(id);
            }
        }
        return links;
    }

    /**
     * Returns the HTML of the url page, following ALL redirects (even HTTP<->HTTPS)
     * @param url the url of the page
     * @return the HTML of the url (or redirect)
     */
    static String getHTMLFromUrl(String url){
        HttpURLConnection conn = null;
        int redirects = 100;
        try {
            URL resourceUrl = new URL(url);

            //adapted from https://stackoverflow.com/questions/1884230/urlconnection-doesnt-follow-redirect
            while (redirects >= 0) {
                //maximum of 100 redirects to avoid infinite loops, check if page is final
                conn = (HttpURLConnection) resourceUrl.openConnection();

                switch (conn.getResponseCode()) {
                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                        //redirections, follow them
                        String location = conn.getHeaderField("Location");
                        conn.disconnect();
                        resourceUrl = new URL(resourceUrl, URLDecoder.decode(location, "UTF-8"));  // Deal with relative URLs
                        redirects -= 1;
                        continue;
                }

                break;
            }


            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder html = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                html.append(line);
            }
            return html.toString();

        } catch (IOException e) {
            //error while finding page
            Log.d("error",e.getMessage());
            e.printStackTrace();
            return "";
        }finally {
            //close connection
            if(conn!=null) {
                conn.disconnect();
            }
        }

    }


    private static Pattern pattern_urls = Pattern.compile("([^\\S]|^)(((https?://)|(www\\.))(\\S+))");

    /**
     * Returns the list of html pages (urls) found in the given text
     * @param text the text where to search
     * @return the list of distinct url founds
     */
    static List<String> getUrlsFromText(String text){
        Matcher matcher = pattern_urls.matcher(text);
        List<String> urls = new ArrayList<>();
        while(matcher.find()){
            //if url found, get
            String url = matcher.group(2);
            if(!urls.contains(url)){
                //if not already present, add
                urls.add(url);
            }
        }
        return urls;
    }

    /**
     * Returns the Bitmap of the image from its url
     * @param url the url of the image
     * @return the Bitmap image, null if couldn't get or not an image
     */
    static Bitmap getImageFromUrl(String url) {
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            return BitmapFactory.decodeStream(in);
        }catch (IOException e){
            return null;
        }finally {
            if (in!=null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static boolean wasLaunchedByDefault(Activity cntx) {

        ResolveInfo resolveInfo = cntx.getPackageManager().resolveActivity(cntx.getIntent(), PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName.equals(cntx.getPackageName());
    }


    public static String parseDuration(String duration_string){
        String time = duration_string.substring(2);
        StringBuilder duration = new StringBuilder();

        int h_index = time.indexOf("H");
        if (h_index != -1){
            String h_value = time.substring(0, h_index);
            duration.append(h_value)
                    .append(":");
            time = time.substring(h_value.length() + 1);
        }

        int m_index = time.indexOf("M");
        if(m_index != -1) {
            String m_value = time.substring(0, m_index);
            if (m_value.length() < 2){
                duration.append("0");
            }
            duration.append(m_value)
                    .append(":");
            time = time.substring(m_value.length() + 1);
        }else{
            duration.append("00:");
        }

        int s_index = time.indexOf("S");
        if (s_index != -1){
            String s_value = time.substring(0, s_index);
            if (s_value.length() < 2){
                duration.append("0");
            }
            duration.append(s_value);
        }else{
            duration.append("00");
        }

        return duration.toString();
    }
}
