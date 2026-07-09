package com.example.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "presupuestos_categoria",
    indices = [
        Index(value = ["usuarioId", "categoria", "mes", "anio"])
    ]
)
data class PresupuestoCategoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val categoria: String,
    val montoLimiteUSD: Double,
    val mes: Int,
    val anio: Int,
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis()
)
