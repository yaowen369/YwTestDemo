package ywdemo.example.yaoxiaowen.floatview

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import ywdemo.example.yaoxiaowen.R


class Fragment_2 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_2, container, false)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.LEFT xor Gravity.TOP

        val floatView = createFloatView()

        val container = root.findViewById<FrameLayout>(R.id.fg2Contaner)
        container.addView(floatView, layoutParams)

        // Inflate the layout for this fragment
        return root
    }

    fun createFloatView(): FloatingBallView {

        // 创建悬浮球视图
        val floatingBallView = FloatingBallView(activity)
        floatingBallView.setOnClickListener { // 处理点击事件
            println("悬浮球被点击了")
        }


        // 设置悬浮球的布局参数
//        val layoutParams = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.WRAP_CONTENT,
//            FrameLayout.LayoutParams.WRAP_CONTENT
//        )
//        layoutParams.gravity = Gravity.LEFT or Gravity.TOP

        // 获取根布局（假设是FrameLayout，根据实际情况修改）


        floatingBallView.setOnTouchListener { v, event ->
            val screenWidth = resources.displayMetrics.widthPixels
            val params = floatingBallView.layoutParams as FrameLayout.LayoutParams
            if (floatingBallView.x < screenWidth / 2) {
                // 靠近左边
                params.gravity = Gravity.LEFT or Gravity.TOP
            } else {
                // 靠近右边
                params.gravity = Gravity.RIGHT or Gravity.TOP
            }
            floatingBallView.layoutParams = params
            false
        }

        return floatingBallView
    }
}