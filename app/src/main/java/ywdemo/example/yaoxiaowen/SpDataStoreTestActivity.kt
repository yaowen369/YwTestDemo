package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import com.tencent.mmkv.MMKV
import ywdemo.example.yaoxiaowen.until.LogUtil


class SpDataStoreTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sp_datastore_test)
        mmkvTest()
    }

    fun spTest() {
        val sp = getSharedPreferences("file1", MODE_MULTI_PROCESS)
        val edit = sp.edit()
        edit.putString("key1", "value1")
        // apply 异步 提交方式
        edit.apply()
        // commit 同步 提交方式, boolean类型 返回值,告知同步是否完成
        val booleanResult = edit.commit()

        // 获取 对应的值 值
        val value2 = sp.getString("key2", "")
    }

    fun mmkvTest(){
        val kv = MMKV.defaultMMKV()
        val startTime = System.currentTimeMillis();
        kv.putString("key1", StringUtil.getLongString())

        LogUtil.i( "保存长字符串耗时:${System.currentTimeMillis() - startTime}")
    }
}