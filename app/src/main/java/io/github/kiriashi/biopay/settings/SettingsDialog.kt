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

package io.github.kiriashi.biopay.settings

import io.github.kiriashi.biopay.BuildConfig
import io.github.kiriashi.biopay.core.util.dp
import io.github.kiriashi.biopay.core.util.isValidActivity
import io.github.kiriashi.biopay.hook.FieldStore
import io.github.kiriashi.biopay.lifecycle.AppState
import io.github.kiriashi.biopay.payment.BiometricType
import io.github.kiriashi.biopay.settings.ui.M3Field
import io.github.kiriashi.biopay.settings.ui.M3Switch
import io.github.kiriashi.biopay.settings.ui.Theme
import io.github.kiriashi.biopay.settings.ui.ThemeColors
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object SettingsDialog {

    fun show(context: Context, state: AppState) {
        if (!context.isValidActivity()) return
        val activity = context as? Activity ?: return
        val dialogHost = DialogHost(context)
        val layout = createDialogContent(context, dialogHost, state)
        dialogHost.onDismiss = { state.fields.removeField(activity, FieldStore.SETTINGS_DIALOG) }
        dialogHost.show(layout)
    }

    private fun createDialogContent(context: Context, dialogHost: DialogHost, state: AppState): LinearLayout {
        val t = Theme.colors(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(24), context.dp(24), context.dp(24), context.dp(24))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(t.surface)
                cornerRadius = context.dp(28).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN && v !is EditText) {
                    v.clearFocus()
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(v.windowToken, 0)
                }
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
                false
            }
        }

        root.addView(TextView(context).apply {
            text = "模块设置"
            textSize = 24f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(t.onSurface)
            gravity = Gravity.CENTER
            setPadding(0, context.dp(8), 0, context.dp(24))
        })

        val bioCard = makeCard(context, t)
        bioCard.addView(makeSectionLabel(context, "生物支付", t))

        val currentType = state.prefs.getBiometricType()
        val fpOn = currentType == BiometricType.FINGERPRINT || currentType == BiometricType.BOTH
        val faceOn = currentType == BiometricType.FACE || currentType == BiometricType.BOTH

        val toggleFp = M3Switch(context, t).apply { isChecked = fpOn }
        val toggleFace = M3Switch(context, t).apply { isChecked = faceOn }

        bioCard.addView(makeSwitchRow(context, "启用指纹支付", "请确保设备已开启指纹解锁", toggleFp, t))
        bioCard.addView(makeDivider(context, t))
        bioCard.addView(makeSwitchRow(context, "启用面容支付", "请确保设备已开启面容解锁", toggleFace, t))
        root.addView(bioCard)

        val pwdCard = makeCard(context, t)
        pwdCard.addView(makeSectionLabel(context, "支付密码", t))
        val pwdInput = M3Field(context, t).apply {
            hint = "未设置密码"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(6))
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        SettingsController.loadSavedPassword(pwdInput, state)
        setPwdEnabled(pwdInput, fpOn || faceOn)

        val clearBtn = makeClearBtn(context, t)
        clearBtn.setOnLongClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            SettingsController.authenticateWithBiometric(context, dialogHost, state, "清除密码，生物支付已关闭") {
                SettingsController.handleClearPassword(context, dialogHost, state)
            }
            true
        }
        setClearBtnEnabled(clearBtn, !state.prefs.getEncodedPassword().isNullOrEmpty(), t)

        val pwdRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(pwdInput, LinearLayout.LayoutParams(
                context.dp(140), LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
            addView(clearBtn)
        }
        pwdCard.addView(pwdRow)
        root.addView(pwdCard)

        if (BuildConfig.DEBUG) {
            val logCard = makeCard(context, t)
            logCard.addView(makeSectionLabel(context, "调试", t))
            val toggleLog = M3Switch(context, t).apply {
                isChecked = state.prefs.isLogCaptureEnabled()
            }
            logCard.addView(makeSwitchRow(context, "日志捕获", "开启后自动记录运行日志", toggleLog, t))
            root.addView(logCard)

            toggleLog.onCheckedChangeListener = { enabled ->
                SettingsController.showToast(context, SettingsController.setLogCaptureEnabled(context, state, enabled))
            }
        }

        root.addView(makeButtonRow(context, dialogHost, pwdInput, state, t) {
            when {
                toggleFp.isChecked && toggleFace.isChecked -> BiometricType.BOTH
                toggleFp.isChecked -> BiometricType.FINGERPRINT
                toggleFace.isChecked -> BiometricType.FACE
                else -> BiometricType.DISABLED
            }
        })

        val updatePwd = { setPwdEnabled(pwdInput, toggleFp.isChecked || toggleFace.isChecked) }
        toggleFp.onCheckedChangeListener = { _ -> updatePwd() }
        toggleFace.onCheckedChangeListener = { _ -> updatePwd() }

        return root
    }

    private fun setPwdEnabled(input: M3Field, enabled: Boolean) {
        input.isEnabled = enabled
        input.alpha = if (enabled) 1f else 0.38f
    }

    private fun makeCard(context: Context, t: ThemeColors): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val r = context.dp(12).toFloat()
            background = ShapeDrawable(RoundRectShape(FloatArray(8) { r }, null, null)).apply {
                paint.color = t.surfaceContainerLow
            }
            elevation = 0f
            setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = context.dp(12) }
        }
    }

    private fun makeSectionLabel(context: Context, text: String, t: ThemeColors): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(t.primary)
            setPadding(0, context.dp(4), 0, context.dp(8))
            letterSpacing = 0.08f
        }
    }

    private fun makeSwitchRow(context: Context, title: String, subtitle: String, switch: M3Switch, t: ThemeColors): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.dp(48)
            setPadding(0, context.dp(4), 0, context.dp(4))
            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, context.dp(4), context.dp(16), context.dp(4))
            }
            textCol.addView(TextView(context).apply {
                this.text = title; textSize = 16f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setTextColor(t.onSurface)
            })
            textCol.addView(TextView(context).apply {
                this.text = subtitle; textSize = 13f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setTextColor(t.onSurfaceVariant)
                setPadding(0, context.dp(2), 0, 0)
            })
            addView(textCol)
            addView(switch)
        }
    }

    private fun makeDivider(context: Context, t: ThemeColors): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(t.divider)
        }
    }

    private fun makeButtonRow(context: Context, dialogHost: DialogHost, pwdInput: M3Field, state: AppState, t: ThemeColors, getSelectedType: () -> Int): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(16), 0, 0)
            addView(makeBtn(context, "关于", t.surfaceContainerHighest, t.onSurface, t.disabledRipple, context.dp(8)) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/kiriashi/BioPay")))
                } catch (_: Throwable) {}
            })
            addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
            addView(makeBtn(context, "取消", t.surfaceContainerHighest, t.onSurface, t.disabledRipple, context.dp(8)) {
                SettingsController.dismissDialog(context, dialogHost, state)
            })
            addView(makeBtn(context, "保存", t.primary, t.onPrimary, t.ripple, 0) {
                SettingsController.handleSave(context, dialogHost, pwdInput, state, getSelectedType())
            })
        }
    }

    private fun makeBtn(context: Context, text: String, bgColor: Int, textColor: Int, rippleColor: Int, marginEnd: Int, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text; textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
            background = makeRipple(bgColor, context.dp(20).toFloat(), rippleColor)
            minimumWidth = context.dp(64)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, context.dp(40)).apply { this.marginEnd = marginEnd }
            setOnClickListener { onClick() }
        }
    }

    private fun makeClearBtn(context: Context, t: ThemeColors): TextView {
        return TextView(context).apply {
            text = "清除"; textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(t.onPrimary)
            gravity = Gravity.CENTER
            background = makeRipple(t.primary, context.dp(10).toFloat(), t.ripple)
            layoutParams = LinearLayout.LayoutParams(context.dp(52), context.dp(32))
        }
    }

    private fun setClearBtnEnabled(btn: TextView, enabled: Boolean, t: ThemeColors) {
        btn.isEnabled = enabled
        val bg = btn.background
        if (bg is RippleDrawable) {
            val inner = bg.getDrawable(0) as? ShapeDrawable
            inner?.paint?.color = if (enabled) t.primary else t.switchOffBg
        }
        btn.setTextColor(if (enabled) t.onPrimary else t.onSurfaceVariant)
    }

    private fun makeRipple(bg: Int, radius: Float, rippleColor: Int): RippleDrawable {
        val outer = FloatArray(8) { radius }
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            ShapeDrawable(RoundRectShape(outer, null, null)).apply { paint.color = bg },
            ShapeDrawable(RoundRectShape(outer, null, null)).apply { paint.color = Color.WHITE }
        )
    }

}
