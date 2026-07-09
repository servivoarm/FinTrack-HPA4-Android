package com.example.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import kotlinx.coroutines.flow.Flow

@Dao
interface PresupuestoCategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(presupuesto: PresupuestoCategoria): Long

    @Update
    suspend fun actualizar(presupuesto: PresupuestoCategoria)

    @Query("DELETE FROM presupuestos_categoria WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    @Query("SELECT * FROM presupuestos_categoria WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): PresupuestoCategoria?

    @Query(
        "SELECT * FROM presupuestos_categoria " +
            "WHERE usuarioId = :usuarioId AND mes = :mes AND anio = :anio AND activo = 1 " +
            "ORDER BY categoria ASC"
    )
    fun obtenerPorUsuarioMesAnio(
        usuarioId: Long,
        mes: Int,
        anio: Int
    ): Flow<List<PresupuestoCategoria>>

    @Query(
        "SELECT * FROM presupuestos_categoria " +
            "WHERE usuarioId = :usuarioId AND mes = :mes AND anio = :anio AND activo = 1 " +
            "ORDER BY categoria ASC"
    )
    suspend fun obtenerPorUsuarioMesAnioSuspend(
        usuarioId: Long,
        mes: Int,
        anio: Int
    ): List<PresupuestoCategoria>

    @Query(
        "SELECT * FROM presupuestos_categoria " +
            "WHERE usuarioId = :usuarioId AND categoria = :categoria AND mes = :mes AND anio = :anio AND activo = 1 " +
            "LIMIT 1"
    )
    suspend fun obtenerPorCategoriaMesAnio(
        usuarioId: Long,
        categoria: String,
        mes: Int,
        anio: Int
    ): PresupuestoCategoria?
}
