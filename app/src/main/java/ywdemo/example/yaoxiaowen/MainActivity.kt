package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import android.os.Trace
import android.util.Log
import android.view.FrameMetrics
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import java.lang.Exception
import kotlin.random.Random

class MainActivity : Activity() {

    val tv:TextView by lazy{
        findViewById<TextView>(R.id.tv)
    }



     val N = 7
    val TAG = "yawen"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)





    }


    override fun onResume() {
        super.onResume()
//        testSleep()
    }



    fun getThreadInfo() :String {
        return "线程信息：${Thread.currentThread()}, id=${Thread.currentThread().id}"
    }
}
