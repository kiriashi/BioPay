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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoInputTimingTest {

    @Test
    fun zeroSampleMapsToMeanDelay() {
        assertEquals(70L, AutoInputTiming.gaussianDelay(0.0))
    }

    @Test
    fun extremeSamplesClampToBounds() {
        assertEquals(60L, AutoInputTiming.gaussianDelay(-10.0))
        assertEquals(80L, AutoInputTiming.gaussianDelay(10.0))
    }

    @Test
    fun typicalSamplesStayWithinBounds() {
        for (sample in -5..5) {
            val delay = AutoInputTiming.gaussianDelay(sample.toDouble())
            assertTrue("delay=$delay for sample=$sample", delay in 60L..80L)
        }
    }
}
