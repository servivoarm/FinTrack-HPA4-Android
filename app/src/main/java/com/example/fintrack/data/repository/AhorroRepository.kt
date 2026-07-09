package com.example.fintrack.data.repository

import com.example.fintrack.data.local.dao.AporteAhorroDao
import com.example.fintrack.data.local.dao.ObjetivoAhorroDao
import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import kotlinx.coroutines.flow.Flow

class AhorroRepository(
    private val objetivoAhorroDao: ObjetivoAhorroDao,
    private val aporteAhorroDao: AporteAhorroDao
) {
    suspend fun crearObjetivo(objetivo: ObjetivoAhorro): Long {
        return objetivoAhorroDao.insertarObjetivo(objetivo)
    }

    suspend fun actualizarObjetivo(objetivo: ObjetivoAhorro) {
        objetivoAhorroDao.actualizarObjetivo(objetivo)
    }

    suspend fun eliminarObjetivo(id: Long) {
        aporteAhorroDao.eliminarAportesPorObjetivo(id)
        objetivoAhorroDao.eliminarObjetivoPorId(id)
    }

    fun obtenerObjetivos(usuarioId: Long): Flow<List<ObjetivoAhorro>> {
        return objetivoAhorroDao.obtenerObjetivosPorUsuario(usuarioId)
    }

    suspend fun obtenerObjetivosSuspend(usuarioId: Long): List<ObjetivoAhorro> {
        return objetivoAhorroDao.obtenerObjetivosPorUsuarioSuspend(usuarioId)
    }

    fun obtenerAportesPorUsuario(usuarioId: Long): Flow<List<AporteAhorro>> {
        return aporteAhorroDao.obtenerAportesPorUsuario(usuarioId)
    }

    suspend fun obtenerAportesPorUsuarioSuspend(usuarioId: Long): List<AporteAhorro> {
        return aporteAhorroDao.obtenerAportesPorUsuarioSuspend(usuarioId)
    }

    suspend fun agregarAporte(
        objetivoId: Long,
        usuarioId: Long,
        montoUSD: Double,
        fecha: String,
        nota: String
    ): Long {
        val objetivo = objetivoAhorroDao.obtenerObjetivoPorId(objetivoId)
        val aporteId = aporteAhorroDao.insertarAporte(
            AporteAhorro(
                usuarioId = usuarioId,
                objetivoId = objetivoId,
                montoUSD = montoUSD,
                fecha = fecha,
                nota = nota
            )
        )

        if (objetivo != null) {
            objetivoAhorroDao.actualizarObjetivo(
                objetivo.copy(montoActualUSD = objetivo.montoActualUSD + montoUSD)
            )
        }

        return aporteId
    }

    suspend fun calcularTotalAhorros(usuarioId: Long): Double {
        return aporteAhorroDao.sumarAportesPorUsuario(usuarioId) ?: 0.0
    }
}
