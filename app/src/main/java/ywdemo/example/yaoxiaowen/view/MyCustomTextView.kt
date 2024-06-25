package ywdemo.example.yaoxiaowen.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.Button
import android.widget.TextView
import ywdemo.example.yaoxiaowen.until.LagUtil
import ywdemo.example.yaoxiaowen.until.LogUtil

@SuppressLint("AppCompatCustomView")
class MyCustomTextView : TextView {

    constructor(context: Context?) : super(context) {
        LagUtil.calc(2)
        LogUtil.i(TAG, "一个参数的构造方法")
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        LagUtil.calc(2)
        LogUtil.i(TAG, "两个参数的构造方法")
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        LagUtil.calc(2)
        LogUtil.i(TAG, "三个参数的构造方法")
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        LagUtil.calc(2)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        LagUtil.calc(2)
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        LagUtil.calc(2)
        super.onDraw(canvas)
    }

    companion object {
        const val TAG = "MyCustomTextView"
    }
}