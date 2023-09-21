package com.adobe.marketing.mobile.messagingsample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class SecondLevelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_level)
        findViewById<Button>(R.id.btn_third_activity)
            .setOnClickListener {
                val intent = Intent(applicationContext, ThirdLevelActivity::class.java)
                startActivity(intent)
            }
    }


}