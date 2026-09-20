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
package io.github.kiriashi.biopay.payment

import io.github.kiriashi.biopay.core.log.LOG_TAG
import io.github.kiriashi.biopay.core.log.LogCapture
import io.github.kiriashi.biopay.core.util.isValidActivity
import io.github.kiriashi.biopay.data.crypto.KeystoreHelper
import io.github.kiriashi.biopay.hook.FieldStore
import io.github.kiriashi.biopay.hook.TopActivityProvider
import io.github.kiriashi.biopay.lifecycle.AppState
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup

object BiometricGate {

    private val TAG = LOG_TAG
    fun triggerBiometricAuth(
        keyboardView: ViewGroup,
        encodedPassword: String,
        state: AppState,
        sessionId: Long
    ): Boolean {
        if (!state.session.isCurrentSession(sessionId)) return false
        val context = keyboardView.context
        if (!context.isValidActivity()) {
            Log.d(TAG, "trigger: context is not valid Activity, skip"); LogCapture.log("trigger: not Activity, skip")
            return false
        }
        if (!state.fields.compareAndSetField(context, FieldStore.BIOMETRIC_IN_PROGRESS, false, true)) {
            Log.d(TAG, "trigger: FIELD_BIOMETRIC_IN_PROGRESS already set, skip"); LogCapture.log("trigger: already in progress, skip")
            return false
        }

        val activity = TopActivityProvider.getTopActivity()
        if (activity == null) {
            Log.d(TAG, "trigger: topActivity null"); LogCapture.log("trigger: topActivity null")
            clearFlagAndShowKeyboard(keyboardView, state)
            return false
        }

        val biometricType = state.prefs.getBiometricType()
        val decryptOperation = KeystoreHelper.createDecryptOperation(encodedPassword)
        if (decryptOperation == null) {
            Log.d(TAG, "trigger: decrypt failed"); LogCapture.log("trigger: decrypt failed")
            state.prefs.clearPassword()
            clearFlagAndShowKeyboard(keyboardView, state)
            return false
        }

        Log.d(TAG, "trigger: biometricType=$biometricType, showing dialog"); LogCapture.log("trigger: type=$biometricType")

        try {
            val executor = context.mainExecutor
            val builder = BiometricPrompt.Builder(context)
                .setTitle("身份验证")
                .setNegativeButton("取消", executor) { _, _ ->
                    Log.d(TAG, "negative button clicked, executing cleanup directly"); LogCapture.log("negative button clicked")
                    decryptOperation.ciphertext.fill(0)
                    if (state.fields.compareAndSetField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS, true, false)) {
                        onBiometricError(keyboardView, state, sessionId)
                    }
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    if (biometricType == BiometricType.FINGERPRINT) {
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                    } else {
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                    }
                )
            }
            when (biometricType) {
                BiometricType.FINGERPRINT -> Unit
                BiometricType.FACE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        builder.setConfirmationRequired(false)
                    }
                }
                BiometricType.BOTH -> Unit
                else -> {
                    Log.w(TAG, "trigger: unsupported biometricType=$biometricType"); LogCapture.log("trigger: unsupported type=$biometricType")
                    decryptOperation.ciphertext.fill(0)
                    clearFlagAndShowKeyboard(keyboardView, state)
                    return false
                }
            }

            val biometricPrompt = builder.build()
            val signal = state.session.createNewSignal()
            val callback = BiometricAuthCallback(keyboardView, decryptOperation, state, sessionId)
            biometricPrompt.authenticate(signal, executor, callback)

            // Keep external payment keyboards attached; GONE can recreate them and cancel auth.
            keyboardView.visibility = View.INVISIBLE
            Log.d(TAG, "trigger: keyboard INVISIBLE, dialog shown"); LogCapture.log("trigger: keyboard INVISIBLE")
            return true
        } catch (e: Throwable) {
            decryptOperation.ciphertext.fill(0)
            Log.w(TAG, "biometric auth failed", e); LogCapture.log("trigger: failed: ${e.message}")
            clearFlagAndShowKeyboard(keyboardView, state)
            return false
        }
    }

    private fun clearFlagAndShowKeyboard(keyboardView: ViewGroup, state: AppState) {
        state.session.cancelCurrentSignal()
        state.fields.removeField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)
        keyboardView.visibility = View.VISIBLE
        Log.d(TAG, "clearFlagAndShowKeyboard: keyboard VISIBLE"); LogCapture.log("clearFlag: keyboard VISIBLE")
    }

    private fun onBiometricSuccess(
        callbackKeyboardView: ViewGroup,
        passwordChars: CharArray,
        state: AppState,
        sessionId: Long
    ) {
        if (!state.session.isCurrentSession(sessionId)) return
        val keyboardView = state.session.getCurrentKeyboardView() ?: callbackKeyboardView
        state.fields.removeField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)
        keyboardView.visibility = View.VISIBLE
        KeyboardCloak.concealActivityWindow(state)
        KeyboardCloak.cloakKeyboardViews(keyboardView)
        PasswordAutoInput.autoInputPassword(keyboardView, passwordChars, state, sessionId)
        state.session.setCurrentEncodedPassword(null)
    }

    private fun onBiometricError(callbackKeyboardView: ViewGroup, state: AppState, sessionId: Long) {
        if (!state.session.isCurrentSession(sessionId)) return
        state.session.cancelCurrentSignal()
        val keyboardView = state.session.getCurrentKeyboardView() ?: callbackKeyboardView
        state.fields.removeField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)
        val inputEditText = state.session.getInputEditText()
        KeyboardCloak.restoreConcealedInputViews()
        if (inputEditText?.isAttachedToWindow == true) {
            inputEditText.requestFocus()
            inputEditText.post {
                val inputMethodManager = inputEditText.context.getSystemService(
                    android.content.Context.INPUT_METHOD_SERVICE
                ) as? android.view.inputmethod.InputMethodManager
                inputMethodManager?.showSoftInput(
                    inputEditText,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
        Log.d(TAG, "onBiometricError: callbackView=${callbackKeyboardView.hashCode()} finalView=${keyboardView.hashCode()}")
        LogCapture.log("onBiometricError: finalView=${keyboardView.hashCode()}")
        KeyboardCloak.uncloakKeyboardViews(keyboardView)
        keyboardView.visibility = View.VISIBLE
        Log.d(TAG, "onBiometricError: keyboard VISIBLE done"); LogCapture.log("onBiometricError: keyboard VISIBLE")
    }

private class BiometricAuthCallback(
        private val keyboardView: ViewGroup,
        private val decryptOperation: KeystoreHelper.DecryptOperation,
        private val state: AppState,
        private val sessionId: Long
    ) : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            Log.d(TAG, "onAuthenticationError: code=$errorCode"); LogCapture.log("onAuthError: code=$errorCode")
            decryptOperation.ciphertext.fill(0)
            onBiometricError(keyboardView, state, sessionId)
        }

        override fun onAuthenticationFailed() {
            Log.d(TAG, "onAuthenticationFailed"); LogCapture.log("onAuthFailed")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            Log.d(TAG, "onAuthenticationSucceeded"); LogCapture.log("onAuthSucceeded")
            try {
                val passwordChars = KeystoreHelper.decryptToCharArray(decryptOperation)
                if (passwordChars == null) {
                    state.prefs.clearPassword()
                    onBiometricError(keyboardView, state, sessionId)
                    return
                }
                onBiometricSuccess(keyboardView, passwordChars, state, sessionId)
            } finally {
                decryptOperation.ciphertext.fill(0)
            }
        }
    }
}
