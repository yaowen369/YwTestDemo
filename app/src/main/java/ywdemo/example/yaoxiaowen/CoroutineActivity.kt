package ywdemo.example.yaoxiaowen

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ywdemo.example.yaoxiaowen.databinding.ActivityCoroutineBinding
import ywdemo.example.yaoxiaowen.until.LogUtil

class CoroutineActivity : Activity() {

    private lateinit var binding:ActivityCoroutineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtil.iWithThreadInfo(TAG, "1 -> onCreate()  start")

        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_coroutine)
        binding  = ActivityCoroutineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        test3()

        LogUtil.iWithThreadInfo(TAG, "4 -> onCreate()结尾运行")
    }

    private fun test1() {
        LogUtil.iWithThreadInfo(TAG, "2 ->test1()")
        runBlocking {
            launch {
                withContext(Dispatchers.IO) {
                    Thread.sleep(2*1000)
                    LogUtil.iWithThreadInfo(TAG, "2 -> withContext中运行")
                }
                Thread.sleep(1*1000)
                LogUtil.iWithThreadInfo(TAG, "3 -> launch中运行")
            }
        }
    }


    private fun test2() {
        LogUtil.iWithThreadInfo(TAG, "test2() -> 1")
        CoroutineScope(Dispatchers.IO).launch {
            Thread.sleep(2*1000)
            LogUtil.iWithThreadInfo(TAG, "test2() -> 2-1, launch中运行")
            withContext(Dispatchers.Main) {
                LogUtil.iWithThreadInfo(TAG, "test2() -> 2-2, launch中切到 Main")
            }
        }
        LogUtil.iWithThreadInfo(TAG, "test2() -> 3, launch中运行")
    }

    private fun test3() {
        LogUtil.iWithThreadInfo(TAG, "test3() -> 1")
        CoroutineScope(Dispatchers.IO).launch {
            LogUtil.iWithThreadInfo(TAG, "test3() -> 2")
            val job = async {
                LogUtil.iWithThreadInfo(TAG, "test3() -> 3")
                Thread.sleep(2*1000)
                LogUtil.iWithThreadInfo(TAG, "test3() -> 4")
                return@async "return value"
            }

            LogUtil.iWithThreadInfo(TAG, "test3() ->5, result=${job.await()}")
        }
        LogUtil.iWithThreadInfo(TAG, "test3() -> 6")

    }

    companion object {
        val TAG = "CoroutineActy"
    }
}