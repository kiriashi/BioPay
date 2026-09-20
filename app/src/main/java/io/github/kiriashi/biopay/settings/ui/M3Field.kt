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

package io.github.kiriashi.biopay.settings.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.annotation.SuppressLint
import android.view.Gravity
import android.widget.EditText

@SuppressLint("ViewConstructor")
class M3Field(context: Context, private val t: ThemeColors) : EditText(context) {

    private val d = context.resources.displayMetrics.density
    private val r = 10f * d
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = makeStrokePaint(1.5f * d)
    private val focusPaint = makeStrokePaint(2f * d)
    private val disPaint = makeStrokePaint(1.5f * d)
    private val path = Path()
    private val rect = RectF()
    private val fullRect = RectF()

    init {
        textSize = 15f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
        val v = (12 * d).toInt()
        setPadding((16 * d).toInt(), v, (16 * d).toInt(), v)
        setTextColor(t.onSurface)
        setHintTextColor(t.onSurfaceVariant)
        background = null
        isFocusable = true; isFocusableInTouchMode = true
        setOnFocusChangeListener { _, f -> isActivated = f; updatePaintColors(); invalidate() }
        setOnLongClickListener { true }
        updatePaintColors()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        fullRect.set(0f, 0f, w.toFloat(), h.toFloat())
        val inset = 1f * d
        rect.set(inset, inset, w - inset, h - inset)
        path.reset()
        path.addRoundRect(rect, r, r, Path.Direction.CW)
    }

    private fun makeStrokePaint(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = width
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    private fun updatePaintColors() {
        fillPaint.color = t.surfaceContainerLow
        strokePaint.color = t.outline
        focusPaint.color = t.primary
        disPaint.color = t.surfaceContainerHighest
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(fullRect, r, r, fillPaint)
        canvas.drawPath(path, when {
            !isEnabled -> disPaint; isActivated -> focusPaint; else -> strokePaint
        })
        super.onDraw(canvas)
    }
}
