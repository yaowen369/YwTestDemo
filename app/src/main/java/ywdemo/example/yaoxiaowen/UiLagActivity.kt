package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.os.Looper
import android.widget.Toast

class UiLagActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Debug.startMethodTracing("fileName", 8*1024*1024)
        // Debug.startMethodTracing()采样方法较全, 生成的文件很巨大, 甚至可以上G, 文件太大Profiler分析不了. 所以需要采样分析.
        // startMethodTracingSampling() 三个参数: tracePath: 文件名称，  bufferSize: 文件大小最大是多少(默认8M),
        // intervalUs：采样频率，单位是 微秒(μs),
        Debug.startMethodTracingSampling("lagFile0625-23", 10000*1024*1024,5)
        Debug.stopMethodTracing()
        setContentView(R.layout.activity_ui_lag)
    }

    override fun onResume() {
        super.onResume()
        Debug.stopMethodTracing()
        Debug.startMethodTracingSampling("lagFile0625-24", 10000*1024*1024, 5)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Toast.makeText(this, "onWindowFocusChanged()", Toast.LENGTH_LONG).show()
        Debug.stopMethodTracing()
    }
}