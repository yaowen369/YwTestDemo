package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import ywdemo.example.yaoxiaowen.floatview.TestFloatViewActivity

class AiScreenActivity : Activity() {

    val jumpBtn: Button by lazy {
        findViewById<Button>(R.id.jumpBtn)
    }

    val inputEt: EditText by lazy {
        findViewById<EditText>(R.id.inputEt)
    }


    val displayTv: TextView by lazy {
        findViewById<TextView>(R.id.displayTv);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_screen)

        jumpBtn.setOnClickListener {
            val intent = Intent(this, TestFloatViewActivity::class.java)
            startActivity(intent)
        }

        inputEt.setOnEditorActionListener { v, actionId, event ->
            displayTv.text = inputEt.text
            true
        }
    }
}