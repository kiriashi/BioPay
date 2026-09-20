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

package io.github.kiriashi.biopay.data.prefs

import io.github.kiriashi.biopay.core.codec.XorCodec

/** Obfuscated SharedPreferences and keystore identifiers. */
object PrefKeys {

    private val prefNameE = intArrayOf(56, 76, 33, 126, 249, 44)
    private val prefKeyOnE = intArrayOf(56, 76, 33, 114, 241)
    private val prefKeyPwdE = intArrayOf(56, 76, 33, 109, 232, 47)
    private val keystoreAliasE = intArrayOf(56, 76, 33, 118, 175)

    val prefName by lazy { XorCodec.decode(prefNameE) }
    val prefKeyOn by lazy { XorCodec.decode(prefKeyOnE) }
    val prefKeyPwd by lazy { XorCodec.decode(prefKeyPwdE) }
    val keystoreAlias by lazy { XorCodec.decode(keystoreAliasE) }
}
