package com.example.morsecodeapp

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.audiofx.LoudnessEnhancer
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*

class OutputActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private lateinit var vibrator: Vibrator
    private var toneGenerator: ToneGenerator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    
    private var isPlaying = false
    private var playJob: Job? = null
    private var selectedBeep = "classic"
    private var soundEnabled = true
    private var vibrationEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_output)

        val morseCode = intent.getStringExtra("MORSE_CODE") ?: ""
        
        val tvMorseDisplay = findViewById<TextView>(R.id.textViewMorseDisplay)
        val tvStatus = findViewById<TextView>(R.id.textViewStatus)
        val btnPlay = findViewById<Button>(R.id.buttonPlay)
        val btnStop = findViewById<Button>(R.id.buttonStop)
        val btnBack = findViewById<Button>(R.id.buttonBack)

        tvMorseDisplay.text = morseCode

        // Load preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        selectedBeep = prefs.getString("beep_sound", "classic") ?: "classic"
        soundEnabled = prefs.getBoolean("enable_sound", true)
        vibrationEnabled = prefs.getBoolean("enable_vibration", true)

        // Initialize services
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    cameraId = id
                    break
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Setup Audio
        if (soundEnabled) {
            if (selectedBeep == "cat") {
                try {
                    mediaPlayer = MediaPlayer.create(this, R.raw.cat)
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                    mediaPlayer?.isLooping = true
                    
                    // Hardware Volume Boost (LoudnessEnhancer)
                    // 2000mB = 20dB boost. This makes the meow ~4x louder than original.
                    loudnessEnhancer = LoudnessEnhancer(mediaPlayer!!.audioSessionId).apply {
                        setTargetGain(2000)
                        enabled = true
                    }
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                try {
                    // Lower ToneGenerator volume (20/100) so the Cat Meow is dominant
                    toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 20)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        btnPlay.setOnClickListener { startPlayback(morseCode, tvStatus, btnPlay, btnStop) }
        btnStop.setOnClickListener { stopPlayback(tvStatus, btnPlay, btnStop) }
        btnBack.setOnClickListener {
            stopPlayback(tvStatus, btnPlay, btnStop)
            finish()
        }
    }

    private fun startPlayback(morseCode: String, tvStatus: TextView, btnPlay: Button, btnStop: Button) {
        if (morseCode.isBlank()) return
        isPlaying = true
        btnPlay.isEnabled = false
        btnStop.isEnabled = true
        tvStatus.text = "Playing..."

        playJob = lifecycleScope.launch(Dispatchers.Default) {
            for (char in morseCode) {
                if (!isPlaying) break
                when (char) {
                    '.' -> { playSignal(200); delay(200) }
                    '-' -> { playSignal(600); delay(200) }
                    ' ' -> delay(400)
                    '/' -> delay(800)
                }
            }
            withContext(Dispatchers.Main) { 
                stopPlayback(tvStatus, btnPlay, btnStop)
                tvStatus.text = "Finished"
            }
        }
    }

    private suspend fun playSignal(duration: Long) {
        try { cameraId?.let { cameraManager.setTorchMode(it, true) } } catch (e: Exception) {}
        if (vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(duration)
            }
        }
        if (soundEnabled) startSound()
        delay(duration)
        if (soundEnabled) stopSound()
        try { cameraId?.let { cameraManager.setTorchMode(it, false) } } catch (e: Exception) {}
    }

    private fun startSound() {
        when (selectedBeep) {
            "classic" -> toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1)
            "cat" -> mediaPlayer?.start()
            "robot" -> toneGenerator?.startTone(ToneGenerator.TONE_SUP_PIP)
        }
    }

    private fun stopSound() {
        when (selectedBeep) {
            "classic", "robot" -> toneGenerator?.stopTone()
            "cat" -> { mediaPlayer?.pause(); mediaPlayer?.seekTo(0) }
        }
    }

    private fun stopPlayback(tvStatus: TextView, btnPlay: Button, btnStop: Button) {
        isPlaying = false
        playJob?.cancel()
        btnPlay.isEnabled = true
        btnStop.isEnabled = false
        tvStatus.text = "Stopped"
        try { cameraId?.let { cameraManager.setTorchMode(it, false) } } catch (e: Exception) {}
        vibrator.cancel()
        if (soundEnabled) stopSound()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
        playJob?.cancel()
        toneGenerator?.release()
        loudnessEnhancer?.release()
        mediaPlayer?.release()
        try { cameraId?.let { cameraManager.setTorchMode(it, false) } } catch (e: Exception) {}
    }
}