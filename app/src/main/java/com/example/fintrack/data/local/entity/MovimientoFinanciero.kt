package com.example.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos_financieros")
data class MovimientoFinanciero(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val tipo: String,
    val etiqueta: String,
    val categoria: String,
    val descripcion: String,
    val montoUSD: Double,
    val fecha: String,
    val frecuencia: String,
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis()
)
