package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Window
import androidx.core.app.ActivityCompat
import ywdemo.example.yaoxiaowen.until.LogUtil

class TurnOffScreenActivity : Activity() {


    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    lateinit private var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turn_off_screen)

        mainHandler = Handler(Looper.getMainLooper());


        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WAKE_LOCK
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                LogUtil.i(TAG, "申请 WAKE_LOCK权限")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WAKE_LOCK),
                    1
                )
            } else {
                LogUtil.i(TAG, "已经有了 WAKE_LOCK权限")
            }
        }


        mainHandler.postDelayed({
            LogUtil.i(TAG, "屏幕亮度设置为0")
            setScreenBrightness(0f)

            mainHandler.postDelayed(Runnable {
                LogUtil.i(TAG, "屏幕亮度设置为255")
                setScreenBrightness(255f)
            }, 15 * 1000)
        }, 15 * 1000)

    }

    // 亮屏
    fun turnOnScreen() {
        if (::powerManager.isInitialized) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "MyApp:MyWakeLockTag"
            )
            wakeLock?.acquire(10 * 1000L) // 10 minutes
        }
    }

    // 熄屏
    fun turnOffScreen() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
        }
    }

    // 设置屏幕亮度
    fun setScreenBrightness(brightness: Float) {
        val window: Window = window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
        }
    }

    companion object {
        const val TAG = "TurnOffScreenActivity";
    }


}