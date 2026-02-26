package ywdemo.example.yaoxiaowen.baiduface

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import ywdemo.example.yaoxiaowen.R
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener
import ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.utils.GateConfigUtils
import ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager
import ywdemo.example.yaoxiaowen.baiduface.registerlibrary.idl.main.facesdk.registerlibrary.user.utils.RegisterConfigUtils
import ywdemo.example.yaoxiaowen.until.LogUtil
import java.util.Timer
import java.util.TimerTask

class BdStartActivity : Activity(), View.OnClickListener {

    val TAG: String = "BdStartActivity"

    private var mContext: Context? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val btnStart: Button by lazy { findViewById<Button>(R.id.btn_start) }
    private val displayTv: TextView by lazy { findViewById<TextView>(R.id.display_tv) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_bd_start)
        mContext = this
        btnStart.setOnClickListener(this)


        val isConfigExit: Boolean = GateConfigUtils.isConfigExit(this)
        val isInitConfig = GateConfigUtils.initConfig()
        val isRegisterConfigExit: Boolean = RegisterConfigUtils.isConfigExit(this)

        val isRegisterInitConfig = RegisterConfigUtils.initConfig()

        if (isInitConfig && isConfigExit && isRegisterInitConfig && isRegisterConfigExit) {
            Toast.makeText(mContext, "初始配置加载成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                mContext,
                "初始配置失败,将重置文件内容为默认配置",
                Toast.LENGTH_SHORT
            ).show()
            GateConfigUtils.modityJson()
            RegisterConfigUtils.modityJson()
        }

        initLicense()
    }

    private fun initLicense() {
        FaceSDKManager.getInstance().init(mContext, object : SdkInitListener {
            override fun initStart() {
                LogUtil.i(TAG, "initStart() ")
            }

            override fun initLicenseSuccess() {
                val task: TimerTask = object : TimerTask() {
                    override fun run() {
                        /**
                         * 要执行的操作
                         */
                        // TODO yaowen 临时屏蔽
                        LogUtil.i(TAG, "initLicenseSuccess() ")
//                        startActivity(Intent(mContext, HomeActivity::class.java))

                        mainHandler.post {
                            displayTv.text = "license加载成功, 可以跳转HomeActivity"
                            displayTv.setTextColor(Color.BLUE)
                        }
//                        finish()
                    }
                }
                val timer = Timer()
                timer.schedule(task, 2000)
            }

            override fun initLicenseFail(errorCode: Int, msg: String?) {
                val task: TimerTask = object : TimerTask() {
                    override fun run() {
                        /**
                         * 要执行的操作
                         */
                        // TODO yaowen 临时屏蔽
                        // log 当中提示内容： errorCode:-1, msg:授权码不存在，请重新输入！
                        LogUtil.e(TAG, "initLicenseFail() -> errorCode:${errorCode}, msg:${msg}")
                        mainHandler.post {
                            displayTv.text = "license加载失败，errorCode:${errorCode}, msg:${msg}"
                            displayTv.setTextColor(Color.RED)
                        }

//                        startActivity(Intent(mContext, ActivitionActivity::class.java))
//                        finish()
                    }
                }
                val timer = Timer()
                timer.schedule(task, 2000)
            }

            override fun initModelSuccess() {
                LogUtil.i(TAG, "initModelSuccess() ")
            }

            override fun initModelFail(errorCode: Int, msg: String?) {
                LogUtil.e(TAG, "initModelFail() -> errorCode:${errorCode}, msg:${msg}")
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start -> {
                startActivity(Intent(mContext, BdHomeActivity::class.java))
            }
        }
    }

}