package com.turndapage.progressnotification_sample.utils;

import android.media.session.PlaybackState;

import com.turndapage.progressnotification_sample.R;


/**
 * Created by jpage on 10/6/2017.
 */

public class PlaybackButtonHelper {
    public static int getNotificationPlaybackButtonResId(int state) {

        // set up the appropriate button configuration
        switch (state) {
            case PlaybackState.STATE_PLAYING:
                return R.drawable.exo_controls_play;
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
                return R.drawable.exo_controls_pause;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                return R.drawable.exo_controls_play;
            default:
                return -1;
        }
    }

    public static int getWidgetPlaybackButtonResId(int state) {

        // set up the appropriate button configuration
        switch (state) {
            case PlaybackState.STATE_PLAYING:
                return R.drawable.exo_controls_pause;
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
                return R.drawable.exo_controls_play;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                return R.drawable.exo_controls_play;
            default:
                return -1;
        }
    }

    public static int getPlayerPlaybackButtonResId(int state) {

        // set up the appropriate button configuration
        switch (state) {
            case PlaybackState.STATE_CONNECTING:
            case PlaybackState.STATE_BUFFERING:
                return R.drawable.exo_controls_play;
            case PlaybackState.STATE_PLAYING:
                return R.drawable.exo_controls_pause;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                return R.drawable.exo_controls_play;
            default:
                return -1;
        }
    }
}
