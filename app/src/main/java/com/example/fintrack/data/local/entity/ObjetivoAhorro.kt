package com.example.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "objetivos_ahorro")
data class ObjetivoAhorro(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val nombre: String,
    val descripcion: String,
    val montoMetaUSD: Double,
    val montoActualUSD: Double = 0.0,
    val frecuenciaAporte: String,
    val aporteSugeridoUSD: Double,
    val fechaCreacion: String,
    val activo: Boolean = true
)
