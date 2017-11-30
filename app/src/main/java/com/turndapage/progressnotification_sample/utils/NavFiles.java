package com.turndapage.progressnotification_sample.utils;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by jpage on 11/5/2017.
 */

public class NavFiles {

    public static ArrayList<File> FetchList(String dir) {
        File f = new File(dir);

        try {
            return new ArrayList<>(Arrays.asList(f.listFiles()));
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String GetExternalDirectory() {
        return Environment.getExternalStorageDirectory().toString();
    }
}
