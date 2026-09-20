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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTokenTest {
    @Test
    fun beginningSessionInvalidatesPreviousSession() {
        val token = SessionToken()
        val first = token.begin()
        val second = token.begin()

        assertNotEquals(first, second)
        assertFalse(token.isCurrent(first))
        assertTrue(token.isCurrent(second))
    }

    @Test
    fun invalidatingSessionRejectsCallbacks() {
        val token = SessionToken()
        val session = token.begin()

        token.invalidate()

        assertFalse(token.isCurrent(session))
        assertFalse(token.isCurrent(0L))
    }
}
