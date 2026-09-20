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

package io.github.kiriashi.biopay.lifecycle

import io.github.kiriashi.biopay.data.prefs.PreferencesStore
import io.github.kiriashi.biopay.hook.FieldStore
import io.github.kiriashi.biopay.payment.PaymentSession
import android.app.Application

/** Transparent composition of per-app collaborators. Constructed once in entry wiring. */
class AppState(
    val app: Application,
    val prefs: PreferencesStore,
    val session: PaymentSession,
    val fields: FieldStore
)
