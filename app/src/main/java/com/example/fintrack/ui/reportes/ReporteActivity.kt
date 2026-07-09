package com.example.fintrack.ui.reportes

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.databinding.ActivityReporteBinding
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.viewmodel.ReporteUiState
import com.example.fintrack.viewmodel.ReporteViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class ReporteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReporteBinding
    private val viewModel: ReporteViewModel by viewModels()
    private var mesActual: YearMonth = YearMonth.now()
    private val localeEs = Locale.forLanguageTag("es-ES")
    private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", localeEs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarGraficas()
        configurarBotones()
        observarEstado()
        cargarReporte()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            cargarReporte()
        }
    }

    private fun configurarGraficas() {
        binding.pieChartGastos.setEmptyMessage("Sin gastos para graficar")
        binding.pieChartIngresos.setEmptyMessage("Sin ingresos para graficar")
        binding.lineChartBalance.setEmptyMessage("Sin balance para graficar")
    }

    private fun configurarBotones() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnMesAnterior.setOnClickListener {
            mesActual = mesActual.minusMonths(1)
            cargarReporte()
        }

        binding.btnMesSiguiente.setOnClickListener {
            mesActual = mesActual.plusMonths(1)
            cargarReporte()
        }

        binding.btnCompartirReporte.setOnClickListener {
            compartirReporte(viewModel.uiState.value.textoCompartir)
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    actualizarUi(state)
                }
            }
        }
    }

    private fun cargarReporte() {
        viewModel.cargarReporte(mesActual.monthValue, mesActual.year)
    }

    private fun actualizarUi(state: ReporteUiState) {
        binding.tvMesReporte.text = formatearMes(state.mes, state.anio)
        binding.tvTotalIngresos.text = MoneyUtils.formatearMonto(state.totalIngresosUSD, state.moneda)
        binding.tvTotalGastos.text = MoneyUtils.formatearMonto(state.totalGastosUSD, state.moneda)
        binding.tvTotalAhorros.text = MoneyUtils.formatearMonto(state.totalAhorrosUSD, state.moneda)
        binding.tvBalanceFinal.text = MoneyUtils.formatearMonto(state.balanceFinalUSD, state.moneda)
        binding.tvMayorGastoCategoria.text = formatearPar("Mayor gasto por categoria", state.categoriaMayorGasto, state.moneda)
        binding.tvMayorIngresoCategoria.text = formatearPar("Mayor ingreso por categoria", state.categoriaMayorIngreso, state.moneda)
        binding.tvMayorGastoMovimiento.text = formatearMovimiento("Mayor gasto individual", state.movimientoMayorGasto, state.moneda)
        binding.tvMayorIngresoMovimiento.text = formatearMovimiento("Mayor ingreso individual", state.movimientoMayorIngreso, state.moneda)
        binding.tvResumenReporte.text = state.resumenTexto
        binding.pieChartGastos.setData(state.distribucionGastos, state.moneda)
        binding.pieChartIngresos.setData(state.distribucionIngresos, state.moneda)
        binding.lineChartBalance.setData(state.puntosBalance, state.moneda)
    }

    private fun formatearPar(
        titulo: String,
        valor: Pair<String, Double>?,
        moneda: String
    ): String {
        return valor?.let {
            "$titulo: ${it.first} - ${MoneyUtils.formatearMonto(it.second, moneda)}"
        } ?: "$titulo: Sin datos"
    }

    private fun formatearMovimiento(
        titulo: String,
        movimiento: MovimientoFinanciero?,
        moneda: String
    ): String {
        return movimiento?.let {
            "$titulo: ${it.etiqueta} - ${MoneyUtils.formatearMonto(it.montoUSD, moneda)} (${it.fecha})"
        } ?: "$titulo: Sin datos"
    }

    private fun compartirReporte(texto: String) {
        if (texto.isBlank()) return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Reporte mensual FinTrack")
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }

    private fun formatearMes(mes: Int, anio: Int): String {
        return YearMonth.of(anio, mes)
            .format(formatoMes)
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(localeEs) else char.toString()
            }
    }
}
