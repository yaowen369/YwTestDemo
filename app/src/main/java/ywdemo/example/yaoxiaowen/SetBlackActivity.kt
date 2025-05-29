package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import ywdemo.example.yaoxiaowen.until.LogUtil


class SetBlackActivity : Activity() {

    val TAG:String  = "SetBlackActy"

    private var blackOverlay: View? = null

    private val mMainHandler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_black)

        mMainHandler.postDelayed(Runnable {
            LogUtil.i(TAG, "添加黑色View遮挡")
            addBlackOverlay()

            mMainHandler.postDelayed({
                LogUtil.i(TAG, "移除黑色View遮挡")
                removeBlackOverlay()

            }, 20 * 1000)

        }, 20 * 1000)

    }


    // 添加全屏黑色遮罩
    private fun addBlackOverlay() {
        if (blackOverlay == null) {
            blackOverlay = View(this)
            blackOverlay?.setBackgroundColor(Color.BLACK)
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            (window.decorView as ViewGroup).addView(blackOverlay, params)
        }
    }

    // 移除全屏黑色遮罩
    private fun removeBlackOverlay() {
        if (blackOverlay != null) {
            (window.decorView as ViewGroup).removeView(blackOverlay)
            blackOverlay = null
        }
    }
}