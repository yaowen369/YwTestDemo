package ywdemo.example.yaoxiaowen.floatview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ywdemo.example.yaoxiaowen.R

class TestFloatViewActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_float_view)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // 创建Fragment列表
        val fragmentList = ArrayList<Fragment>()
        fragmentList.add(Fragment_1())
        fragmentList.add(Fragment_2())
        fragmentList.add(Fragment_3())

        // 设置ViewPager2的Adapter
        viewPager.adapter = MyViewPagerAdapter(fragmentList)

        // 将TabLayout和ViewPager2关联起来
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "按钮1"
                1 -> tab.text = "按钮2"
                2 -> tab.text = "按钮3"
            }
        }.attach()
    }

    // 自定义ViewPager2的Adapter
    inner class MyViewPagerAdapter(private val fragmentList: ArrayList<Fragment>) :
        androidx.viewpager2.adapter.FragmentStateAdapter(this) {

        override fun getItemCount(): Int {
            return fragmentList.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentList[position]
        }
    }
}