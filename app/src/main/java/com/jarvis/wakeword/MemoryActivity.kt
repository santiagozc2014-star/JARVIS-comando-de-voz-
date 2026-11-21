package com.jarvis.wakeword

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.wakeword.memory.EncryptedMemoryStore
import com.jarvis.wakeword.memory.MemoryEntry
import com.jarvis.wakeword.memory.MemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Local memory manager. Nothing is stored unless the user explicitly saves it.
 */
class MemoryActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var entriesContainer: LinearLayout
    private lateinit var emptyState: TextView
    private lateinit var addButton: Button

    private val repository by lazy { MemoryRepository(EncryptedMemoryStore(applicationContext)) }
    private var currentEntries: List<MemoryEntry> = emptyList()
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory)
        searchInput = findViewById(R.id.memorySearch)
        entriesContainer = findViewById(R.id.memoryEntries)
        emptyState = findViewById(R.id.memoryEmpty)
        addButton = findViewById(R.id.memoryAddButton)

        addButton.setOnClickListener { showEditor(null) }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                refresh()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        refresh()
    }

    private fun refresh() {
        addButton.isEnabled = false
        lifecycleScope.launch {
            try {
                currentEntries = if (searchQuery.isBlank()) repository.all()
                    else repository.search(searchQuery, limit = 100)
                renderEntries()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Toast.makeText(this@MemoryActivity, "No se pudo leer la memoria cifrada.", Toast.LENGTH_LONG).show()
            } finally {
                addButton.isEnabled = true
            }
        }
    }

    private fun renderEntries() {
        entriesContainer.removeAllViews()
        emptyState.visibility = if (currentEntries.isEmpty()) TextView.VISIBLE else TextView.GONE
        emptyState.text = if (searchQuery.isBlank()) {
            "Todavía no hay recuerdos guardados. Añade uno cuando quieras que JARVIS lo conserve."
        } else {
            "No se encontraron recuerdos para esa búsqueda."
        }

        currentEntries.forEach { entry ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
                setBackgroundResource(R.drawable.chat_message_background)
            }
            val title = TextView(this).apply {
                text = entry.title
                textSize = 17f
                setTextColor(getColor(R.color.jarvis_cyan))
            }
            val content = TextView(this).apply {
                text = entry.content
                textSize = 14f
                setTextColor(getColor(R.color.jarvis_text))
                setPadding(0, 8, 0, 4)
            }
            val tags = TextView(this).apply {
                text = if (entry.tags.isEmpty()) "Sin etiquetas" else entry.tags.joinToString(" · ")
                textSize = 12f
                setTextColor(getColor(R.color.jarvis_muted))
            }
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val edit = Button(this).apply {
                text = "Editar"
                setOnClickListener { showEditor(entry) }
            }
            val delete = Button(this).apply {
                text = "Borrar"
                setOnClickListener { confirmDelete(entry) }
            }
            actions.addView(edit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(delete, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(title)
            card.addView(content)
            card.addView(tags)
            card.addView(actions)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            entriesContainer.addView(card, params)
        }
    }

    private fun showEditor(existing: MemoryEntry?) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 0)
        }
        val titleInput = EditText(this).apply {
            hint = "Título"
            singleLine = true
            setText(existing?.title.orEmpty())
        }
        val contentInput = EditText(this).apply {
            hint = "Contenido del recuerdo"
            minLines = 3
            maxLines = 6
            setText(existing?.content.orEmpty())
        }
        val tagsInput = EditText(this).apply {
            hint = "Etiquetas separadas por comas (opcional)"
            setText(existing?.tags?.joinToString(", ").orEmpty())
        }
        form.addView(titleInput)
        form.addView(contentInput)
        form.addView(tagsInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nuevo recuerdo" else "Editar recuerdo")
            .setMessage("Solo se guardará cuando pulses Guardar. El contenido permanecerá cifrado en este dispositivo.")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text.toString().trim()
                val content = contentInput.text.toString().trim()
                if (title.isEmpty() || content.isEmpty()) {
                    Toast.makeText(this, "Escribe un título y un contenido.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val tags = tagsInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                lifecycleScope.launch {
                    try {
                        if (existing == null) repository.add(title, content, tags)
                        else repository.update(existing.id, title, content, tags)
                        dialog.dismiss()
                        refresh()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        Toast.makeText(this@MemoryActivity, "No se pudo guardar el recuerdo.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(entry: MemoryEntry) {
        AlertDialog.Builder(this)
            .setTitle("Borrar recuerdo")
            .setMessage("¿Quieres borrar «${entry.title}» de la memoria local? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Borrar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.delete(entry.id)
                        refresh()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        Toast.makeText(this@MemoryActivity, "No se pudo borrar el recuerdo.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }
}
