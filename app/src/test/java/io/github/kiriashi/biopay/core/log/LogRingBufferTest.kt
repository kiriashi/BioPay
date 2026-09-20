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

package io.github.kiriashi.biopay.core.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRingBufferTest {

    @Test
    fun swapDrainsAppendedContent() {
        val ring = LogRingBuffer()
        ring.append("a")
        ring.append("b")

        val (content, _) = ring.swap()

        assertEquals("a\nb\n", content)
    }

    @Test
    fun swapOnEmptyBufferReturnsEmpty() {
        val ring = LogRingBuffer()

        val (content, _) = ring.swap()

        assertEquals("", content)
    }

    @Test
    fun overCapacityTrimsOldestLinesKeepingLineBoundary() {
        val ring = LogRingBuffer(maxBufferSize = 17, flushThreshold = 10)
        for (i in 0..5) ring.append("l$i")

        ring.append("l6")

        assertEquals("l4\nl5\nl6\n", ring.snapshot())
    }

    @Test
    fun trimWithoutNewlineClearsBuffer() {
        val ring = LogRingBuffer(maxBufferSize = 10, flushThreshold = 5)
        ring.append("0123456789abcdef")

        ring.append("next")

        assertEquals("next\n", ring.snapshot())
    }

    @Test
    fun clearFlushedSkipsBufferThatReceivedNewLines() {
        val ring = LogRingBuffer()
        ring.append("old")
        val (_, generation) = ring.swap()
        ring.append("new")

        ring.clearFlushed(generation)

        assertTrue(ring.snapshot().contains("new\n"))
    }

    @Test
    fun clearFlushedDrainsQuietBuffer() {
        val ring = LogRingBuffer()
        ring.append("old")
        val (_, generation) = ring.swap()

        ring.clearFlushed(generation)

        assertEquals("", ring.snapshot())
    }

    @Test
    fun clearResetsToEmpty() {
        val ring = LogRingBuffer()
        ring.append("a")
        ring.swap()

        ring.clear()

        assertEquals("", ring.snapshot())
    }
}
