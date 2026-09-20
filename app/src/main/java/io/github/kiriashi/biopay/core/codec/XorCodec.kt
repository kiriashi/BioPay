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

/** Single-byte XOR codec used to keep hook targets out of plain strings. */
object XorCodec {

    private val K = intArrayOf(0x5A, 0x3C, 0x7E, 0x1D, 0x9F, 0x4B, 0xA2, 0x68)

    fun decode(encoded: IntArray): String {
        val decoded = ByteArray(encoded.size)
        for (i in encoded.indices) decoded[i] = (encoded[i] xor K[i % K.size]).toByte()
        return String(decoded, Charsets.UTF_8)
    }
}
