package com.jarvis.wakeword.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRepositoryTest {

    @Test
    fun addRequiresNonBlankTitleAndContent() = runBlocking {
        val repository = repository(FakeMemoryStore())

        try {
            repository.add(" ", "Contenido")
            throw AssertionError("Se esperaba rechazar el título vacío.")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        try {
            repository.add("Título", " ")
            throw AssertionError("Se esperaba rechazar el contenido vacío.")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        assertTrue(repository.all().isEmpty())
    }

    @Test
    fun addTrimsFieldsAndNormalizesDuplicateTags() = runBlocking {
        val repository = repository(FakeMemoryStore())

        val entry = repository.add(
            "  Proyecto JARVIS ",
            "  Diseñar el asistente personal. ",
            listOf("Robótica", " robótica ", "Android")
        )

        assertEquals("Proyecto JARVIS", entry.title)
        assertEquals("Diseñar el asistente personal.", entry.content)
        assertEquals(listOf("Robótica", "Android"), entry.tags)
        assertEquals(1234L, entry.createdAtMillis)
        assertEquals(entry.createdAtMillis, entry.updatedAtMillis)
    }

    @Test
    fun updatePreservesCreationTimeAndChangesUpdateTime() = runBlocking {
        var now = 100L
        val store = FakeMemoryStore()
        val repository = MemoryRepository(store, clock = { now }, idFactory = { "memory-1" })
        val original = repository.add("Antes", "Contenido")

        now = 200L
        val updated = repository.update(original.id, "Después", "Nuevo contenido")

        assertEquals(100L, updated?.createdAtMillis)
        assertEquals(200L, updated?.updatedAtMillis)
        assertEquals("Después", updated?.title)
    }

    @Test
    fun deleteRemovesOnlyRequestedMemory() = runBlocking {
        val store = FakeMemoryStore()
        var id = 0
        val repository = MemoryRepository(store, idFactory = { "id-${++id}" })
        val first = repository.add("Uno", "Primera memoria")
        repository.add("Dos", "Segunda memoria")

        assertTrue(repository.delete(first.id))
        assertFalse(repository.delete(first.id))
        assertEquals(listOf("Dos"), repository.all().map { it.title })
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndRequiresEveryTerm() = runBlocking {
        val repository = repository(FakeMemoryStore())
        repository.add("Máquina del tiempo", "Proyecto de Tecnología", listOf("Tinkercad"))
        repository.add("Proyecto JARVIS", "Asistente y memoria local", listOf("Android"))

        assertEquals(
            listOf("Máquina del tiempo"),
            repository.search("MAQUINA tecnologia").map { it.title }
        )
        assertTrue(repository.search("máquina Android").isEmpty())
    }

    @Test
    fun failedPersistenceRollsBackInMemoryMutation() = runBlocking {
        val store = FakeMemoryStore()
        val repository = repository(store)
        store.failWrites = true

        try {
            repository.add("No guardar", "Este cambio debe revertirse")
            throw AssertionError("Se esperaba un fallo de almacenamiento.")
        } catch (_: IllegalStateException) {
            // Expected.
        }

        assertTrue(repository.all().isEmpty())
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun failedUpdateAndDeleteRestorePreviousEntry() = runBlocking {
        val store = FakeMemoryStore()
        val repository = repository(store)
        val original = repository.add("Original", "Contenido original")
        store.failWrites = true

        try {
            repository.update(original.id, "Modificado", "Contenido modificado")
            throw AssertionError("Se esperaba un fallo al actualizar.")
        } catch (_: IllegalStateException) {
            // Expected.
        }
        assertEquals(original, repository.all().single())

        try {
            repository.delete(original.id)
            throw AssertionError("Se esperaba un fallo al borrar.")
        } catch (_: IllegalStateException) {
            // Expected.
        }
        assertEquals(original, repository.all().single())
    }

    @Test
    fun failedClearRestoresInMemoryEntries() = runBlocking {
        val store = FakeMemoryStore()
        val repository = repository(store)
        val original = repository.add("Conservar", "No borrar")
        store.failClear = true

        try {
            repository.clear()
            throw AssertionError("Se esperaba un fallo al limpiar.")
        } catch (_: IllegalStateException) {
            // Expected.
        }

        assertEquals(listOf(original), repository.all())
        assertEquals(listOf(original), store.saved)
    }

    @Test
    fun memoriesAreRestoredFromStore() = runBlocking {
        val store = FakeMemoryStore()
        val firstRepository = repository(store)
        firstRepository.add("Proyecto", "Continuar JARVIS")

        val secondRepository = repository(store)
        assertEquals("Proyecto", secondRepository.all().single().title)
    }

    @Test
    fun clearRemovesAllEntriesFromRepositoryAndStore() = runBlocking {
        val store = FakeMemoryStore()
        val repository = repository(store)
        repository.add("Proyecto", "JARVIS")

        repository.clear()

        assertTrue(repository.all().isEmpty())
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun updateUnknownIdReturnsNull() = runBlocking {
        val repository = repository(FakeMemoryStore())
        assertNull(repository.update("missing", "Título", "Contenido"))
    }

    private fun repository(store: FakeMemoryStore): MemoryRepository {
        var nextId = 0
        return MemoryRepository(
            store,
            clock = { 1234L },
            idFactory = { "memory-${++nextId}" }
        )
    }

    private class FakeMemoryStore : MemoryStore {
        var saved: List<MemoryEntry> = emptyList()
        var failWrites: Boolean = false
        var failClear: Boolean = false

        override suspend fun readAll(): List<MemoryEntry> = saved

        override suspend fun writeAll(entries: List<MemoryEntry>) {
            if (failWrites) throw IllegalStateException("Fallo simulado de almacenamiento.")
            saved = entries.toList()
        }

        override suspend fun clear() {
            if (failClear) throw IllegalStateException("Fallo simulado al limpiar.")
            saved = emptyList()
        }
    }
}
