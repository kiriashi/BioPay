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

/** Human-like auto-input timing. Pure JVM: no Android dependencies. */
internal object AutoInputTiming {

    const val GAUSSIAN_MEAN_MS = 70.0
    const val GAUSSIAN_STDDEV = 3.33
    const val GAUSSIAN_MIN_MS = 60L
    const val GAUSSIAN_MAX_MS = 80L

    fun gaussianDelay(sample: Double): Long =
        (sample * GAUSSIAN_STDDEV + GAUSSIAN_MEAN_MS).toLong().coerceIn(GAUSSIAN_MIN_MS, GAUSSIAN_MAX_MS)
}
