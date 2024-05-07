package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import java.io.File
import java.io.FileOutputStream


class BitmapTestActivity : Activity() {

    private val saveImageBtn1: Button by lazy {
        findViewById(R.id.saveImageBtn1)
    }

    private val saveImageFromDrawingCacheBtn:Button by lazy {
        findViewById(R.id.saveImageFromDrawingCache)
    }

    private val ll:LinearLayout by lazy {
        findViewById(R.id.ll)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap_test)
        saveImageBtn1.setOnClickListener(View.OnClickListener {
            saveBitmap(createBitmap(ll))
        })
        saveImageFromDrawingCacheBtn.setOnClickListener(View.OnClickListener {
            saveBitmap(createBitmapFromDrawingCache(ll))
        })
    }

    fun createBitmap(view: View): Bitmap? {
        // 1, 创建一个 Bitmap 对象
        val startTime = System.currentTimeMillis()

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.RGB_565)
        // 2,  创建一个 Canvas 对象，并关联到 Bitmap

        val canvas = Canvas(bitmap)
        // 3,  将 View 绘制到 Canvas

        view.draw(canvas)
        // 4, 此时Bitmap已经生成了，可以对其做进一步处理
        Log.i(TAG, "createBitmap()共计耗时: ${System.currentTimeMillis()-startTime}, bitmap是否为null:${bitmap==null}")
        return bitmap
    }

    fun createBitmapFromDrawingCache(view: View) : Bitmap {
        val startTime = System.currentTimeMillis()
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val bitmap = view.getDrawingCache()
//        view.destroyDrawingCache()
        Log.i(TAG, "createBitmapFromDrawingCache()共计耗时: ${System.currentTimeMillis()-startTime}, bitmap是否为null:${bitmap==null}")

        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap?) {
        bitmap ?: return

        try {
            bitmap ?: return
            val app = this.application
            app ?: return
            val filesPath = "${app.filesDir}/tempfiles/"
            val filesDir = File(filesPath)
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            val file = File(filesDir, "${System.currentTimeMillis()}.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmapFailed exception:${Log.getStackTraceString(e)}")
        }
    }

    companion object {
        const val TAG = "BitmapTestActivity"
    }
}