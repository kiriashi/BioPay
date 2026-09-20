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
package io.github.kiriashi.biopay.hook

import io.github.kiriashi.biopay.core.log.LOG_TAG
import io.github.kiriashi.biopay.core.log.LogCapture
import io.github.kiriashi.biopay.lifecycle.AppState
import io.github.kiriashi.biopay.payment.BiometricPaymentController
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import io.github.libxposed.api.XposedInterface

object KeyboardWindowHook {
    private val TAG = LOG_TAG

    const val HOOK_ID = "bp_keyboard_window"
    fun register(cl: ClassLoader, xposed: XposedInterface, state: AppState) {
        try {
            val clazz = cl.loadClass(HookTargets.MyKeyboardWindow)
            val method = clazz.getDeclaredMethod("setInputEditText", EditText::class.java)
            xposed.hook(method).setId(HOOK_ID).intercept(makeInterceptor(state))
        } catch (e: Throwable) {
            Log.w(TAG, "register failed", e)
        }
    }
    fun makeInterceptor(state: AppState): XposedInterface.Hooker {
        return XposedInterface.Hooker { chain ->
            try {
                if (!state.prefs.isBioPayEnabled()) {
                    return@Hooker chain.proceed()
                }

                val encodedPassword = state.prefs.getEncodedPassword()
                if (encodedPassword.isNullOrEmpty()) {
                    return@Hooker chain.proceed()
                }

                val inputEditText = chain.args[0] as? EditText
                if (inputEditText != null) {
                    state.session.setInputEditText(inputEditText)
                }

                val keyboardView = chain.thisObject as? ViewGroup
                if (keyboardView != null) {
                    Log.d(TAG, "setInputEditText intercepted, view=${keyboardView.hashCode()}, biometricInProgress=${state.fields.hasField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)}")
                    LogCapture.log("setInputEditText: view=${keyboardView.hashCode()}, inProgress=${state.fields.hasField(keyboardView.context, FieldStore.BIOMETRIC_IN_PROGRESS)}")
                    BiometricPaymentController.setupBiometricAuth(keyboardView, encodedPassword, state)
                }
                chain.proceed()
            } catch (e: Throwable) {
                Log.w(TAG, "keyboardWindow interceptor failed", e)
                chain.proceed()
            }
        }
    }
}
