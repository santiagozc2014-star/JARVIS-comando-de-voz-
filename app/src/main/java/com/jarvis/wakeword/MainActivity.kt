package com.jarvis.wakeword

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnTestChatGpt: Button
    private lateinit var btnOpenJarvisChat: Button
    private lateinit var thresholdSeek: SeekBar
    private lateinit var thresholdLabel: TextView
    private lateinit var lastScoreText: TextView

    private var isServiceRunning = false
    private var engineState = WakeWordService.STATE_STOPPED
    private var currentThreshold = 0.50f

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startService()
        } else {
            Toast.makeText(this, "Se necesita permiso de micrófono", Toast.LENGTH_LONG).show()
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != WakeWordService.ACTION_STATUS) return
            val listening = intent.getBooleanExtra(WakeWordService.EXTRA_LISTENING, false)
            isServiceRunning = listening
            engineState = intent.getStringExtra(WakeWordService.EXTRA_ENGINE_STATE)
                ?: if (listening) WakeWordDetector.STATE_STARTING else WakeWordService.STATE_STOPPED
            updateUI()

            val score = intent.getFloatExtra(WakeWordService.EXTRA_LAST_SCORE, -1f)
            if (score >= 0f) {
                lastScoreText.text = "Última detección: score %.2f".format(score)
            }
            val error = intent.getStringExtra(WakeWordService.EXTRA_ERROR)
            if (!error.isNullOrBlank()) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
            }
            val autoOpened = intent.getBooleanExtra(WakeWordService.EXTRA_AUTO_OPENED, false)
            if (autoOpened) {
                Toast.makeText(this@MainActivity, "ChatGPT abierto automáticamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnToggle = findViewById(R.id.btnToggle)
        btnTestChatGpt = findViewById(R.id.btnTestChatGpt)
        btnOpenJarvisChat = findViewById(R.id.btnOpenJarvisChat)
        findViewById<Button>(R.id.btnOpenMemory).setOnClickListener {
            startActivity(Intent(this, MemoryActivity::class.java))
        }
        thresholdSeek = findViewById(R.id.thresholdSeek)
        thresholdLabel = findViewById(R.id.thresholdLabel)
        lastScoreText = findViewById(R.id.lastScoreText)

        currentThreshold = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
            .getFloat("threshold", 0.50f)
        thresholdSeek.progress = ((currentThreshold - 0.25f) / 0.60f * 100).toInt().coerceIn(0, 100)
        updateThresholdLabel()

        thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentThreshold = 0.25f + (progress / 100f) * 0.60f
                updateThresholdLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
                    .edit()
                    .putFloat("threshold", currentThreshold)
                    .apply()
            }
        })

        btnToggle.setOnClickListener {
            if (isServiceRunning) stopService() else checkPermissionAndStart()
        }

        btnOpenJarvisChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnTestChatGpt.setOnClickListener {
            val ok = ChatGPTLauncher.launch(this)
            if (!ok) {
                Toast.makeText(this, "ChatGPT no está instalado o no se pudo abrir", Toast.LENGTH_LONG).show()
            }
        }

        isServiceRunning = WakeWordService.isMarkedListening(this)
        engineState = WakeWordService.getMarkedEngineState(this)
        updateUI()
        handleLaunchIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(WakeWordService.ACTION_STATUS)
        ContextCompat.registerReceiver(
            this, statusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isServiceRunning = WakeWordService.isMarkedListening(this)
        engineState = WakeWordService.getMarkedEngineState(this)
        updateUI()
    }

    override fun onStop() {
        try { unregisterReceiver(statusReceiver) } catch (_: Exception) {}
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action == WakeWordService.ACTION_OPEN_CHATGPT) {
            ChatGPTLauncher.launchVoiceAssistant(this)
            intent.action = null
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED -> startService()
            else -> requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startService() {
        val intent = Intent(this, WakeWordService::class.java).apply {
            putExtra(WakeWordService.EXTRA_THRESHOLD, currentThreshold)
        }
        ContextCompat.startForegroundService(this, intent)
        isServiceRunning = true
        engineState = WakeWordDetector.STATE_STARTING
        updateUI()
    }

    private fun stopService() {
        val intent = Intent(this, WakeWordService::class.java).apply {
            action = WakeWordService.ACTION_STOP
        }
        startService(intent)
        isServiceRunning = false
        engineState = WakeWordService.STATE_STOPPED
        updateUI()
    }

    private fun updateThresholdLabel() {
        thresholdLabel.text = "Sensibilidad: %.2f  (más bajo = más sensible)".format(currentThreshold)
    }

    private fun updateUI() {
        if (isServiceRunning) {
            val detail = when (engineState) {
                WakeWordDetector.STATE_STARTING -> "Preparando el motor de reconocimiento…"
                WakeWordDetector.STATE_LOADING -> "Cargando modelos de voz…"
                WakeWordDetector.STATE_CHECKING -> "Comprobando el micrófono…"
                WakeWordDetector.STATE_CAPTURING -> "Micrófono activo. Di «Hey Jarvis»."
                else -> "Servicio activo; verificando el motor de audio…"
            }
            statusText.text = "Estado: $engineState\n$detail"
            btnToggle.text = "Detener escucha"
        } else {
            statusText.text = if (engineState == WakeWordService.STATE_ERROR) {
                "Estado: ERROR\nRevisa la notificación de JARVIS."
            } else {
                "Estado: DETENIDO"
            }
            btnToggle.text = "Iniciar escucha continua"
        }
    }
}
