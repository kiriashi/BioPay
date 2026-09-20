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
import android.app.Activity
import android.util.Log
import android.view.KeyEvent
import io.github.libxposed.api.XposedInterface

object VolumeKeyHook {
    private val TAG = LOG_TAG

    const val HOOK_ID = "bp_volume_key"
    fun register(xposed: XposedInterface, state: AppState) {
        try {
            val method = Activity::class.java.getDeclaredMethod(
                "dispatchKeyEvent", KeyEvent::class.java
            )
            xposed.hook(method).setId(HOOK_ID).intercept(makeInterceptor(state))
        } catch (e: Throwable) {
            Log.w(TAG, "register failed", e)
        }
    }
    fun makeInterceptor(state: AppState): XposedInterface.Hooker {
        return XposedInterface.Hooker { chain ->
            try {
                if (state.session.isInPaymentMode()) {
                    val event = chain.args[0] as? KeyEvent
                    if (event?.action == KeyEvent.ACTION_DOWN) {
                        val keyCode = event.keyCode
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            Log.d(TAG, "volume key intercepted: $keyCode, triggering toggle")
                            LogCapture.log("volume key: $keyCode")
                            BiometricPaymentController.toggleBetweenBiometricAndKeyboard(state)
                            return@Hooker true
                        }
                    }
                }
                chain.proceed()
            } catch (e: Throwable) {
                Log.w(TAG, "volumeKey interceptor failed", e)
                chain.proceed()
            }
        }
    }
}
