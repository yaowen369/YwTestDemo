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
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ywdemo.example.yaoxiaowen.R
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.api.FaceApi
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.DBLoadListener
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User
import ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.utils.GateConfigUtils
import ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager
import ywdemo.example.yaoxiaowen.baiduface.registerlibrary.idl.main.facesdk.registerlibrary.user.utils.RegisterConfigUtils
import ywdemo.example.yaoxiaowen.until.LogUtil

class BdStartActivity : Activity(), View.OnClickListener {

    companion object {
        // 常量定义，避免硬编码魔法数字
        private const val SMALL_DATA_THRESHOLD = 5000
        private const val PROGRESS_UPDATE_DELAY = 10L
        private const val NAVIGATION_DELAY = 1500L
        private const val PROGRESS_STEP = 100
    }

    val TAG: String = "BdStartActivity"

    private var mContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // UI组件
    private val btnStart: Button by lazy { findViewById<Button>(R.id.btn_start) }
    private val displayTv: TextView by lazy { findViewById<TextView>(R.id.display_tv) }
    private val progressGroup: View by lazy { findViewById<View>(R.id.progress_group) }
    private val progressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }
    private val progressText: TextView by lazy { findViewById<TextView>(R.id.progress_text) }

    // 状态管理
    private enum class LoadingState {
        IDLE,               // 初始状态
        LICENSING,          // License激活中
        LICENSE_FAILED,     // License失败
        DB_LOADING,         // 人脸库加载中
        DB_SUCCESS,         // 人脸库成功
        DB_FAILED           // 人脸库失败
    }

    private var currentState = LoadingState.IDLE
    private var loadJob: Job? = null
    private val scopeJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + scopeJob)

    // 人脸库加载状态
    private var isDBLoad = false
    private var lastErrorMsg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_bd_start)
        mContext = this

        // 初始化配置
        initConfig()

        // 设置按钮点击监听
        btnStart.setOnClickListener(this)

        // 开始License激活
        initLicense()
    }

    /**
     * 初始化配置文件
     */
    private fun initConfig() {
        val isConfigExit: Boolean = GateConfigUtils.isConfigExit(this)
        val isInitConfig = GateConfigUtils.initConfig()
        val isRegisterConfigExit: Boolean = RegisterConfigUtils.isConfigExit(this)
        val isRegisterInitConfig = RegisterConfigUtils.initConfig()

        val gateConfigValid = isInitConfig && isConfigExit
        val registerConfigValid = isRegisterInitConfig && isRegisterConfigExit

        if (gateConfigValid && registerConfigValid) {
            LogUtil.i(TAG, "初始配置加载成功")
        } else {
            LogUtil.w(TAG, "初始配置失败,将重置文件内容为默认配置")
            GateConfigUtils.modityJson()
            RegisterConfigUtils.modityJson()
        }
    }

    /**
     * License激活
     */
    private fun initLicense() {
        updateState(LoadingState.LICENSING)

        FaceSDKManager.getInstance().init(mContext, object : SdkInitListener {
            override fun initStart() {
                LogUtil.i(TAG, "initStart()")
            }

            override fun initLicenseSuccess() {
                LogUtil.i(TAG, "License激活成功")
                // 修复: 回调在后台线程，需要切换到主线程更新UI
                mainHandler.post {
                    if (!isFinishing && !isDestroyed) {
                        updateState(LoadingState.DB_LOADING)
                        // 开始加载人脸库
                        loadFaceLibrary()
                    }
                }
            }

            override fun initLicenseFail(errorCode: Int, msg: String?) {
                LogUtil.e(TAG, "License激活失败: errorCode=$errorCode, msg=$msg")
                lastErrorMsg = "License激活失败\ncode: $errorCode\n$msg"
                // 修复: 回调在后台线程，需要切换到主线程更新UI
                mainHandler.post {
                    if (!isFinishing && !isDestroyed) {
                        updateState(LoadingState.LICENSE_FAILED)
                    }
                }
            }

            override fun initModelSuccess() {
                LogUtil.i(TAG, "initModelSuccess()")
            }

            override fun initModelFail(errorCode: Int, msg: String?) {
                LogUtil.e(TAG, "initModelFail() -> errorCode:$errorCode, msg:$msg")
            }
        })
    }

    /**
     * 加载人脸库（使用协程在后台执行）
     */
    private fun loadFaceLibrary() {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FaceApi.getInstance().init(object : DBLoadListener {
                        override fun onStart(successCount: Int) {
                            // 小数据量时使用模拟进度
                            if (successCount < SMALL_DATA_THRESHOLD && successCount != 0) {
                                mainHandler.post {
                                    if (!isFinishing && !isDestroyed) {
                                        loadProgress(PROGRESS_STEP.toFloat())
                                    }
                                }
                            }
                        }

                        override fun onLoad(finishCount: Int, successCount: Int, progress: Float) {
                            // 大数据量时显示真实进度
                            if (successCount > SMALL_DATA_THRESHOLD || successCount == 0) {
                                mainHandler.post {
                                    if (!isFinishing && !isDestroyed) {
                                        val progressInt = (progress * 100).toInt()
                                        progressBar.setProgress(progressInt)
                                        progressText.text = "$progressInt%"
                                    }
                                }
                            }
                        }

                        override fun onComplete(users: List<User?>?, successCount: Int) {
                            mainHandler.post {
                                if (!isFinishing && !isDestroyed) {
                                    FaceApi.getInstance().setUsers(users)
                                    isDBLoad = true
                                    updateState(LoadingState.DB_SUCCESS)
                                }
                            }
                        }

                        override fun onFail(finishCount: Int, successCount: Int, users: List<User?>?) {
                            mainHandler.post {
                                if (!isFinishing && !isDestroyed) {
                                    FaceApi.getInstance().setUsers(users)
                                    lastErrorMsg = "人脸库加载失败\n共: $successCount 条\n已加载: $finishCount 条"
                                    updateState(LoadingState.DB_FAILED)
                                }
                            }
                        }
                    }, this@BdStartActivity)
                }
            } catch (e: Exception) {
                // 修复: LogUtil.e没有接受Throwable的重载，只记录消息
                LogUtil.e(TAG, "人脸库加载异常: ${e.message}")
                lastErrorMsg = "人脸库加载异常: ${e.message}"
                updateState(LoadingState.DB_FAILED)
            }
        }
    }

    /**
     * 模拟进度显示（小数据量时使用）
     */
    private fun loadProgress(i: Float) {
        if (currentState != LoadingState.DB_LOADING) return

        // 修复: 使用mainHandler而不是new Handler，避免内存泄漏
        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                val progress = ((i / SMALL_DATA_THRESHOLD) * 100).toInt()
                progressBar.setProgress(progress)
                progressText.text = "$progress%"

                if (i < SMALL_DATA_THRESHOLD) {
                    loadProgress(i + PROGRESS_STEP)
                } else {
                    isDBLoad = true
                    updateState(LoadingState.DB_SUCCESS)
                }
            }
        }, PROGRESS_UPDATE_DELAY)
    }

    /**
     * 更新UI状态
     */
    private fun updateState(state: LoadingState) {
        currentState = state
        if (isFinishing || isDestroyed) return

        when (state) {
            LoadingState.IDLE -> {
                displayTv.text = "准备初始化..."
                displayTv.setTextColor(Color.BLACK)
                progressGroup.visibility = View.GONE
                btnStart.visibility = View.GONE
            }
            LoadingState.LICENSING -> {
                displayTv.text = "正在激活License..."
                displayTv.setTextColor(Color.BLUE)
                progressGroup.visibility = View.GONE
                btnStart.visibility = View.GONE
            }
            LoadingState.LICENSE_FAILED -> {
                displayTv.text = lastErrorMsg ?: "License激活失败"
                displayTv.setTextColor(Color.RED)
                progressGroup.visibility = View.GONE
                btnStart.visibility = View.VISIBLE
                btnStart.text = "重试激活"
            }
            LoadingState.DB_LOADING -> {
                displayTv.text = "正在加载人脸库..."
                displayTv.setTextColor(Color.BLUE)
                progressGroup.visibility = View.VISIBLE
                progressBar.progress = 0
                progressText.text = "0%"
                btnStart.visibility = View.GONE
            }
            LoadingState.DB_SUCCESS -> {
                displayTv.text = "初始化完成，即将跳转..."
                displayTv.setTextColor(Color.GREEN)
                progressGroup.visibility = View.GONE
                btnStart.visibility = View.GONE

                // 延迟后跳转
                mainHandler.postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        startActivity(Intent(mContext, BdFaceDepthGateActivity::class.java))
                        finish()
                    }
                }, NAVIGATION_DELAY)
            }
            LoadingState.DB_FAILED -> {
                displayTv.text = lastErrorMsg ?: "人脸库加载失败"
                displayTv.setTextColor(Color.RED)
                progressGroup.visibility = View.GONE
                btnStart.visibility = View.VISIBLE
                btnStart.text = "重试加载"
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start -> {
                when (currentState) {
                    LoadingState.LICENSE_FAILED -> {
                        // 重试License激活
                        initLicense()
                    }
                    LoadingState.DB_FAILED -> {
                        // 重试人脸库加载
                        updateState(LoadingState.DB_LOADING)
                        loadFaceLibrary()
                    }
                    else -> {
                        LogUtil.w(TAG, "当前状态不允许重试: $currentState")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消协程任务
        loadJob?.cancel()
        // 修复: 取消整个协程作用域，避免内存泄漏
        scopeJob.cancel()
        // 移除所有回调
        mainHandler.removeCallbacksAndMessages(null)
    }
}
