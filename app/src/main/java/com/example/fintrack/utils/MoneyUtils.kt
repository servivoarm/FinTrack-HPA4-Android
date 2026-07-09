package com.example.fintrack.utils

import java.util.Locale

object MoneyUtils {
    private fun obtenerTasa(moneda: String): Double {
        return when (moneda.uppercase(Locale.getDefault())) {
            "PAB" -> 1.0
            "EUR" -> 0.92
            "COP" -> 4000.0
            "MXN" -> 18.0
            else -> 1.0
        }
    }

    fun obtenerSimbolo(moneda: String): String {
        return when (moneda.uppercase(Locale.getDefault())) {
            "PAB" -> "B/."
            "EUR" -> "\u20AC"
            "COP" -> "COP $"
            "MXN" -> "MXN $"
            else -> "$"
        }
    }

    fun convertirDesdeUSD(montoUSD: Double, moneda: String): Double {
        return montoUSD * obtenerTasa(moneda)
    }

    fun convertirAUSD(monto: Double, moneda: String): Double {
        return monto / obtenerTasa(moneda)
    }

    fun formatearMonto(montoUSD: Double, moneda: String): String {
        val codigo = moneda.uppercase(Locale.getDefault())
        val montoConvertido = convertirDesdeUSD(montoUSD, codigo)
        val simbolo = obtenerSimbolo(codigo)

        return if (codigo == "COP") {
            String.format(Locale.US, "%s%.0f", simbolo, montoConvertido)
        } else {
            String.format(Locale.US, "%s%.2f", simbolo, montoConvertido)
        }
    }
}
