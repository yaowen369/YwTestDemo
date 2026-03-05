package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastUtils {

    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void toast(final Context context, final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // 检查 Activity 是否还在运行，避免 BadTokenException
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toast(final Context context, final int resId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // 检查 Activity 是否还在运行，避免 BadTokenException
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                }
                Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
