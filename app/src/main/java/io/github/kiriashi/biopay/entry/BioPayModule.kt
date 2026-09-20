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

package io.github.kiriashi.biopay.entry

import io.github.kiriashi.biopay.core.log.LOG_TAG
import io.github.kiriashi.biopay.core.log.LogCapture
import io.github.kiriashi.biopay.hook.HookManager
import io.github.kiriashi.biopay.payment.BiometricPaymentController
import android.app.Application
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class BioPayModule : XposedModule() {

    private val wiring = AppWiring()
    private val initLock = Any()
    @Volatile private var initializedApplication: Application? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        wiring.onModuleLoaded()
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.tencent.mm") {
            return
        }

        if (!param.isFirstPackage) {
            return
        }

        val processName = Application.getProcessName()
        Log.d(LOG_TAG, "main package loaded, isFirstPkg=${param.isFirstPackage}, process=$processName")

        hookApplicationOnCreate()
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean = true

    override fun onHotReloaded(param: HotReloadedParam) {
        val app = try {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication")
                .invoke(null) as? Application
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "hot reload: failed to get Application via ActivityThread", e)
            null
        }

        if (app != null) {
            wiring.destroy()
            val state = wiring.init(app)
            HookManager.replaceHooksFromOldGeneration(param.oldHookHandles, state)
            BiometricPaymentController.reset()
            Log.d(LOG_TAG, "hot reload: hooks replaced successfully")
        } else {
            Log.w(LOG_TAG, "hot reload: no Application available, hooks not replaced")
        }
    }

    private fun hookApplicationOnCreate() {
        try {
            val module = this@BioPayModule
            val method = Application::class.java.getDeclaredMethod("onCreate")
            hook(method).setId("bp_app_oncreate").intercept { chain ->
                try {
                    val application = chain.thisObject as? Application
                    if (application != null) {
                        val processName = Application.getProcessName()
                        Log.d(LOG_TAG, "Application.onCreate, process=$processName")
                        if (processName == "com.tencent.mm") {
                            val shouldInitialize = synchronized(initLock) {
                                if (initializedApplication === application) {
                                    false
                                } else {
                                    initializedApplication = application
                                    true
                                }
                            }
                            if (shouldInitialize) {
                                val state = wiring.init(application)
                                HookManager.init(application.classLoader, module, state)
                                application.registerActivityLifecycleCallbacks(AppLifecycleCallbacks(state))
                                if (state.prefs.isLogCaptureEnabled()) {
                                    LogCapture.start(application)
                                }
                            }
                        } else {
                            Log.d(LOG_TAG, "skipping non-main process: $processName")
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(LOG_TAG, "init failed", e)
                }
                chain.proceed()
            }
            Log.d(LOG_TAG, "hookApplicationOnCreate registered")
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "hook onCreate failed", e)
        }
    }
}
