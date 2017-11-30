package com.turndapage.progressnotification_sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.turndapage.progressnotification_sample.utils.DateUtils;

import net.vrallev.android.cat.Cat;

public class MainActivity extends WearableActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Episode episode = new Episode("Sample Episode",
                "My Podcast",
                "https://cps-static.rovicorp.com/3/JPG_500/MI0002/019/MI0002019848.jpg?partner=allrovi.com",
                "Podcast Description",
                "http://www.podtrac.com/pts/redirect.mp3/traffic.libsyn.com/aliceisntdead/I_Only_Listen_to_the_Mountain_Goats_-_Episode_1_-_AID_Feed.mp3?dest-id=330583",
                null,
                DateUtils.getCurrentDate().toString(),
                0,
                0);
        episode.setDownloadStatus(Episode.STATUS_NONE);

        Button downloadButton = findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDownload(episode);
            }
        });

        checkPermissions();

        // Enables Always-on
        setAmbientEnabled();
    }
    public void startDownload(Episode episode) {

        Intent intent = new Intent(this, DownloadService.class);
        switch (episode.getDownloadStatus()) {
            case Episode.STATUS_NONE:
                intent.setAction(DownloadService.ACTION_START);
                break;
            case Episode.STATUS_FAILED:
                intent.setAction(DownloadService.ACTION_RETRY);
                break;
            case Episode.STATUS_DOWNLOADING:
                intent.setAction(DownloadService.ACTION_PAUSE);
                break;
            case Episode.STATUS_PAUSED:
                intent.setAction(DownloadService.ACTION_RESUME);
        }
        intent.putExtra("episode", episode);

        startService(intent);
        Cat.d("Starting download");
    }

    static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    public void checkPermissions() {
        int readCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (readCheck == PackageManager.PERMISSION_GRANTED) {
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //If the user already denied permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                // First time asking.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        if(writeCheck == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
