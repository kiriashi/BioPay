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

/**
 * Bounded double-buffer for log lines. Pure JVM: no Android dependencies.
 *
 * Callers must synchronize externally; generation counting lets the writer
 * skip clearing a buffer that received new lines while it was flushed.
 */
class LogRingBuffer(
    private val maxBufferSize: Int = 64 * 1024,
    private val flushThreshold: Int = maxBufferSize * 3 / 4
) {

    private val bufferA = StringBuilder()
    private val bufferB = StringBuilder()
    private var activeBuffer = bufferA
    var dirtyGeneration = 0
        private set

    fun append(line: String) {
        if (activeBuffer.length > maxBufferSize) {
            val keepFrom = activeBuffer.indexOf("\n", flushThreshold)
            if (keepFrom > 0) {
                activeBuffer.delete(0, keepFrom + 1)
            } else {
                activeBuffer.clear()
            }
        }
        activeBuffer.append(line).append("\n")
        dirtyGeneration++
    }

    fun appendRaw(text: String) {
        activeBuffer.append(text)
        dirtyGeneration++
    }

    /** Swaps the buffers and returns the drained content with its generation. */
    fun swap(): Pair<String, Int> {
        if (activeBuffer.isEmpty()) return "" to dirtyGeneration
        activeBuffer = if (activeBuffer === bufferA) bufferB else bufferA
        val flushing = if (activeBuffer === bufferA) bufferB else bufferA
        return flushing.toString() to dirtyGeneration
    }

    /** Clears the drained buffer only if nothing was appended while flushing. */
    fun clearFlushed(generation: Int) {
        if (dirtyGeneration != generation) return
        if (activeBuffer === bufferA) bufferB.clear() else bufferA.clear()
    }

    fun snapshot(): String = bufferA.toString() + bufferB.toString()

    fun clear() {
        bufferA.clear()
        bufferB.clear()
        activeBuffer = bufferA
    }
}
