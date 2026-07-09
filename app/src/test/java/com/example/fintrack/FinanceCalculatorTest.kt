package com.example.fintrack

import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.FinanceConstants
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorTest {
    private val julio2026 = LocalDate.of(2026, 7, 7)

    @Test
    fun movimientoSemanalEnMesGeneraTodasLasOcurrenciasDelRango() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorTiempo(
            movimientos = listOf(movimiento(20.0, "2026-07-01", FinanceConstants.FRECUENCIA_SEMANAL)),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(listOf("01/07", "08/07", "15/07", "22/07", "29/07"), puntos.map { it.label })
        assertEquals(100.0, puntos.sumOf { it.value }, 0.001)
    }

    @Test
    fun movimientoQuincenalEnMesGeneraOcurrenciasCadaQuinceDias() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorTiempo(
            movimientos = listOf(movimiento(300.0, "2026-07-01", FinanceConstants.FRECUENCIA_QUINCENAL)),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(listOf("01/07", "16/07", "31/07"), puntos.map { it.label })
        assertEquals(900.0, puntos.sumOf { it.value }, 0.001)
    }

    @Test
    fun movimientoMensualEnMesGeneraUnPunto() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorTiempo(
            movimientos = listOf(movimiento(800.0, "2026-07-01", FinanceConstants.FRECUENCIA_MENSUAL)),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(listOf("01/07"), puntos.map { it.label })
        assertEquals(800.0, puntos.sumOf { it.value }, 0.001)
    }

    @Test
    fun movimientoUnicoEnMesApareceSoloEnSuFecha() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorTiempo(
            movimientos = listOf(movimiento(50.0, "2026-07-10", FinanceConstants.FRECUENCIA_UNICO)),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(listOf("10/07"), puntos.map { it.label })
        assertEquals(50.0, puntos.sumOf { it.value }, 0.001)
    }

    @Test
    fun movimientosDelMismoDiaSeAgrupan() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorTiempo(
            movimientos = listOf(
                movimiento(50.0, "2026-07-10", FinanceConstants.FRECUENCIA_UNICO),
                movimiento(75.0, "2026-07-10", FinanceConstants.FRECUENCIA_UNICO)
            ),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(1, puntos.size)
        assertEquals("10/07", puntos.first().label)
        assertEquals(125.0, puntos.first().value, 0.001)
    }

    @Test
    fun fechaInvalidaSeIgnora() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorTiempo(
            movimientos = listOf(movimiento(50.0, "fecha-mala", FinanceConstants.FRECUENCIA_UNICO)),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertTrue(puntos.isEmpty())
    }

    @Test
    fun generarPuntosProyectadosPorRangoUsaFechasPersonalizadas() {
        val puntos = FinanceCalculator.generarPuntosProyectadosPorRango(
            movimientos = listOf(movimiento(20.0, "2026-07-01", FinanceConstants.FRECUENCIA_SEMANAL)),
            fechaInicio = LocalDate.of(2026, 7, 10),
            fechaFin = LocalDate.of(2026, 7, 25)
        )

        assertEquals(listOf("15/07", "22/07"), puntos.map { it.label })
        assertEquals(40.0, puntos.sumOf { it.value }, 0.001)
    }

    @Test
    fun distribucionPorCategoriaEnMesUsaFrecuenciasRecurrentes() {
        val slices = FinanceCalculator.distribucionMovimientosPorCategoria(
            movimientos = listOf(
                movimiento(
                    montoUSD = 20.0,
                    fecha = "2026-07-01",
                    frecuencia = FinanceConstants.FRECUENCIA_SEMANAL,
                    categoria = "Transporte"
                ),
                movimiento(
                    montoUSD = 40.0,
                    fecha = "2026-07-01",
                    frecuencia = FinanceConstants.FRECUENCIA_SEMANAL,
                    categoria = "Alimentación"
                ),
                movimiento(
                    montoUSD = 30.0,
                    fecha = "2026-07-01",
                    frecuencia = FinanceConstants.FRECUENCIA_MENSUAL,
                    categoria = "Salud"
                )
            ),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(listOf("Alimentación", "Transporte", "Salud"), slices.map { it.label })
        assertEquals(200.0, slices[0].value, 0.001)
        assertEquals(100.0, slices[1].value, 0.001)
        assertEquals(30.0, slices[2].value, 0.001)
    }

    @Test
    fun distribucionPorCategoriaEnMesUsaOcurrenciasQuincenalesDelRango() {
        val slices = FinanceCalculator.distribucionMovimientosPorCategoria(
            movimientos = listOf(
                movimiento(
                    montoUSD = 300.0,
                    fecha = "2026-07-01",
                    frecuencia = FinanceConstants.FRECUENCIA_QUINCENAL,
                    categoria = "Salario"
                )
            ),
            periodoVista = FinanceConstants.PERIODO_MES,
            fechaBase = julio2026
        )

        assertEquals(1, slices.size)
        assertEquals("Salario", slices.first().label)
        assertEquals(900.0, slices.first().value, 0.001)
    }

    @Test
    fun distribucionAhorroPorObjetivoIgnoraObjetivosSinAhorro() {
        val slices = FinanceCalculator.distribucionAhorroPorObjetivo(
            listOf(
                objetivo(nombre = "Viaje", montoActualUSD = 100.0),
                objetivo(nombre = "Laptop", montoActualUSD = 0.0),
                objetivo(nombre = "Fondo", montoActualUSD = 150.0)
            )
        )

        assertEquals(listOf("Fondo", "Viaje"), slices.map { it.label })
        assertEquals(150.0, slices[0].value, 0.001)
        assertEquals(100.0, slices[1].value, 0.001)
    }

    @Test
    fun distribucionResumenGeneralSoloIncluyeValoresPositivos() {
        val slices = FinanceCalculator.distribucionResumenGeneral(
            totalIngresosUSD = 800.0,
            totalGastosUSD = 0.0,
            totalAhorrosUSD = 120.0
        )

        assertEquals(listOf("Ingresos", "Ahorros"), slices.map { it.label })
        assertEquals(800.0, slices[0].value, 0.001)
        assertEquals(120.0, slices[1].value, 0.001)
    }

    private fun movimiento(
        montoUSD: Double,
        fecha: String,
        frecuencia: String,
        categoria: String = "Otro",
        etiqueta: String = "Prueba"
    ): MovimientoFinanciero {
        return MovimientoFinanciero(
            usuarioId = 1L,
            tipo = FinanceConstants.TIPO_INGRESO,
            etiqueta = etiqueta,
            categoria = categoria,
            descripcion = "",
            montoUSD = montoUSD,
            fecha = fecha,
            frecuencia = frecuencia
        )
    }

    private fun objetivo(nombre: String, montoActualUSD: Double): ObjetivoAhorro {
        return ObjetivoAhorro(
            usuarioId = 1L,
            nombre = nombre,
            descripcion = "",
            montoMetaUSD = 500.0,
            montoActualUSD = montoActualUSD,
            frecuenciaAporte = FinanceConstants.FRECUENCIA_MENSUAL,
            aporteSugeridoUSD = 50.0,
            fechaCreacion = "2026-07-01"
        )
    }
}
