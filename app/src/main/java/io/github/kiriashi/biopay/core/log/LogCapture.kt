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

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import io.github.kiriashi.biopay.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object LogCapture {

    private val ring = LogRingBuffer()
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var bgThread: HandlerThread? = null
    private var bgHandler: Handler? = null
    private val flushInterval = 5000L
    @Volatile private var running = false
    @Volatile private var outputDir: File? = null
    private val timeFormatter = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    private val headerFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    private val flushRunnable = object : Runnable {
        override fun run() {
            bgHandler?.post { flushToDisk() }
            if (running) handler.postDelayed(this, flushInterval)
        }
    }

    fun start(context: Context) {
        if (!BuildConfig.DEBUG) return
        synchronized(lock) {
            if (running) return
            if (bgThread == null) {
                bgThread = HandlerThread("LogCapture").apply { start() }
                bgHandler = Handler(bgThread!!.looper)
            }
            ring.clear()
            outputDir = File(context.filesDir, "BioPay")
            ring.appendRaw(
                LogFormat.header(
                    device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    androidRelease = android.os.Build.VERSION.RELEASE,
                    apiLevel = android.os.Build.VERSION.SDK_INT,
                    startTime = headerFormatter.get()!!.format(System.currentTimeMillis())
                )
            )
            running = true
            handler.postDelayed(flushRunnable, flushInterval)
            Log.d(LOG_TAG, "LogCapture started")
        }
    }

    fun stop(context: Context): String? {
        if (!BuildConfig.DEBUG) return null
        var flushLatch: CountDownLatch? = null
        var threadToStop: HandlerThread? = null
        synchronized(lock) {
            if (!running) return null
            running = false
            handler.removeCallbacks(flushRunnable)
            ring.appendRaw(LogFormat.footer(headerFormatter.get()!!.format(System.currentTimeMillis())))
            bgHandler?.let { writer ->
                val latch = CountDownLatch(1)
                flushLatch = latch
                writer.post {
                    try {
                        flushToDisk()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            threadToStop = bgThread
        }

        flushLatch?.await(2, TimeUnit.SECONDS)

        synchronized(lock) {
            val path = saveToAppFiles(context)
            ring.clear()
            threadToStop?.quitSafely()
            bgThread = null
            bgHandler = null
            outputDir = null
            Log.d(LOG_TAG, "LogCapture saved to: $path")
            return path
        }
    }

    internal fun log(msg: String) {
        if (!BuildConfig.DEBUG) return
        val time = timeFormatter.get()!!.format(System.currentTimeMillis())
        synchronized(lock) {
            if (!running) return
            ring.append("$time $msg")
        }
    }

    private fun getLogDir(context: Context): File {
        val dir = File(context.filesDir, "BioPay")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun flushToDisk() {
        val (content, generation) = synchronized(lock) {
            ring.swap()
        }
        if (content.isEmpty()) return
        try {
            val dir = outputDir ?: return
            if (!dir.exists()) dir.mkdirs()
            LogFileWriter.writeAtomically(File(dir, "biopay_log.txt"), content)
            synchronized(lock) {
                ring.clearFlushed(generation)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to flush log to disk", e)
            synchronized(lock) {
                ring.clearFlushed(generation)
            }
        }
    }

    private fun saveToAppFiles(context: Context): String? {
        return try {
            val dir = getLogDir(context)
            val file = File(dir, LogFormat.reportFileName(headerFormatter.get()!!.format(System.currentTimeMillis())))
            val content = synchronized(lock) {
                ring.snapshot()
            }
            LogFileWriter.writeAtomically(file, content)
            file.absolutePath
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to save log", e)
            null
        }
    }
}
