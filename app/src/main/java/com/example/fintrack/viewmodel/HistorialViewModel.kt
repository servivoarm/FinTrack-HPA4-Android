package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.data.repository.MovimientoRepository
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.SessionManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistorialUiState(
    val movimientos: List<MovimientoFinanciero> = emptyList(),
    val categorias: List<String> = listOf(HistorialViewModel.FILTRO_TODAS),
    val moneda: String = "USD"
)

class HistorialViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.obtenerInstancia(application)
    private val repository = MovimientoRepository(
        database.movimientoDao(),
        database.aporteAhorroDao()
    )
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    private val usuarioId: Long
        get() = sessionManager.obtenerUsuarioId()

    fun cargarDatos(
        tipoFiltro: String = FILTRO_TODOS,
        categoriaFiltro: String = FILTRO_TODAS,
        fechaInicio: LocalDate? = null,
        fechaFin: LocalDate? = null
    ) {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                construirHistorial(tipoFiltro, categoriaFiltro, fechaInicio, fechaFin)
            }
            _uiState.value = state
        }
    }

    suspend fun eliminarMovimiento(id: Long) {
        withContext(Dispatchers.IO) {
            repository.eliminarMovimiento(id)
        }
    }

    private suspend fun construirHistorial(
        tipoFiltro: String,
        categoriaFiltro: String,
        fechaInicio: LocalDate?,
        fechaFin: LocalDate?
    ): HistorialUiState {
        val todos = repository.obtenerTodosSuspend(usuarioId)
        val categorias = construirCategorias(todos)
        val porTipo = when (tipoFiltro) {
            FILTRO_INGRESOS -> todos.filter { it.tipo == FinanceConstants.TIPO_INGRESO }
            FILTRO_GASTOS -> todos.filter { it.tipo == FinanceConstants.TIPO_GASTO }
            else -> todos
        }
        val porCategoria = if (categoriaFiltro == FILTRO_TODAS) {
            porTipo
        } else {
            porTipo.filter { it.categoria == categoriaFiltro }
        }
        val filtrados = FinanceCalculator.filtrarMovimientosPorRango(
            movimientos = porCategoria,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin
        ).ordenadosRecientes()

        return HistorialUiState(
            movimientos = filtrados,
            categorias = categorias,
            moneda = sessionManager.obtenerMoneda()
        )
    }

    private fun construirCategorias(movimientos: List<MovimientoFinanciero>): List<String> {
        val categoriasBase = FinanceConstants.CATEGORIAS_INGRESO + FinanceConstants.CATEGORIAS_GASTO
        val categoriasGuardadas = movimientos.map { it.categoria }.filter { it.isNotBlank() }
        return listOf(FILTRO_TODAS) + (categoriasBase + categoriasGuardadas)
            .distinct()
            .sorted()
    }

    private fun List<MovimientoFinanciero>.ordenadosRecientes(): List<MovimientoFinanciero> {
        return sortedWith(
            compareByDescending<MovimientoFinanciero> { it.fecha }
                .thenByDescending { it.creadoEn }
        )
    }

    companion object {
        const val FILTRO_TODOS = "Todos"
        const val FILTRO_INGRESOS = "Ingresos"
        const val FILTRO_GASTOS = "Gastos"
        const val FILTRO_TODAS = "Todas"
    }
}
