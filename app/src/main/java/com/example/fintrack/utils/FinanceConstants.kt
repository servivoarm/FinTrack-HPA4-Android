package com.example.fintrack.utils

object FinanceConstants {
    const val TIPO_INGRESO = "INGRESO"
    const val TIPO_GASTO = "GASTO"

    const val FRECUENCIA_UNICO = "UNICO"
    const val FRECUENCIA_SEMANAL = "SEMANAL"
    const val FRECUENCIA_QUINCENAL = "QUINCENAL"
    const val FRECUENCIA_MENSUAL = "MENSUAL"

    const val PERIODO_SEMANA = "SEMANA"
    const val PERIODO_QUINCENA = "QUINCENA"
    const val PERIODO_MES = "MES"

    val FRECUENCIAS = listOf(
        FRECUENCIA_UNICO,
        FRECUENCIA_SEMANAL,
        FRECUENCIA_QUINCENAL,
        FRECUENCIA_MENSUAL
    )

    val FRECUENCIAS_AHORRO = listOf(
        FRECUENCIA_SEMANAL,
        FRECUENCIA_QUINCENAL,
        FRECUENCIA_MENSUAL
    )

    val PERIODOS = listOf(
        PERIODO_SEMANA,
        PERIODO_QUINCENA,
        PERIODO_MES
    )

    val CATEGORIAS_INGRESO = listOf(
        "Salario",
        "Inversiones",
        "Negocio",
        "Extra",
        "Regalo",
        "Otro"
    )

    val CATEGORIAS_GASTO = listOf(
        "Alimentación",
        "Transporte",
        "Servicios",
        "Salud",
        "Entretenimiento",
        "Educación",
        "Gimnasio",
        "Otro"
    )
}
