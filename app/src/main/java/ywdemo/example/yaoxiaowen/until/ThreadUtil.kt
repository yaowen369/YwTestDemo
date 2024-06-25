package ywdemo.example.yaoxiaowen.until

import android.os.Looper
import java.lang.StringBuilder

class ThreadUtil {
    companion object {
        fun getThreadInfo():String {
            val sb = StringBuilder()
            sb.append("线程信息：${Thread.currentThread()}, id=${Thread.currentThread().id}")
            sb.append(",  是否主线程: ${Looper.myLooper() == Looper.getMainLooper()}")
            return sb.toString()
        }
    }
}