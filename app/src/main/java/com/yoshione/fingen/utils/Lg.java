package com.yoshione.fingen.utils;

import android.os.Looper;
import android.util.Log;

import com.yoshione.fingen.BuildConfig;

import java.util.concurrent.TimeUnit;

/**
 * Created by slv on 11.04.2016.
 *
 */
public class Lg {
    public static void d(String tag, String msg) {
        if (!BuildConfig.DEBUG) {
//            Crashlytics.log(android.util.Log.DEBUG, tag, msg);
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(tag, msg);
        }
    }

    public static void log(String msg, String... args) {
        if (BuildConfig.DEBUG) {
            Log.d("log: ", String.format(Thread.currentThread().getStackTrace()[3].getMethodName() + " : " + msg, (Object[]) args) +
                    String.format(" (Main thread == %s, Thread ID == %s)", Looper.myLooper() == Looper.getMainLooper(), Thread.currentThread().getId()));
        }
    }
}
