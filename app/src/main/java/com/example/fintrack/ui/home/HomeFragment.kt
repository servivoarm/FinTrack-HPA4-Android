package com.example.fintrack.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fintrack.R
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.databinding.FragmentHomeBinding
import com.example.fintrack.ui.ahorro.ObjetivoAhorroFormActivity
import com.example.fintrack.ui.historial.HistorialActivity
import com.example.fintrack.ui.movimientos.MovimientoFormActivity
import com.example.fintrack.ui.profile.ProfileHeaderController
import com.example.fintrack.ui.reportes.ReporteActivity
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.MovimientosUiState
import com.example.fintrack.viewmodel.MovimientosViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MovimientosViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var profileHeaderController: ProfileHeaderController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[MovimientosViewModel::class.java]
        profileHeaderController = ProfileHeaderController(this, binding.ivPerfilMini)

        binding.lineChartSaldo.setEmptyMessage("Sin datos para graficar")
        binding.pieChartResumen.setEmptyMessage("Sin datos para graficar")
        profileHeaderController.configurar()
        configurarAccesosRapidos()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    actualizarDatos(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.recargarDatos()
        }
        if (::profileHeaderController.isInitialized) {
            profileHeaderController.recargarFoto()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun actualizarDatos(state: MovimientosUiState) {
        val moneda = state.moneda
        val ingresosMes = FinanceCalculator.totalProyectadoPorPeriodo(state.ingresos, FinanceConstants.PERIODO_MES)
        val gastosMes = FinanceCalculator.totalProyectadoPorPeriodo(state.gastos, FinanceConstants.PERIODO_MES)
        val balanceMensual = ingresosMes - gastosMes - state.totalAhorrosUSD

        binding.tvHola.text = "Hola, ${sessionManager.obtenerNombre()}"
        binding.tvSaldoDisponible.text = MoneyUtils.formatearMonto(state.saldoActualUSD, moneda)
        binding.tvResumenIngresosMonto.text = MoneyUtils.formatearMonto(state.totalIngresosUSD, moneda)
        binding.tvResumenGastosMonto.text = MoneyUtils.formatearMonto(state.totalGastosUSD, moneda)
        binding.tvResumenAhorradoMonto.text = MoneyUtils.formatearMonto(state.totalAhorrosUSD, moneda)
        binding.tvResumenBalanceMonto.text = MoneyUtils.formatearMonto(balanceMensual, moneda)
        binding.pieChartResumen.setData(
            FinanceCalculator.distribucionResumenGeneral(
                totalIngresosUSD = state.totalIngresosUSD,
                totalGastosUSD = state.totalGastosUSD,
                totalAhorrosUSD = state.totalAhorrosUSD
            ),
            moneda
        )
        binding.lineChartSaldo.setData(
            FinanceCalculator.generarPuntosSaldoAcumulado(state.todos, state.aportes),
            moneda
        )
        mostrarUltimosMovimientos(state.todos.take(5), moneda)
    }

    private fun mostrarUltimosMovimientos(movimientos: List<MovimientoFinanciero>, moneda: String) {
        binding.contenedorUltimosMovimientos.removeAllViews()
        binding.tvSinMovimientos.visibility = if (movimientos.isEmpty()) View.VISIBLE else View.GONE

        movimientos.forEach { movimiento ->
            val esIngreso = movimiento.tipo == FinanceConstants.TIPO_INGRESO
            val contenedor = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 14, 18, 14)
                setBackgroundColor(Color.WHITE)
            }
            val titulo = TextView(requireContext()).apply {
                text = movimiento.etiqueta
                textSize = 16f
                setTextColor(Color.parseColor("#172033"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val detalle = TextView(requireContext()).apply {
                val tipo = if (esIngreso) "Ingreso" else "Gasto"
                text = "$tipo - ${movimiento.categoria} - ${movimiento.fecha}"
                textSize = 13f
                setTextColor(Color.parseColor("#738197"))
                setPadding(0, 4, 0, 0)
            }
            val monto = TextView(requireContext()).apply {
                val signo = if (esIngreso) "+" else "-"
                text = "$signo${MoneyUtils.formatearMonto(movimiento.montoUSD, moneda)}"
                textSize = 15f
                setTextColor(if (esIngreso) Color.parseColor("#2E7D55") else Color.parseColor("#B44B4B"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 6, 0, 0)
            }

            contenedor.addView(titulo)
            contenedor.addView(detalle)
            contenedor.addView(monto)
            binding.contenedorUltimosMovimientos.addView(contenedor)
        }
    }

    private fun configurarAccesosRapidos() {
        binding.btnQuickIngreso.setOnClickListener {
            val intent = Intent(requireContext(), MovimientoFormActivity::class.java).apply {
                putExtra(MovimientoFormActivity.EXTRA_TIPO, FinanceConstants.TIPO_INGRESO)
            }
            startActivity(intent)
        }

        binding.btnQuickGasto.setOnClickListener {
            val intent = Intent(requireContext(), MovimientoFormActivity::class.java).apply {
                putExtra(MovimientoFormActivity.EXTRA_TIPO, FinanceConstants.TIPO_GASTO)
            }
            startActivity(intent)
        }

        binding.btnQuickObjetivo.setOnClickListener {
            startActivity(Intent(requireContext(), ObjetivoAhorroFormActivity::class.java))
        }

        binding.btnQuickReporte.setOnClickListener {
            startActivity(Intent(requireContext(), ReporteActivity::class.java))
        }

        binding.btnQuickHistorial.setOnClickListener {
            startActivity(Intent(requireContext(), HistorialActivity::class.java))
        }
    }
}
