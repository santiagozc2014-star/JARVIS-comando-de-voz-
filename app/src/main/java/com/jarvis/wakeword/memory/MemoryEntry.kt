package com.jarvis.wakeword.memory

/**
 * A user-approved fact or note retained by JARVIS.
 * Entries are created only through an explicit save action.
 */
data class MemoryEntry(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
