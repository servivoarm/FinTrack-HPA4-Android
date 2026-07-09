package com.example.fintrack.data.repository

import com.example.fintrack.data.local.dao.PresupuestoCategoriaDao
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import kotlinx.coroutines.flow.Flow

class PresupuestoCategoriaRepository(
    private val presupuestoCategoriaDao: PresupuestoCategoriaDao
) {
    suspend fun crearPresupuesto(presupuesto: PresupuestoCategoria): Long {
        return presupuestoCategoriaDao.insertar(presupuesto)
    }

    suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoria) {
        presupuestoCategoriaDao.actualizar(presupuesto)
    }

    suspend fun eliminarPresupuesto(id: Long) {
        presupuestoCategoriaDao.eliminarPorId(id)
    }

    fun obtenerPresupuestosDelMes(
        usuarioId: Long,
        mes: Int,
        anio: Int
    ): Flow<List<PresupuestoCategoria>> {
        return presupuestoCategoriaDao.obtenerPorUsuarioMesAnio(usuarioId, mes, anio)
    }

    suspend fun obtenerPresupuestosDelMesSuspend(
        usuarioId: Long,
        mes: Int,
        anio: Int
    ): List<PresupuestoCategoria> {
        return presupuestoCategoriaDao.obtenerPorUsuarioMesAnioSuspend(usuarioId, mes, anio)
    }

    suspend fun obtenerPorId(id: Long): PresupuestoCategoria? {
        return presupuestoCategoriaDao.obtenerPorId(id)
    }

    suspend fun obtenerPorCategoriaMesAnio(
        usuarioId: Long,
        categoria: String,
        mes: Int,
        anio: Int
    ): PresupuestoCategoria? {
        return presupuestoCategoriaDao.obtenerPorCategoriaMesAnio(usuarioId, categoria, mes, anio)
    }

    suspend fun validarDuplicado(
        usuarioId: Long,
        categoria: String,
        mes: Int,
        anio: Int,
        presupuestoActualId: Long = 0L
    ): Boolean {
        val existente = obtenerPorCategoriaMesAnio(usuarioId, categoria, mes, anio)
        return existente != null && existente.id != presupuestoActualId
    }
}
