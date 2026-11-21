package com.jarvis.wakeword

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.wakeword.ai.AiResult
import com.jarvis.wakeword.ai.ConversationMessage
import com.jarvis.wakeword.ai.ConversationRepository
import com.jarvis.wakeword.ai.EncryptedConversationHistoryStore
import com.jarvis.wakeword.ai.NotConfiguredAiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Native conversation screen with opt-in, encrypted local history.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var transcript: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var sendButton: Button
    private lateinit var saveHistoryCheck: CheckBox
    private lateinit var clearHistoryButton: Button

    private val conversation by lazy {
        ConversationRepository(
            NotConfiguredAiProvider(),
            EncryptedConversationHistoryStore(this)
        )
    }

    private val memoryPreferences by lazy {
        getSharedPreferences("jarvis_memory", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        transcript = findViewById(R.id.chatTranscript)
        input = findViewById(R.id.chatInput)
        scroll = findViewById(R.id.chatScroll)
        sendButton = findViewById(R.id.chatSendButton)
        saveHistoryCheck = findViewById(R.id.chatSaveHistory)
        clearHistoryButton = findViewById(R.id.chatClearHistory)

        findViewById<TextView>(R.id.chatProviderStatus).text =
            "Proveedor: ${conversation.providerName} · No conectado"

        saveHistoryCheck.isChecked =
            memoryPreferences.getBoolean(KEY_SAVE_HISTORY, false)

        sendButton.setOnClickListener { sendMessage() }
        saveHistoryCheck.setOnCheckedChangeListener { _, enabled ->
            setHistoryPersistence(enabled)
        }
        clearHistoryButton.setOnClickListener { confirmClearHistory() }

        if (saveHistoryCheck.isChecked) {
            setHistoryPersistence(true)
        } else {
            showInitialMessages()
        }
    }

    private fun setHistoryPersistence(enabled: Boolean) {
        saveHistoryCheck.isEnabled = false
        lifecycleScope.launch {
            try {
                if (enabled) {
                    conversation.enablePersistence()
                    memoryPreferences.edit().putBoolean(KEY_SAVE_HISTORY, true).apply()
                    renderHistory()
                    Toast.makeText(
                        this@ChatActivity,
                        "Historial local cifrado activado.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    conversation.disablePersistence()
                    memoryPreferences.edit().putBoolean(KEY_SAVE_HISTORY, false).apply()
                    Toast.makeText(
                        this@ChatActivity,
                        "Guardado desactivado; copia local eliminada.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                saveHistoryCheck.setOnCheckedChangeListener(null)
                saveHistoryCheck.isChecked = !enabled
                saveHistoryCheck.setOnCheckedChangeListener { _, checked ->
                    setHistoryPersistence(checked)
                }
                Toast.makeText(
                    this@ChatActivity,
                    "No se pudo cambiar la configuración del historial.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                saveHistoryCheck.isEnabled = true
            }
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty() || !sendButton.isEnabled) return

        addMessage("TÚ", message)
        input.text.clear()
        sendButton.isEnabled = false

        lifecycleScope.launch {
            try {
                when (val result = conversation.send(message)) {
                    is AiResult.Success -> {
                        addMessage("JARVIS", result.text)
                        conversation.persistenceWarning?.let {
                            Toast.makeText(this@ChatActivity, it, Toast.LENGTH_LONG).show()
                        }
                    }
                    is AiResult.Unavailable -> addMessage("SISTEMA", result.reason)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                addMessage("SISTEMA", "No se pudo procesar el mensaje. No se ha confirmado ninguna conexión.")
            } finally {
                sendButton.isEnabled = true
            }
        }
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle("Borrar historial local")
            .setMessage("Se eliminará el historial guardado en este dispositivo y el contexto de esta sesión. Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Borrar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        conversation.clearHistory()
                        transcript.removeAllViews()
                        showInitialMessages()
                        Toast.makeText(
                            this@ChatActivity,
                            "Historial local borrado.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@ChatActivity,
                            "No se pudo borrar el archivo de historial.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun renderHistory() {
        transcript.removeAllViews()
        val history = conversation.history()
        if (history.isEmpty()) {
            showInitialMessages()
            return
        }
        history.forEach { message ->
            val sender = when (message.role) {
                ConversationMessage.Role.USER -> "TÚ"
                ConversationMessage.Role.ASSISTANT -> "JARVIS"
            }
            addMessage(sender, message.content)
        }
    }

    private fun showInitialMessages() {
        transcript.removeAllViews()
        addMessage("JARVIS", "Buenos días, Santi. Interfaz conversacional inicializada.")
        addMessage(
            "SISTEMA",
            "La conexión oficial con ChatGPT todavía está pendiente. Esta pantalla no simula respuestas de inteligencia artificial."
        )
    }

    private fun addMessage(sender: String, body: String) {
        val item = TextView(this).apply {
            text = "$sender\n$body"
            textSize = 16f
            setTextColor(getColor(R.color.jarvis_text))
            setPadding(14, 12, 14, 12)
            gravity = Gravity.START
            setBackgroundResource(R.drawable.chat_message_background)
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 }
        transcript.addView(item, params)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private companion object {
        const val KEY_SAVE_HISTORY = "save_history"
    }
}
