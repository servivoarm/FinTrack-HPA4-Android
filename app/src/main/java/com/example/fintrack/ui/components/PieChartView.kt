package com.example.fintrack.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.example.fintrack.utils.MoneyUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var slices: List<PieSlice> = emptyList()
    private var moneda: String = "USD"
    private var showAsMoney: Boolean = true
    private var emptyMessage: String = "Sin datos para graficar"

    private val colors = listOf(
        Color.parseColor("#4E79A7"),
        Color.parseColor("#59A14F"),
        Color.parseColor("#F28E2B"),
        Color.parseColor("#E15759"),
        Color.parseColor("#76B7B2"),
        Color.parseColor("#B07AA1"),
        Color.parseColor("#EDC948"),
        Color.parseColor("#9C755F")
    )

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FBF7EF")
        style = Paint.Style.FILL
    }
    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3728")
        textAlign = Paint.Align.CENTER
        textSize = sp(12f)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#75685C")
        textAlign = Paint.Align.CENTER
        textSize = sp(10f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3728")
        textSize = sp(11f)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#75685C")
        textAlign = Paint.Align.RIGHT
        textSize = sp(10.5f)
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#75685C")
        textAlign = Paint.Align.CENTER
        textSize = sp(14f)
    }

    init {
        minimumHeight = dp(260f).toInt()
    }

    fun setData(
        slices: List<PieSlice>,
        moneda: String,
        showAsMoney: Boolean = true
    ) {
        this.slices = prepararSlices(slices)
        this.moneda = moneda
        this.showAsMoney = showAsMoney
        invalidate()
    }

    fun setEmptyMessage(message: String) {
        emptyMessage = message
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val backgroundRect = RectF(dp(4f), dp(4f), width - dp(4f), height - dp(4f))
        canvas.drawRoundRect(backgroundRect, dp(18f), dp(18f), backgroundPaint)

        if (slices.isEmpty()) {
            canvas.drawText(emptyMessage, width / 2f, height / 2f + centeredTextOffset(emptyPaint), emptyPaint)
            return
        }

        val total = slices.sumOf { it.value }
        if (total <= 0.0) {
            canvas.drawText(emptyMessage, width / 2f, height / 2f + centeredTextOffset(emptyPaint), emptyPaint)
            return
        }

        val chartDiameter = min(width - dp(56f), height * 0.42f)
        val chartLeft = (width - chartDiameter) / 2f
        val chartTop = dp(18f)
        val chartRect = RectF(chartLeft, chartTop, chartLeft + chartDiameter, chartTop + chartDiameter)

        drawDonut(canvas, chartRect, total)
        drawCenterText(canvas, chartRect, total)
        drawLegend(canvas, chartRect.bottom + dp(20f), total)
    }

    private fun drawDonut(canvas: Canvas, chartRect: RectF, total: Double) {
        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = ((slice.value / total) * 360.0).toFloat()
            slicePaint.color = colors[index % colors.size]
            canvas.drawArc(chartRect, startAngle, sweep, true, slicePaint)
            startAngle += sweep
        }

        startAngle = -90f
        slices.forEach { slice ->
            val sweep = ((slice.value / total) * 360.0).toFloat()
            canvas.drawArc(chartRect, startAngle, sweep, true, separatorPaint)
            startAngle += sweep
        }

        canvas.drawCircle(
            chartRect.centerX(),
            chartRect.centerY(),
            chartRect.width() * 0.29f,
            holePaint
        )
    }

    private fun drawCenterText(canvas: Canvas, chartRect: RectF, total: Double) {
        val centerX = chartRect.centerX()
        val centerY = chartRect.centerY()
        canvas.drawText("Total", centerX, centerY - dp(3f), subtitlePaint)
        val value = if (showAsMoney) formatMoneyShort(total) else "100%"
        val fitted = fitText(value, chartRect.width() * 0.42f, titlePaint)
        canvas.drawText(fitted, centerX, centerY + dp(15f), titlePaint)
    }

    private fun drawLegend(canvas: Canvas, legendTop: Float, total: Double) {
        val left = dp(22f)
        val right = width - dp(22f)
        val rowHeight = dp(28f)
        val dotRadius = dp(5f)

        slices.forEachIndexed { index, slice ->
            val y = legendTop + (index * rowHeight)
            if (y + rowHeight > height - dp(8f)) return@forEachIndexed

            slicePaint.color = colors[index % colors.size]
            canvas.drawCircle(left + dotRadius, y + dp(10f), dotRadius, slicePaint)

            val valueText = buildLegendValue(slice, total)
            val labelMaxWidth = right - left - dp(28f) - valuePaint.measureText(valueText)
            val label = fitText(slice.label.ifBlank { "Sin etiqueta" }, labelMaxWidth, labelPaint)

            canvas.drawText(label, left + dp(20f), y + dp(14f), labelPaint)
            canvas.drawText(valueText, right, y + dp(14f), valuePaint)
        }
    }

    private fun buildLegendValue(slice: PieSlice, total: Double): String {
        val percent = (slice.value / total) * 100.0
        val percentText = if (percent >= 10.0 || abs(percent - percent.toInt()) < 0.05) {
            String.format(Locale.US, "%.0f%%", percent)
        } else {
            String.format(Locale.US, "%.1f%%", percent)
        }

        return if (showAsMoney) {
            "$percentText - ${MoneyUtils.formatearMonto(slice.value, moneda)}"
        } else {
            percentText
        }
    }

    private fun prepararSlices(rawSlices: List<PieSlice>): List<PieSlice> {
        val validSlices = rawSlices
            .filter { it.value > 0.0 && !it.value.isNaN() && !it.value.isInfinite() }
            .map {
                PieSlice(
                    label = it.label.ifBlank { "Sin etiqueta" },
                    value = it.value
                )
            }
            .sortedByDescending { it.value }

        if (validSlices.size <= MAX_VISIBLE_SLICES) return validSlices

        val principales = validSlices.take(MAX_VISIBLE_SLICES - 1)
        val otros = validSlices.drop(MAX_VISIBLE_SLICES - 1).sumOf { it.value }
        return principales + PieSlice("Otros", otros)
    }

    private fun formatMoneyShort(montoUSD: Double): String {
        val converted = MoneyUtils.convertirDesdeUSD(montoUSD, moneda)
        val symbol = MoneyUtils.obtenerSimbolo(moneda)
        val absolute = abs(converted)
        val sign = if (converted < 0) "-" else ""
        val (divisor, suffix) = when {
            absolute >= 1_000_000.0 -> 1_000_000.0 to "M"
            absolute >= 1_000.0 -> 1_000.0 to "K"
            else -> 1.0 to ""
        }
        val scaled = absolute / divisor
        val pattern = if (suffix.isEmpty() || scaled >= 10.0) "%.0f" else "%.1f"
        return "$sign$symbol${String.format(Locale.US, pattern, scaled)}$suffix"
    }

    private fun fitText(text: String, maxWidth: Float, paint: Paint): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text

        var fitted = text
        while (fitted.length > 3 && paint.measureText("$fitted...") > maxWidth) {
            fitted = fitted.dropLast(1)
        }
        return if (fitted.length <= 3) fitted else "$fitted..."
    }

    private fun centeredTextOffset(paint: Paint): Float {
        return (abs(paint.ascent()) - paint.descent()) / 2f
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }

    private companion object {
        const val MAX_VISIBLE_SLICES = 6
    }
}
