package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class SplashActivity : Activity(), View.OnClickListener {

    val jumpMainBtn:Button by lazy{
        findViewById<Button>(R.id.jumpMainActy)
    }

    val jumpSpBtn:Button by lazy{
        findViewById<Button>(R.id.jumpSpActy)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        jumpMainBtn.setOnClickListener(this)
        jumpSpBtn.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.jumpMainActy -> {
                val intent1 = Intent(this, MainActivity::class.java)
                startActivity(intent1)
            }

            R.id.jumpSpActy -> {
                val intent2 = Intent(this, SpDataStoreTestActivity::class.java)
                startActivity(intent2)
            }
        }
    }
}