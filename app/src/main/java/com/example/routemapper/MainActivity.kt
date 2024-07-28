package com.example.routemapper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.routemapper.map.MapActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        // Finish the MainActivity
        finish()
    }
}