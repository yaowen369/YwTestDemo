package ywdemo.example.yaoxiaowen.until;

import android.util.Log;

public class LogUtil {

    private static final String PREFIX = "LogPrefix";

    public static void v(String tag, String msg) {
        Log.v(PREFIX + " " + tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(PREFIX + " " + tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(PREFIX + " " + tag, msg);
    }

    public static void iWithThreadInfo(String tag, String msg) {
        // 已删除 ThreadUtil 引用，使用 Thread.currentThread() 替代
        Log.i(PREFIX + " " + tag, msg + ", " + Thread.currentThread().getName());
    }

    public static void w(String tag, String msg) {
        Log.w(PREFIX + " " + tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(PREFIX + " " + tag, msg);
    }

    public static void v(String msg) {
        Log.v(PREFIX, msg);
    }

    public static void d(String msg) {
        Log.d(PREFIX + " ", msg);
    }

    public static void i(String msg) {
        // 已删除 ThreadUtil 引用，使用 Thread.currentThread() 替代
        Log.i(PREFIX + " ", msg + ", " + Thread.currentThread().getName());
    }

    public static void iWithThreadInfo(String msg) {
        Log.i(PREFIX + " ", msg);
    }

    public static void w(String msg) {
        Log.w(PREFIX + " ", msg);
    }

    public static void e(String msg) {
        Log.e(PREFIX, msg);
    }
}
