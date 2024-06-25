package ywdemo.example.yaoxiaowen.until

import android.util.Log

class LogUtil  {

    companion object {

        private const val  prefix = "LogPrefix"

        @JvmStatic
        fun v(tag: String?, msg: String) {
            Log.v("$prefix $tag", msg);
        }

        @JvmStatic
        fun d(tag: String?, msg: String) {
            Log.d("$prefix $tag", msg);
        }

        @JvmStatic
        fun i(tag: String?, msg: String) {
            Log.i("$prefix $tag", msg);
        }

        @JvmStatic
        fun w(tag: String?, msg: String) {
            Log.w("$prefix $tag", msg);
        }

        @JvmStatic
        fun e(tag: String?, msg: String) {
            Log.e("$prefix $tag", msg);
        }



        @JvmStatic
        fun v( msg: String) {
            Log.v("$prefix", msg);
        }

        @JvmStatic
        fun d( msg: String) {
            Log.d("$prefix ", msg);
        }

        @JvmStatic
        fun i( msg: String) {
            Log.i("$prefix ", msg);
        }

        @JvmStatic
        fun w( msg: String) {
            Log.w("$prefix ", msg);
        }

        @JvmStatic
        fun e( msg: String) {
            Log.e("$prefix", msg);
        }
    }


}