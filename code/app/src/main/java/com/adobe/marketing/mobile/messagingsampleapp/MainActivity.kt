package com.adobe.marketing.mobile.messagingsampleapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import com.adobe.marketing.mobile.MobileCore
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_collectMsgInfo.setOnClickListener {
            MobileCore.collectMessageInfo(collectMessageInfoMap)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    private val collectMessageInfoMap: Map<String, Any>
        private get() {
            val map: MutableMap<String, Any> = HashMap()
            map["eventType"] = "track.applicationOpened"
            map["id"] = "31369"
            map["applicationOpened"] = true
            return map
        }
}