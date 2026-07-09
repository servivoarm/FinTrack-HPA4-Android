package com.example.fintrack.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.example.fintrack.utils.MoneyUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var points: List<ChartPoint> = emptyList()
    private var moneda: String = "USD"
    private var emptyMessage: String = "Sin datos para graficar"

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FBF7EF")
        style = Paint.Style.FILL
    }
    private val plotAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8DED1")
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDA88F")
        strokeWidth = dp(1.6f)
        style = Paint.Style.STROKE
    }
    private val zeroAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9D8368")
        strokeWidth = dp(1.3f)
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3728")
        strokeWidth = dp(3.8f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33C9A876")
        style = Paint.Style.FILL
    }
    private val pointHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A3728")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#75685C")
        textSize = sp(11f)
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#75685C")
        textSize = sp(14f)
        textAlign = Paint.Align.CENTER
    }

    init {
        minimumHeight = dp(220f).toInt()
    }

    fun setData(points: List<ChartPoint>, moneda: String) {
        this.points = points.filter { !it.value.isNaN() && !it.value.isInfinite() }
        this.moneda = moneda
        invalidate()
    }

    fun setEmptyMessage(message: String) {
        emptyMessage = message
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) {
            return
        }

        val outerRect = RectF(dp(4f), dp(4f), width - dp(4f), height - dp(4f))
        canvas.drawRoundRect(outerRect, dp(18f), dp(18f), backgroundPaint)

        if (points.isEmpty()) {
            canvas.drawText(emptyMessage, width / 2f, height / 2f + centeredTextOffset(emptyPaint), emptyPaint)
            return
        }

        val rawMaxValue = points.maxOfOrNull { it.value } ?: 0.0
        val rawMinValue = points.minOfOrNull { it.value } ?: 0.0
        val maxValue = max(0.0, rawMaxValue)
        val minValue = min(0.0, rawMinValue)
        val chartMinValue = if (maxValue == minValue) minValue - 1.0 else minValue
        val chartMaxValue = if (maxValue == minValue) maxValue + 1.0 else maxValue
        val chartRange = (chartMaxValue - chartMinValue).takeIf { it > 0.0 } ?: 1.0
        val yValues = buildYAxisValues(chartMinValue, chartMaxValue)
        val widestYLabel = yValues.maxOf { textPaint.measureText(formatMoneyShort(it)) }

        val left = max(dp(52f), widestYLabel + dp(12f))
        val top = dp(22f)
        val right = width - dp(18f)
        val bottom = height - if (points.size > 6) dp(62f) else dp(44f)
        val chartWidth = max(1f, right - left)
        val chartHeight = max(1f, bottom - top)
        val plotRect = RectF(left, top, right, bottom)

        canvas.drawRoundRect(plotRect, dp(12f), dp(12f), plotAreaPaint)
        drawGridAndYAxisLabels(canvas, left, right, top, bottom, yValues)

        if (chartMinValue < 0.0 && chartMaxValue > 0.0) {
            val zeroY = yForValue(0.0, top, bottom, chartMinValue, chartRange)
            canvas.drawLine(left, zeroY, right, zeroY, zeroAxisPaint)
        }

        val spacing = if (points.size == 1) 0f else chartWidth / (points.size - 1)
        val coordinates = points.mapIndexed { index, point ->
            val x = if (points.size == 1) left + chartWidth / 2f else left + index * spacing
            val y = yForValue(point.value, top, bottom, chartMinValue, chartRange)
            x to y
        }

        if (coordinates.size > 1) {
            val path = Path()
            val fillPath = Path()
            coordinates.forEachIndexed { index, (x, y) ->
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, bottom)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(coordinates.last().first, bottom)
            fillPath.close()

            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(path, linePaint)
        } else {
            val (x, y) = coordinates.first()
            canvas.drawLine(x, bottom, x, y, gridPaint)
        }

        coordinates.forEachIndexed { index, (x, y) ->
            canvas.drawCircle(x, y, dp(6.6f), pointHaloPaint)
            canvas.drawCircle(x, y, dp(4.5f), pointPaint)
            drawXLabel(canvas, points[index].label, x, bottom + dp(28f), index, chartWidth)
        }

        canvas.drawLine(left, top, left, bottom, axisPaint)
        canvas.drawLine(left, bottom, right, bottom, axisPaint)
    }

    private fun drawGridAndYAxisLabels(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        values: List<Double>
    ) {
        if (values.isEmpty()) return

        val previousAlign = textPaint.textAlign
        textPaint.textAlign = Paint.Align.RIGHT
        values.forEachIndexed { index, value ->
            val y = top + ((bottom - top) / (values.lastIndex.coerceAtLeast(1))) * index
            canvas.drawLine(left, y, right, y, gridPaint)
            canvas.drawText(formatMoneyShort(value), left - dp(10f), y + centeredTextOffset(textPaint), textPaint)
        }
        textPaint.textAlign = previousAlign
    }

    private fun drawXLabel(canvas: Canvas, label: String, x: Float, y: Float, index: Int, chartWidth: Float) {
        val maxVisibleLabels = min(6, max(2, (chartWidth / dp(64f)).toInt()))
        val stride = max(1, ceil(points.size / maxVisibleLabels.toDouble()).toInt())
        val shouldDraw = points.size <= maxVisibleLabels ||
            index == 0 ||
            index == points.lastIndex ||
            index % stride == 0
        if (!shouldDraw) return

        val previousAlign = textPaint.textAlign
        val previousSize = textPaint.textSize
        val needsRotation = points.size > maxVisibleLabels
        textPaint.textSize = if (needsRotation) sp(10f) else sp(11f)
        textPaint.textAlign = when (index) {
            0 -> Paint.Align.LEFT
            points.lastIndex -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        val labelAbreviada = abbreviateXLabel(label)
        if (needsRotation) {
            canvas.save()
            canvas.rotate(-35f, x, y)
            canvas.drawText(labelAbreviada, x, y, textPaint)
            canvas.restore()
        } else {
            canvas.drawText(labelAbreviada, x, y, textPaint)
        }
        textPaint.textAlign = previousAlign
        textPaint.textSize = previousSize
    }

    private fun buildYAxisValues(minValue: Double, maxValue: Double): List<Double> {
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val tickCount = 4
        return (0..tickCount).map { index ->
            maxValue - (range * index / tickCount)
        }
    }

    private fun yForValue(value: Double, top: Float, bottom: Float, minValue: Double, valueRange: Double): Float {
        return bottom - (((value - minValue) / valueRange) * (bottom - top)).toFloat()
    }

    private fun formatMoneyShort(montoUSD: Double): String {
        val converted = MoneyUtils.convertirDesdeUSD(montoUSD, moneda)
        val symbol = MoneyUtils.obtenerSimbolo(moneda)
        val absolute = abs(converted)
        val sign = if (converted < 0) "-" else ""
        val (divisor, suffix) = when {
            absolute >= 1_000_000.0 -> 1_000_000.0 to "M"
            absolute >= 1_000.0 -> 1_000.0 to "k"
            else -> 1.0 to ""
        }
        val scaled = absolute / divisor
        val pattern = if (suffix.isEmpty() || scaled >= 10.0) "%.0f" else "%.1f"
        return "$sign$symbol${String.format(Locale.US, pattern, scaled)}$suffix"
    }

    private fun abbreviateXLabel(label: String): String {
        val trimmed = label.trim()
        if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            return "${trimmed.substring(8, 10)}/${trimmed.substring(5, 7)}"
        }
        if (trimmed.length == 5 && trimmed[2] == '-') {
            return "${trimmed.substring(3, 5)}/${trimmed.substring(0, 2)}"
        }
        return trimmed
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
}
