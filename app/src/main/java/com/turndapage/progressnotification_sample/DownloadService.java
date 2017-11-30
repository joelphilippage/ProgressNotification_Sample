package com.turndapage.progressnotification_sample;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.exception.NotUsableException;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;
import com.turndapage.progressnotification_sample.utils.IDHelper;

import net.vrallev.android.cat.Cat;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by jpage on 10/1/2017.
 */

public class DownloadService extends Service implements Thread.UncaughtExceptionHandler {

    public static final String ACTION_START = "com.turndapage.navcasts.ACTION_START";
    public static final String ACTION_CANCEL = "com.turndapage.navcasts.ACTION_CANCEL";
    public static final String ACTION_PAUSE = "com.turndapage.navcasts.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.turndapage.navcasts.ACTION_RESUME";
    public static final String ACTION_RETRY = "com.turndapage.navcasts.ACTION_RETRY";
    public static final String UPDATE_PROGRESS = "com.turndapage.navcasts.UPDATE_PROGRESS";
    public static final String DOWNLOAD_STARTED = "com.turndapage.navcasts.DOWNLOAD_STARTED";

    private static final String DOWNLOAD_FAILED = "com.turndapage.navcasts.DOWNLOAD_FAILED";
    private static final String DISMISS_NOTIFICATION = "com.turndapage.navcasts.DISMISS_NOTIFICATION";
    private static final String UPDATE_NOTIFICATION = "com.turndapage.navcasts.UPDATE_NOTIFICATION";
    private static final String STARTING_NOTIFICATION = "com.turndapage.navcasts.STARTING_NOTIFICATION";

    private static NotificationCompat.Builder builder;
    private Fetch fetch;
    private Context context;
    private boolean notificationStarted;
    private CancelReceiver cancelReceiver = new CancelReceiver();
    private RetryReceiver retryReceiver = new RetryReceiver();
    private AsyncTask getURLTask;
    private static Episode currentEpisode;

    private static final int DOWNLOADING_ID = 1545644567;
    private static final String NOTIFICATION_CHANNEL_ID = "nav_casts";

    private static ArrayList<NavFetchListener> fetchListeners = new ArrayList<>();
    private static ArrayList<Episode> downloadQueue = new ArrayList<>();


    private PendingIntent contentIntent;
    private PendingIntent pendingIntentCancel;
    private static final NotificationCompat.Action.WearableExtender actionExtender =
            new NotificationCompat.Action.WearableExtender()
                    .setHintDisplayActionInline(true);

    private NotificationCompat.Action cancelAction = new NotificationCompat.Action
            .Builder(R.drawable.ic_clear_white_24dp, "cancel", pendingIntentCancel)
            .extend(actionExtender)
            .build();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;

        initContentIntent();

        Intent cancelIntent = new Intent();
        cancelIntent.setAction(ACTION_CANCEL);
        pendingIntentCancel = PendingIntent.getBroadcast(context, IDHelper.getUniqueID(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        cancelAction = new NotificationCompat.Action
                .Builder(R.drawable.ic_clear_white_24dp, "cancel", pendingIntentCancel)
                .extend(actionExtender)
                .build();

        builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_file_download_white_24dp)
                .addAction(cancelAction);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NavCasts", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if(notificationManager != null)
                notificationManager.createNotificationChannel(notificationChannel);
        }

        registerReceivers();
        if(intent == null)
            stopSelf();
        else {
            if (fetch == null) {
                fetch = Fetch.newInstance(this);
            }

            new Fetch.Settings(getApplicationContext())
                    .setAllowedNetwork(Fetch.NETWORK_ALL)
                    .enableLogging(true)
                    .setConcurrentDownloadsLimit(1)
                    .apply();

            Cat.d("Got download intent");

            Episode episode = intent.getParcelableExtra("episode");

            if(currentEpisode != null) { // If this is not the first episode to download
                Cat.d("Added to queue");
                downloadQueue.add(episode);
            } else {
                if (intent.getAction() != null && episode != null) {
                    Cat.d("Starting action");
                    currentEpisode = episode;
                    initContentIntent();
                    switch (intent.getAction()) {
                        case ACTION_START:
                            startDownload(context, episode, fetch);
                            break;
                        case ACTION_PAUSE:
                            pauseDownload(episode);
                            break;
                        case ACTION_RESUME:
                            resumeDownload(episode);
                            break;
                        case ACTION_RETRY:
                            retryDownload(episode);
                            break;
                        case ACTION_CANCEL:
                            fetch.removeRequest(episode.getDownloadId());
                            for (int i = 0; i < fetchListeners.size(); i++) {
                                if (fetchListeners.get(i).notificationID == episode.getDownloadId()) {
                                    fetch.removeFetchListener(fetchListeners.get(i));
                                    break;
                                }
                            }
                            break;
                    }

                } else {
                    Cat.e("Didn't get information for download.");
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void initContentIntent() {
        Intent openMainActivity = new Intent(context, MainActivity.class);
        openMainActivity.putExtra("episode_downloading", currentEpisode);

        contentIntent = PendingIntent.getActivity(context, 0, openMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String returnSafeString(String string) {
        String safeString = string.replaceAll("[^a-zA-Z0-9.-]", " ");
        return safeString.trim().replaceAll(" +", " ");
    }

    private static void startDownload(Context context, Episode episode, Fetch fetch) {
        episode.setDownloadStatus(Episode.STATUS_DOWNLOADING);
        sendStartedNotification(context,episode);
        String storageDir = Environment.getExternalStorageDirectory().getPath();
        String fileDir = storageDir + "/Podcasts/" + returnSafeString(episode.getPodcastTitle());

        File folder = new File(fileDir);

        boolean success = true;
        if(!folder.exists()) {
            success = folder.mkdirs();
        }
        if(success) {
            Cat.d("Starting download task");
            new GetURLTask(context, fetch, episode).execute();
        } else {
            Cat.e("Couldn't create the directory to download to.");
            downloadFailed(context, episode);
            sendDismiss(context, DOWNLOADING_ID);
        }
    }

    private void pauseDownload(Episode episode) {
        fetch.pause(episode.getDownloadId());
        episode.setDownloadStatus(Episode.STATUS_PAUSED);
        updatePodcasts(context, episode);
    }

    private void resumeDownload(Episode episode) {
        if(episodeInFetch(episode)) {
            fetch.resume(episode.getDownloadId());
            episode.setDownloadStatus(Episode.STATUS_DOWNLOADING);
            updatePodcasts(context, episode);
        } else
            startDownload(context, episode, fetch);
    }

    private void retryDownload(Episode episode) {
        if(episodeInFetch(episode)) {
            fetch.retry(episode.getDownloadId());
            episode.setDownloadStatus(Episode.STATUS_DOWNLOADING);
            updatePodcasts(context, episode);
        } else
            startDownload(context, episode, fetch);
    }

    private static HttpUrl run(String url) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        response.request();
        return response.request().url();
    }

    private static class NavFetchListener implements FetchListener {
        private Episode episode;
        private int notificationID;
        WeakReference<Context> contextWeakReference;
        WeakReference<Fetch> fetchWeakReference;
        private NavFetchListener(Context context, Fetch fetch, Episode episode) {
            this.contextWeakReference = new WeakReference<>(context);
            this.fetchWeakReference = new WeakReference<>(fetch);
            this.episode = episode;
        }

        @Override
        public void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error) {
            Context context = contextWeakReference.get();
            Fetch fetch = fetchWeakReference.get();
            if(context != null && fetch != null) {
                notificationID = (int) id;
                switch (status) {
                    case Fetch.STATUS_DOWNLOADING:
                        sendUpdate(context, episode, progress);
                        sendUpdateNotification(context, progress);
                        break;
                    case Fetch.STATUS_DONE:
                        sendUpdate(context, episode, 100);
                        episode.setDownloadStatus(Episode.STATUS_DONE);
                        updatePodcasts(context, episode);
                        Intent dismissIntent = new Intent(DISMISS_NOTIFICATION);
                        dismissIntent.getIntExtra("notification_id", notificationID);
                        context.sendBroadcast(dismissIntent);
                        fetch.removeFetchListener(this);
                        if(downloadQueue.size() > 0) {
                            currentEpisode = downloadQueue.get(0);
                            startDownload(context, currentEpisode, fetch);
                            downloadQueue.remove(0);
                        } else {
                            currentEpisode = null;
                        }
                        break;
                    case Fetch.STATUS_ERROR:
                        Cat.e("There was a download error");
                        episode.setUri(null);
                        episode.setDownloadStatus(Episode.STATUS_FAILED);
                        updatePodcasts(context, episode);

                        Intent dismissIntent2 = new Intent(DISMISS_NOTIFICATION);
                        dismissIntent2.getIntExtra("notification_id", notificationID);
                        context.sendBroadcast(dismissIntent2);

                        showFailedNotification(context, episode, notificationID);
                        break;
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void sendUpdateNotification(Context context, int progress) {
        Intent updateIntent = new Intent(UPDATE_NOTIFICATION);
        updateIntent.putExtra("progress", progress);
        context.sendBroadcast(updateIntent);
    }

    private static void sendStartedNotification(Context context, Episode episode) {
        Intent startedIntent = new Intent(STARTING_NOTIFICATION);
        startedIntent.putExtra("episode", episode);
        context.sendBroadcast(startedIntent);
    }

    private static void sendUpdate(Context context, Episode episode, int progress) {
        Intent broadcastIntent = new Intent(UPDATE_PROGRESS);
        broadcastIntent.putExtra("episode", episode);
        broadcastIntent.putExtra("progress", progress);
        context.sendBroadcast(broadcastIntent);
    }

    private static void sendDownloadStartedConfirmation(Context context, Episode episode) {
        Intent broadcastIntent = new Intent(DOWNLOAD_STARTED);
        broadcastIntent.putExtra("episode", episode);
        context.sendBroadcast(broadcastIntent);
    }

    private static void updatePodcasts(Context context, Episode episode) {

    }

    private static void showFailedNotification(Context context, Episode episode, int NOTIFICATION_ID) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Intent retryIntent = new Intent();
        retryIntent.setAction(ACTION_RETRY);
        retryIntent.putExtra("episode", episode);
        PendingIntent pendingIntentRetry = PendingIntent.getBroadcast(context, IDHelper.getUniqueID(), retryIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action retryAction = new NotificationCompat.Action
                .Builder(R.drawable.ic_refresh_white_24dp, "cancel", pendingIntentRetry)
                .extend(actionExtender)
                .build();

        builder.setContentIntent(contentIntent)
                .setContentTitle(context.getString(R.string.download_failed))
                .setContentText(episode.getTitle())
                .setOngoing(false)
                .setProgress(0,0, false)
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .addAction(retryAction);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private final BroadcastReceiver updateNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", -1);

            builder.setProgress(100, progress, false);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.notify(DOWNLOADING_ID, builder.build());
        }
    };

    private final BroadcastReceiver startingNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Episode episode = intent.getParcelableExtra("episode");
            builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setContentIntent(contentIntent)
                    .setContentTitle(episode.getShortPodcastTitle())
                    .setContentText(episode.getTitle())
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_file_download_white_24dp)
                    .setProgress(100, 0, true)
                    .addAction(cancelAction);
            notificationStarted = true;


            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                startForeground(DOWNLOADING_ID, builder.build());
        }
    };

    private final BroadcastReceiver dismissNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int NOTIF_ID = intent.getIntExtra("notification_id", -1);
            notificationStarted = false;
            stopForeground(true);
            if(NOTIF_ID != -1) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIF_ID);
                }
            }
        }
    };

    public class CancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(fetchListeners.size() > 0) {
                NavFetchListener fetchListener = fetchListeners.get(fetchListeners.size() - 1);
                Episode episode = fetchListener.episode;
                int id = fetchListener.notificationID;
                fetch.remove(id);
                fetch.removeFetchListener(fetchListener);

                episode.setDownloadStatus(Episode.STATUS_NONE);

                // Clear current episode and download queue so that new downloads can start
                currentEpisode = null;
                downloadQueue.clear();

                sendDismiss(context, id);
                sendUpdate(context, episode, 0);

                Cat.d("Got cancel message");
            }
        }
    }

    private class RetryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Episode episode = intent.getParcelableExtra("episode");
            retryDownload(episode);
            sendDismiss(context, (int)episode.getDownloadId());
        }
    }

    private static void sendDismiss(Context context, int id) {
        Intent dismissIntent = new Intent(DISMISS_NOTIFICATION);
        dismissIntent.getIntExtra("notification_id", id);
        context.sendBroadcast(dismissIntent);
    }

    private void registerReceivers() {
        IntentFilter dismissIntentFilter = new IntentFilter(DISMISS_NOTIFICATION);
        registerReceiver(dismissNotification, dismissIntentFilter);
        IntentFilter updateIntentFilter = new IntentFilter(UPDATE_NOTIFICATION);
        registerReceiver(updateNotification, updateIntentFilter);
        IntentFilter startingIntentFilter = new IntentFilter(STARTING_NOTIFICATION);
        registerReceiver(startingNotification, startingIntentFilter);
        IntentFilter cancelIntentFilter = new IntentFilter(ACTION_CANCEL);
        registerReceiver(cancelReceiver, cancelIntentFilter);
        IntentFilter retryIntentFilter = new IntentFilter(ACTION_RETRY);
        registerReceiver(retryReceiver, retryIntentFilter);
    }

    private void unregisterReceivers() {
        unregisterReceiver(dismissNotification);
        unregisterReceiver(updateNotification);
        unregisterReceiver(startingNotification);
        unregisterReceiver(cancelReceiver);
        unregisterReceiver(retryReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove all current downloads if they are interrupted and not done.
        for(int i = 0; i < fetchListeners.size(); i++) {
            final Episode episode = fetchListeners.get(i).episode;
            if(episode.getDownloadStatus() != Episode.STATUS_DONE) {
                Cat.e("Download service is stopping before downloads are done!");
                episode.setDownloadStatus(Episode.STATUS_FAILED);
                episode.setUri(null);

                new UpdateTask(this, episode).execute();
            }
        }
        if(fetch != null) {
            fetch.removeRequests();
            fetch.removeFetchListeners();
            fetch.release();
        }
        if(getURLTask != null)
            getURLTask.cancel(true);
        unregisterReceivers();
    }

    private static class UpdateTask extends AsyncTask<Void, Void, Void> {
        WeakReference<Context> contextWeakReference;
        Episode episode;

        private UpdateTask(Context context, Episode episode) {
            contextWeakReference = new WeakReference<>(context);
            this.episode = episode;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = contextWeakReference.get();
            if(context != null)
                updatePodcasts(context, episode);
            return null;
        }
    }

    private static class GetURLTask extends AsyncTask<String, String, String> {

        private Episode episode;
        private WeakReference<Context> contextWeakReference;
        private WeakReference<Fetch> fetchWeakReference;

        private GetURLTask(Context context, Fetch fetch, Episode episode) {
            this.contextWeakReference = new WeakReference<>(context);
            this.fetchWeakReference = new WeakReference<>(fetch);
            this.episode = episode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        private static boolean createPodcastDirectory(Episode episode) {
            boolean success = true;
            File folder = new File(Environment.getExternalStorageDirectory() + File.separator +
                    "Podcasts/" + returnSafeString(episode.getPodcastTitle()));
            if(!folder.exists()) {
                success = folder.mkdirs();
            }
            return success;
        }

        @Override
        protected String doInBackground(String... strings) {
            Cat.d("Starting background task");
            Context context = contextWeakReference.get();
            Fetch fetch = fetchWeakReference.get();
            if(context != null && fetch != null) {
                boolean success = createPodcastDirectory(episode);

                String fileDir = Environment.getExternalStorageDirectory() + File.separator +
                        "Podcasts/" + returnSafeString(episode.getPodcastTitle());
                Cat.d("Creating directory");
                if(success) {
                    Cat.d("Created directory");
                    String url = episode.getUrl();
                    String filename = url.substring(url.lastIndexOf("/") + 1);
                    // Remove anything after the extension
                    if (filename.contains(".mp3"))
                        filename = filename.substring(0, filename.indexOf(".mp3"));
                    filename = returnSafeString(filename);
                    String uniqueFileName = filename + IDHelper.getUniqueID() + ".mp3";
                    try {
                        // Set to downloading before it tries to get the url
                        episode.setUri(fileDir + "/" + uniqueFileName);
                        episode.setDownloadStatus(Episode.STATUS_DOWNLOADING);
                        updatePodcasts(context, episode);
                        sendDownloadStartedConfirmation(context, episode);

                        Cat.d("Running url request: " + url);
                        url = run(url).toString();
                        Cat.d("Got url: " + url);
                        Request request = new Request(url, fileDir, uniqueFileName);
                        try {
                            long downloadId = fetch.enqueue(request);

                            if (downloadId != Fetch.ENQUEUE_ERROR_ID) {
                                NavFetchListener fetchListener = new NavFetchListener(context, fetch, episode);

                                fetch.addFetchListener(fetchListener);
                                fetchListeners.add(fetchListener);
                                episode.setUri(fileDir + "/" + uniqueFileName);
                                episode.setDownloadId(downloadId);
                                updatePodcasts(context, episode);
                                Cat.d("Download successfully started");
                                sendUpdateNotification(context,0);

                            } else {
                                Cat.e("Failed to add download to queue");
                                downloadFailed(context, episode);
                            }
                        } catch (NotUsableException ex) {
                            ex.printStackTrace();
                            Cat.e("Download URL not usable");
                            downloadFailed(context, episode);
                        }
                    } catch (IOException ex) {
                        downloadFailed(context, episode);
                        ex.printStackTrace();
                        Cat.e("Couldn't get re-directed URL");
                    }
                } else {
                    downloadFailed(context, episode);
                    Cat.e("Couldn't create a directory for the podcast.");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    private static void downloadFailed(Context context, Episode episode) {
        episode.setDownloadStatus(Episode.STATUS_FAILED);
        sendUpdate(context, episode, -1);
        updatePodcasts(context, episode);
        sendDismiss(context, DOWNLOADING_ID);
        showFailedNotification(context, episode, IDHelper.getUniqueID());
    }

    private boolean episodeInFetch(Episode episode) {
        for(int i = 0; i < fetchListeners.size(); i++) {
            if(fetchListeners.get(i).episode.getDownloadId() == episode.getDownloadId())
                return true;
        }
        return false;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // dismiss all notifications so none get stuck

        if(fetchListeners.size() > 0) {
            for (int i = 0; i < fetchListeners.size(); i++) {
                sendDismiss(this, fetchListeners.get(i).notificationID);
            }
        }
        sendDismiss(this, DOWNLOADING_ID);
    }

}
