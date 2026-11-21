package com.jarvis.wakeword

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.wakeword.ai.ProviderConnectionState

/**
 * Native conversation screen. It deliberately does not fabricate AI answers:
 * until an authorized provider is connected, it explains the connection state.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var transcript: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        transcript = findViewById(R.id.chatTranscript)
        input = findViewById(R.id.chatInput)
        scroll = findViewById(R.id.chatScroll)

        addMessage("JARVIS", "Buenos días, Santi. Interfaz conversacional inicializada.")
        addMessage(
            "SISTEMA",
            when (ProviderConnectionState.NOT_CONFIGURED) {
                ProviderConnectionState.NOT_CONFIGURED ->
                    "Conexión con ChatGPT pendiente de autorización oficial. No se ha enviado ningún mensaje a un modelo."
                ProviderConnectionState.AUTHORIZATION_REQUIRED ->
                    "Se necesita autorización para conectar el proveedor."
                ProviderConnectionState.CONNECTED -> "Proveedor conectado."
                ProviderConnectionState.ERROR -> "El proveedor comunicó un error."
            }
        )

        findViewById<Button>(R.id.chatSendButton).setOnClickListener {
            val message = input.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessage("TÚ", message)
                input.text.clear()
                addMessage(
                    "JARVIS",
                    "He recibido tu mensaje. Mi conector de inteligencia todavía no está autorizado; no voy a simular una respuesta de ChatGPT."
                )
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
