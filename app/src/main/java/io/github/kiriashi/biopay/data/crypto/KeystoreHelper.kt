/*
 * BioPay - biometric payment assistance for WeChat Tenpay keyboard.
 *
 * Copyright (C) 2026 kiriashi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.kiriashi.biopay.data.crypto

import io.github.kiriashi.biopay.core.log.LOG_TAG
import io.github.kiriashi.biopay.data.prefs.PrefKeys
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps the payment password behind a user-authenticated Keystore key. */
object KeystoreHelper {

    const val PASSWORD_LENGTH = 6
    private const val TAG = LOG_TAG
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val KEY_VERSION = "_compat_v3"

    private val keyAlias get() = PrefKeys.keystoreAlias + KEY_VERSION
    private val keyStoreLock = Any()
    private var keyStore: KeyStore? = null
    private var secretKey: SecretKey? = null

    data class DecryptOperation(
        val cipher: Cipher,
        val ciphertext: ByteArray
    )

    private fun getKeyStore(): KeyStore {
        keyStore?.let { return it }
        synchronized(keyStoreLock) {
            keyStore?.let { return it }
            return KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }.also { keyStore = it }
        }
    }

    private fun getSecretKey(): SecretKey {
        secretKey?.let { return it }
        synchronized(keyStoreLock) {
            secretKey?.let { return it }
            try {
                if (!getKeyStore().containsAlias(keyAlias)) generateKey()
                return (getKeyStore().getKey(keyAlias, null) as SecretKey).also { secretKey = it }
            } catch (e: Throwable) {
                if (e is KeyPermanentlyInvalidatedException || e.cause is KeyPermanentlyInvalidatedException) {
                    Log.w(TAG, "encryption key invalidated; clearing key")
                    getKeyStore().deleteEntry(keyAlias)
                    secretKey = null
                    throw IllegalStateException("encryption key invalidated", e)
                }
                throw e
            }
        }
    }

    private fun generateKey() {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
        generator.init(builder.build())
        generator.generateKey()
    }

    fun createEncryptionCipher(): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, getSecretKey())
    }

    fun encrypt(plainText: String, cipher: Cipher): String {
        require(plainText.length == PASSWORD_LENGTH) { "Invalid password length" }
        val bytes = plainText.toByteArray(Charsets.UTF_8)
        return try {
            val encrypted = cipher.doFinal(bytes)
            Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        } finally {
            bytes.fill(0)
        }
    }

    fun createDecryptOperation(encoded: String): DecryptOperation? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return null
            val iv = combined.copyOfRange(0, IV_LENGTH)
            val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getSecretKey(),
                    GCMParameterSpec(GCM_TAG_LENGTH, iv)
                )
            }
            DecryptOperation(cipher, ciphertext)
        } catch (e: Throwable) {
            Log.w(TAG, "failed to prepare password decryption", e)
            null
        }
    }

    fun decryptToCharArray(operation: DecryptOperation): CharArray? {
        var bytes: ByteArray? = null
        return try {
            bytes = operation.cipher.doFinal(operation.ciphertext)
            CharArray(bytes.size) { i -> (bytes!![i].toInt() and 0xff).toChar() }
        } catch (e: Throwable) {
            Log.w(TAG, "password decryption failed", e)
            null
        } finally {
            bytes?.fill(0)
            operation.ciphertext.fill(0)
        }
    }

}
