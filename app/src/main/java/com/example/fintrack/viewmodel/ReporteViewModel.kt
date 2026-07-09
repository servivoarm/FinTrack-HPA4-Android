package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.data.repository.MovimientoRepository
import com.example.fintrack.ui.components.ChartPoint
import com.example.fintrack.ui.components.PieSlice
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.utils.SessionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReporteUiState(
    val mes: Int = LocalDate.now().monthValue,
    val anio: Int = LocalDate.now().year,
    val moneda: String = "USD",
    val totalIngresosUSD: Double = 0.0,
    val totalGastosUSD: Double = 0.0,
    val totalAhorrosUSD: Double = 0.0,
    val balanceFinalUSD: Double = 0.0,
    val categoriaMayorGasto: Pair<String, Double>? = null,
    val categoriaMayorIngreso: Pair<String, Double>? = null,
    val movimientoMayorGasto: MovimientoFinanciero? = null,
    val movimientoMayorIngreso: MovimientoFinanciero? = null,
    val distribucionGastos: List<PieSlice> = emptyList(),
    val distribucionIngresos: List<PieSlice> = emptyList(),
    val puntosBalance: List<ChartPoint> = emptyList(),
    val resumenTexto: String = "Sin datos para este mes",
    val textoCompartir: String = "",
    val cargando: Boolean = false
)

class ReporteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.obtenerInstancia(application)
    private val repository = MovimientoRepository(
        database.movimientoDao(),
        database.aporteAhorroDao()
    )
    private val sessionManager = SessionManager(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES"))

    private val _uiState = MutableStateFlow(ReporteUiState())
    val uiState: StateFlow<ReporteUiState> = _uiState.asStateFlow()

    private val usuarioId: Long
        get() = sessionManager.obtenerUsuarioId()

    fun cargarReporte(mes: Int, anio: Int) {
        _uiState.value = _uiState.value.copy(cargando = true, mes = mes, anio = anio)
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                construirReporte(mes, anio)
            }
            _uiState.value = state
        }
    }

    private suspend fun construirReporte(mes: Int, anio: Int): ReporteUiState {
        val moneda = sessionManager.obtenerMoneda()
        val movimientos = repository.obtenerTodosSuspend(usuarioId)
        val aportes = repository.obtenerAportesPorUsuarioSuspend(usuarioId)
        val movimientosMes = FinanceCalculator.filtrarMovimientosPorMes(movimientos, mes, anio)
        val aportesMes = filtrarAportesPorMes(aportes, mes, anio)
        val ingresosMes = movimientosMes.filter { it.tipo == FinanceConstants.TIPO_INGRESO }
        val gastosMes = movimientosMes.filter { it.tipo == FinanceConstants.TIPO_GASTO }
        val totalIngresos = ingresosMes.sumOf { it.montoUSD }
        val totalGastos = gastosMes.sumOf { it.montoUSD }
        val totalAhorros = aportesMes.sumOf { it.montoUSD }
        val balanceFinal = totalIngresos - totalGastos - totalAhorros
        val categoriaMayorGasto = FinanceCalculator.obtenerCategoriaMayor(movimientosMes, FinanceConstants.TIPO_GASTO)
        val categoriaMayorIngreso = FinanceCalculator.obtenerCategoriaMayor(movimientosMes, FinanceConstants.TIPO_INGRESO)
        val movimientoMayorGasto = FinanceCalculator.obtenerMovimientoMayor(movimientosMes, FinanceConstants.TIPO_GASTO)
        val movimientoMayorIngreso = FinanceCalculator.obtenerMovimientoMayor(movimientosMes, FinanceConstants.TIPO_INGRESO)
        val tituloMes = formatearMes(mes, anio)

        val resumen = construirResumen(
            tituloMes = tituloMes,
            totalIngresos = totalIngresos,
            totalGastos = totalGastos,
            totalAhorros = totalAhorros,
            balanceFinal = balanceFinal,
            moneda = moneda
        )

        return ReporteUiState(
            mes = mes,
            anio = anio,
            moneda = moneda,
            totalIngresosUSD = totalIngresos,
            totalGastosUSD = totalGastos,
            totalAhorrosUSD = totalAhorros,
            balanceFinalUSD = balanceFinal,
            categoriaMayorGasto = categoriaMayorGasto,
            categoriaMayorIngreso = categoriaMayorIngreso,
            movimientoMayorGasto = movimientoMayorGasto,
            movimientoMayorIngreso = movimientoMayorIngreso,
            distribucionGastos = distribucionPorCategoria(gastosMes),
            distribucionIngresos = distribucionPorCategoria(ingresosMes),
            puntosBalance = FinanceCalculator.generarPuntosBalanceAcumuladoMensual(
                movimientos = movimientos,
                aportes = aportes,
                mes = mes,
                anio = anio
            ),
            resumenTexto = resumen,
            textoCompartir = construirTextoCompartir(
                tituloMes = tituloMes,
                totalIngresos = totalIngresos,
                totalGastos = totalGastos,
                totalAhorros = totalAhorros,
                balanceFinal = balanceFinal,
                categoriaMayorGasto = categoriaMayorGasto,
                categoriaMayorIngreso = categoriaMayorIngreso,
                moneda = moneda
            ),
            cargando = false
        )
    }

    private fun filtrarAportesPorMes(
        aportes: List<AporteAhorro>,
        mes: Int,
        anio: Int
    ): List<AporteAhorro> {
        return aportes.filter { aporte ->
            val fecha = parseFecha(aporte.fecha) ?: return@filter false
            fecha.monthValue == mes && fecha.year == anio
        }
    }

    private fun distribucionPorCategoria(movimientos: List<MovimientoFinanciero>): List<PieSlice> {
        return movimientos
            .filter { it.activo }
            .groupBy { it.categoria.ifBlank { "Sin categoria" } }
            .map { (categoria, movimientosCategoria) ->
                PieSlice(categoria, movimientosCategoria.sumOf { it.montoUSD })
            }
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
    }

    private fun construirResumen(
        tituloMes: String,
        totalIngresos: Double,
        totalGastos: Double,
        totalAhorros: Double,
        balanceFinal: Double,
        moneda: String
    ): String {
        val balanceTexto = MoneyUtils.formatearMonto(balanceFinal, moneda)
        val estado = when {
            balanceFinal > 0.0 -> "cerraste el mes con balance positivo."
            balanceFinal < 0.0 -> "cerraste el mes con balance negativo."
            else -> "cerraste el mes en equilibrio."
        }
        return "En $tituloMes registraste ${MoneyUtils.formatearMonto(totalIngresos, moneda)} en ingresos, " +
            "${MoneyUtils.formatearMonto(totalGastos, moneda)} en gastos y " +
            "${MoneyUtils.formatearMonto(totalAhorros, moneda)} en ahorros. " +
            "Tu balance final fue $balanceTexto; $estado"
    }

    private fun construirTextoCompartir(
        tituloMes: String,
        totalIngresos: Double,
        totalGastos: Double,
        totalAhorros: Double,
        balanceFinal: Double,
        categoriaMayorGasto: Pair<String, Double>?,
        categoriaMayorIngreso: Pair<String, Double>?,
        moneda: String
    ): String {
        val mayorGasto = categoriaMayorGasto?.let { "${it.first}: ${MoneyUtils.formatearMonto(it.second, moneda)}" }
            ?: "Sin datos"
        val mayorIngreso = categoriaMayorIngreso?.let { "${it.first}: ${MoneyUtils.formatearMonto(it.second, moneda)}" }
            ?: "Sin datos"

        return """
            Reporte mensual FinTrack - $tituloMes
            Ingresos: ${MoneyUtils.formatearMonto(totalIngresos, moneda)}
            Gastos: ${MoneyUtils.formatearMonto(totalGastos, moneda)}
            Ahorros: ${MoneyUtils.formatearMonto(totalAhorros, moneda)}
            Balance final: ${MoneyUtils.formatearMonto(balanceFinal, moneda)}
            Mayor gasto por categoria: $mayorGasto
            Mayor ingreso por categoria: $mayorIngreso
        """.trimIndent()
    }

    private fun formatearMes(mes: Int, anio: Int): String {
        return LocalDate.of(anio, mes, 1)
            .format(monthFormatter)
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.forLanguageTag("es-ES")) else char.toString()
            }
    }

    private fun parseFecha(fecha: String): LocalDate? {
        return runCatching { LocalDate.parse(fecha, dateFormatter) }.getOrNull()
    }
}
