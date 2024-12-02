package ywdemo.example.yaoxiaowen.floatview

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import ywdemo.example.yaoxiaowen.R
import ywdemo.example.yaoxiaowen.floatview.BaseFloatView.OnFloatClickListener
import ywdemo.example.yaoxiaowen.until.LogUtil


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

    fun createFloatView(): AvatarFloatView {
        // 创建悬浮球视图
        val floatingBallView = AvatarFloatView(requireContext())

        floatingBallView.setOnFloatClickListener(object : OnFloatClickListener {
            override fun onClick(view: View) {
                LogUtil.i("悬浮球被点击")
            }
        })

        return floatingBallView
    }
}