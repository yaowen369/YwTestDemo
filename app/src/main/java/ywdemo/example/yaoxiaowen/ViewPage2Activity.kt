package ywdemo.example.yaoxiaowen

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import ywdemo.example.yaoxiaowen.viewpager.ViewPage2Adapter
import ywdemo.example.yaoxiaowen.viewpager.ViewPageData
import java.util.Arrays
import java.util.LinkedList

class ViewPage2Activity : Activity() {

    val mViewPage2: ViewPager2 by lazy { findViewById(R.id.myViewPage2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_page2)

        val mDatas: ArrayList<ViewPageData> = ArrayList<ViewPageData>()
        mDatas.add(ViewPageData("1"))
        mDatas.add(ViewPageData("2"))
        mDatas.add(ViewPageData("3"))
        mDatas.add(ViewPageData("4"))


        val mAdapter = ViewPage2Adapter(mDatas)
        mViewPage2.adapter = mAdapter
    }
}