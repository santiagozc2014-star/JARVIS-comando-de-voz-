package com.jarvis.wakeword.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted, app-private conversation history.
 *
 * AES-256-GCM protects the transcript; its key is generated and retained by
 * Android Keystore and is never written to the repository or exported.
 */
class EncryptedConversationHistoryStore(context: Context) : ConversationHistoryStore {
    private val appContext = context.applicationContext
    private val atomicFile = AtomicFile(File(appContext.filesDir, FILE_NAME))

    override suspend fun read(): List<ConversationMessage> = withContext(Dispatchers.IO) {
        // openRead() restores AtomicFile's backup after an interrupted write.
        val input = try {
            atomicFile.openRead()
        } catch (_: FileNotFoundException) {
            return@withContext emptyList()
        }
        val encrypted = input.use {
            if (atomicFile.baseFile.length() > MAX_FILE_BYTES) {
                throw IllegalStateException("El historial cifrado supera el tamaño permitido.")
            }
            it.readBytes()
        }
        decrypt(encrypted)
    }

    override suspend fun write(messages: List<ConversationMessage>) = withContext(Dispatchers.IO) {
        val payload = serialize(messages.takeLast(MAX_MESSAGES))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(payload)
        if (encrypted.size > MAX_FILE_BYTES) {
            throw IllegalArgumentException("El historial es demasiado grande para guardarlo.")
        }

        var output = atomicFile.startWrite()
        try {
            output.write(encrypted)
            atomicFile.finishWrite(output)
        } catch (error: Exception) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        atomicFile.delete()
    }

    private fun decrypt(encrypted: ByteArray): List<ConversationMessage> {
        if (encrypted.size < GCM_IV_BYTES + GCM_TAG_BYTES) {
            throw IllegalStateException("El archivo de historial está incompleto.")
        }
        val iv = encrypted.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = encrypted.copyOfRange(GCM_IV_BYTES, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return deserialize(cipher.doFinal(ciphertext))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun serialize(messages: List<ConversationMessage>): ByteArray {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { output ->
            output.writeInt(messages.size)
            messages.forEach { message ->
                val content = message.content.toByteArray(Charsets.UTF_8)
                require(content.size <= MAX_MESSAGE_BYTES) { "Un mensaje supera el límite permitido." }
                output.writeByte(message.role.ordinal)
                output.writeInt(content.size)
                output.write(content)
            }
        }
        return buffer.toByteArray()
    }

    private fun deserialize(payload: ByteArray): List<ConversationMessage> {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val count = input.readInt()
            if (count !in 0..MAX_MESSAGES) {
                throw IllegalStateException("El historial contiene un número de mensajes no válido.")
            }
            val result = ArrayList<ConversationMessage>(count)
            repeat(count) {
                val roleIndex = input.readUnsignedByte()
                val role = ConversationMessage.Role.values().getOrNull(roleIndex)
                    ?: throw IllegalStateException("El historial contiene un rol no válido.")
                val length = input.readInt()
                if (length !in 0..MAX_MESSAGE_BYTES) {
                    throw IllegalStateException("El historial contiene un mensaje no válido.")
                }
                val bytes = ByteArray(length)
                input.readFully(bytes)
                result += ConversationMessage(role, String(bytes, Charsets.UTF_8))
            }
            if (input.available() != 0) {
                throw IllegalStateException("El historial contiene datos adicionales no válidos.")
            }
            return result
        }
    }

    private companion object {
        const val FILE_NAME = "jarvis_conversation_history.enc"
        const val KEY_ALIAS = "com.jarvis.wakeword.conversation_history"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val MAX_MESSAGES = 500
        const val MAX_MESSAGE_BYTES = 48 * 1024
        const val MAX_FILE_BYTES = 2 * 1024 * 1024
    }
}
