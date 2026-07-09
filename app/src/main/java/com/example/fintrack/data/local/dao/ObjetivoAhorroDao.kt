package com.example.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjetivoAhorroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarObjetivo(objetivo: ObjetivoAhorro): Long

    @Update
    suspend fun actualizarObjetivo(objetivo: ObjetivoAhorro)

    @Query("DELETE FROM objetivos_ahorro WHERE id = :id")
    suspend fun eliminarObjetivoPorId(id: Long)

    @Query("SELECT * FROM objetivos_ahorro WHERE usuarioId = :usuarioId AND activo = 1 ORDER BY id DESC")
    fun obtenerObjetivosPorUsuario(usuarioId: Long): Flow<List<ObjetivoAhorro>>

    @Query("SELECT * FROM objetivos_ahorro WHERE usuarioId = :usuarioId AND activo = 1 ORDER BY id DESC")
    suspend fun obtenerObjetivosPorUsuarioSuspend(usuarioId: Long): List<ObjetivoAhorro>

    @Query("SELECT * FROM objetivos_ahorro WHERE id = :id LIMIT 1")
    suspend fun obtenerObjetivoPorId(id: Long): ObjetivoAhorro?
}
