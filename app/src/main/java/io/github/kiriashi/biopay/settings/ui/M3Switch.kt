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

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.OvershootInterpolator

@SuppressLint("ViewConstructor")
class M3Switch(context: Context, private val t: ThemeColors) : View(context) {

    var isChecked = false
        set(value) {
            if (field != value) {
                field = value
                animateTo()
                onCheckedChangeListener?.invoke(value)
            }
        }
    var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    private val d = context.resources.displayMetrics.density
    private val trackW = 52f * d
    private val trackH = 32f * d
    private val cornerR = 16f * d
    private val thumbD = 28f * d
    private val thumbR = thumbD / 2f
    private val trackPad = 2f * d

    private var thumbX = 0f
    private var trackColor = t.switchOffBg
    private var animator: ValueAnimator? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val trackRect = RectF()

    init { setOnClickListener { isChecked = !isChecked } }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(trackW.toInt(), trackH.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        trackRect.set(0f, 0f, trackW, trackH)
        thumbX = if (isChecked) trackW - trackPad - thumbR else trackPad + thumbR
        trackColor = if (isChecked) t.switchOn else t.switchOffBg
    }

    private val argbEvaluator = ArgbEvaluator()

    private fun animateTo() {
        animator?.cancel()
        val fromX = thumbX
        val toX = if (isChecked) trackW - trackPad - thumbR else trackPad + thumbR
        val fromTrack = trackColor
        val toTrack = if (isChecked) t.switchOn else t.switchOffBg

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { a ->
                val f = a.animatedFraction
                thumbX = fromX + (toX - fromX) * f
                trackColor = argbEvaluator.evaluate(f, fromTrack, toTrack) as Int
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(trackRect, cornerR, cornerR, trackPaint.apply { color = trackColor })
        val cy = trackH / 2f
        canvas.drawCircle(thumbX, cy, thumbR, thumbPaint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
