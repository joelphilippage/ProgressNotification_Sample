package com.turndapage.progressnotification_sample.utils;

import net.vrallev.android.cat.Cat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    private static SimpleDateFormat[] dateFormats = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"),
            new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z"),
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm Z"),
            new SimpleDateFormat("dd MMM yyyy HH:mm Z")
    };

    public static Date stringToDate(String dt) {

        Date date = null;

        for (SimpleDateFormat dateFormat : DateUtils.dateFormats) {
            try {
                date = dateFormat.parse(dt);
                break;
            } catch (ParseException e) {
                //This format didn't work, keep going
            }
        }

        return date;
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        Cat.d(cal1.toString() + " : " + cal2.toString());
        return isSameDay(cal1, cal2);
    }

    public static boolean isToday(Date date) {
        return isSameDay(date, Calendar.getInstance().getTime());
    }

    public static Date getCurrentDate() {
        return new Date(System.currentTimeMillis());
    }

    public static long getDifferenceDays(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }
}
