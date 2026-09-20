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
package io.github.kiriashi.biopay.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View

class DialogHost(context: Context) {
        private val contextRef = java.lang.ref.WeakReference(context)
        private var dialog: AlertDialog? = null
        var onDismiss: (() -> Unit)? = null
        private val ctx get() = contextRef.get()
        fun show(content: View) {
            val c = ctx ?: return
            if (c is Activity && (c.isFinishing || c.isDestroyed)) return
            dialog = AlertDialog.Builder(c).setView(content).setCancelable(false).create()
            dialog?.setOnDismissListener { onDismiss?.invoke() }
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog?.show()
        }
        fun dismiss() {
            val c = ctx ?: return
            if (c is Activity && (c.isFinishing || c.isDestroyed)) return
            dialog?.let { if (it.isShowing) it.dismiss() }
        }
    }
