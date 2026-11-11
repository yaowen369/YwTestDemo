package ywdemo.example.yaoxiaowen

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.*
import android.widget.*
import kotlin.math.min

object StationColors {
    val background = Color.parseColor("#0B192B")
    val surface = Color.parseColor("#15283E")
    val ink = Color.parseColor("#EDF5FB")
    val muted = Color.parseColor("#9EB2C7")
    val mint = Color.parseColor("#52E0C4")
    val line = Color.parseColor("#2B4056")
    val error = Color.parseColor("#FFB0A0")
    val secondary = Color.parseColor("#1C344B")
}

fun Context.dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
fun Context.rounded(color: Int, radius: Int = 14, border: Int? = null) = GradientDrawable().apply {
    setColor(color); cornerRadius = dp(radius).toFloat()
    border?.let { setStroke(dp(1), it) }
}
fun View.touchSurface(color: Int, radius: Int = 10, border: Int? = null) {
    background = RippleDrawable(ColorStateList.valueOf(0x3352E0C4), context.rounded(color, radius, border), context.rounded(Color.WHITE, radius))
    isClickable = true; isFocusable = true
}

/** 原生线条图形与确认稿的简洁图标保持一致，不依赖字体符号。 */
class StationIcon(context: Context, private val kind: String, private val tint: Int = StationColors.muted) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; style = Paint.Style.STROKE; strokeWidth = 1.7f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save(); canvas.scale(width / 24f, height / 24f)
        fun path(vararg points: Float) { val p = Path(); p.moveTo(points[0], points[1]); var i = 2; while (i < points.size) { p.lineTo(points[i], points[i + 1]); i += 2 }; canvas.drawPath(p, paint) }
        when (kind) {
            "back" -> path(15f, 5f, 8f, 12f, 15f, 19f)
            "next" -> path(9f, 5f, 16f, 12f, 9f, 19f)
            "check" -> path(5f, 12f, 10f, 17f, 20f, 7f)
            "shield" -> { path(12f, 2f, 20f, 6f, 19f, 15f, 12f, 22f, 5f, 15f, 4f, 6f, 12f, 2f); path(8f, 12f, 11f, 15f, 16f, 9f) }
            "wrench" -> { path(14f, 3f, 12f, 8f, 16f, 12f, 21f, 10f, 20f, 16f, 15f, 17f, 7f, 23f, 3f, 19f, 10f, 12f, 9f, 7f, 14f, 3f) }
            "location" -> { canvas.drawPath(Path().apply { moveTo(12f, 22f); cubicTo(3f, 13f, 2f, 9f, 6f, 5f); cubicTo(9f, 1f, 15f, 1f, 18f, 5f); cubicTo(22f, 9f, 21f, 13f, 12f, 22f) }, paint); canvas.drawCircle(12f, 9f, 3f, paint) }
            "scan" -> { path(3f, 8f, 3f, 4f, 7f, 4f); path(17f, 4f, 21f, 4f, 21f, 8f); path(21f, 16f, 21f, 20f, 17f, 20f); path(7f, 20f, 3f, 20f, 3f, 16f); path(7f, 12f, 17f, 12f) }
            "empty" -> { canvas.drawRoundRect(5f, 4f, 19f, 22f, 2f, 2f, paint); canvas.drawRoundRect(9f, 2f, 15f, 6f, 1f, 1f, paint); path(9f, 11f, 15f, 11f); path(9f, 16f, 15f, 16f) }
            else -> canvas.drawCircle(12f, 12f, 9f, paint)
        }
        canvas.restore()
    }
}

/** 支持双指缩放、双击还原与拖动；始终先完整展示照片。 */
class ZoomPhotoView(context: Context) : View(context) {
    private var bitmap: Bitmap? = null
    private var zoom = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val scale = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean { zoom = (zoom * detector.scaleFactor).coerceIn(1f, 5f); clamp(); invalidate(); return true }
    })
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: android.view.MotionEvent) = true
        override fun onScroll(first: android.view.MotionEvent, last: android.view.MotionEvent, dx: Float, dy: Float): Boolean { if (zoom > 1) { offsetX -= dx; offsetY -= dy; clamp(); invalidate() }; return true }
        override fun onDoubleTap(event: android.view.MotionEvent): Boolean { toggleZoom(); return true }
        override fun onSingleTapUp(event: android.view.MotionEvent): Boolean { performClick(); return true }
    })
    fun setPhoto(value: Bitmap?) { bitmap = value; zoom = 1f; offsetX = 0f; offsetY = 0f; invalidate() }
    fun toggleZoom() { zoom = if (zoom == 1f) 2f else 1f; offsetX = 0f; offsetY = 0f; invalidate() }
    private fun clamp() {
        val image = bitmap ?: return
        val ratio = min(width.toFloat() / image.width, height.toFloat() / image.height) * zoom
        val x = ((image.width * ratio - width) / 2).coerceAtLeast(0f)
        val y = ((image.height * ratio - height) / 2).coerceAtLeast(0f)
        offsetX = offsetX.coerceIn(-x, x); offsetY = offsetY.coerceIn(-y, y)
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { clamp() }
    override fun onDraw(canvas: Canvas) {
        val image = bitmap ?: return
        val ratio = min(width.toFloat() / image.width, height.toFloat() / image.height) * zoom
        val w = image.width * ratio; val h = image.height * ratio
        canvas.drawBitmap(image, null, RectF((width - w) / 2 + offsetX, (height - h) / 2 + offsetY, (width + w) / 2 + offsetX, (height + h) / 2 + offsetY), paint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean { scale.onTouchEvent(event); gestures.onTouchEvent(event); return true }
    override fun performClick(): Boolean { super.performClick(); return true }
}
