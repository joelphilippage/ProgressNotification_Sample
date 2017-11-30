package com.turndapage.progressnotification_sample.utils;

import java.util.Date;

/**
 * Created by jpage on 10/5/2017.
 */

public class IDHelper {
    public static int getUniqueID () {
        long time = new Date().getTime();
        String tmpStr = String.valueOf(time);
        String last4Str = tmpStr.substring(tmpStr.length() - 5);
        return Integer.valueOf(last4Str);
    }
}
