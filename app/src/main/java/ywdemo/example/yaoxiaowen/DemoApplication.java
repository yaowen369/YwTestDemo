package ywdemo.example.yaoxiaowen;

import android.app.Application;
import com.tencent.mmkv.MMKV;
import ywdemo.example.yaoxiaowen.until.LogUtil;


public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        String rootDir = MMKV.initialize(this);
        LogUtil.i("mmkv root: " + rootDir);
    }
}
