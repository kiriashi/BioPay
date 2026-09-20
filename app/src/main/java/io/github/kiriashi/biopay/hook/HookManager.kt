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
import io.github.kiriashi.biopay.lifecycle.AppState
import android.util.Log
import io.github.libxposed.api.XposedInterface

object HookManager {

    private val TAG = LOG_TAG

    fun init(classLoader: ClassLoader, xposed: XposedInterface, state: AppState) {
        Log.d(TAG, "HookManager.init classLoader=${classLoader.javaClass.name}@${Integer.toHexString(classLoader.hashCode())}")
        TopActivityProvider.resolve(classLoader)
        PullDownHook.register(classLoader, xposed, state)
        KeyboardWindowHook.register(classLoader, xposed, state)
        VolumeKeyHook.register(xposed, state)
    }

    fun replaceHooksFromOldGeneration(oldHandles: List<XposedInterface.HookHandle>, state: AppState) {
        TopActivityProvider.resolveFromHandles(oldHandles)
        for (handle in oldHandles) {
            when (handle.id) {
                "bp_app_oncreate" -> handle.unhook()
                PullDownHook.HOOK_ID -> handle.replaceHook(PullDownHook.makeInterceptor(state))
                KeyboardWindowHook.HOOK_ID -> handle.replaceHook(KeyboardWindowHook.makeInterceptor(state))
                VolumeKeyHook.HOOK_ID -> handle.replaceHook(VolumeKeyHook.makeInterceptor(state))
            }
        }
        Log.d(TAG, "replaceHooksFromOldGeneration: ${oldHandles.size} handles processed")
    }
}
