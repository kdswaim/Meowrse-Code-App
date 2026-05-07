package com.example.morsecodeapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private val morseMap = mapOf(
        'A' to ".-",   'B' to "-...", 'C' to "-.-.", 'D' to "-..",
        'E' to ".",    'F' to "..-.", 'G' to "--.",  'H' to "....",
        'I' to "..",   'J' to ".---", 'K' to "-.-",  'L' to ".-..",
        'M' to "--",   'N' to "-.",   'O' to "---",  'P' to ".--.",
        'Q' to "--.-", 'R' to ".-.",  'S' to "...",  'T' to "-",
        'U' to "..-",  'V' to "...-", 'W' to ".--",  'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme preference before super.onCreate
        applyThemeFromPreferences()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText  = findViewById<EditText>(R.id.editTextInput)
        val outputText = findViewById<TextView>(R.id.textViewMorseOutput)
        val btnConvert = findViewById<Button>(R.id.buttonConvert)
        val btnSendToOutput = findViewById<Button>(R.id.buttonSendToOutput)
        val btnSettings = findViewById<Button>(R.id.buttonSettings)
        val btnHelp = findViewById<Button>(R.id.buttonHelp)

        btnConvert.setOnClickListener {
            val userInput = inputText.text.toString().uppercase()
            if (userInput.isNotBlank()) {
                outputText.text = translateToMorse(userInput)
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendToOutput.setOnClickListener {
            val morseCode = outputText.text.toString()
            if (morseCode.isNotBlank() && morseCode != "Your morse code will appear here") {
                val intent = Intent(this, OutputActivity::class.java)
                intent.putExtra("MORSE_CODE", morseCode)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Convert some text to Morse first", Toast.LENGTH_SHORT).show()
            }
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnHelp.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun applyThemeFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = prefs.getBoolean("dark_mode", false)
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun translateToMorse(text: String): String {
        return text.map { char ->
            when {
                char == ' ' -> "/"
                morseMap.containsKey(char) -> morseMap[char]!!
                else -> "?"
            }
        }.joinToString(" ")
    }

    override fun onResume() {
        super.onResume()
        // Re-apply theme in case it was changed in SettingsActivity
        applyThemeFromPreferences()
    }
}