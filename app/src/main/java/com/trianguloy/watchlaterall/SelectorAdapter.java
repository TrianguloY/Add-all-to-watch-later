package com.trianguloy.watchlaterall;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.services.youtube.model.Video;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Extension of Array Adapter to show videos in the popup
 */
public class SelectorAdapter extends ArrayAdapter<SelectorAdapter.VideoContainer> {

    private final Context cntx;

    /**
     * Constructor, populates the array of videos
     *
     * @param context the context used for context-related things
     * @param videos  list of videos to ppopulate
     */
    SelectorAdapter(Context context, List<Video> videos) {
        super(context, R.layout.video_display, R.id.txt_title, new ArrayList<VideoContainer>(videos.size()));
        cntx = context;
        for (Video video : videos) {
            //foreach video, adds to the list
            super.add(new VideoContainer(video, new Preferences(context).getDefaultSelection()));
        }

    }

    /**
     * Returns the corresponding populated video view
     *
     * @param position    to super
     * @param convertView to super
     * @param parent      to super
     * @return the populated view
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //get views
        View view = super.getView(position, convertView, parent);
        CheckBox selected = view.findViewById(R.id.chk_selected);
        TextView title = view.findViewById(R.id.txt_title);
        TextView description = view.findViewById(R.id.txt_description);
        ImageView thumbnail = view.findViewById(R.id.img_tumbnail);
        TextView publishDate = view.findViewById(R.id.txt_publishDate);
        TextView channelTitle = view.findViewById(R.id.txt_channelTitle);
        TextView duration = view.findViewById(R.id.txt_duration);

        //get video
        final VideoContainer item = getItem(position);

        if (item != null) {
            //populate views
            title.setText(item.getTitle());
            selected.setChecked(item.isSelected());
            selected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    item.toggleSelected();
                }
            });
            description.setText(item.getDescription());
            description.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(SelectorAdapter.super.getContext())
                            .setMessage(item.getDescription())
                            .setCancelable(true)
                            .show();
                }
            });
            thumbnail.setImageBitmap(item.getThumbnail());
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utilities.openInYoutube(item.getUrl(), cntx);
                }
            });
            publishDate.setText(item.getPublishDate());
            channelTitle.setText(item.getChannelTitle());
            duration.setText(item.getVideoDuration());
        }

        //return
        view.requestLayout();
        return view;
    }

    /**
     * Toggles the selected state of the item at the specified position
     *
     * @param position position of the item to toggle
     */
    void toggleItem(int position) {
        VideoContainer item = getItem(position);
        if (item != null) item.toggleSelected();
    }

    /**
     * Returns a list of the selected video Ids
     *
     * @return list of selected video ids
     */
    List<VideoContainer> getSelectedVideos() {

        List<VideoContainer> videos = new ArrayList<>();

        for (int i = 0; i < getCount(); i++) {
            VideoContainer item = getItem(i);
            //foreach item, check selected
            if (item != null && item.isSelected()) {
                //if selected, add
                videos.add(item);
            }

        }
        return videos;
    }


    /**
     * Internal class to save each element of the list.
     * A video, the selected state, and the thumbnail
     */
    static class VideoContainer {

        static final String INVALID = "---";

        //------------- variables ------------//
        private Video video;
        private boolean selected;
        private Bitmap thumbnail;

        /**
         * Constructor, fills the data with the provided input
         * IMPORTANT: makes an internet connection to retrieve the thumbnail of the video
         *
         * @param video    the base video
         * @param selected the default selected state
         */
        VideoContainer(Video video, boolean selected) {
            this.video = video;
            this.selected = selected;

            try {
                //gets the thumbnail of the video, and saves it
                this.thumbnail = Utilities.getImageFromUrl(video.getSnippet().getThumbnails().getDefault().getUrl());
            } catch (Exception e) {
                //error while retrieving the thumbnail, set a dummy thumbnail
                int[] colors = new int[12];
                for (int i = 0; i < 12; i++) {
                    colors[i] = i == 0 || i == 3 ? Color.GRAY : Color.BLACK;
                }
                this.thumbnail = Bitmap.createBitmap(colors, 4, 3, Bitmap.Config.ALPHA_8);
            }
        }

        /**
         * Returns the title of the video
         *
         * @return the title, dummy string if invalid
         */
        String getTitle() {
            return video == null ? INVALID : video.getSnippet().getTitle();
        }

        /**
         * Returns the id of the video
         *
         * @return id of the video, null if invalid
         */
        String getId() {
            return video == null ? null : video.getId();
        }

        /**
         * Returns the description of the video
         *
         * @return the description, dummy string if invalid
         */
        String getDescription() {
            return video == null ? INVALID : video.getSnippet().getDescription();
        }

        /**
         * Returns the selected state of the video
         *
         * @return true if selected, false otherwise
         */
        boolean isSelected() {
            return selected;
        }

        /**
         * Toggles the selected state of the video
         */
        void toggleSelected() {
            selected = !selected;
        }

        /**
         * Returns the thumbnail of the video
         *
         * @return the thumbnail
         */
        Bitmap getThumbnail() {
            return thumbnail;
        }

        /**
         * Returns the publication date, in the device format
         *
         * @return the publication date, dummy string if invalid
         */
        String getPublishDate() {
            if (video == null) {
                return INVALID;
            }

            long millis = video.getSnippet().getPublishedAt().getValue();

            return SimpleDateFormat.getDateTimeInstance().format(new Date(millis));
        }

        /**
         * Returns the channel title
         *
         * @return the channel title, dummy string if invalid
         */
        String getChannelTitle() {
            return video == null ? INVALID : video.getSnippet().getChannelTitle();
        }

        String getVideoDuration() {
            if (video == null) return INVALID;

            String timeString = video.getContentDetails().getDuration();
            return Utilities.parseDuration(timeString);
        }

        String getUrl() {
            if (video == null) return null;
            return "https://www.youtube.com/watch?v=" + video.getId();
        }
    }

}



