package com.example.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.data.local.entity.AporteAhorro
import kotlinx.coroutines.flow.Flow

@Dao
interface AporteAhorroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAporte(aporte: AporteAhorro): Long

    @Query("SELECT * FROM aportes_ahorro WHERE objetivoId = :objetivoId ORDER BY fecha DESC, id DESC")
    fun obtenerAportesPorObjetivo(objetivoId: Long): Flow<List<AporteAhorro>>

    @Query("SELECT * FROM aportes_ahorro WHERE usuarioId = :usuarioId ORDER BY fecha ASC, id ASC")
    fun obtenerAportesPorUsuario(usuarioId: Long): Flow<List<AporteAhorro>>

    @Query("SELECT * FROM aportes_ahorro WHERE usuarioId = :usuarioId ORDER BY fecha ASC, id ASC")
    suspend fun obtenerAportesPorUsuarioSuspend(usuarioId: Long): List<AporteAhorro>

    @Query("SELECT SUM(montoUSD) FROM aportes_ahorro WHERE usuarioId = :usuarioId")
    suspend fun sumarAportesPorUsuario(usuarioId: Long): Double?

    @Query("SELECT SUM(montoUSD) FROM aportes_ahorro WHERE objetivoId = :objetivoId")
    suspend fun sumarAportesPorObjetivo(objetivoId: Long): Double?

    @Query("DELETE FROM aportes_ahorro WHERE objetivoId = :objetivoId")
    suspend fun eliminarAportesPorObjetivo(objetivoId: Long)
}
