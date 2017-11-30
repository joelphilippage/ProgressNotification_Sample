package com.turndapage.progressnotification_sample.utils;

import android.content.Context;
import android.content.SharedPreferences;


import com.turndapage.progressnotification_sample.R;

import java.util.Date;

/**
 * Created by jpage on 8/13/2017.
 */

public class SettingsUtil {
    private static String AUTO_DELETE_DOWNLOADS = "Auto Delete Downloads";
    private static String CONFIRM_METERED = "Confirm metered downloads";
    private static String AUTO_DOWNLOAD = "Automatic Downloads";
    private static String LAST_SYNC_DATE = "Last Sync Date";

    public static void SetAutoDelete(Context context, boolean autoDelete) {
        SharedPreferences settings = getSharedPrefrences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(AUTO_DELETE_DOWNLOADS, autoDelete).apply();
    }

    public static boolean GetAutoDelete(Context context) {
        SharedPreferences settings = getSharedPrefrences(context);

        try {
            return settings.getBoolean(AUTO_DELETE_DOWNLOADS, false);
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            SetAutoDelete(context, false);
            return false;
        }
    }

    public static void SetConfirmMetered(Context context, boolean autoDelete) {
        SharedPreferences settings = getSharedPrefrences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(CONFIRM_METERED, autoDelete).apply();
    }

    public static boolean GetConfirmMetered(Context context) {
        SharedPreferences settings = getSharedPrefrences(context);

        try {
            return settings.getBoolean(CONFIRM_METERED, true);
        } catch (ClassCastException ex) {
            return true;
        }
    }

    public static void SetAutoDownload(Context context, boolean autoDownload) {
        SharedPreferences settings = getSharedPrefrences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(AUTO_DOWNLOAD, autoDownload).apply();
    }

    public static boolean GetAutoDownload(Context context) {
        SharedPreferences settings = getSharedPrefrences(context);

        try {
            return settings.getBoolean(AUTO_DOWNLOAD, false);
        } catch (ClassCastException ex) {
            return false;
        }
    }

    public static void SetLastSyncDate(Context context) {
        SharedPreferences settings = getSharedPrefrences(context);
        SharedPreferences.Editor editor = settings.edit();

        Date date = new Date(System.currentTimeMillis());
        //converting it back to a milliseconds representation:
        long millis = date.getTime();

        editor.putLong(LAST_SYNC_DATE, millis).apply();
    }

    public static Date GetLastSync(Context context) {
        SharedPreferences settings = getSharedPrefrences(context);
        Date date = new Date(System.currentTimeMillis());

        try {
            return new Date(settings.getLong(LAST_SYNC_DATE, date.getTime()));
        } catch (ClassCastException ex) {
            return new Date(date.getTime());
        }
    }

    private static SharedPreferences getSharedPrefrences(Context context) {
        String appName = context.getString(R.string.app_name);
        return context.getSharedPreferences(appName, Context.MODE_PRIVATE);
    }
}
