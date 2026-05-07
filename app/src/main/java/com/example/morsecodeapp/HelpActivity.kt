package com.example.morsecodeapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Just a back button to return to the main screen
        val btnBack = findViewById<Button>(R.id.buttonHelpBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}