package com.jarvis.wakeword.memory

/**
 * Persistence boundary for approved memories.
 * The production implementation must encrypt data at rest.
 */
interface MemoryStore {
    suspend fun readAll(): List<MemoryEntry>
    suspend fun writeAll(entries: List<MemoryEntry>)
    suspend fun clear()
}
