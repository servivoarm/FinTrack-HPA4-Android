package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.data.repository.MovimientoRepository
import com.example.fintrack.data.repository.PresupuestoCategoriaRepository
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.SessionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OperacionResult(
    val exitoso: Boolean,
    val mensaje: String,
    val movimientoId: Long? = null
)

data class MovimientosUiState(
    val ingresos: List<MovimientoFinanciero> = emptyList(),
    val gastos: List<MovimientoFinanciero> = emptyList(),
    val todos: List<MovimientoFinanciero> = emptyList(),
    val aportes: List<AporteAhorro> = emptyList(),
    val saldoActualUSD: Double = 0.0,
    val totalIngresosUSD: Double = 0.0,
    val totalGastosUSD: Double = 0.0,
    val totalAhorrosUSD: Double = 0.0,
    val moneda: String = "USD"
)

data class PresupuestoCategoriaAlerta(
    val categoria: String,
    val porcentajeUsado: Double,
    val estado: String
)

class MovimientosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.obtenerInstancia(application)
    private val repository = MovimientoRepository(
        database.movimientoDao(),
        database.aporteAhorroDao()
    )
    private val presupuestoRepository = PresupuestoCategoriaRepository(
        database.presupuestoCategoriaDao()
    )
    private val sessionManager = SessionManager(application)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _uiState = MutableStateFlow(MovimientosUiState())
    val uiState: StateFlow<MovimientosUiState> = _uiState.asStateFlow()

    private val usuarioId: Long
        get() = sessionManager.obtenerUsuarioId()

    init {
        observarMovimientos()
    }

    private fun observarMovimientos() {
        viewModelScope.launch {
            repository.obtenerTodos(usuarioId).collect { movimientos ->
                actualizarEstado(movimientos)
            }
        }
    }

    fun recargarDatos() {
        viewModelScope.launch {
            val movimientos = withContext(Dispatchers.IO) {
                repository.obtenerTodosSuspend(usuarioId)
            }
            actualizarEstado(movimientos)
        }
    }

    suspend fun obtenerMovimientoPorId(id: Long): MovimientoFinanciero? {
        return withContext(Dispatchers.IO) {
            repository.obtenerMovimientoPorId(id)
        }
    }

    suspend fun guardarMovimiento(movimiento: MovimientoFinanciero): OperacionResult {
        return withContext(Dispatchers.IO) {
            val movimientoId = repository.guardarMovimiento(movimiento)
            OperacionResult(true, "Movimiento guardado", movimientoId)
        }
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoFinanciero): OperacionResult {
        return withContext(Dispatchers.IO) {
            repository.actualizarMovimiento(movimiento)
            OperacionResult(true, "Movimiento actualizado", movimiento.id)
        }
    }

    suspend fun obtenerAlertaPresupuestoCategoria(
        categoria: String,
        fecha: String
    ): PresupuestoCategoriaAlerta? {
        return withContext(Dispatchers.IO) {
            val fechaGasto = runCatching { LocalDate.parse(fecha, dateFormatter) }.getOrNull()
                ?: return@withContext null
            val presupuesto = presupuestoRepository.obtenerPorCategoriaMesAnio(
                usuarioId = usuarioId,
                categoria = categoria,
                mes = fechaGasto.monthValue,
                anio = fechaGasto.year
            ) ?: return@withContext null

            val inicio = fechaGasto.withDayOfMonth(1)
            val fin = inicio.withDayOfMonth(inicio.lengthOfMonth())
            val movimientos = repository.obtenerTodosSuspend(usuarioId)
            val gastosCategoria = movimientos.filter {
                it.tipo == FinanceConstants.TIPO_GASTO && it.categoria == categoria
            }
            val gastado = FinanceCalculator.totalProyectadoPorRango(gastosCategoria, inicio, fin)
            val porcentaje = if (presupuesto.montoLimiteUSD > 0.0) {
                (gastado / presupuesto.montoLimiteUSD) * 100.0
            } else {
                0.0
            }

            when {
                porcentaje >= 100.0 -> PresupuestoCategoriaAlerta(categoria, porcentaje, PresupuestosViewModel.ESTADO_SUPERADO)
                porcentaje >= 80.0 -> PresupuestoCategoriaAlerta(categoria, porcentaje, PresupuestosViewModel.ESTADO_ALERTA)
                else -> null
            }
        }
    }

    fun eliminarMovimiento(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminarMovimiento(id)
            }
            recargarDatos()
        }
    }

    private suspend fun actualizarEstado(movimientos: List<MovimientoFinanciero>) {
        val movimientosOrdenados = movimientos.ordenadosRecientes()
        val ingresos = movimientosOrdenados.filter { it.tipo == FinanceConstants.TIPO_INGRESO }
        val gastos = movimientosOrdenados.filter { it.tipo == FinanceConstants.TIPO_GASTO }
        val totalIngresos = withContext(Dispatchers.IO) { repository.calcularTotalIngresos(usuarioId) }
        val totalGastos = withContext(Dispatchers.IO) { repository.calcularTotalGastos(usuarioId) }
        val totalAhorros = withContext(Dispatchers.IO) { repository.calcularTotalAhorros(usuarioId) }
        val aportes = withContext(Dispatchers.IO) {
            repository.obtenerAportesPorUsuarioSuspend(usuarioId)
        }

        _uiState.value = MovimientosUiState(
            ingresos = ingresos,
            gastos = gastos,
            todos = movimientosOrdenados,
            aportes = aportes,
            saldoActualUSD = totalIngresos - totalGastos - totalAhorros,
            totalIngresosUSD = totalIngresos,
            totalGastosUSD = totalGastos,
            totalAhorrosUSD = totalAhorros,
            moneda = sessionManager.obtenerMoneda()
        )
    }

    private fun List<MovimientoFinanciero>.ordenadosRecientes(): List<MovimientoFinanciero> {
        return sortedWith(
            compareByDescending<MovimientoFinanciero> { it.fecha }
                .thenByDescending { it.creadoEn }
        )
    }
}
