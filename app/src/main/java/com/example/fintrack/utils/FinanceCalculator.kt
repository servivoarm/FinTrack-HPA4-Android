package com.example.fintrack.utils

import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.ui.components.ChartPoint
import com.example.fintrack.ui.components.PieSlice
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

object FinanceCalculator {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val labelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

    fun calcularMontoParaPeriodo(montoUSD: Double, frecuencia: String, periodoVista: String): Double {
        return when (frecuencia) {
            FinanceConstants.FRECUENCIA_SEMANAL -> when (periodoVista) {
                FinanceConstants.PERIODO_SEMANA -> montoUSD
                FinanceConstants.PERIODO_QUINCENA -> montoUSD * 2
                FinanceConstants.PERIODO_MES -> montoUSD * 4
                else -> montoUSD
            }
            FinanceConstants.FRECUENCIA_QUINCENAL -> when (periodoVista) {
                FinanceConstants.PERIODO_SEMANA -> montoUSD / 2
                FinanceConstants.PERIODO_QUINCENA -> montoUSD
                FinanceConstants.PERIODO_MES -> montoUSD * 2
                else -> montoUSD
            }
            FinanceConstants.FRECUENCIA_MENSUAL -> when (periodoVista) {
                FinanceConstants.PERIODO_SEMANA -> montoUSD / 4
                FinanceConstants.PERIODO_QUINCENA -> montoUSD / 2
                FinanceConstants.PERIODO_MES -> montoUSD
                else -> montoUSD
            }
            else -> montoUSD
        }
    }

    fun agruparPorEtiqueta(
        lista: List<MovimientoFinanciero>,
        periodoVista: String
    ): Map<String, Double> {
        return lista
            .filter { it.activo }
            .groupBy { it.etiqueta.ifBlank { it.categoria } }
            .mapValues { entry ->
                entry.value.sumOf {
                    calcularMontoParaPeriodo(it.montoUSD, it.frecuencia, periodoVista)
                }
            }
            .filterValues { it > 0.0 }
    }

    fun totalPorPeriodo(lista: List<MovimientoFinanciero>, periodoVista: String): Double {
        return lista
            .filter { it.activo }
            .sumOf { calcularMontoParaPeriodo(it.montoUSD, it.frecuencia, periodoVista) }
    }

    fun totalProyectadoPorPeriodo(
        lista: List<MovimientoFinanciero>,
        periodoVista: String,
        fechaBase: LocalDate = LocalDate.now()
    ): Double {
        val (inicio, fin) = obtenerRangoPeriodo(periodoVista, fechaBase)
        return generarMontosProyectadosPorFecha(lista, inicio, fin).values.sum()
    }

    fun totalProyectadoPorRango(
        lista: List<MovimientoFinanciero>,
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): Double {
        return generarMontosProyectadosPorFecha(lista, fechaInicio, fechaFin).values.sum()
    }

    fun distribucionMovimientosPorCategoria(
        movimientos: List<MovimientoFinanciero>,
        periodoVista: String,
        fechaBase: LocalDate = LocalDate.now()
    ): List<PieSlice> {
        val (inicio, fin) = obtenerRangoPeriodo(periodoVista, fechaBase)
        return distribucionMovimientosPorCategoriaEnRango(movimientos, inicio, fin)
    }

    fun distribucionMovimientosPorCategoriaEnRango(
        movimientos: List<MovimientoFinanciero>,
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): List<PieSlice> {
        return distribucionMovimientosProyectadosEnRango(
            movimientos = movimientos,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            selector = { it.categoria.ifBlank { it.etiqueta }.ifBlank { "Sin categoria" } }
        )
    }

    fun distribucionMovimientosPorEtiqueta(
        movimientos: List<MovimientoFinanciero>,
        periodoVista: String,
        fechaBase: LocalDate = LocalDate.now()
    ): List<PieSlice> {
        val (inicio, fin) = obtenerRangoPeriodo(periodoVista, fechaBase)
        return distribucionMovimientosPorEtiquetaEnRango(movimientos, inicio, fin)
    }

    fun distribucionMovimientosPorEtiquetaEnRango(
        movimientos: List<MovimientoFinanciero>,
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): List<PieSlice> {
        return distribucionMovimientosProyectadosEnRango(
            movimientos = movimientos,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            selector = { it.etiqueta.ifBlank { it.categoria }.ifBlank { "Sin etiqueta" } }
        )
    }

    fun distribucionAhorroPorObjetivo(objetivos: List<ObjetivoAhorro>): List<PieSlice> {
        return objetivos
            .filter { it.activo && it.montoActualUSD > 0.0 }
            .map {
                PieSlice(
                    label = it.nombre.ifBlank { "Objetivo" },
                    value = it.montoActualUSD
                )
            }
            .sortedByDescending { it.value }
    }

    fun distribucionResumenGeneral(
        totalIngresosUSD: Double,
        totalGastosUSD: Double,
        totalAhorrosUSD: Double
    ): List<PieSlice> {
        return listOf(
            PieSlice("Ingresos", totalIngresosUSD),
            PieSlice("Gastos", totalGastosUSD),
            PieSlice("Ahorros", totalAhorrosUSD)
        ).filter { it.value > 0.0 }
    }

    fun generarPuntosProyectadosPorTiempo(
        movimientos: List<MovimientoFinanciero>,
        periodoVista: String,
        fechaBase: LocalDate = LocalDate.now()
    ): List<ChartPoint> {
        val (inicio, fin) = obtenerRangoPeriodo(periodoVista, fechaBase)
        return generarPuntosProyectadosPorRango(movimientos, inicio, fin)
    }

    fun generarPuntosProyectadosPorRango(
        movimientos: List<MovimientoFinanciero>,
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): List<ChartPoint> {
        return generarMontosProyectadosPorFecha(movimientos, fechaInicio, fechaFin)
            .map { (fecha, monto) ->
                ChartPoint(
                    label = fecha.format(labelFormatter),
                    value = monto
                )
            }
    }

    fun generarPuntosPorTiempo(
        movimientos: List<MovimientoFinanciero>,
        periodoVista: String
    ): List<ChartPoint> {
        val hoy = LocalDate.now()
        val inicio = when (periodoVista) {
            FinanceConstants.PERIODO_SEMANA -> hoy.minusDays(6)
            FinanceConstants.PERIODO_QUINCENA -> hoy.minusDays(14)
            FinanceConstants.PERIODO_MES -> hoy.withDayOfMonth(1)
            else -> hoy.withDayOfMonth(1)
        }

        return movimientos
            .filter { it.activo }
            .mapNotNull { movimiento ->
                val fecha = parseFecha(movimiento.fecha) ?: return@mapNotNull null
                if (fecha.isBefore(inicio) || fecha.isAfter(hoy)) {
                    null
                } else {
                    fecha to movimiento.montoUSD
                }
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { (fecha, montos) ->
                ChartPoint(
                    label = fecha.format(labelFormatter),
                    value = montos.sum()
                )
            }
    }

    fun filtrarMovimientosPorMes(
        movimientos: List<MovimientoFinanciero>,
        mes: Int,
        anio: Int
    ): List<MovimientoFinanciero> {
        return movimientos.filter { movimiento ->
            val fecha = parseFecha(movimiento.fecha) ?: return@filter false
            movimiento.activo && fecha.monthValue == mes && fecha.year == anio
        }
    }

    fun filtrarMovimientosPorRango(
        movimientos: List<MovimientoFinanciero>,
        fechaInicio: LocalDate?,
        fechaFin: LocalDate?
    ): List<MovimientoFinanciero> {
        return movimientos.filter { movimiento ->
            val fecha = parseFecha(movimiento.fecha) ?: return@filter false
            movimiento.activo &&
                (fechaInicio == null || !fecha.isBefore(fechaInicio)) &&
                (fechaFin == null || !fecha.isAfter(fechaFin))
        }
    }

    fun generarPuntosBalanceAcumuladoMensual(
        movimientos: List<MovimientoFinanciero>,
        aportes: List<AporteAhorro>,
        mes: Int,
        anio: Int
    ): List<ChartPoint> {
        val valoresPorFecha = mutableMapOf<LocalDate, Double>()

        movimientos
            .filter { it.activo }
            .forEach { movimiento ->
                val fecha = parseFecha(movimiento.fecha) ?: return@forEach
                if (fecha.monthValue != mes || fecha.year != anio) return@forEach

                val monto = if (movimiento.tipo == FinanceConstants.TIPO_INGRESO) {
                    movimiento.montoUSD
                } else {
                    -movimiento.montoUSD
                }
                valoresPorFecha[fecha] = (valoresPorFecha[fecha] ?: 0.0) + monto
            }

        aportes.forEach { aporte ->
            val fecha = parseFecha(aporte.fecha) ?: return@forEach
            if (fecha.monthValue != mes || fecha.year != anio) return@forEach
            valoresPorFecha[fecha] = (valoresPorFecha[fecha] ?: 0.0) - aporte.montoUSD
        }

        var acumulado = 0.0
        return valoresPorFecha
            .toSortedMap()
            .map { (fecha, monto) ->
                acumulado += monto
                ChartPoint(
                    label = fecha.format(labelFormatter),
                    value = acumulado
                )
            }
    }

    fun obtenerCategoriaMayor(
        movimientos: List<MovimientoFinanciero>,
        tipo: String
    ): Pair<String, Double>? {
        return movimientos
            .filter { it.activo && it.tipo == tipo }
            .groupBy { it.categoria.ifBlank { "Sin categoria" } }
            .mapValues { entry -> entry.value.sumOf { it.montoUSD } }
            .filterValues { it > 0.0 }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }
    }

    fun obtenerMovimientoMayor(
        movimientos: List<MovimientoFinanciero>,
        tipo: String
    ): MovimientoFinanciero? {
        return movimientos
            .filter { it.activo && it.tipo == tipo && it.montoUSD > 0.0 }
            .maxByOrNull { it.montoUSD }
    }

    private fun distribucionMovimientosProyectadosEnRango(
        movimientos: List<MovimientoFinanciero>,
        fechaInicio: LocalDate,
        fechaFin: LocalDate,
        selector: (MovimientoFinanciero) -> String
    ): List<PieSlice> {
        val montosPorGrupo = mutableMapOf<String, Double>()

        movimientos
            .filter { it.activo }
            .forEach { movimiento ->
                val ocurrencias = generarFechasProyectadasEnRango(movimiento, fechaInicio, fechaFin).size
                if (ocurrencias > 0) {
                    val grupo = selector(movimiento)
                    montosPorGrupo[grupo] = (montosPorGrupo[grupo] ?: 0.0) + (movimiento.montoUSD * ocurrencias)
                }
            }

        return montosPorGrupo
            .map { (label, value) -> PieSlice(label, value) }
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
    }

    private fun generarMontosProyectadosPorFecha(
        movimientos: List<MovimientoFinanciero>,
        fechaInicio: LocalDate,
        fechaFin: LocalDate
    ): Map<LocalDate, Double> {
        val inicio = minOf(fechaInicio, fechaFin)
        val fin = maxOf(fechaInicio, fechaFin)
        val montosPorFecha = mutableMapOf<LocalDate, Double>()

        movimientos
            .filter { it.activo }
            .forEach { movimiento ->
                generarFechasProyectadasEnRango(movimiento, inicio, fin).forEach { fecha ->
                    montosPorFecha[fecha] = (montosPorFecha[fecha] ?: 0.0) + movimiento.montoUSD
                }
            }

        return montosPorFecha.toSortedMap()
    }

    fun obtenerRangoPeriodo(periodoVista: String, fechaBase: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        return when (periodoVista) {
            FinanceConstants.PERIODO_SEMANA -> fechaBase to fechaBase.plusDays(6)
            FinanceConstants.PERIODO_QUINCENA -> fechaBase to fechaBase.plusDays(14)
            FinanceConstants.PERIODO_MES -> {
                val inicioMes = fechaBase.withDayOfMonth(1)
                inicioMes to inicioMes.withDayOfMonth(inicioMes.lengthOfMonth())
            }
            else -> {
                val inicioMes = fechaBase.withDayOfMonth(1)
                inicioMes to inicioMes.withDayOfMonth(inicioMes.lengthOfMonth())
            }
        }
    }

    private fun generarFechasProyectadasEnRango(
        movimiento: MovimientoFinanciero,
        inicio: LocalDate,
        fin: LocalDate
    ): List<LocalDate> {
        val fechaBase = parseFecha(movimiento.fecha) ?: return emptyList()
        if (fechaBase.isAfter(fin)) return emptyList()

        return when (movimiento.frecuencia.trim().uppercase()) {
            FinanceConstants.FRECUENCIA_UNICO -> {
                if (fechaBase in inicio..fin) listOf(fechaBase) else emptyList()
            }
            FinanceConstants.FRECUENCIA_SEMANAL -> generarFechasPorIntervalo(
                fechaBase = fechaBase,
                inicio = inicio,
                fin = fin,
                diasIntervalo = 7,
                maxOcurrencias = Int.MAX_VALUE
            )
            FinanceConstants.FRECUENCIA_QUINCENAL -> generarFechasPorIntervalo(
                fechaBase = fechaBase,
                inicio = inicio,
                fin = fin,
                diasIntervalo = 15,
                maxOcurrencias = Int.MAX_VALUE
            )
            FinanceConstants.FRECUENCIA_MENSUAL -> generarFechasMensuales(
                fechaBase = fechaBase,
                inicio = inicio,
                fin = fin,
                maxOcurrencias = Int.MAX_VALUE
            )
            else -> {
                if (fechaBase in inicio..fin) listOf(fechaBase) else emptyList()
            }
        }
    }

    private fun generarFechasPorIntervalo(
        fechaBase: LocalDate,
        inicio: LocalDate,
        fin: LocalDate,
        diasIntervalo: Int,
        maxOcurrencias: Int
    ): List<LocalDate> {
        if (maxOcurrencias <= 0) return emptyList()

        var fecha = fechaBase
        if (fecha.isBefore(inicio)) {
            val diasHastaInicio = ChronoUnit.DAYS.between(fechaBase, inicio)
            val saltos = (diasHastaInicio + diasIntervalo - 1) / diasIntervalo
            fecha = fechaBase.plusDays(saltos * diasIntervalo)
        }

        val fechas = mutableListOf<LocalDate>()
        while (!fecha.isAfter(fin) && fechas.size < maxOcurrencias) {
            fechas.add(fecha)
            fecha = fecha.plusDays(diasIntervalo.toLong())
        }
        return fechas
    }

    private fun generarFechasMensuales(
        fechaBase: LocalDate,
        inicio: LocalDate,
        fin: LocalDate,
        maxOcurrencias: Int
    ): List<LocalDate> {
        if (maxOcurrencias <= 0) return emptyList()

        var mesesDesdeBase = if (fechaBase.isBefore(inicio)) {
            ChronoUnit.MONTHS.between(YearMonth.from(fechaBase), YearMonth.from(inicio))
        } else {
            0L
        }
        var fecha = fechaMensual(fechaBase, mesesDesdeBase)
        if (fecha.isBefore(inicio)) {
            mesesDesdeBase += 1
            fecha = fechaMensual(fechaBase, mesesDesdeBase)
        }

        val fechas = mutableListOf<LocalDate>()
        while (!fecha.isAfter(fin) && fechas.size < maxOcurrencias) {
            fechas.add(fecha)
            mesesDesdeBase += 1
            fecha = fechaMensual(fechaBase, mesesDesdeBase)
        }
        return fechas
    }

    private fun fechaMensual(fechaBase: LocalDate, mesesDesdeBase: Long): LocalDate {
        val mesObjetivo = YearMonth.from(fechaBase).plusMonths(mesesDesdeBase)
        return mesObjetivo.atDay(min(fechaBase.dayOfMonth, mesObjetivo.lengthOfMonth()))
    }

    fun generarPuntosAhorroAcumulado(aportes: List<AporteAhorro>): List<ChartPoint> {
        var acumulado = 0.0

        return aportes
            .mapNotNull { aporte ->
                val fecha = parseFecha(aporte.fecha) ?: return@mapNotNull null
                fecha to aporte.montoUSD
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { (fecha, montos) ->
                acumulado += montos.sum()
                ChartPoint(
                    label = fecha.format(labelFormatter),
                    value = acumulado
                )
            }
    }

    fun generarPuntosSaldoAcumulado(
        movimientos: List<MovimientoFinanciero>,
        aportes: List<AporteAhorro>
    ): List<ChartPoint> {
        val valoresPorFecha = mutableMapOf<LocalDate, Double>()

        movimientos
            .filter { it.activo }
            .forEach { movimiento ->
                val fecha = parseFecha(movimiento.fecha) ?: return@forEach
                val monto = if (movimiento.tipo == FinanceConstants.TIPO_INGRESO) {
                    movimiento.montoUSD
                } else {
                    -movimiento.montoUSD
                }
                valoresPorFecha[fecha] = (valoresPorFecha[fecha] ?: 0.0) + monto
            }

        aportes.forEach { aporte ->
            val fecha = parseFecha(aporte.fecha) ?: return@forEach
            valoresPorFecha[fecha] = (valoresPorFecha[fecha] ?: 0.0) - aporte.montoUSD
        }

        var acumulado = 0.0
        return valoresPorFecha
            .toSortedMap()
            .map { (fecha, monto) ->
                acumulado += monto
                ChartPoint(
                    label = fecha.format(labelFormatter),
                    value = acumulado
                )
            }
    }

    private fun parseFecha(fecha: String): LocalDate? {
        return runCatching {
            LocalDate.parse(fecha, dateFormatter)
        }.getOrNull()
    }
}
