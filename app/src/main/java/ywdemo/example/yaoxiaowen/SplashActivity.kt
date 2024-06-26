package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import ywdemo.example.yaoxiaowen.databinding.ActivitySplashBinding

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
        findViewById<Button>(R.id.jumpThreadLocalActy).setOnClickListener(this)
        findViewById<Button>(R.id.jumpUiLagActy).setOnClickListener(this)
        findViewById<Button>(R.id.jumpViewPage2Acty).setOnClickListener(this)
        findViewById<Button>(R.id.jumpCoroutineActy).setOnClickListener(this)
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
            R.id.jumpThreadLocalActy -> {
                val intent3 = Intent(this, ThreadLockActivity::class.java)
                startActivity(intent3)
            }
            R.id.jumpUiLagActy -> {
                val intent4 = Intent(this, UiLagActivity::class.java)
                startActivity(intent4)
            }
            R.id.jumpCoroutineActy -> {
                val intent5 = Intent(this, CoroutineActivity::class.java)
                startActivity(intent5)
            }
        }
    }
}