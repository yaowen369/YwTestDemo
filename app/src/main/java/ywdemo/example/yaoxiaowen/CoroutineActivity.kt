package ywdemo.example.yaoxiaowen

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ywdemo.example.yaoxiaowen.databinding.ActivityCoroutineBinding

class CoroutineActivity : Activity() {

    private lateinit var binding:ActivityCoroutineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_coroutine)
        binding  = ActivityCoroutineBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}