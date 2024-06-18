package ywdemo.example.yaoxiaowen

import android.app.Application
import com.tencent.mmkv.MMKV




class DemoApplication : Application() {


    override fun onCreate() {
        super.onCreate()

        val rootDir = MMKV.initialize(this)
        LogUtil.i("mmkv root: $rootDir")
    }
}