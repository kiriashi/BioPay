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

package io.github.kiriashi.biopay.data.prefs

import io.github.kiriashi.biopay.core.log.LOG_TAG
import io.github.kiriashi.biopay.data.crypto.KeystoreHelper
import io.github.kiriashi.biopay.payment.BiometricType
import android.content.SharedPreferences
import android.util.Log

private const val KEY_LOG_CAPTURE = "log_capture"
private const val KEY_PASSWORD_VERSION = "password_version"
private const val KEY_BIOMETRIC_TYPE = "bt"

class PreferencesStore(private val pref: SharedPreferences) {

    private val TAG = LOG_TAG

    fun isBioPayEnabled(): Boolean {
        return pref.getBoolean(PrefKeys.prefKeyOn, false)
    }

    fun isLogCaptureEnabled(): Boolean {
        return pref.getBoolean(KEY_LOG_CAPTURE, false)
    }

    fun setLogCaptureEnabled(enabled: Boolean) {
        pref.edit().putBoolean(KEY_LOG_CAPTURE, enabled).apply()
    }

    fun getEncodedPassword(): String? {
        return pref.getString(PrefKeys.prefKeyPwd, "")
    }

    fun savePassword(password: String, cipher: javax.crypto.Cipher, passwordVersion: Int): Result<Unit> {
        return try {
            val encrypted = KeystoreHelper.encrypt(password, cipher)
            pref.edit().apply {
                putString(PrefKeys.prefKeyPwd, encrypted)
                putBoolean(PrefKeys.prefKeyOn, true)
                putInt(KEY_PASSWORD_VERSION, passwordVersion)
            }.apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "savePassword failed", e)
            Result.failure(e)
        }
    }

    fun clearPassword() {
        pref.edit().apply {
            putBoolean(PrefKeys.prefKeyOn, false)
            putString(PrefKeys.prefKeyPwd, "")
            putInt(KEY_PASSWORD_VERSION, 0)
        }.apply()
    }

    fun setBioPayEnabled(enabled: Boolean) {
        pref.edit().putBoolean(PrefKeys.prefKeyOn, enabled).apply()
    }

    fun saveBiometricType(type: Int) {
        pref.edit().putInt(KEY_BIOMETRIC_TYPE, type).apply()
    }

    fun getBiometricType(): Int {
        return pref.getInt(KEY_BIOMETRIC_TYPE, BiometricType.DISABLED)
    }

    fun getPasswordVersion(): Int = pref.getInt(KEY_PASSWORD_VERSION, 0)
}
