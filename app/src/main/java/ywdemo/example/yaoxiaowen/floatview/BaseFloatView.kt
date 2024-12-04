package ywdemo.example.yaoxiaowen.floatview

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * GitHub : https://github.com/yechaoa
 * CSDN : http://blog.csdn.net/yechaoa
 *
 * Created by yechao on 2022/7/26.
 * Describe : 随意拖拽+自动吸边
 */
abstract class BaseFloatView : FrameLayout {

    private var mViewWidth = 0
    private var mViewHeight = 0
    private var mToolBarHeight = dp2px(56F) // toolbar默认高度
    private var mDragDistance = 0.5 // 默认吸边需要的拖拽距离为屏幕的一半

    private var mFirstY = 0
    private var mFirstX = 0

    open var TAG: String = "wenyao, BaseFloatView"

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    ) {
        initView()
    }

    private fun initView() {
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.topMargin = mToolBarHeight
        layoutParams = lp

        val childView = getChildView()
        addView(childView)
//        setOnTouchListener(this)

        post {
            // 获取一下view宽高，方便后面计算，省的bottom-top麻烦
            mViewWidth = this.width
            mViewHeight = this.height
            Log.i(TAG, "initView(), ${getLocation()}")
        }
    }

    /**
     * 获取子view
     */
    protected abstract fun getChildView(): View

    /**
     * 点击事件
     */
    protected var mOnFloatClickListener: OnFloatClickListener? = null

    interface OnFloatClickListener {
        fun onClick(view: View)
    }

    fun setOnFloatClickListener(listener: OnFloatClickListener) {
        mOnFloatClickListener = listener
    }

    private var mRun = Runnable {
        Log.i(TAG, "当前位置信息: ${getLocation()}")
    }


    private var lastX = 0f
    private var lastY = 0f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        Log.i(
            TAG,
            "onInterceptTouchEvent(), 2, 事件类型:${MotionEvent.actionToString(event.action)}, event坐标:(${event.x}, ${event.y})"
        )
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i(TAG, "onInterceptTouchEvent(), DOWM事件, event坐标:(${x}, ${y})")
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                Log.i(TAG, "onInterceptTouchEvent(), MOVE事件, event坐标:(${x}, ${y})")
                return true
            }


            MotionEvent.ACTION_UP -> {
                Log.i(TAG, "onInterceptTouchEvent(), UP事件, event坐标:(${x}, ${y})")


            }
        }

        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.i(
            TAG,
            ">>> onTouchEvent(), 事件类型, ${MotionEvent.actionToString(event.action)}, event坐标:(${event.x}, ${event.y})"
        )

        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y

                mFirstY = event.rawY.roundToInt()
                mFirstX = event.rawX.roundToInt()
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - lastX).toInt()
                val dy = (event.y - lastY).toInt()
                translationX += dx
                translationY += dy
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                Log.i(
                    TAG,
                    ">>> onTouchEvent(), ${MotionEvent.actionToString(event.action)}事件, 此时要实现 吸边效果 event原始坐标:(${event.rawX}, ${event.rawY}),屏幕宽度:${getScreenWidth()}, viewWidth=${mViewWidth}"
                )

                adsorbLeftAndRight(event)

            }
        }
        return false
    }


    /**
     * 左右吸边
     */
    private fun adsorbLeftAndRight(event: MotionEvent) {

        Log.i(TAG, "**** adsorbLeftAndRight(), 左右吸边, event坐标:(${event.x}, ${event.y})")

        /**
         * 1.判断滑动距离是否超过半屏
         * 2.判断起始位置在左/右半屏
         * 3.左半屏：
         *      3.1.滑动距离<半屏=吸左
         *      3.2.滑动距离>半屏=吸右
         * 4.右半屏：
         *      4.1.滑动距离<半屏=吸右
         *      4.2.滑动距离>半屏=吸左
         */
        if (isOriginalFromLeft()) {
            // 左半屏
            val centerX = mViewWidth / 2 + abs(event.rawX - mFirstX)
            if (centerX < getAdsorbWidth()) {
                //滑动距离<半屏=吸左
                val leftX = 0f
                Log.i(TAG, "**** adsorbLeftAndRight(), 左半屏  吸左, left=${leftX}")
//                animate().setInterpolator(DecelerateInterpolator()).setDuration(300).x(leftX)
//                    .start()
                this.x = leftX
            } else {
                //滑动距离<半屏=吸右
                val rightX = getScreenWidth() - mViewWidth

                Log.i(
                    TAG, "**** adsorbLeftAndRight(), 左半屏  吸右, rightX=${rightX}, " +
                            "ScreenWidth()=${getScreenWidth()}, mViewWidth=${mViewWidth}"
                )

//                animate().setInterpolator(DecelerateInterpolator()).setDuration(300)
//                    .x(rightX.toFloat()).start()
                this.x = rightX.toFloat()

            }
        } else {
            // 右半屏
            val centerX = mViewWidth / 2 + abs(event.rawX - mFirstX)
            if (centerX < getAdsorbWidth()) {
                //滑动距离<半屏=吸右
                val rightX = getScreenWidth() - mViewWidth

                Log.i(
                    TAG, "**** adsorbLeftAndRight(), 右半屏  吸右, rightX=${rightX}, " +
                            "ScreenWidth()=${getScreenWidth()}, mViewWidth=${mViewWidth}"
                )

//                animate().setInterpolator(DecelerateInterpolator()).setDuration(300)
//                    .x(rightX.toFloat()).start()

                this.x = rightX.toFloat()
            } else {
                //滑动距离<半屏=吸左
                val leftX = 0f
                Log.i(TAG, "**** adsorbLeftAndRight(), 右半屏  吸左, left=${leftX}")

//                animate().setInterpolator(DecelerateInterpolator()).setDuration(300).x(leftX)
//                    .start()

                this.x = leftX
            }
        }

        postDelayed(mRun, 1 * 1000)
        postDelayed(mRun, 2 * 1000)
        postDelayed(mRun, 3 * 1000)
        postDelayed(mRun, 4 * 1000)
        postDelayed(mRun, 5 * 1000)
        postDelayed(mRun, 6 * 1000)
    }


    private fun getLocation(): String {

        val location = IntArray(2)
        getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]

        val sb = StringBuilder("当前坐标:($x, $y), 可见性:${visibility2String()}")
        return sb.toString()
    }

    // 初始位置是否在左边
    private fun isOriginalFromLeft(): Boolean {
        return mFirstX < getScreenWidth() / 2
    }

    /**
     * 获取上下吸边时需要拖拽的距离
     */
    private fun getAdsorbHeight(): Double {
        return getScreenHeight() * mDragDistance
    }

    /**
     * 获取左右吸边时需要拖拽的距离
     */
    private fun getAdsorbWidth(): Double {
        return getScreenWidth() * mDragDistance
    }

    /**
     * dp2px
     */
    private fun dp2px(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    /**
     * 获取屏幕高度
     */
    private fun getScreenHeight(): Int {
        val dm = DisplayMetrics()
        (context as? Activity)?.windowManager?.defaultDisplay?.getMetrics(dm)
        return dm.heightPixels
    }

    /**
     * 获取屏幕宽度
     */
    private fun getScreenWidth(): Int {
        val dm = DisplayMetrics()
        (context as? Activity)?.windowManager?.defaultDisplay?.getMetrics(dm)
        return dm.widthPixels
    }

    /**
     * 回收
     */
    fun release() {
        // do something
    }


    private fun visibility2String(): String {
        when (VISIBLE) {
            View.VISIBLE -> return "VISIBLE"
            View.GONE -> return "GONE"
            View.INVISIBLE -> return "INVISIBLE"

        }
        return "-"
    }
}