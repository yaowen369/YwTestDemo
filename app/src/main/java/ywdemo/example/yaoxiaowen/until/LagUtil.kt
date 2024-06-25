package ywdemo.example.yaoxiaowen.until

import android.util.Log
import kotlin.random.Random

class LagUtil {

    companion object {
        fun calc(n:Int = 1) {
            val startTime = System.currentTimeMillis()

            for (i in 0 until n * 100000) {
                val number1 = Random.nextDouble(0.1, 100000.0)
                val number2 = Random.nextDouble(0.1, 100000.0)

                // 乘法运算
                val result1 = number1 * number2

                // 除法运算
                val result2 = number1 / number2

                val result3  = result1 * result2 / result2 / result1 * number1 + number2 / result2

                if (result3 > Integer.MAX_VALUE / 2  && result1 <Integer.MIN_VALUE / 2) {
                    println("计算结果")
                }
            }

            val endTime = System.currentTimeMillis()
            val executionTime = endTime - startTime

            LogUtil.i("", "计算$n 次, 耗时:${executionTime}, ${ThreadUtil.getThreadInfo()}")
        }
    }
}