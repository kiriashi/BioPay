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
import io.github.kiriashi.biopay.data.crypto.PasswordVersionPolicy
import io.github.kiriashi.biopay.hook.FieldStore
import io.github.kiriashi.biopay.lifecycle.AppState
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

object BiometricPaymentController {

    private val TAG = LOG_TAG
    private val attachLock = Any()
    private var attachListener: KeyboardAttachListener? = null
    private var attachedViewRef: WeakReference<ViewGroup>? = null
    private val setupLock = Any()
    fun setupBiometricAuth(keyboardView: ViewGroup, encodedPassword: String, state: AppState) {
        if (PasswordVersionPolicy.requiresReentry(encodedPassword, state.prefs.getPasswordVersion())) {
            Log.w(TAG, "setupBiometricAuth: stored password format requires re-entry")
            LogCapture.log("setup: stored password format requires re-entry")
            state.prefs.clearPassword()
            return
        }

        val (sessionId, shouldTrigger) = synchronized(setupLock) {
            val alreadyInProgress = state.fields.hasField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)
            val id = if (alreadyInProgress) state.session.currentSessionId() else state.session.beginSession()
            removeListenersFromOldView()

            state.session.setCurrentKeyboardView(keyboardView)
            state.session.setCurrentEncodedPassword(encodedPassword)
            state.session.setInPaymentMode(true)

            synchronized(attachLock) {
                if (attachListener == null) {
                    attachListener = KeyboardAttachListener(state)
                }
                attachListener!!.keyboardView = keyboardView
                attachListener!!.sessionId = id
            }

            id to !alreadyInProgress
        }

        if (shouldTrigger) {
            Log.d(TAG, "setupBiometricAuth: no biometric in progress, triggering")
            LogCapture.log("setup: triggering auth, view=${keyboardView.hashCode()}")
            if (BiometricGate.triggerBiometricAuth(keyboardView, encodedPassword, state, sessionId)) {
                val listener = synchronized(attachLock) { attachListener } ?: return
                keyboardView.addOnAttachStateChangeListener(listener)
                synchronized(attachLock) {
                    attachedViewRef = WeakReference(keyboardView)
                }
                Log.d(TAG, "setupBiometricAuth: listener added, view=${keyboardView.hashCode()}")
            }
        } else {
            val listener = synchronized(attachLock) { attachListener }
            if (listener != null) {
                keyboardView.addOnAttachStateChangeListener(listener)
                synchronized(attachLock) {
                    attachedViewRef = WeakReference(keyboardView)
                }
            }
            Log.d(TAG, "setupBiometricAuth: biometric in progress, skipping trigger. newView=${keyboardView.hashCode()}")
            LogCapture.log("setup: in progress, skip. newView=${keyboardView.hashCode()}")
        }
    }

    fun toggleBetweenBiometricAndKeyboard(state: AppState) {
        try {
            val keyboardView = state.session.getCurrentKeyboardView() ?: return
            val encodedPassword = state.session.getCurrentEncodedPassword() ?: return

            if (!state.fields.hasField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)) {
                BiometricGate.triggerBiometricAuth(keyboardView, encodedPassword, state, state.session.currentSessionId())
            }
        } catch (e: Throwable) {
            Log.d(TAG, "toggleBetweenBiometricAndKeyboard failed", e)
        }
    }

    fun reset() {
        PasswordAutoInput.cancelPendingRunnables()
        KeyboardCloak.reset()
        synchronized(attachLock) {
            attachListener?.let { l ->
                attachedViewRef?.get()?.removeOnAttachStateChangeListener(l)
            }
            attachListener = null
            attachedViewRef = null
        }
    }

    private fun removeListenersFromOldView() {
        synchronized(attachLock) {
            val oldView = attachedViewRef?.get()
            if (oldView != null) {
                attachListener?.let { oldView.removeOnAttachStateChangeListener(it) }
            }
            attachedViewRef = null
        }
    }

private class KeyboardAttachListener(private val state: AppState) : View.OnAttachStateChangeListener {
        private var keyboardViewRef: WeakReference<ViewGroup>? = null
        var keyboardView: ViewGroup?
            get() = keyboardViewRef?.get()
            set(value) { keyboardViewRef = value?.let(::WeakReference) }
        var sessionId: Long = 0L

        override fun onViewAttachedToWindow(view: View) {
            keyboardView?.let { kv ->
                if (!state.fields.hasField(kv.context, FieldStore.BIOMETRIC_IN_PROGRESS)) {
                    val encoded = state.prefs.getEncodedPassword()
                    if (!encoded.isNullOrEmpty()) {
                        Log.d(TAG, "onViewAttached: triggering auth, view=${kv.hashCode()}")
                        LogCapture.log("onViewAttached: triggering auth")
                        BiometricGate.triggerBiometricAuth(kv, encoded, state, sessionId)
                    }
                } else {
                    Log.d(TAG, "onViewAttached: biometric in progress, skipping, view=${kv.hashCode()}")
                }
            }
        }

        override fun onViewDetachedFromWindow(view: View) {
            val detachedView = view as? ViewGroup ?: return
            view.removeOnAttachStateChangeListener(this)
            if (state.session.getCurrentKeyboardView() !== detachedView) return

            KeyboardCloak.uncloakKeyboardViews(detachedView)
            PasswordAutoInput.cancelPendingRunnables()

            if (state.fields.hasField(detachedView.context, FieldStore.BIOMETRIC_IN_PROGRESS)) {
                // Some external payment Activities recreate the keyboard when it is hidden.
                // Keep the session alive so the authentication callback can use the new view.
                Log.d(TAG, "onViewDetached: keeping payment session during biometric auth, view=${detachedView.hashCode()}")
                LogCapture.log("onViewDetached: keeping session during auth")
                return
            }

            state.session.cancelCurrentSignal()
            Log.d(TAG, "onViewDetached: clearing payment state, view=${detachedView.hashCode()}")
            LogCapture.log("onViewDetached: clearing state")
            state.session.setInPaymentMode(false)
            state.session.setCurrentKeyboardView(null)
            state.session.setCurrentEncodedPassword(null)
            state.fields.removeField(detachedView.context, FieldStore.BIOMETRIC_IN_PROGRESS)
            state.session.endSession(sessionId)
            keyboardView = null
            sessionId = 0L
        }
    }
}
