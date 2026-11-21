package com.jarvis.wakeword.memory

import java.text.Normalizer
import java.util.Locale
import java.util.UUID

/**
 * Manages structured memories explicitly saved by the user.
 *
 * This repository does not infer or save facts automatically. A future UI or
 * assistant action must ask for approval before calling add().
 */
class MemoryRepository(
    private val store: MemoryStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    private var loaded = false
    private val entries = linkedMapOf<String, MemoryEntry>()

    suspend fun add(title: String, content: String, tags: List<String> = emptyList()): MemoryEntry {
        ensureLoaded()
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        require(cleanTitle.isNotEmpty()) { "El título de la memoria no puede estar vacío." }
        require(cleanContent.isNotEmpty()) { "El contenido de la memoria no puede estar vacío." }

        val now = clock()
        val entry = MemoryEntry(
            id = idFactory(),
            title = cleanTitle,
            content = cleanContent,
            tags = normalizeTags(tags),
            createdAtMillis = now,
            updatedAtMillis = now
        )
        require(!entries.containsKey(entry.id)) { "El identificador de memoria ya existe." }
        entries[entry.id] = entry
        persist()
        return entry
    }

    suspend fun update(
        id: String,
        title: String,
        content: String,
        tags: List<String> = emptyList()
    ): MemoryEntry? {
        ensureLoaded()
        val previous = entries[id] ?: return null
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        require(cleanTitle.isNotEmpty()) { "El título de la memoria no puede estar vacío." }
        require(cleanContent.isNotEmpty()) { "El contenido de la memoria no puede estar vacío." }

        val updated = previous.copy(
            title = cleanTitle,
            content = cleanContent,
            tags = normalizeTags(tags),
            updatedAtMillis = clock()
        )
        entries[id] = updated
        persist()
        return updated
    }

    suspend fun delete(id: String): Boolean {
        ensureLoaded()
        if (entries.remove(id) == null) return false
        persist()
        return true
    }

    suspend fun clear() {
        ensureLoaded()
        entries.clear()
        store.clear()
    }

    suspend fun all(): List<MemoryEntry> {
        ensureLoaded()
        return entries.values.sortedByDescending { it.updatedAtMillis }
    }

    /**
     * Case- and accent-insensitive keyword search.
     * Every query term must occur in the title, content, or tags.
     */
    suspend fun search(query: String, limit: Int = DEFAULT_LIMIT): List<MemoryEntry> {
        require(limit in 1..MAX_LIMIT) { "El límite de búsqueda no es válido." }
        ensureLoaded()
        val terms = tokenize(query)
        if (terms.isEmpty()) return emptyList()

        return entries.values
            .mapNotNull { entry ->
                val title = normalize(entry.title)
                val content = normalize(entry.content)
                val tags = entry.tags.map(::normalize)
                val searchable = listOf(title, content) + tags
                if (!terms.all { term -> searchable.any { it.contains(term) } }) {
                    return@mapNotNull null
                }
                val score = terms.sumOf { term ->
                    (if (title.contains(term)) 4 else 0) +
                        (if (tags.any { it.contains(term) }) 3 else 0) +
                        (if (content.contains(term)) 1 else 0
                }
                entry to score
            }
            .sortedWith(
                compareByDescending<Pair<MemoryEntry, Int>> { it.second }
                    .thenByDescending { it.first.updatedAtMillis }
            )
            .take(limit)
            .map { it.first }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        entries.clear()
        store.readAll().forEach { entry -> entries[entry.id] = entry }
        loaded = true
    }

    private suspend fun persist() {
        store.writeAll(entries.values.toList())
    }

    private fun normalizeTags(tags: List<String>): List<String> =
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy(::normalize)
            .take(MAX_TAGS)

    private fun tokenize(value: String): List<String> =
        normalize(value)
            .split(WORD_SEPARATOR)
            .filter { it.isNotBlank() }
            .distinct()

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)

    private companion object {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100
        const val MAX_TAGS = 20
        val WORD_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")
        val DIACRITICS = Regex("\\p{Mn}+")
    }
}
