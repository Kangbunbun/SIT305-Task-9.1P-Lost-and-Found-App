package com.example.lostfoundapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.lostfoundapp.R

class MainActivity : ComponentActivity() {

    private lateinit var btnCreateAdvert: Button
    private lateinit var btnShowItems: Button
    private lateinit var btnShowOnMap: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCreateAdvert = findViewById(R.id.btnCreateAdvert)
        btnShowItems = findViewById(R.id.btnShowItems)
        btnShowOnMap = findViewById(R.id.btnShowOnMap)

        btnCreateAdvert.setOnClickListener {
            val intent = Intent(this, CreateAdvertActivity::class.java)
            startActivity(intent)
        }

        btnShowItems.setOnClickListener {
            val intent = Intent(this, ItemListActivity::class.java)
            startActivity(intent)
        }

        btnShowOnMap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}