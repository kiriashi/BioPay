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

package io.github.kiriashi.biopay.core.codec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class XorCodecTest {

    @Test
    fun decodeEmptyArrayYieldsEmptyString() {
        assertEquals("", XorCodec.decode(intArrayOf()))
    }

    @Test
    fun decodeIsDeterministic() {
        val encoded = intArrayOf(57, 83, 19, 51, 235)

        assertEquals(XorCodec.decode(encoded), XorCodec.decode(encoded))
    }

    @Test
    fun decodeDistinguishesInputs() {
        assertNotEquals(
            XorCodec.decode(intArrayOf(57, 83, 19)),
            XorCodec.decode(intArrayOf(57, 83, 20))
        )
    }
}
