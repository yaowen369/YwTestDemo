package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * 图片分辨率测试Activity
 * 用于验证不同分辨率图片在手机上的显示效果
 */
class ImageResolutionTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_resolution_test)

        // 获取设备屏幕密度信息
        val density = resources.displayMetrics.density
        val densityDpi = resources.displayMetrics.densityDpi

        // 设置1倍图信息
        val tv1x = findViewById<TextView>(R.id.tv_1x)
        tv1x.text = "1倍图，实际尺寸21*21，分辨率72*72\n" +
                "设备密度: $densityDpi dpi (${density}x)\n" +
                "显示大小: 42dp × 42dp"

        // 设置2倍图信息
        val tv2x = findViewById<TextView>(R.id.tv_2x)
        tv2x.text = "2倍图，实际尺寸42*42，分辨率144*144\n" +
                "设备密度: $densityDpi dpi (${density}x)\n" +
                "显示大小: 42dp × 42dp"

        // 设置3倍图信息
        val tv3x = findViewById<TextView>(R.id.tv_3x)
        tv3x.text = "3倍图，实际尺寸63*63，分辨率216*216\n" +
                "设备密度: $densityDpi dpi (${density}x)\n" +
                "显示大小: 42dp × 42dp"
    }
}