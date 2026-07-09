package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import com.example.fintrack.data.repository.AhorroRepository
import com.example.fintrack.data.repository.MovimientoRepository
import com.example.fintrack.data.repository.PresupuestoCategoriaRepository
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.SessionManager
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PresupuestosUiState(
    val objetivos: List<ObjetivoAhorro> = emptyList(),
    val aportes: List<AporteAhorro> = emptyList(),
    val presupuestosCategoria: List<PresupuestoCategoriaResumen> = emptyList(),
    val totalPresupuestadoUSD: Double = 0.0,
    val totalGastadoPresupuestosUSD: Double = 0.0,
    val totalRestantePresupuestosUSD: Double = 0.0,
    val mesPresupuestos: Int = LocalDate.now().monthValue,
    val anioPresupuestos: Int = LocalDate.now().year,
    val ingresosMensualesEstimadosUSD: Double = 0.0,
    val gastosMensualesEstimadosUSD: Double = 0.0,
    val totalAhorrosUSD: Double = 0.0,
    val saldoDisponibleEstimadoUSD: Double = 0.0,
    val saldoActualUSD: Double = 0.0,
    val moneda: String = "USD"
)

data class PresupuestoCategoriaResumen(
    val presupuesto: PresupuestoCategoria,
    val gastadoUSD: Double,
    val restanteUSD: Double,
    val porcentajeUsado: Double,
    val estado: String
)

class PresupuestosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.obtenerInstancia(application)
    private val ahorroRepository = AhorroRepository(
        database.objetivoAhorroDao(),
        database.aporteAhorroDao()
    )
    private val movimientoRepository = MovimientoRepository(
        database.movimientoDao(),
        database.aporteAhorroDao()
    )
    private val presupuestoRepository = PresupuestoCategoriaRepository(
        database.presupuestoCategoriaDao()
    )
    private val sessionManager = SessionManager(application)
    private val mesActual = LocalDate.now().monthValue
    private val anioActual = LocalDate.now().year

    private val _uiState = MutableStateFlow(PresupuestosUiState())
    val uiState: StateFlow<PresupuestosUiState> = _uiState.asStateFlow()

    private val usuarioId: Long
        get() = sessionManager.obtenerUsuarioId()

    init {
        observarDatos()
    }

    private fun observarDatos() {
        viewModelScope.launch {
            combine(
                ahorroRepository.obtenerObjetivos(usuarioId),
                presupuestoRepository.obtenerPresupuestosDelMes(usuarioId, mesActual, anioActual)
            ) { objetivos, presupuestos ->
                objetivos to presupuestos
            }.collect { (objetivos, presupuestos) ->
                actualizarEstado(objetivos, presupuestos)
            }
        }
    }

    fun recargarDatos() {
        viewModelScope.launch {
            val objetivos = withContext(Dispatchers.IO) {
                ahorroRepository.obtenerObjetivosSuspend(usuarioId)
            }
            val presupuestos = withContext(Dispatchers.IO) {
                presupuestoRepository.obtenerPresupuestosDelMesSuspend(usuarioId, mesActual, anioActual)
            }
            actualizarEstado(objetivos, presupuestos)
        }
    }

    suspend fun crearObjetivo(objetivo: ObjetivoAhorro): OperacionResult {
        return withContext(Dispatchers.IO) {
            ahorroRepository.crearObjetivo(objetivo)
            OperacionResult(true, "Objetivo guardado")
        }
    }

    suspend fun obtenerPresupuestoCategoriaPorId(id: Long): PresupuestoCategoria? {
        return withContext(Dispatchers.IO) {
            presupuestoRepository.obtenerPorId(id)
        }
    }

    suspend fun guardarPresupuestoCategoria(presupuesto: PresupuestoCategoria): OperacionResult {
        return withContext(Dispatchers.IO) {
            val duplicado = presupuestoRepository.validarDuplicado(
                usuarioId = presupuesto.usuarioId,
                categoria = presupuesto.categoria,
                mes = presupuesto.mes,
                anio = presupuesto.anio,
                presupuestoActualId = presupuesto.id
            )
            if (duplicado) {
                return@withContext OperacionResult(
                    exitoso = false,
                    mensaje = "Ya existe un presupuesto para esta categoría en este mes"
                )
            }

            if (presupuesto.id == 0L) {
                val id = presupuestoRepository.crearPresupuesto(presupuesto)
                OperacionResult(true, "Presupuesto guardado", id)
            } else {
                presupuestoRepository.actualizarPresupuesto(presupuesto)
                OperacionResult(true, "Presupuesto actualizado", presupuesto.id)
            }
        }
    }

    suspend fun agregarAporte(
        objetivoId: Long,
        montoUSD: Double,
        nota: String
    ): OperacionResult {
        return withContext(Dispatchers.IO) {
            val saldoActual = movimientoRepository.calcularSaldoActual(usuarioId)
            if (montoUSD > saldoActual) {
                return@withContext OperacionResult(false, "No puedes aportar mas que el saldo disponible")
            }

            ahorroRepository.agregarAporte(
                objetivoId = objetivoId,
                usuarioId = usuarioId,
                montoUSD = montoUSD,
                fecha = fechaActual(),
                nota = nota
            )
            OperacionResult(true, "Aporte agregado")
        }
    }

    fun eliminarObjetivo(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ahorroRepository.eliminarObjetivo(id)
            }
            recargarDatos()
        }
    }

    fun eliminarPresupuestoCategoria(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                presupuestoRepository.eliminarPresupuesto(id)
            }
            recargarDatos()
        }
    }

    private suspend fun actualizarEstado(
        objetivos: List<ObjetivoAhorro>,
        presupuestos: List<PresupuestoCategoria>
    ) {
        val movimientos = withContext(Dispatchers.IO) {
            movimientoRepository.obtenerTodosSuspend(usuarioId)
        }
        val ingresos = movimientos.filter { it.tipo == FinanceConstants.TIPO_INGRESO }
        val gastos = movimientos.filter { it.tipo == FinanceConstants.TIPO_GASTO }
        val ingresosMes = FinanceCalculator.totalProyectadoPorPeriodo(ingresos, FinanceConstants.PERIODO_MES)
        val gastosMes = FinanceCalculator.totalProyectadoPorPeriodo(gastos, FinanceConstants.PERIODO_MES)
        val totalAhorros = withContext(Dispatchers.IO) {
            ahorroRepository.calcularTotalAhorros(usuarioId)
        }
        val aportes = withContext(Dispatchers.IO) {
            ahorroRepository.obtenerAportesPorUsuarioSuspend(usuarioId)
        }
        val saldoActual = withContext(Dispatchers.IO) {
            movimientoRepository.calcularSaldoActual(usuarioId)
        }
        val resumenPresupuestos = calcularResumenPresupuestos(presupuestos, gastos)
        val totalPresupuestado = resumenPresupuestos.sumOf { it.presupuesto.montoLimiteUSD }
        val totalGastadoPresupuestos = resumenPresupuestos.sumOf { it.gastadoUSD }
        val totalRestantePresupuestos = resumenPresupuestos.sumOf { it.restanteUSD }

        _uiState.value = PresupuestosUiState(
            objetivos = objetivos,
            aportes = aportes,
            presupuestosCategoria = resumenPresupuestos,
            totalPresupuestadoUSD = totalPresupuestado,
            totalGastadoPresupuestosUSD = totalGastadoPresupuestos,
            totalRestantePresupuestosUSD = totalRestantePresupuestos,
            mesPresupuestos = mesActual,
            anioPresupuestos = anioActual,
            ingresosMensualesEstimadosUSD = ingresosMes,
            gastosMensualesEstimadosUSD = gastosMes,
            totalAhorrosUSD = totalAhorros,
            saldoDisponibleEstimadoUSD = ingresosMes - gastosMes - totalAhorros,
            saldoActualUSD = saldoActual,
            moneda = sessionManager.obtenerMoneda()
        )
    }

    private fun calcularResumenPresupuestos(
        presupuestos: List<PresupuestoCategoria>,
        gastos: List<com.example.fintrack.data.local.entity.MovimientoFinanciero>
    ): List<PresupuestoCategoriaResumen> {
        return presupuestos.map { presupuesto ->
            val inicio = LocalDate.of(presupuesto.anio, presupuesto.mes, 1)
            val fin = inicio.withDayOfMonth(inicio.lengthOfMonth())
            val gastosCategoria = gastos.filter { it.categoria == presupuesto.categoria }
            val gastado = FinanceCalculator.totalProyectadoPorRango(gastosCategoria, inicio, fin)
            val porcentaje = if (presupuesto.montoLimiteUSD > 0.0) {
                (gastado / presupuesto.montoLimiteUSD) * 100.0
            } else {
                0.0
            }
            val estado = when {
                porcentaje >= 100.0 -> ESTADO_SUPERADO
                porcentaje >= 80.0 -> ESTADO_ALERTA
                else -> ESTADO_NORMAL
            }
            PresupuestoCategoriaResumen(
                presupuesto = presupuesto,
                gastadoUSD = gastado,
                restanteUSD = presupuesto.montoLimiteUSD - gastado,
                porcentajeUsado = porcentaje,
                estado = estado
            )
        }.sortedBy { it.presupuesto.categoria }
    }

    private fun fechaActual(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    companion object {
        const val ESTADO_NORMAL = "NORMAL"
        const val ESTADO_ALERTA = "ALERTA"
        const val ESTADO_SUPERADO = "SUPERADO"
    }
}
