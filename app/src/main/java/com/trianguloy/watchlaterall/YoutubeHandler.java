package com.trianguloy.watchlaterall;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that interacts with the Youtube API.
 * TODO: separate into multiple classes to avoid spaguetti code
 */

class YoutubeHandler {

    //-------------------------- used classes --------------------//
    private BackgroundActivity mainActivity;
    private YouTube mService;

    private Preferences prefs;

    /**
     * Constructor that populates the used classes
     * @param activity the background activity that uses this
     * @param credential the google credentials to initialize the Youtube api
     */
    YoutubeHandler(BackgroundActivity activity, GoogleAccountCredential credential) {
        mainActivity = activity;
        prefs = new Preferences(mainActivity);
        mProgress = new ProgressDialog(activity);
        mProgress.setCancelable(false);

        mService = new YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(mainActivity.getString(R.string.app_name))
                .build();

    }

    /**
     * Phase 1.1: in the foreground, starts a background task and ends immediately
     * @param intent passed to phase 1.2
     */
    void startTask(final Intent intent) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    _startTask(intent);
                }catch(IOException e){
                    onError(e);
                }
            }
        });
    }

    /**
     * Phase 1.2: in the background, parses the intent and extracts the videos
     * @param intent the intent used to start the activity, where to extract the text
     * @throws IOException if something bad happened
     */
    private void _startTask(Intent intent) throws IOException {

        //get the text from the intent
        String text = Utilities.getTextFromIntent(intent);
        if(text==null) {
            //no text found, notify and exit
            showToast(R.string.toast_noLinksFound);
            finish();
            return;
        }

        //get the ids from the text
        final List<String> ids = Utilities.getIdsFromText(text);
        if (ids.isEmpty()) {
            //text doesn't contains valid youtube links, lets try to find normal ones and parse them
            for (String url : Utilities.getUrlsFromText(text)) {
                //foreach found url, find in the html of the page and add to the list of videos
                showProgress(R.string.progress_loadingUrl,url);
                String html = Utilities.getHTMLFromUrl(url);
                for (String id : Utilities.getIdsFromText(html)) {
                    //foreach found, check if already present
                    if (!ids.contains(id)){
                        //if not present, add
                        ids.add(id);
                    }
                }
            }
        }

        //check if no videos where found in the text
        if (ids.isEmpty()) {
            //no videos found, notify and exit
            showToast(R.string.toast_noVideosFound);
            finish();
            return;
        }

        //remove from blacklist
        ids.removeAll( Utilities.getIdsFromText(prefs.getBlackList()) );
    
        //check tutorial text
        if(text.equals(mainActivity.getString(R.string.edTxt_exampleVideo))){
            if(ids.isEmpty()){
                //tutorial link in blacklist
                showToast(R.string.toast_easterEgg);
                finish();
                return;
            }
            //check tutorial link, show notification
            showToast(R.string.toast_congratulations);
        }
    
        //blacklisted all
        if(ids.isEmpty()){
            //no valid videos found, notify and exit
            showToast(R.string.toast_noVideosFound);
            finish();
            return;
        }

        //auto add
        if (ids.size() <= prefs.getAutoAdd()){
            hideProgress(false);
            _onChoosed(ids);
            return;
        }

        //load video information from ids
        showProgress(R.string.progress_loadingDetails,ids.size());
        List<Video> videos = new ArrayList<>(ids.size());
        for (String id : ids) {
            //foreach id, get video
            Video video = getVideoFromId(id);
            if (video != null) {
                //if valid video, add to the list
                videos.add(video);
            }
        }

        
        //no valid videos found
        if(videos.isEmpty()){
            showToast(R.string.toast_noVideosFound);
            finish();
            return;
        }

        //auto add again
        if (videos.size() <= prefs.getAutoAdd()){
            hideProgress(false);
            ids.clear();
            for (Video video : videos) {
                ids.add(video.getId());
            }
            _onChoosed(ids);
            return;
        }

        //create dialog and show it
        AlertDialog.Builder dialog = new AlertDialog.Builder(mainActivity);
        dialog.setTitle(R.string.dialogTitle_chooseVideos);
        final SelectorAdapter selectorAdapter = new SelectorAdapter(mainActivity, videos);
        dialog.setAdapter(selectorAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selectorAdapter.toggleItem(i);
            }
        });
        dialog.setPositiveButton(R.string.button_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onChoosed(selectorAdapter.getSelectedVideos());
            }
        });
        dialog.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        dialog.setCancelable(false);
        hideProgress(false);
        showListToChoose(dialog);
    }

    /**
     * Phase 2: in the background, send the dialog to foreground to show and ends inmediately
     * @param dialog the dialog to show
     */
    private void showListToChoose(final AlertDialog.Builder dialog){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }


    /**
     * Phase 3.1: in the foreground, starts a background task and ends inmediately
     * @param ids passed to phase 3.2
     */
    private void onChoosed(final List<String> ids){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                _onChoosed(ids);
            }
        });
    }

    /**
     * Phase 3.2: in the background, adds the selected videos to the playlist
     * @param ids the selected videos to add
     */
    private void _onChoosed(List<String> ids){
        if(ids.isEmpty()){
            //no videos added, notify
            showToast(R.string.toast_noVideosSelected);
        }else{
            //videos added, add to playlist
            StringBuilder output = new StringBuilder(mainActivity.getString(R.string.toast_status_start));
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                //foreach video id, notify and add
                output.append("\n");
                try {
                    showProgress(R.string.progress_addingVideo, i + 1, ids.size());
                    String name = insertPlaylistItem(id);
                    output.append(mainActivity.getString(R.string.toast_status_goodPrefix))
                            .append(name);
                } catch (GoogleJsonResponseException e){
                    //error while adding video, add to errors
                    output.append(mainActivity.getString(R.string.toast_status_badPrefix))
                            .append(e.getDetails().getMessage());
                } catch (IOException e) {
                    //error while adding video, add to errors
                    output.append(mainActivity.getString(R.string.toast_status_badPrefix))
                            .append(e.getMessage());
                }
            }
            hideProgress(false);
            showToast(output.toString());
        }
        finish();
    }

    /**
     * An error ocurred, try to recover or exit
     * @param error the error
     */
    private void onError(IOException error) {
        if (error instanceof GooglePlayServicesAvailabilityIOException) {
            //playservices not available (even though we checked them)
            mainActivity.showGooglePlayServicesAvailabilityErrorDialog(((GooglePlayServicesAvailabilityIOException) error).getConnectionStatusCode());
        } else if (error instanceof UserRecoverableAuthIOException) {
            //an error than can be recovered, try to recover
            mainActivity.startActivityForResult(((UserRecoverableAuthIOException) error).getIntent(), BackgroundActivity.REQUEST_AUTHORIZATION);
        } else {
            //unrecoverable error, notify and exit
            showToast(mainActivity.getString(R.string.toast_internalError));
            error.printStackTrace();
            finish();
        }
    }


    /**
     * Get the video from their id
     * @param id the id of the video to find
     * @return the video, null if nothing found
     * @throws IOException while retrieving the video
     */
    private Video getVideoFromId(String id) throws IOException{
        YouTube.Videos.List videoRequest = mService.videos().list("snippet,contentDetails");
        videoRequest.setId(id);
        VideoListResponse listResponse = videoRequest.execute();
        List<Video> videoList = listResponse.getItems();

        return videoList.isEmpty() ? null : videoList.get(0);
    }


    /**
     * Create a playlist item with the specified video ID and add it to the watch later playlist
     * @param videoId id of the video to add
     * @throws IOException while inserting the video
     */
    private String insertPlaylistItem(String videoId) throws IOException {

        if(videoId==null){
            //invalid id, return
            throw new IOException(mainActivity.getString(R.string.toast_videosAdded_invalidUrl));
        }

        // Define a resourceId that identifies the video being added to the playlist.
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);


        // Set fields included in the playlistItem resource's "snippet" part.
        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setTitle(mainActivity.getString(R.string.snippetTitle_videoAddedWIth,mainActivity.getString(R.string.app_name)));
        playlistItemSnippet.setPlaylistId("WL");
        playlistItemSnippet.setResourceId(resourceId);

        // Create the playlistItem resource and set its snippet to the object created above.
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);

        // Call the API to add the playlist item to the specified playlist.
        // In the API call, the first argument identifies the resource parts
        // that the API response should contain, and the second argument is
        // the playlist item being inserted.
        YouTube.PlaylistItems.Insert playlistItemsInsertCommand = mService.playlistItems().insert("snippet,contentDetails", playlistItem);
        PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand.execute();
        return returnedPlaylistItem.getSnippet().getTitle();
    }




    //-------------------- utilities ------------------------------//

    /**
     * Wrapper to show a toast on the foreground
     * @param messageId message resource id(use method overloaded with message as string)
     */
    private void showToast(int messageId) {
        showToast(mainActivity.getString(messageId));
    }
    private void showToast(final String message){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity,message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Wrapper to end the activity
     */
    private void finish(){
        hideProgress(true);
        mainActivity.finish();
    }


    private ProgressDialog mProgress;
    /**
     * Wrapper to show a progress circle on the foreground
     * @param messageId the resource id
     * @param formatArgs the arguments
     */
    private void showProgress(int messageId, Object ... formatArgs){
        final String message = mainActivity.getString(messageId, formatArgs);
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.setMessage(message);
                mProgress.show();
            }
        });
    }

    /**
     * Wrapper to hide the progress bar
     * @param remove if true the progress is removed (to use before finishing) if false it is simply hidden
     */
    private void hideProgress(final boolean remove){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(remove){
                    //remove progress, can safely end the activity now and can't be shown again
                    mProgress.dismiss();
                }else {
                    //hide the progress, can be shown again and can't finishing the activity yet
                    mProgress.hide();
                }
            }
        });
    }
}
