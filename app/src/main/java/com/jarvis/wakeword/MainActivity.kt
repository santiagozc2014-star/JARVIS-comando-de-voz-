package com.jarvis.wakeword

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.app.role.RoleManager
import android.os.Build
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
    private lateinit var assistantStatusText: TextView
    private lateinit var btnSetDefaultAssistant: Button

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

    private val requestAssistantRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateAssistantStatus()
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

            val launchResultName = intent.getStringExtra(WakeWordService.EXTRA_LAUNCH_RESULT)
            val launchResult = launchResultName?.let {
                runCatching { LaunchRequestResult.valueOf(it) }.getOrNull()
            }
            if (launchResult != null) {
                Toast.makeText(this@MainActivity, launchResult.userMessage, Toast.LENGTH_LONG).show()
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
        assistantStatusText = findViewById(R.id.assistantStatusText)
        btnSetDefaultAssistant = findViewById(R.id.btnSetDefaultAssistant)
        btnSetDefaultAssistant.setOnClickListener { requestAssistantRole() }
        findViewById<Button>(R.id.btnSamsungBatterySettings).setOnClickListener {
            openSamsungBackgroundSettings()
        }

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

        isServiceRunning = WakeWordService.isMarkedListening()
        engineState = WakeWordService.getMarkedEngineState(this)
        updateUI()
        updateAssistantStatus()
    }

    override fun onResume() {
        super.onResume()
        updateAssistantStatus()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(WakeWordService.ACTION_STATUS)
        ContextCompat.registerReceiver(
            this, statusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isServiceRunning = WakeWordService.isMarkedListening()
        engineState = WakeWordService.getMarkedEngineState(this)
        updateUI()
    }

    override fun onStop() {
        try { unregisterReceiver(statusReceiver) } catch (_: Exception) {}
        super.onStop()
    }

    private fun requestAssistantRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "La selección de asistente necesita Android 10 o posterior.", Toast.LENGTH_LONG).show()
            return
        }
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
            try {
                startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Este sistema no ofrece la selección de asistente.", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            updateAssistantStatus()
            Toast.makeText(this, "JARVIS ya es el asistente predeterminado.", Toast.LENGTH_LONG).show()
            return
        }
        requestAssistantRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))
    }

    private fun updateAssistantStatus() {
        if (!::assistantStatusText.isInitialized || !::btnSetDefaultAssistant.isInitialized) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            assistantStatusText.text = "La selección de asistente requiere Android 10 o posterior."
            btnSetDefaultAssistant.isEnabled = false
            return
        }
        val roleManager = getSystemService(RoleManager::class.java)
        val available = roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
        val held = available && roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        when {
            held -> {
                assistantStatusText.text = "JARVIS es el asistente predeterminado. Las detecciones podrán usar la sesión de voz del sistema."
                btnSetDefaultAssistant.text = "JARVIS ya es el asistente activo"
                btnSetDefaultAssistant.isEnabled = false
            }
            available -> {
                assistantStatusText.text = "Selecciona JARVIS como asistente predeterminado para habilitar la apertura mediante la sesión de voz de Android."
                btnSetDefaultAssistant.text = "Establecer JARVIS como asistente"
                btnSetDefaultAssistant.isEnabled = true
            }
            else -> {
                assistantStatusText.text = "One UI no ofrece el rol de asistente a esta aplicación. Se mantendrá la apertura anterior."
                btnSetDefaultAssistant.text = "Abrir ajustes de asistente"
                btnSetDefaultAssistant.isEnabled = true
            }
        }
    }

    private fun openSamsungBackgroundSettings() {
        // Samsung documents this deep link for its "Never sleeping apps" list.
        val samsungIntent = Intent().apply {
            action = "com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY"
            setPackage("com.samsung.android.lool")
            putExtra("activity_type", 2)
        }

        try {
            startActivity(samsungIntent)
        } catch (_: Exception) {
            // Non-Samsung firmware or a One UI build without this activity.
            val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            try {
                startActivity(appDetails)
                Toast.makeText(
                    this,
                    "Abre Batería y permite que JARVIS no se suspenda.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(this, "No se pudieron abrir los ajustes de batería.", Toast.LENGTH_LONG).show()
            }
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