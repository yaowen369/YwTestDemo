package ywdemo.example.yaoxiaowen

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ywdemo.example.yaoxiaowen.until.LogUtil

class SplashActivity : Activity(), View.OnClickListener {

    private val LOCATION_PERMISSION_REQUEST_CODE = 100


    val jumpMainBtn: Button by lazy {
        findViewById<Button>(R.id.jumpMainActy)
    }

    val jumpSpBtn: Button by lazy {
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
        findViewById<Button>(R.id.testPermissionBtn).setOnClickListener(this)
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

            R.id.testPermissionBtn -> {
                testPermission()
            }
        }
    }

    fun testPermission() {
        checkLocationPermission();
    }


    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予，可以执行相应操作
                LogUtil.i(TAG, "权限已授予")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) -> {
                // 显示权限请求理由
                showDialogA()
            }

            else -> {
                // 直接请求权限
                requestLocationPermission()
            }
        }
    }

    private fun showDialogA() {
        AlertDialog.Builder(this)
            .setTitle("需要定位权限")
            .setMessage("为了提供更好的服务，我们需要您的定位权限。")
            .setPositiveButton("确定") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("取消") { _, _ ->
                LogUtil.i(TAG, "弹窗A,用户点击 取消")
            }
            .create()
            .show()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            when {
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // 权限被授予，可以执行相应操作
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) -> {
                    // 用户拒绝了权限请求，再次显示DialogA
                    showDialogA()
                }

                else -> {
                    // 用户拒绝并选择不再询问，显示DialogB
                    showDialogB()
                }
            }
        }
    }

    private fun showDialogB() {
        AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("您已拒绝了定位权限，请在设置中手动开启。")
            .setPositiveButton("设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
    }

    companion object {
        val TAG = "SplashActivity"
    }
}