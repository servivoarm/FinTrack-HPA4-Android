package com.example.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aportes_ahorro")
data class AporteAhorro(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val objetivoId: Long,
    val montoUSD: Double,
    val fecha: String,
    val nota: String
)
