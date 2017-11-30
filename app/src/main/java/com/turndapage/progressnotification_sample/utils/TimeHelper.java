package com.turndapage.progressnotification_sample.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by jpage on 7/28/2017.
 */

public class TimeHelper {
    public static String getReadableTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("h:mm", Locale.US);
        return df.format(c.getTime());
    }
}
