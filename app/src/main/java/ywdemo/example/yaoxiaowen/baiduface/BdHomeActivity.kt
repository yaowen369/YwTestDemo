package ywdemo.example.yaoxiaowen.baiduface

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import ywdemo.example.yaoxiaowen.R
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.api.FaceApi
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.DBLoadListener
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User

class BdHomeActivity : Activity() {


    private var isDBLoad = false
    private val mainTv:TextView by lazy { findViewById<TextView>(R.id.main_tv) }
    private val progressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }
    private val progressText: TextView by lazy { findViewById<TextView>(R.id.progress_text) }
    private val progressGroup: View by lazy { findViewById<View>(R.id.progress_group) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bd_home)

        initDb()
    }


    private fun initDb() {
        FaceApi.getInstance().init(object : DBLoadListener {
            override fun onStart(successCount: Int) {
                if (successCount < 5000 && successCount != 0) {
                    runOnUiThread { loadProgress(10f) }
                }
            }

            override fun onLoad(finishCount: Int, successCount: Int, progress: Float) {
                if (successCount > 5000 || successCount == 0) {
                    runOnUiThread {
                        progressBar.setProgress((progress * 100).toInt())
                        progressText.setText(((progress * 100).toInt()).toString() + "%")
                    }
                }
            }

            override fun onComplete(users: List<User?>?, successCount: Int) {
//                        FileUtils.saveDBList(HomeActivity.this, users);
                runOnUiThread {
                    FaceApi.getInstance().setUsers(users)
                    if (successCount > 5000 || successCount == 0) {
                        progressGroup.setVisibility(View.GONE)
                        mainTv.setText("人脸库加载成功")
                        mainTv.setTextColor(Color.BLUE)
                        isDBLoad = true
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onFail(finishCount: Int, successCount: Int, users: List<User?>?) {
                runOnUiThread {
                    FaceApi.getInstance().setUsers(users)
                    progressGroup.setVisibility(View.GONE)

                    Toast.makeText(this@BdHomeActivity, "人脸库加载失败,共" + successCount + "条数据, 已加载" + finishCount + "条数据", Toast.LENGTH_SHORT).show()

                    mainTv.text = "人脸库加载失败,共" + successCount + "条数据, 已加载" + finishCount + "条数据"
                    mainTv.setTextColor(Color.RED)


                    isDBLoad = true
                }
            }
        }, this)
    }

    private fun loadProgress(i: Float) {
        Handler().postDelayed({
            progressBar.setProgress(((i / 5000f) * 100).toInt())
            progressText.setText((((i / 5000f) * 100).toInt()).toString() + "%")
            if (i < 5000) {
                loadProgress(i + 100)
            } else {
                progressGroup.setVisibility(View.GONE)
                isDBLoad = true
            }
        }, 10)
    }


    fun jumpToFaceDepthActivity(view: View) {
        if (isDBLoad) {
            startActivity(Intent(this, BdFaceDepthGateActivity::class.java))
        } else {
            Toast.makeText(this, "请等待人脸库加载完成", Toast.LENGTH_SHORT).show()
        }
    }
}