package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import ywdemo.example.yaoxiaowen.until.LogUtil

// 测试 后台弹窗App
class BackgroundPopActivity : Activity() {

    private val popDialogBtn: Button by lazy {
        findViewById(R.id.popDialogBtn)
    }

    private val popActyBtn: Button by lazy {
        findViewById(R.id.popActyBtn)
    }


    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_pop)

        popActy();
    }


    private fun popDialog() {


    }


    private fun popActy() {
        mHandler.postDelayed(Runnable {
            LogUtil.i(TAG, "后台弹出Acty")
            val intent = Intent(this, ViewPage2Activity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK xor Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            startActivity(intent);
        }, 30 * 1000)


    }

    companion object {
        const val TAG = "BackgroundPopActy"
    }
}