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

import java.util.concurrent.atomic.AtomicLong

internal class SessionToken {
    private val nextId = AtomicLong(0)
    @Volatile private var currentId = 0L

    fun begin(): Long {
        val id = nextId.incrementAndGet()
        currentId = id
        return id
    }

    fun current(): Long = currentId

    fun isCurrent(id: Long): Boolean = id != 0L && currentId == id

    fun invalidate() {
        currentId = 0L
    }
}
