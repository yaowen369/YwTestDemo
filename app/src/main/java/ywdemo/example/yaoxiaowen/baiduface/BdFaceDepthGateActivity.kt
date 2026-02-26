package ywdemo.example.yaoxiaowen.baiduface

import android.app.Activity
import android.os.Bundle
import org.openni.OpenNI
import ywdemo.example.yaoxiaowen.R

class BdFaceDepthGateActivity : Activity() {

    private var isFirstOpenOrbbecSDK = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在 setContentView 之前初始化 OpenNI，加载 native 库
        if (isFirstOpenOrbbecSDK) {
            initializeOpenNI()
            isFirstOpenOrbbecSDK = false
        }

        setContentView(R.layout.activity_face_depth_gate)
    }

    /**
     * 初始化 OpenNI SDK，必须在使用 OpenNIView 之前调用
     * 如果没有  OpenNI.initialize() 这行初始化代码则会报错，android.view.InflateException: Binary XML file line #67: Binary XML file
     * 而在 FaceSDKAndroid 项目中，相关初始化代码 位于 BaseOrbbecActivity.java 中
     */
    private fun initializeOpenNI() {
        try {
            // 设置SDK Log 日志是否输出
            OpenNI.setLogAndroidOutput(true)
            // 设置Log日志输出级别
            OpenNI.setLogMinSeverity(0)
            // 初始化SDK（这会加载必要的 native 库）
            OpenNI.initialize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}