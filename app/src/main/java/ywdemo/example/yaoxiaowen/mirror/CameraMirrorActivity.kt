package ywdemo.example.yaoxiaowen.mirror

import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ywdemo.example.yaoxiaowen.R

class CameraMirrorActivity : Activity() {


    private val TAG = "MirrorApp"
    private val REQUEST_CAMERA_PERMISSION = 100
    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private var mHandler:Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏设置
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_camera_mirror)

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            mHandler = mHandler ?: Handler(mainLooper)
            mHandler?.post(Runnable {
                initCamera()
            })


        }
    }

    // 初始化相机
    private fun initCamera() {
        // 获取前置摄像头
        mCamera = getFrontCamera() ?: run {
            Toast.makeText(this, "无法获取前置摄像头", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 优化预览尺寸
        optimizePreviewSize()

        // 创建并添加预览视图
        mPreview = CameraPreview(this, mCamera!!)
        val previewFrame = findViewById<FrameLayout>(R.id.preview_frame)
        previewFrame.addView(mPreview)
    }

    // 获取前置摄像头实例
    private fun getFrontCamera(): Camera? {
        val cameraCount = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()

        for (i in 0 until cameraCount) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return try {
                    Camera.open(i)
                } catch (e: Exception) {
                    Log.e(TAG, "打开前置摄像头失败: ${e.message}")
                    null
                }
            }
        }
        return null
    }

    // 优化预览尺寸以匹配屏幕
    private fun optimizePreviewSize() {
        val camera = mCamera ?: return
        val parameters = camera.parameters
        val supportedSizes = parameters.supportedPreviewSizes
        val previewFrame = findViewById<FrameLayout>(R.id.preview_frame)

        var surfaceWidth = previewFrame.width
        var surfaceHeight = previewFrame.height

        // 如果还未测量完成，使用屏幕尺寸
        if (surfaceWidth == 0 || surfaceHeight == 0) {
            val display = windowManager.defaultDisplay
            surfaceWidth = display.width
            surfaceHeight = display.height
        }

        // 设置最佳预览尺寸
        getOptimalPreviewSize(supportedSizes, surfaceWidth, surfaceHeight)?.let {
            parameters.setPreviewSize(it.width, it.height)
            camera.parameters = parameters
        }
    }

    // 计算与屏幕比例最匹配的预览尺寸
    private fun getOptimalPreviewSize(
        sizes: List<Camera.Size>?,
        w: Int,
        h: Int
    ): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = w.toDouble() / h

        if (sizes == null) return null

        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE
        val targetHeight = h

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        // 如果没有找到匹配比例的，选择高度最接近的
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera()
            } else {
                Toast.makeText(this, "需要相机权限才能使用镜子功能", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // 释放相机资源
    private fun releaseCamera() {
        mCamera?.apply {
            stopPreview()
            release()
        }
        mCamera = null
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    override fun onResume() {
        super.onResume()
        if (mCamera == null && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCamera()
    }

}