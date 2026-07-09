package com.example.fintrack.data.repository

import com.example.fintrack.data.local.dao.AporteAhorroDao
import com.example.fintrack.data.local.dao.MovimientoDao
import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.utils.FinanceConstants
import kotlinx.coroutines.flow.Flow

class MovimientoRepository(
    private val movimientoDao: MovimientoDao,
    private val aporteAhorroDao: AporteAhorroDao
) {
    suspend fun guardarMovimiento(movimiento: MovimientoFinanciero): Long {
        return movimientoDao.insertar(movimiento)
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoFinanciero) {
        movimientoDao.actualizar(movimiento)
    }

    suspend fun eliminarMovimiento(id: Long) {
        movimientoDao.eliminarPorId(id)
    }

    suspend fun obtenerMovimientoPorId(id: Long): MovimientoFinanciero? {
        return movimientoDao.obtenerPorId(id)
    }

    fun obtenerIngresos(usuarioId: Long): Flow<List<MovimientoFinanciero>> {
        return movimientoDao.obtenerPorUsuarioYTipo(usuarioId, FinanceConstants.TIPO_INGRESO)
    }

    fun obtenerGastos(usuarioId: Long): Flow<List<MovimientoFinanciero>> {
        return movimientoDao.obtenerPorUsuarioYTipo(usuarioId, FinanceConstants.TIPO_GASTO)
    }

    fun obtenerTodos(usuarioId: Long): Flow<List<MovimientoFinanciero>> {
        return movimientoDao.obtenerTodosPorUsuario(usuarioId)
    }

    suspend fun obtenerTodosSuspend(usuarioId: Long): List<MovimientoFinanciero> {
        return movimientoDao.obtenerTodosPorUsuarioSuspend(usuarioId)
    }

    suspend fun obtenerAportesPorUsuarioSuspend(usuarioId: Long): List<AporteAhorro> {
        return aporteAhorroDao.obtenerAportesPorUsuarioSuspend(usuarioId)
    }

    suspend fun calcularSaldoActual(usuarioId: Long): Double {
        return calcularTotalIngresos(usuarioId) - calcularTotalGastos(usuarioId) - calcularTotalAhorros(usuarioId)
    }

    suspend fun calcularTotalIngresos(usuarioId: Long): Double {
        return movimientoDao.sumarIngresos(usuarioId) ?: 0.0
    }

    suspend fun calcularTotalGastos(usuarioId: Long): Double {
        return movimientoDao.sumarGastos(usuarioId) ?: 0.0
    }

    suspend fun calcularTotalAhorros(usuarioId: Long): Double {
        return aporteAhorroDao.sumarAportesPorUsuario(usuarioId) ?: 0.0
    }
}
