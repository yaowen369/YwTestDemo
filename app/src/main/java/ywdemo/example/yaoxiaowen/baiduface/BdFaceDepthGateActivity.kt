package ywdemo.example.yaoxiaowen.baiduface

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon
import com.baidu.idl.main.facesdk.utils.PreferencesUtil
import org.openni.Device
import org.openni.ImageRegistrationMode
import org.openni.OpenNI
import org.openni.PixelFormat
import org.openni.SensorType
import org.openni.VideoStream
import org.openni.android.OpenNIHelper
import org.openni.android.OpenNIHelper.DeviceOpenListener
import org.openni.android.OpenNIView
import ywdemo.example.yaoxiaowen.R
import ywdemo.example.yaoxiaowen.baiduface.aobilibrary.idl.main.facesdk.utils.FaceUtils
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.activity.BaseOrbbecActivity
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.api.FaceApi
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback.FaceDetectCallBack
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gatecamera.CameraPreviewManager
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.view.GlMantleSurfacView
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager.FaceSDKManager
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager.SaveImageManager
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SaveImageListener
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDFaceCheckConfig
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDFaceImageConfig
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDLiveConfig
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.LivenessModel
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.BitmapUtils
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.FaceOnDrawTexturViewUtil
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.FileUtils
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.ToastUtils
import ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.model.SingleBaseConfig
import ywdemo.example.yaoxiaowen.until.LogUtil
import java.util.concurrent.TimeoutException

class BdFaceDepthGateActivity : BaseOrbbecActivity(), View.OnClickListener, DeviceOpenListener {

    val TAG: String = "BdFaceActy"

    private var isFirstOpenOrbbecSDK = true

    val DEPTH_NEED_PERMISSION: Int = 33

    /*RGB摄像头图像宽和高*/

    val RGB_WIDTH: Int = SingleBaseConfig.getBaseConfig().getRgbAndNirWidth()
    val RGB_HEIGHT: Int = SingleBaseConfig.getBaseConfig().getRgbAndNirHeight()



//    // Depth摄像头图像宽和高
    private var depthWidth: Int = SingleBaseConfig.getBaseConfig().getDepthWidth()
    private var depthHeight: Int = SingleBaseConfig.getBaseConfig().getDepthHeight()



    private lateinit var mContext: Context

    // 调试页面控件
    private lateinit var mFaceDetectImageView: ImageView
    private lateinit var mTvDetect: TextView
    private lateinit var mTvLive: TextView
    private lateinit var mTvLiveScore: TextView
    private lateinit var mNum: TextView

    // 深度数据显示
    private lateinit var mTvDepth: TextView
    private lateinit var mTvDepthScore: TextView

    private lateinit var mTvFeature: TextView
    private lateinit var mTvAll: TextView
    private lateinit var mTvAllTime: TextView

    // 显示Depth图
    private lateinit var mDepthGLView: OpenNIView

    // 设备初始化状态标记
    private var initOk = false

    // 摄像头驱动
    private var mDevice: Device? = null
    private lateinit var thread: Thread
    private var mOpenNIHelper: OpenNIHelper? = null
    private var mDepthStream: VideoStream? = null

    private val sync = Any()

    // 循环取深度图像数据
    private var exit = false

    /*当前摄像头类型*/
    var cameraType: Int = 0

    private var rgbLiveScore = 0f
    private var depthLiveScore = 0f
    private var isCheck = false
    private var isCompareCheck = false
    private lateinit var preText: TextView
    private lateinit var deveLop: TextView
    private lateinit var preViewRelativeLayout: RelativeLayout
    private lateinit var deveLopRelativeLayout: RelativeLayout
    // 已删除冗余的预览模式UI变量（activity_itme_gate 已删除）
    // private lateinit var textHuanying: RelativeLayout
    // private lateinit var userNameLayout: RelativeLayout
    // private lateinit var nameImage: ImageView
    // private lateinit var nameText: TextView
    private lateinit var detectSurfaceText: TextView
    private lateinit var isRgbCheckImage: ImageView
    private lateinit var isDepthCheckImage: ImageView
    private lateinit var preView: View
    private lateinit var developView: View
    private lateinit var view: View
    private lateinit var layoutCompareStatus: RelativeLayout
    private lateinit var textCompareStatus: TextView
    private lateinit var nirSurfaceText: TextView
    private lateinit var logoText: TextView
    private var mUser: User? = null
    private var isTime = true
    private var startTime: Long = 0
    private var detectCount = false
    private lateinit var saveCamera: View
    private var isSaveImage = true
    private lateinit var spot: View
    private lateinit var glSurfaceView: GlMantleSurfacView
    private var bdFaceImageConfig: BDFaceImageConfig? = null
    private var bdDepthFaceImageConfig: BDFaceImageConfig? = null
    private var bdFaceCheckConfig: BDFaceCheckConfig? = null
    private var bdLiveConfig: BDLiveConfig? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mContext = this
        initListener()
        // 人脸库已在BdStartActivity初始化，此处不再重复初始化
        // FaceSDKManager.getInstance().initDataBases(this)

        setContentView(R.layout.activity_face_depth_gate)

        exit = false
        PreferencesUtil.initPrefs(this)
        cameraType = SingleBaseConfig.getBaseConfig().cameraType
        initFaceCheck()
        initView()
    }


    private fun initFaceConfig(height: Int, width: Int) {
        bdFaceImageConfig = BDFaceImageConfig(
            height, width,
            SingleBaseConfig.getBaseConfig().rgbDetectDirection,
            SingleBaseConfig.getBaseConfig().mirrorDetectRGB,
            BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_YUV_NV21
        )
    }

    private fun initDepthFaceConfig(height: Int, width: Int) {
        bdDepthFaceImageConfig = BDFaceImageConfig(
            height, width,
            SingleBaseConfig.getBaseConfig().nirDetectDirection,
            SingleBaseConfig.getBaseConfig().mirrorDetectNIR,
            BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_DEPTH
        )
    }

    private fun initFaceCheck() {
        bdFaceCheckConfig = FaceUtils.getInstance().getBDFaceCheckConfig()
        bdLiveConfig = FaceUtils.getInstance().getBDLiveConfig()
    }


    private fun initListener() {
        if (FaceSDKManager.initStatus !== FaceSDKManager.SDK_MODEL_LOAD_SUCCESS) {
            FaceSDKManager.getInstance().initModel(
                mContext,
                FaceUtils.getInstance().bdFaceSDKConfig, object : SdkInitListener {
                    override fun initStart() {
                    }

                    override fun initLicenseSuccess() {
                    }

                    override fun initLicenseFail(errorCode: Int, msg: String?) {
                    }

                    override fun initModelSuccess() {
                        FaceSDKManager.initModelSuccess = true
                        ToastUtils.toast(mContext, "模型加载成功，欢迎使用")
                    }

                    override fun initModelFail(errorCode: Int, msg: String?) {
                        FaceSDKManager.initModelSuccess = false
                        LogUtil.e(TAG,"initModelFail() -> " + errorCode + "  " + msg)
                        if (errorCode != -12) {
                            ToastUtils.toast(mContext, "模型加载失败，请尝试重启应用")
                        }
                    }
                })
        }
    }

    /**
     * 开启Debug View
     */
    private fun initView() {
        depthWidth = SingleBaseConfig.getBaseConfig().depthWidth
        depthHeight = SingleBaseConfig.getBaseConfig().depthHeight
        // RGB 阈值
        rgbLiveScore = SingleBaseConfig.getBaseConfig().rgbLiveScore
        // depth 阈值
        depthLiveScore = SingleBaseConfig.getBaseConfig().depthLiveScore
        logoText = findViewById(R.id.logo_text)
        logoText.visibility = View.VISIBLE

        // 返回
        val mButReturn: ImageView = findViewById(R.id.btn_back)
        mButReturn.setOnClickListener(this)
        // 设置
        val mBtSetting: ImageView = findViewById(R.id.btn_setting)
        mBtSetting.setOnClickListener(this)
        // 预览模式
        preText = findViewById(R.id.preview_text)
        preText.setOnClickListener(this)
        preText.setTextColor(Color.parseColor("#ffffff"))
        preViewRelativeLayout = findViewById(R.id.yvlan_relativeLayout)
        preView = findViewById(R.id.preview_view)
        // 开发模式
        deveLop = findViewById(R.id.develop_text)
        deveLop.setOnClickListener(this)
        deveLopRelativeLayout = findViewById(R.id.kaifa_relativeLayout)
        developView = findViewById(R.id.develop_view)
        developView.visibility = View.GONE
        layoutCompareStatus = findViewById(R.id.layout_compare_status)
        layoutCompareStatus.visibility = View.GONE
        textCompareStatus = findViewById(R.id.text_compare_status)
        // 存图按钮
        saveCamera = findViewById(R.id.save_camera)
        saveCamera.setOnClickListener(this)
        spot = findViewById(R.id.spot)

        // ***************开发模式*************
        isRgbCheckImage = findViewById(R.id.is_check_image)
        isDepthCheckImage = findViewById(R.id.depth_is_check_image)
        // RGB 阈值
        rgbLiveScore = SingleBaseConfig.getBaseConfig().rgbLiveScore
        // Live 阈值
        depthLiveScore = SingleBaseConfig.getBaseConfig().depthLiveScore
        // 送检RGB 图像回显
        mFaceDetectImageView = findViewById(R.id.face_detect_image_view)
        mFaceDetectImageView.visibility = View.VISIBLE
        // 深度摄像头数据回显
        mDepthGLView = findViewById(R.id.depth_camera_preview_view)
        mDepthGLView.visibility = View.INVISIBLE

        // 存在底库的数量
        mNum = findViewById(R.id.tv_num)
        mNum.text = java.lang.String.format(
            "底库 ： %s 个样本",
            FaceApi.getInstance().getmUserNum()
        )
        // 检测耗时
        mTvDetect = findViewById(R.id.tv_detect_time)
        // RGB活体
        mTvLive = findViewById(R.id.tv_rgb_live_time)
        mTvLiveScore = findViewById(R.id.tv_rgb_live_score)
        // depth活体
        mTvDepth = findViewById(R.id.tv_depth_live_time)
        mTvDepthScore = findViewById(R.id.tv_depth_live_score)
        // 特征提取
        mTvFeature = findViewById(R.id.tv_feature_time)
        // 检索
        mTvAll = findViewById(R.id.tv_feature_search_time)
        // 总耗时
        mTvAllTime = findViewById(R.id.tv_all_time)


        // ***************预览模式*************
        // 已删除冗余的预览模式UI引用（activity_itme_gate 已删除）
        // textHuanying = findViewById(R.id.huanying_relative)
        // userNameLayout = findViewById(R.id.user_name_layout)
        // nameImage = findViewById(R.id.name_image)
        // nameText = findViewById(R.id.name_text)
        detectSurfaceText = findViewById(R.id.detect_surface_text)
        mFaceDetectImageView.visibility = View.GONE
        saveCamera.visibility = View.GONE
        detectSurfaceText.visibility = View.GONE
        view = findViewById(R.id.mongolia_view)
        view.setAlpha(0.85f)
        view.setBackgroundColor(Color.parseColor("#ffffff"))
        nirSurfaceText = findViewById(R.id.depth_surface_text)
        nirSurfaceText.visibility = View.GONE


        glSurfaceView = findViewById(R.id.camera_textureview)
        glSurfaceView.initSurface(
            SingleBaseConfig.getBaseConfig().rgbRevert,
            SingleBaseConfig.getBaseConfig().mirrorVideoRGB,
            SingleBaseConfig.getBaseConfig().isOpenGl
        )
        CameraPreviewManager.getInstance().startPreview( /*mContext, */glSurfaceView,
            SingleBaseConfig.getBaseConfig().rgbVideoDirection,
            RGB_WIDTH,
            RGB_HEIGHT
        )
    }

    /**
     * 在device 启动时候初始化USB 驱动
     *
     * @param device
     */
    private fun initUsbDevice(device: UsbDevice) {
        val opennilist = OpenNI.enumerateDevices()
        android.util.Log.d("BdFaceDepthGate", "enumerateDevices: ${opennilist.size} devices")
        if (opennilist.size <= 0) {
            Toast.makeText(this, " openni enumerateDevices 0 devices", Toast.LENGTH_LONG).show()
            return
        }
        // 不再显式设置为 null，保持现有值
        // Find mDevice ID
        for (i in opennilist.indices) {
            android.util.Log.d("BdFaceDepthGate", "Device[$i]: usbProductId=${opennilist[i].usbProductId}, looking for ${device.productId}")
            if (opennilist[i].usbProductId == device.productId) {
                android.util.Log.d("BdFaceDepthGate", "Found matching device, opening...")
                this.mDevice = Device.open()
                android.util.Log.d("BdFaceDepthGate", "Device.open() returned: $mDevice")
                break
            }
        }

        if (this.mDevice == null) {
            android.util.Log.e("BdFaceDepthGate", "mDevice is still null after initUsbDevice")
            Toast.makeText(
                this, " openni open devices failed: " + device.deviceName,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        android.util.Log.d("BdFaceDepthGate", "initUsbDevice succeeded, mDevice=$mDevice")
    }


    /**
     * 摄像头图像预览
     */
    private fun startCameraPreview() {
        // 设置USB摄像头
        val baseConfig = SingleBaseConfig.getBaseConfig()
        if (baseConfig.getRBGCameraId() != -1) {
            CameraPreviewManager.getInstance().setCameraFacing(baseConfig.getRBGCameraId())
        } else {
            CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_USB)
        }
        val cameraSize = CameraPreviewManager.getInstance().initCamera()
        initFaceConfig(cameraSize[1], cameraSize[0])

        CameraPreviewManager.getInstance().setmCameraDataCallback { data, _, _, _ ->
            // 摄像头预览数据进行人脸检测
            dealRgb(data)
        }
    }


    override fun onDeviceOpened(usbDevice: UsbDevice) {
        initUsbDevice(usbDevice)
        // 添加安全检查，确保 mDevice 不为 null
        if (this.mDevice == null) {
            onDeviceOpenFailed("Device is null after initialization")
            return
        }

        LogUtil.i(TAG, "onDeviceOpened, usbDevice:$usbDevice")

        mDepthStream = VideoStream.create(this.mDevice, SensorType.DEPTH)
        if (mDepthStream != null) {
            val mVideoModes = mDepthStream!!.sensorInfo.supportedVideoModes
            for (mode in mVideoModes) {
                val x = mode.resolutionX
                val y = mode.resolutionY
                val fps = mode.fps
                if (cameraType == 1) {
                    if (x == depthHeight && y == depthWidth && mode.pixelFormat == PixelFormat.DEPTH_1_MM) {
                        mDepthStream!!.videoMode = mode
                        mDevice!!.imageRegistrationMode = ImageRegistrationMode.DEPTH_TO_COLOR
                        break
                    }
                } else {
                    if (x == depthWidth && y == depthHeight && mode.pixelFormat == PixelFormat.DEPTH_1_MM) {
                        mDepthStream!!.videoMode = mode
                        mDevice!!.imageRegistrationMode = ImageRegistrationMode.DEPTH_TO_COLOR
                        break
                    }
                }
            }
            initDepthFaceConfig(depthHeight, depthWidth)
            startThread()
        }
    }


    /**
     * 开启线程接收深度数据
     */
    private fun startThread() {
        initOk = true
        thread = object : Thread() {
            override fun run() {
                val streams: MutableList<VideoStream> = ArrayList()

                streams.add(mDepthStream!!)
                mDepthStream!!.start()

                while (!exit) {
                    try {
                        OpenNI.waitForAnyStream(streams, 2000)
                    } catch (e: TimeoutException) {
                        e.printStackTrace()
                        continue
                    }

                    synchronized(sync) {
                        if (mDepthStream != null) {
                            mDepthGLView.update(mDepthStream)
                            try {
                                val videoFrameRef = mDepthStream!!.readFrame()
                                val depthByteBuf = videoFrameRef.data
                                if (depthByteBuf != null) {
                                    val depthLen = depthByteBuf.remaining()
                                    val depthByte = ByteArray(depthLen)
                                    depthByteBuf[depthByte]
                                    dealDepth(depthByte)
                                }
                                videoFrameRef.release()
                            } catch (e: java.lang.Exception) {
                                e.localizedMessage
                            }
                        }
                    }
                }
            }
        }

        thread.start()
    }


    private fun dealDepth(data: ByteArray) {
        // 修复: 避免非空断言，安全处理
        bdDepthFaceImageConfig?.setData(data) ?: run {
            LogUtil.w(TAG, "bdDepthFaceImageConfig 未初始化，跳过深度数据处理")
            return
        }
        checkData()
    }



    /**
     * 处理RGB摄像头数据，进行人脸检测
     */
    private fun dealRgb(data: ByteArray) {
        bdFaceImageConfig?.setData(data)
        glSurfaceView.setFrame()
        checkData()
    }


    @Synchronized
    private fun checkData() {
        val rgbData = bdFaceImageConfig?.data
        val depthConfig = bdDepthFaceImageConfig
        val depthData = depthConfig?.data

        if (rgbData != null && depthConfig != null && depthData != null) {
            android.util.Log.d(TAG, "checkData: 调用人脸检测，RGB数据长度=${rgbData.size}, Depth数据长度=${depthData.size}")
            FaceSDKManager.getInstance().onDetectCheck(
                bdFaceImageConfig, null, bdDepthFaceImageConfig,
                bdFaceCheckConfig, object : FaceDetectCallBack {
                    override fun onFaceDetectCallback(livenessModel: LivenessModel?) {
                        android.util.Log.d(TAG, "onFaceDetectCallback: livenessModel=$livenessModel")
                        // 输出结果
                        checkCloseDebugResult(livenessModel)
                        // 开发模式
                        checkOpenDebugResult(livenessModel)
                        if (isSaveImage) {
                            // 检查 livenessModel 是否为 null，避免空指针异常
                            if (livenessModel != null) {
                                SaveImageManager.getInstance().saveImage(mContext, livenessModel, bdLiveConfig,
                                    object : SaveImageListener {
                                        override fun onSaveSuccess(savedCount: Int, filePaths: MutableList<String>, timestamp: String) {
                                            // 检查Activity是否已销毁，避免内存泄漏
                                            if (!isFinishing && !isDestroyed) {
                                                LogUtil.i(TAG, "图片保存成功: 数量=$savedCount, 时间戳=$timestamp")
                                                LogUtil.d(TAG, "保存的图片路径: $filePaths")
                                            }
                                        }

                                        override fun onSaveFailed(errorCode: Int, errorMsg: String) {
                                            if (!isFinishing && !isDestroyed) {
                                                LogUtil.e(TAG, "图片保存失败: errorCode=$errorCode, errorMsg=$errorMsg")
                                            }
                                        }

                                        override fun onConditionNotMet(rgbScore: Float, depthScore: Float, threshold: Float) {
                                            if (!isFinishing && !isDestroyed) {
                                                LogUtil.d(TAG, String.format("图片未保存: RGB(%.2f) Depth(%.2f) 阈值(%.2f)",
                                                    rgbScore, depthScore, threshold))
                                            }
                                        }
                                    })
                            } else {
                                LogUtil.w(TAG, "livenessModel 为 null，跳过保存图片")
                            }
                        }
                    }

                    override fun onTip(code: Int, msg: String?) {
                        android.util.Log.d(TAG, "onTip: code=$code, msg=$msg")
                    }

                    override fun onFaceDetectDarwCallback(livenessModel: LivenessModel?) {
                        showFrame(livenessModel)
                    }
                })
        } else {
            android.util.Log.d(TAG, "checkData: 条件不满足，rgbData=${rgbData != null}, depthConfig=${depthConfig != null}, depthData=${depthData != null}")
        }
    }


    // ***************预览模式结果输出*************
    private fun checkCloseDebugResult(livenessModel: LivenessModel?) {
        // 当未检测到人脸UI显示
        runOnUiThread(Runnable {
            // 已删除冗余的预览模式UI操作（activity_itme_gate 已删除）
            /*
            if (livenessModel == null) {
                // 对背景色颜色进行改变，操作的属性为"alpha",此处必须这样写
                // 不能全小写,后面设置的是对view的渐变
                if (isTime) {
                    isTime = false
                    startTime = System.currentTimeMillis()
                }
                detectCount = true
                val endTime = System.currentTimeMillis() - startTime

                if (endTime < 10000) {
                    textHuanying.visibility = View.VISIBLE
                    userNameLayout.visibility = View.GONE
                    return@Runnable
                } else {
                    view.visibility = View.VISIBLE
                }

                textHuanying.visibility = View.VISIBLE
                userNameLayout.visibility = View.GONE
                return@Runnable
            }
            */

            // 保留核心逻辑
            if (livenessModel != null) {
                isTime = true
                if (detectCount) {
                    detectCount = false
                    objectAnimator()
                } else {
                    view.visibility = View.GONE
                }

                val user = livenessModel.user
                if (user == null) {
                    mUser = null
                    // 预览模式UI操作已删除
                } else {
                    mUser = user
                    // 预览模式UI操作已删除
                }
            }
        })
    }


    // ***************开发模式结果输出*************
    private fun checkOpenDebugResult(livenessModel: LivenessModel?) {
        // 当未检测到人脸UI显示

        runOnUiThread(Runnable {
            if (livenessModel == null) {
                layoutCompareStatus.visibility = View.GONE
                isRgbCheckImage.visibility = View.GONE
                isDepthCheckImage.visibility = View.GONE
                mFaceDetectImageView.setImageResource(R.mipmap.ic_image_video)
                mTvDetect.text = String.format("检测耗时 ：%s ms", 0)
                mTvLive.text = String.format("RGB活体检测耗时 ：%s ms", 0)
                mTvLiveScore.text = String.format("RGB活体得分 ：%s", 0)
                mTvDepth.text = String.format("Depth活体检测耗时 ：%s ms", 0)
                mTvDepthScore.text = String.format("Depth活体得分 ：%s", 0)
                mTvFeature.text = String.format("特征抽取耗时 ：%s ms", 0)
                mTvAll.text = String.format("特征比对耗时 ：%s ms", 0)
                mTvAllTime.text = String.format("总耗时 ：%s ms", 0)
                return@Runnable
            }
            val image = livenessModel.bdFaceImageInstance
            if (image != null) {
                mFaceDetectImageView.setImageBitmap(BitmapUtils.getInstaceBmp(image))
                image.destory()
            }

            val rgbLivenessScore = livenessModel.rgbLivenessScore
            if (rgbLivenessScore < rgbLiveScore) {
                if (isCheck) {
                    isRgbCheckImage.visibility = View.VISIBLE
                    isRgbCheckImage.setImageResource(R.mipmap.ic_icon_develop_fail)
                }
            } else {
                if (isCheck) {
                    isRgbCheckImage.visibility = View.VISIBLE
                    isRgbCheckImage.setImageResource(R.mipmap.ic_icon_develop_success)
                }
            }

            val depthLivenessScore = livenessModel.depthLivenessScore
            if (depthLivenessScore < depthLiveScore) {
                if (isCheck) {
                    isDepthCheckImage.visibility = View.VISIBLE
                    isDepthCheckImage.setImageResource(R.mipmap.ic_icon_develop_fail)
                }
            } else {
                if (isCheck) {
                    isDepthCheckImage.visibility = View.VISIBLE
                    isDepthCheckImage.setImageResource(R.mipmap.ic_icon_develop_success)
                }
            }
            if (livenessModel.isQualityCheck) {
                if (isCompareCheck) {
                    layoutCompareStatus.visibility = View.VISIBLE
                    textCompareStatus.setTextColor(Color.parseColor("#FFFEC133"))
                    //                                                textCompareStatus.setMaxEms(6);
                    textCompareStatus.text = "请正视摄像头"
                }
            } else if (rgbLivenessScore < rgbLiveScore || depthLivenessScore < depthLiveScore) {
                if (isCompareCheck) {
                    layoutCompareStatus.visibility = View.VISIBLE
                    textCompareStatus.setTextColor(Color.parseColor("#FFFEC133"))

                    //                            textCompareStatus.setMaxEms(7);
                    textCompareStatus.text = "活体检测未通过"
                }
            } else {
                val user = livenessModel.user
                if (user == null) {
                    mUser = null
                    if (isCompareCheck) {
                        if (livenessModel.isMultiFrame) {
                            layoutCompareStatus.visibility = View.VISIBLE
                            textCompareStatus.setTextColor(Color.parseColor("#FFFEC133"))
                            textCompareStatus.text = "识别未通过"
                        } else {
                            layoutCompareStatus.visibility = View.GONE
                        }
                    }
                } else {
                    mUser = user
                    if (isCompareCheck) {
                        layoutCompareStatus.visibility = View.VISIBLE
                        textCompareStatus.setTextColor(Color.parseColor("#FF00BAF2"))

                        //                            textCompareStatus.setMaxEms(5);
                        textCompareStatus.text = FileUtils.spotString(mUser!!.userName)
                    }
                }
            }

            mTvDetect.text =
                java.lang.String.format("检测耗时 ：%s ms", livenessModel.rgbDetectDuration)
            mTvLive.text =
                java.lang.String.format("RGB活体检测耗时 ：%s ms", livenessModel.rgbLivenessDuration)
            mTvLiveScore.text =
                java.lang.String.format("RGB活体得分 ：%s", livenessModel.rgbLivenessScore)
            mTvDepth.text = java.lang.String.format(
                "Depth活体检测耗时 ：%s ms",
                livenessModel.depthtLivenessDuration
            )
            mTvDepthScore.text =
                java.lang.String.format("Depth活体得分 ：%s", livenessModel.depthLivenessScore)
            mTvFeature.text =
                java.lang.String.format("特征抽取耗时 ：%s ms", livenessModel.featureDuration)
            mTvAll.text =
                java.lang.String.format("特征比对耗时 ：%s ms", livenessModel.checkDuration)
            mTvAllTime.text =
                java.lang.String.format("总耗时 ：%s ms", livenessModel.allDetectDuration)
        })
    }


    override fun onClick(v: View) {
        val id = v.id // 返回
        if (id == R.id.btn_back) {
            if (!FaceSDKManager.initModelSuccess) {
                Toast.makeText(
                    mContext, "SDK正在加载模型，请稍后再试",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            if (thread != null) {
                thread!!.interrupt()
            }
            finish()
            // 设置
        } else if (id == R.id.btn_setting) {
            if (!FaceSDKManager.initModelSuccess) {
                Toast.makeText(
                    mContext, "SDK正在加载模型，请稍后再试",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            if (thread != null) {
                thread!!.interrupt()
            }
            // TODO 临时屏蔽，该页面 还没有 开始
//            startActivity(Intent(mContext, GateSettingActivity::class.java))
            finish()
        } else if (id == R.id.preview_text) {
            isRgbCheckImage.visibility = View.GONE
            isDepthCheckImage.visibility = View.GONE
            mFaceDetectImageView.visibility = View.GONE
            saveCamera.visibility = View.GONE
            detectSurfaceText.visibility = View.GONE
            nirSurfaceText.visibility = View.GONE
            layoutCompareStatus.visibility = View.GONE
            mDepthGLView.visibility = View.GONE
            view.visibility = View.VISIBLE
            deveLop.setTextColor(Color.parseColor("#a9a9a9"))
            preText.setTextColor(Color.parseColor("#ffffff"))
            preView.visibility = View.VISIBLE
            developView.visibility = View.GONE
            preViewRelativeLayout.visibility = View.VISIBLE
            deveLopRelativeLayout.visibility = View.GONE
            logoText.visibility = View.VISIBLE
            isCheck = false
            isCompareCheck = false
        } else if (id == R.id.develop_text) {
            isCheck = true
            isCompareCheck = true
            isRgbCheckImage.visibility = View.VISIBLE
            isDepthCheckImage.visibility = View.VISIBLE
            mFaceDetectImageView.visibility = View.VISIBLE
            saveCamera.visibility = View.VISIBLE
            detectSurfaceText.visibility = View.VISIBLE
            nirSurfaceText.visibility = View.VISIBLE
            view.visibility = View.GONE
            deveLop.setTextColor(Color.parseColor("#ffffff"))
            preText.setTextColor(Color.parseColor("#a9a9a9"))
            preView.visibility = View.GONE
            developView.visibility = View.VISIBLE
            deveLopRelativeLayout.visibility = View.VISIBLE
            preViewRelativeLayout.visibility = View.GONE
            mDepthGLView.visibility = View.VISIBLE
            logoText.visibility = View.GONE
            judgeFirst()
        } else if (id == R.id.save_camera) {
//            isSaveImage = !isSaveImage
            // 临时屏蔽，先确认其修改
            isSaveImage = true
            if (isSaveImage) {
                spot.visibility = View.VISIBLE
                ToastUtils.toast(this@BdFaceDepthGateActivity, "存图功能已开启再次点击可关闭")
            } else {
                spot.visibility = View.GONE
            }
        }
    }


    private fun judgeFirst() {
        val sharedPreferences = this.getSharedPreferences("share", MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isGateFirstSave", true)
        val editor = sharedPreferences.edit()
        if (isFirstRun) {
            setFirstView(View.VISIBLE)
            Handler().postDelayed({ setFirstView(View.GONE) }, 3000)
            editor.putBoolean("isGateFirstSave", false)
            editor.commit()
        }
    }


    private fun setFirstView(visibility: Int) {
        findViewById<LinearLayout>(R.id.first_text_tips).setVisibility(visibility)
        findViewById<View>(R.id.first_circular_tips).setVisibility(visibility)
    }


    override fun onResume() {
        super.onResume()
        // 摄像头图像预览
        startCameraPreview()

        // 初始化 深度摄像头
        mOpenNIHelper = OpenNIHelper(this)
        mOpenNIHelper!!.requestDeviceOpen(this)
    }


    /**
     * 释放深度摄像头资源
     * 修复: 提取公共方法，避免onPause和onDestroy中的代码重复
     */
    private fun releaseDepthResources() {
        exit = true
        if (initOk) {
            if (thread != null) {
                try {
                    // 先设置exit标志，再interrupt
                    exit = true
                    thread!!.interrupt()
                    thread!!.join(1000)  // 等待最多1秒
                } catch (e: Exception) {
                    LogUtil.e(TAG, "停止线程失败: ${e.message}")
                }
            }
            if (mDepthStream != null) {
                mDepthStream!!.stop()
                mDepthStream!!.destroy()
                mDepthStream = null
            }
            if (mDevice != null) {
                mDevice!!.close()
                mDevice = null
            }
        }
        if (mOpenNIHelper != null) {
            mOpenNIHelper!!.shutdown()
            mOpenNIHelper = null
        }
    }

    override fun onPause() {
        super.onPause()
        releaseDepthResources()
    }

    override fun onStop() {
        super.onStop()
    }


    public override fun onDestroy() {
        super.onDestroy()

        CameraPreviewManager.getInstance().stopPreview()
        releaseDepthResources()
    }

    override fun onDeviceOpenFailed(msg: String) {
        LogUtil.e(TAG, "onDeviceOpenFailed, msg:$msg")
        showAlertAndExit("Open Device failed: $msg")
    }

    override fun onDeviceNotFound() {
        LogUtil.e(TAG, "onDeviceNotFound")
        showAlertAndExit("Open Device failed: NotFound")
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == DEPTH_NEED_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, "Permission Grant", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 绘制人脸框
     */
    private fun showFrame(model: LivenessModel?) {
        if (model == null) {
            return
        }
        glSurfaceView.onGlDraw(
            model.trackFaceInfo,
            model.bdFaceImageInstance,
            FaceOnDrawTexturViewUtil.drawFaceColor(mUser, model)
        )
    }

    private fun showAlertAndExit(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setNeutralButton("OK") { dialog, which -> finish() }
        builder.show()
    }

    // 蒙层动画
    private fun objectAnimator() {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 0.0f, 0.85f)
        animator.setDuration(500)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                animator.cancel()
            }

            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                view.setBackgroundColor(Color.parseColor("#ffffff"))
            }
        })
        animator.start()
    }

}