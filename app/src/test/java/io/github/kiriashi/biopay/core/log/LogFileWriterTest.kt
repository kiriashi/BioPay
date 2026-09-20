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
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LogFileWriterTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun atomicWriteStoresExactContentWithoutTempLeftovers() {
        val file = folder.newFile("biopay_log.txt")

        LogFileWriter.writeAtomically(file, "line1\nline2\n")

        assertEquals("line1\nline2\n", file.readText())
        assertFalse(folder.root.listFiles()!!.any { it.name.endsWith(".tmp") })
    }

    @Test
    fun atomicWriteReplacesPreviousContent() {
        val file = folder.newFile("biopay_log.txt")
        file.writeText("stale")

        LogFileWriter.writeAtomically(file, "fresh")

        assertEquals("fresh", file.readText())
    }
}
