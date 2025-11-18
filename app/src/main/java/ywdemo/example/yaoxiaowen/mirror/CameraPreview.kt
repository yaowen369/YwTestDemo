package ywdemo.example.yaoxiaowen.mirror

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

class CameraPreview(context: Context, private val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback {
    private val TAG = "CameraPreview"
    private val mHolder: SurfaceHolder = holder.apply {
        addCallback(this@CameraPreview)
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            mCamera.setPreviewDisplay(holder)
            mCamera.startPreview()
        } catch (e: IOException) {
            Log.e(TAG, "预览启动失败: ${e.message}")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // 由Activity负责释放相机资源
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mHolder.surface == null) return

        // 停止当前预览
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            Log.e(TAG, "停止预览失败: ${e.message}")
        }

        // 重新配置并启动预览
        try {
            mCamera.setDisplayOrientation(90) // 修正前置摄像头旋转问题
            mCamera.setPreviewDisplay(mHolder)
            mCamera.startPreview()
        } catch (e: Exception) {
            Log.e(TAG, "预览重启失败: ${e.message}")
        }
    }
}