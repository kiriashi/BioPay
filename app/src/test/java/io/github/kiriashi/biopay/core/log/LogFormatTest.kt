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

class LogFormatTest {

    @Test
    fun headerContainsMarkersAndDeviceLine() {
        val header = LogFormat.header(
            device = "Maker Model",
            androidRelease = "14",
            apiLevel = 34,
            startTime = "2026-09-20 13:00:00"
        )

        assertTrue(header.startsWith("=== BioPay Log Capture ===\n"))
        assertTrue(header.contains("Device: Maker Model\n"))
        assertTrue(header.contains("Android: 14 (API 34)\n"))
        assertTrue(header.contains("Start: 2026-09-20 13:00:00\n"))
    }

    @Test
    fun footerContainsEndMarker() {
        val footer = LogFormat.footer("2026-09-20 13:05:00")

        assertTrue(footer.contains("End: 2026-09-20 13:05:00\n"))
        assertTrue(footer.trimEnd().endsWith("=========================="))
    }

    @Test
    fun reportFileNameSanitizesTimestamp() {
        assertEquals(
            "biopay_log_20260920_133339.txt",
            LogFormat.reportFileName("2026-09-20 13:33:39")
        )
    }
}
