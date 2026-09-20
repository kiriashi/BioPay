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
import io.github.kiriashi.biopay.hook.HookTargets
import io.github.kiriashi.biopay.lifecycle.AppState
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.concurrent.ThreadLocalRandom

object PasswordAutoInput {

    private val TAG = LOG_TAG
    private val handler = Handler(Looper.getMainLooper())
    private val pendingRunnablesLock = Any()
    private val pendingRunnables = mutableListOf<Runnable>()
    private val resourceCache = ResourceIdCache()
    fun autoInputPassword(
        keyboardView: ViewGroup,
        passwordChars: CharArray,
        state: AppState,
        sessionId: Long
    ) {
        val currentApp = state.app ?: return
        cancelPendingRunnables()

        val keyboardPrefix = HookTargets.tenpayKeyboard
        for (digit in 0..9) {
            resourceCache.getOrCreateResourceId(currentApp, keyboardPrefix + digit)
        }
        val resourceNames = Array(passwordChars.size) { i -> keyboardPrefix + passwordChars[i] }

        var cumulativeDelay = 0L
        val totalDigits = passwordChars.size

        for (i in passwordChars.indices) {
            val resName = resourceNames[i]
            val delay = getGaussianDelay()
            cumulativeDelay += delay
            val isLast = i == totalDigits - 1

            val runnable = Runnable {
                try {
                    if (!state.session.isCurrentSession(sessionId)) return@Runnable
                    val resId = resourceCache.getOrCreateResourceId(currentApp, resName)
                    if (resId != 0) {
                        val keyView = keyboardView.findViewById<View>(resId)
                        if (keyView != null) {
                            dispatchFakeTouch(keyView)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "autoInput digit failed", e)
                } finally {
                    if (isLast) {
                        synchronized(pendingRunnablesLock) {
                            pendingRunnables.clear()
                        }
                        if (state.session.isCurrentSession(sessionId)) state.session.setInputEditText(null)
                        handler.postDelayed({
                            if (state.session.isCurrentSession(sessionId)) {
                                KeyboardCloak.restoreConcealedInputViews(animated = true)
                            }
                        }, 250L)
                    }
                }
            }
            synchronized(pendingRunnablesLock) {
                pendingRunnables.add(runnable)
            }
            handler.postDelayed(runnable, cumulativeDelay)
        }
    }
    fun cancelPendingRunnables() {
        synchronized(pendingRunnablesLock) {
            for (r in pendingRunnables) {
                handler.removeCallbacks(r)
            }
            pendingRunnables.clear()
        }
    }
    private fun getGaussianDelay(): Long =
        AutoInputTiming.gaussianDelay(ThreadLocalRandom.current().nextGaussian())
    private fun dispatchFakeTouch(view: View) {
        val random = ThreadLocalRandom.current()
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        val x = random.nextInt(width).toFloat()
        val y = random.nextInt(height).toFloat()
        val downTime = SystemClock.uptimeMillis()

        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(down)
        down.recycle()

        val upTime = downTime + random.nextLong(2, 6)
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(up)
        up.recycle()
    }

    private class ResourceIdCache {

        private val lock = Any()
        private val cache = object : LinkedHashMap<String, Int>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?): Boolean {
                return size > 100
            }
        }
        @Volatile private var packageNamePrefix: String = ""

        fun getOrCreateResourceId(app: Application, name: String): Int {
            if (packageNamePrefix.isEmpty()) {
                synchronized(lock) {
                    if (packageNamePrefix.isEmpty()) {
                        packageNamePrefix = "${app.packageName}:id:"
                    }
                }
            }
            val key = packageNamePrefix + name
            synchronized(lock) {
                cache[key]?.let { return it }
                val id = app.resources.getIdentifier(name, "id", app.packageName)
                cache[key] = id
                return id
            }
        }
    }
}
