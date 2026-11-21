package com.jarvis.wakeword

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.wakeword.ai.AiResult
import com.jarvis.wakeword.ai.ConversationRepository
import com.jarvis.wakeword.ai.NotConfiguredAiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Native conversation screen.
 * Messages are routed through a provider-neutral repository; the default
 * provider explicitly reports that ChatGPT has not been authorized yet.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var transcript: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var sendButton: Button

    private val conversation = ConversationRepository(NotConfiguredAiProvider())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        transcript = findViewById(R.id.chatTranscript)
        input = findViewById(R.id.chatInput)
        scroll = findViewById(R.id.chatScroll)
        sendButton = findViewById(R.id.chatSendButton)

        findViewById<TextView>(R.id.chatProviderStatus).text =
            "Proveedor: ${conversation.providerName} · No conectado"

        addMessage("JARVIS", "Buenos días, Santi. Interfaz conversacional inicializada.")
        addMessage(
            "SISTEMA",
            "La conexión oficial con ChatGPT todavía está pendiente. Esta pantalla no simula respuestas de inteligencia artificial."
        )

        sendButton.setOnClickListener { sendMessage() }
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
                    is AiResult.Success -> addMessage("JARVIS", result.text)
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
}
