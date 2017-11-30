package com.turndapage.progressnotification_sample.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.support.v4.content.ContextCompat;

import com.turndapage.progressnotification_sample.DownloadService;
import com.turndapage.progressnotification_sample.Episode;

import net.vrallev.android.cat.Cat;

/**
 * Created by jpage on 11/6/2017.
 */

public class DownloadStarter {
    private Context context;
    private Episode episode;
    public DownloadStarter(Context context, Episode episode) {
        this.context = context;
        this.episode = episode;
    }

    public void start() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(mConnectivityManager != null) {
            Network activeNetwork = mConnectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                Cat.d("Has connection");
                checkPermissions(mConnectivityManager);
            }
        }
    }

    private void checkPermissions(ConnectivityManager connectivityManager) {
        int readCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(readCheck == PackageManager.PERMISSION_GRANTED && writeCheck == PackageManager.PERMISSION_GRANTED) {
            Cat.d("Has permission");
            checkForMeteredConnection(connectivityManager);
        }
    }

    private void checkForMeteredConnection(ConnectivityManager connectivityManager) {
        if(!connectivityManager.isActiveNetworkMetered() || !SettingsUtil.GetConfirmMetered(context)) {
            Cat.d("skipping meter");
            startDownload();
        }
    }

    private void startDownload() {
        Intent intent = new Intent(context, DownloadService.class);

        intent.setAction(DownloadService.ACTION_START);
        intent.putExtra("episode", episode);

        context.startService(intent);
        Cat.d("Starting download");
    }
}
