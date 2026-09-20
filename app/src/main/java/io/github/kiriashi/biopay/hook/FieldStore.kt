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

package io.github.kiriashi.biopay.hook

import io.github.kiriashi.biopay.core.log.LOG_TAG
import android.app.Activity
import android.util.Log
import java.util.WeakHashMap

class FieldStore {

    companion object {
        const val SETTINGS_DIALOG = "a"
        const val BIOMETRIC_IN_PROGRESS = "c"
    }

    private val fields: MutableMap<Any, HashMap<String, Any>> = WeakHashMap()

    fun hasField(obj: Any, name: String): Boolean {
        synchronized(fields) {
            return fields[obj]?.containsKey(name) == true
        }
    }

    fun compareAndSetField(obj: Any, name: String, expected: Boolean, newValue: Any): Boolean {
        synchronized(fields) {
            val map = fields.getOrPut(obj) { HashMap() }
            val exists = map.containsKey(name)
            if (exists == expected) {
                map[name] = newValue
                return true
            }
            return false
        }
    }

    fun setField(obj: Any, name: String, value: Any) {
        synchronized(fields) {
            fields.getOrPut(obj) { HashMap() }[name] = value
        }
    }

    fun removeField(obj: Any, name: String) {
        synchronized(fields) {
            fields[obj]?.remove(name)
        }
    }

    fun clearFieldsForActivity(activity: Activity) {
        synchronized(fields) {
            val map = fields[activity] ?: return
            Log.d(LOG_TAG, "clearFieldsForActivity: keys=${map.keys.toList()}")
            map.remove(SETTINGS_DIALOG)
        }
    }

    fun cleanupActivity(activity: Activity) {
        synchronized(fields) {
            fields.remove(activity)
        }
    }
}
