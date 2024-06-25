package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import ywdemo.example.yaoxiaowen.until.LogUtil

class ThreadLockActivity : Activity() {
    private object lock1
    private object lock2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thread_lock)

        treadRun()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Toast.makeText(this, "onWindowFocusChanged()", Toast.LENGTH_LONG).show()
    }


    fun treadRun() {

        val thread1 = Thread({
            synchronized(lock1) {
                LogUtil.i("Thread 1 got lock 1")
                // 尝试获取锁 2，可能导致死锁
                synchronized(lock2) {
                    LogUtil.i("Thread 1 got lock 2")
                }
            }
        }, "lockThread1")

        val thread2 = Thread({
            synchronized(lock2) {
                LogUtil.i("Thread 2 got lock 2")
                // 尝试获取锁 1，可能导致死锁
                synchronized(lock1) {
                    LogUtil.i("Thread 2 got lock 1")
                }
            }
        }, "lockThread2")

        thread1.start()
        thread2.start()
    }
}