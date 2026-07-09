package com.example.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(movimiento: MovimientoFinanciero): Long

    @Update
    suspend fun actualizar(movimiento: MovimientoFinanciero)

    @Query("DELETE FROM movimientos_financieros WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    @Query("SELECT * FROM movimientos_financieros WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): MovimientoFinanciero?

    @Query("SELECT * FROM movimientos_financieros WHERE usuarioId = :usuarioId AND tipo = :tipo AND activo = 1 ORDER BY fecha DESC, creadoEn DESC")
    fun obtenerPorUsuarioYTipo(usuarioId: Long, tipo: String): Flow<List<MovimientoFinanciero>>

    @Query("SELECT * FROM movimientos_financieros WHERE usuarioId = :usuarioId AND activo = 1 ORDER BY fecha DESC, creadoEn DESC")
    fun obtenerTodosPorUsuario(usuarioId: Long): Flow<List<MovimientoFinanciero>>

    @Query("SELECT * FROM movimientos_financieros WHERE usuarioId = :usuarioId AND activo = 1 ORDER BY fecha DESC, creadoEn DESC")
    suspend fun obtenerTodosPorUsuarioSuspend(usuarioId: Long): List<MovimientoFinanciero>

    @Query("SELECT SUM(montoUSD) FROM movimientos_financieros WHERE usuarioId = :usuarioId AND tipo = 'INGRESO' AND activo = 1")
    suspend fun sumarIngresos(usuarioId: Long): Double?

    @Query("SELECT SUM(montoUSD) FROM movimientos_financieros WHERE usuarioId = :usuarioId AND tipo = 'GASTO' AND activo = 1")
    suspend fun sumarGastos(usuarioId: Long): Double?
}
