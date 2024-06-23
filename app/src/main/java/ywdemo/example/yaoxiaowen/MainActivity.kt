package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.os.Trace
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
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

        Trace.beginSection("yyysectionTest1")
//                Debug.startMethodTracing()

        setContentView(R.layout.activity_main)
        Log.i(TAG,"enter onCreate()")
        moreThreadCalc()

        tv.setOnClickListener {
            moreThreadCalc()
        }

        Trace.endSection()
    }

    fun moreThreadCalc() {

        for (i in 0 until N) {
            Thread(Runnable {
                Log.i(TAG, "子线程运行," + getThreadInfo())
                calcFun()
            },  "ywThread-${i}").start()

        }
        Log.i(TAG, "这个在主线程运行，当前线程信息：${Thread.currentThread()}, id=${Thread.currentThread().id}")
        calcFun()
    }

    override fun onResume() {
        super.onResume()
//        testSleep()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Toast.makeText(this, "onWindowFocusChanged()", Toast.LENGTH_LONG).show()
    }

    private fun calcFun() {
        val iterations = 200000 // 运算迭代次数

        val startTime = System.currentTimeMillis()

        for (i in 0 until iterations) {
            val number1 = Random.nextDouble(0.1, 100000.0)
            val number2 = Random.nextDouble(0.1, 100000.0)

            // 乘法运算
            val result1 = number1 * number2

            // 除法运算
            val result2 = number1 / number2

            val result3  = result1 * result2 / result2 / result1 * number1 + number2 / result2

        }

        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime

        Log.i("yaowen", "耗时:${executionTime}, ${getThreadInfo()}")

    }

    fun getThreadInfo() :String {
        return "线程信息：${Thread.currentThread()}, id=${Thread.currentThread().id}"
    }
}
