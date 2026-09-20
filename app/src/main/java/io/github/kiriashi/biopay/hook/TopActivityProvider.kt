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
import android.app.Activity
import android.util.Log

object TopActivityProvider {
    private val TAG = LOG_TAG

    private var kindaContextClass: Class<*>? = null
    @Volatile private var getTopActivityMethod: java.lang.reflect.Method? = null

    fun resolve(classLoader: ClassLoader) {
        try {
            kindaContextClass = classLoader.loadClass(HookTargets.KindaContext)
        } catch (e: Throwable) {
            Log.w(TAG, "loadClass failed", e)
        }
    }

    fun resolveFromHandles(oldHandles: List<io.github.libxposed.api.XposedInterface.HookHandle>) {
        for (handle in oldHandles) {
            try {
                val cl = handle.executable.declaringClass.classLoader ?: continue
                kindaContextClass = cl.loadClass(HookTargets.KindaContext)
                break
            } catch (_: Throwable) {}
        }
    }

    fun reset() {
        getTopActivityMethod = null
    }

    fun getTopActivity(): Activity? {
        val clazz = kindaContextClass ?: return null
        return try {
            if (getTopActivityMethod == null) {
                synchronized(this) {
                    if (getTopActivityMethod == null) {
                        getTopActivityMethod = clazz.getDeclaredMethod("getTopActivity")
                    }
                }
            }
            val result = getTopActivityMethod!!.invoke(null)
            if (result is Activity) result else null
        } catch (e: Throwable) {
            Log.w(TAG, "getTopActivity failed", e)
            null
        }
    }
}
