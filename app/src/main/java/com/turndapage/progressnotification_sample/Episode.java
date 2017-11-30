package com.turndapage.progressnotification_sample;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.turndapage.progressnotification_sample.utils.SettingsUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jpage on 11/30/2017.
 */

public class Episode implements Parcelable {

    private int id = -1;
    private String title;
    private String podcastTitle;
    private String image;
    private String description;
    private String url;
    private String uri;
    private String date;
    private long currentPosition = 0;
    private long duration = 0;
    private boolean played = false;
    private int downloadStatus = STATUS_NONE;
    private long downloadId;
    private int primaryColor;
    private int accentColor;

    public static final int STATUS_NONE = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_DONE = 4;


    public Episode(String title, String podcastTitle, String image, String description, String url, String uri, String date, int primaryColor, int accentColor) {
        this.title = title;
        this.podcastTitle = podcastTitle;
        this.image = image;
        this.description = description;
        this.url = url;
        this.uri = uri;
        this.date = date;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
    }

    private Episode(Parcel parcel) {
        this.id = parcel.readInt();
        this.title = parcel.readString();
        this.podcastTitle = parcel.readString();
        this.image = parcel.readString();
        this.description = parcel.readString();
        this.url = parcel.readString();
        this.uri = parcel.readString();
        this.date = parcel.readString();
        this.currentPosition = parcel.readLong();
        this.duration = parcel.readLong();
        this.played = parcel.readInt() != 0;
        this.primaryColor = parcel.readInt();
        this.accentColor = parcel.readInt();
        this.downloadId = parcel.readLong();
        this.downloadStatus = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(podcastTitle);
        parcel.writeString(image);
        parcel.writeString(description);
        parcel.writeString(url);
        parcel.writeString(uri);
        parcel.writeString(date);
        parcel.writeLong(currentPosition);
        parcel.writeLong(duration);
        parcel.writeInt(played ? 1 : 0);
        parcel.writeInt(primaryColor);
        parcel.writeInt(accentColor);
        parcel.writeLong(downloadId);
        parcel.writeInt(downloadStatus);
    }

    public static final Parcelable.Creator<Episode> CREATOR = new Parcelable.Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel parcel) {
            return new Episode(parcel);
        }

        @Override
        public Episode[] newArray(int i) {
            return new Episode[0];
        }
    };

    public int getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getPodcastTitle() { return this.podcastTitle; }
    public String getShortPodcastTitle() {
        // This is an attempt to remove extra info from extraordinarily long titles
        // by removing all content after |:,...
        int endIndex = indexOf(Pattern.compile("[^a-zA-Z0-9 ,]"), this.podcastTitle);

        if(endIndex != -1)
            return this.podcastTitle.substring(0, endIndex);
        else
            return this.podcastTitle;
    }
    private static int indexOf(Pattern pattern, String s) {
        Matcher matcher = pattern.matcher(s);
        return matcher.find() ? matcher.start() : -1;
    }
    public String getImage() { return this.image; }
    public String getDescription() { return this.description; }
    public String getUrl() { return this.url; }
    public String getUri() { return this.uri; }
    public String getPrettyDate() {
        try {
            return date.substring(4, 10).trim();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return  "-";
        }
    }
    public Date getDate() {
        if(this.date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy", Locale.US);
            try {
                return sdf.parse(this.date);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
    public String getDateString() {
        return this.date;
    }
    public long getCurrentPosition() { return this.currentPosition; }
    public long getDuration() { return this.duration; }
    public int getPrimaryColor() { return this.primaryColor; }
    public int getAccentColor() { return this.accentColor; }
    public boolean getPlayed() { return this.played; }
    public int getDownloadStatus() {return this.downloadStatus; }
    public long getDownloadId() { return this.downloadId; }

    public void setId(int id) {
        this.id = id;
    }
    public void initPlayed(boolean played) {
        this.played = played;
    }
    public void setPlayed(Context context, boolean played) {
        this.played = played;
    }
    public void setDownloadId(long downloadId) { this.downloadId = downloadId; }
    public void setUri(String uri) { this.uri = uri; }
    public void setDownloadStatus(int downloadStatus) { this.downloadStatus = downloadStatus; }
}
