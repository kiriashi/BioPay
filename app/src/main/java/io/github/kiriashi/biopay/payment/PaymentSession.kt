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

import android.app.Activity
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.EditText
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class PaymentSession {

    private val isInPaymentMode = AtomicBoolean(false)
    private val sessionToken = SessionToken()

    @Volatile
    private var cancelSignal: CancellationSignal? = null
    private val signalLock = Any()

    @Volatile
    private var currentKeyboardViewRef: WeakReference<ViewGroup>? = null
    @Volatile
    private var currentEncodedPassword: String? = null
    @Volatile
    private var inputEditTextRef: WeakReference<EditText>? = null

    @Volatile
    private var lastKeyboardAccessTime: Long = 0

    private val cleanupHandler = Handler(Looper.getMainLooper())
    private val cleanupRunning = AtomicBoolean(false)
    private val cleanupRunnable = object : Runnable {
        override fun run() {
            if (!cleanupRunning.get()) return
            cleanupExpiredReferences()
            if (cleanupRunning.get()) cleanupHandler.postDelayed(this, 60_000L)
        }
    }

    fun startCleanup() {
        if (cleanupRunning.compareAndSet(false, true)) {
            cleanupHandler.postDelayed(cleanupRunnable, 60_000L)
        }
    }

    fun stopCleanup() {
        cleanupRunning.set(false)
        cleanupHandler.removeCallbacks(cleanupRunnable)
    }

    fun isInPaymentMode(): Boolean = isInPaymentMode.get()

    fun beginSession(): Long {
        cancelCurrentSignal()
        return sessionToken.begin()
    }

    fun isCurrentSession(id: Long): Boolean = sessionToken.isCurrent(id)

    fun currentSessionId(): Long = sessionToken.current()

    fun endSession(id: Long) {
        if (isCurrentSession(id)) destroy()
    }

    fun endSessionForActivity(activity: Activity) {
        val view = currentKeyboardViewRef?.get()
        if (view?.context === activity) destroy()
    }

    fun setInPaymentMode(value: Boolean) {
        isInPaymentMode.set(value)
    }

    fun setCurrentKeyboardView(view: ViewGroup?) {
        currentKeyboardViewRef = if (view != null) WeakReference(view) else null
        lastKeyboardAccessTime = SystemClock.uptimeMillis()
    }

    fun getCurrentKeyboardView(): ViewGroup? {
        lastKeyboardAccessTime = SystemClock.uptimeMillis()
        return currentKeyboardViewRef?.get()
    }

    fun setCurrentEncodedPassword(password: String?) {
        currentEncodedPassword = password
    }

    fun getCurrentEncodedPassword(): String? {
        return currentEncodedPassword
    }

    fun setInputEditText(editText: EditText?) {
        inputEditTextRef = if (editText != null) WeakReference(editText) else null
    }

    fun getInputEditText(): EditText? {
        return inputEditTextRef?.get()
    }

    fun createNewSignal(): CancellationSignal {
        synchronized(signalLock) {
            cancelSignal?.cancel()
            cancelSignal = CancellationSignal()
            return cancelSignal!!
        }
    }

    fun cancelCurrentSignal() {
        synchronized(signalLock) {
            val signal = cancelSignal
            if (signal != null && !signal.isCanceled) {
                signal.cancel()
                cancelSignal = null
            }
        }
    }

    fun cleanupExpiredReferences() {
        val now = SystemClock.uptimeMillis()

        if (currentKeyboardViewRef?.get() == null) {
            currentKeyboardViewRef = null
            inputEditTextRef = null
            currentEncodedPassword = null
            isInPaymentMode.set(false)
            cancelCurrentSignal()
        } else if (now - lastKeyboardAccessTime > 30_000) {
            destroy()
        }

        if (inputEditTextRef?.get() == null) {
            inputEditTextRef = null
        }
    }

    fun destroy() {
        stopCleanup()
        sessionToken.invalidate()
        isInPaymentMode.set(false)
        currentKeyboardViewRef = null
        inputEditTextRef = null
        currentEncodedPassword = null
        synchronized(signalLock) {
            cancelSignal?.cancel()
            cancelSignal = null
        }
    }
}
