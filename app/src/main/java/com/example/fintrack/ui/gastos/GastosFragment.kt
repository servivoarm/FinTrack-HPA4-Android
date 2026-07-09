package com.example.fintrack.ui.gastos

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintrack.R
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.databinding.FragmentGastosBinding
import com.example.fintrack.ui.movimientos.MovimientoAdapter
import com.example.fintrack.ui.movimientos.MovimientoFormActivity
import com.example.fintrack.ui.profile.ProfileHeaderController
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.utils.NotificationScheduler
import com.example.fintrack.viewmodel.MovimientosUiState
import com.example.fintrack.viewmodel.MovimientosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class GastosFragment : Fragment() {
    private var _binding: FragmentGastosBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MovimientosViewModel
    private lateinit var adapter: MovimientoAdapter
    private lateinit var profileHeaderController: ProfileHeaderController
    private var periodoVista = FinanceConstants.PERIODO_MES
    private var fechaReferencia = LocalDate.now()
    private var fechaInicioSeleccionada: LocalDate? = null
    private var fechaFinSeleccionada: LocalDate? = null
    private var rangoPersonalizadoActivo = false
    private val localeEs = Locale.forLanguageTag("es-ES")
    private val formatoCampoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val formatoRangoCorto = DateTimeFormatter.ofPattern("dd MMM", localeEs)
    private val formatoRangoCompleto = DateTimeFormatter.ofPattern("dd MMM yyyy", localeEs)
    private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", localeEs)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGastosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MovimientosViewModel::class.java]
        profileHeaderController = ProfileHeaderController(this, binding.ivPerfilGastos)
        configurarRecycler()
        configurarPeriodo()
        configurarNavegacionTemporal()
        profileHeaderController.configurar()
        binding.lineChartGastos.setEmptyMessage("Sin gastos para graficar")
        binding.pieChartGastos.setEmptyMessage("Sin gastos para graficar")

        binding.btnAgregarGasto.setOnClickListener {
            val intent = Intent(requireContext(), MovimientoFormActivity::class.java).apply {
                putExtra(MovimientoFormActivity.EXTRA_TIPO, FinanceConstants.TIPO_GASTO)
            }
            startActivity(intent)
        }

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

    private fun configurarRecycler() {
        adapter = MovimientoAdapter(
            onEditar = { movimiento ->
                val intent = Intent(requireContext(), MovimientoFormActivity::class.java).apply {
                    putExtra(MovimientoFormActivity.EXTRA_TIPO, FinanceConstants.TIPO_GASTO)
                    putExtra(MovimientoFormActivity.EXTRA_MOVIMIENTO_ID, movimiento.id)
                }
                startActivity(intent)
            },
            onEliminar = { movimiento ->
                confirmarEliminacion(movimiento)
            }
        )
        binding.rvGastos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGastos.adapter = adapter
        binding.rvGastos.isNestedScrollingEnabled = true
        binding.rvGastos.setHasFixedSize(false)
    }

    private fun configurarPeriodo() {
        val opciones = listOf("Semana", "Quincena", "Mes")
        binding.spinnerPeriodo.adapter = ArrayAdapter(
            requireContext(),
            com.example.fintrack.R.layout.spinner_item_selected,
            opciones
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerPeriodo.setSelection(2)
        binding.spinnerPeriodo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                periodoVista = when (position) {
                    0 -> FinanceConstants.PERIODO_SEMANA
                    1 -> FinanceConstants.PERIODO_QUINCENA
                    else -> FinanceConstants.PERIODO_MES
                }
                limpiarRangoPersonalizado(actualizar = false)
                if (::viewModel.isInitialized) {
                    actualizarDatos(viewModel.uiState.value)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun configurarNavegacionTemporal() {
        binding.btnPeriodoAnterior.setOnClickListener {
            limpiarRangoPersonalizado(actualizar = false)
            fechaReferencia = moverFechaReferencia(haciaAdelante = false)
            actualizarDatos(viewModel.uiState.value)
        }

        binding.btnPeriodoSiguiente.setOnClickListener {
            limpiarRangoPersonalizado(actualizar = false)
            fechaReferencia = moverFechaReferencia(haciaAdelante = true)
            actualizarDatos(viewModel.uiState.value)
        }

        binding.tvFechaInicio.setOnClickListener {
            mostrarSelectorFecha(fechaInicioSeleccionada ?: rangoActivo().first) { fecha ->
                fechaInicioSeleccionada = fecha
                actualizarCamposRango()
            }
        }

        binding.tvFechaFin.setOnClickListener {
            mostrarSelectorFecha(fechaFinSeleccionada ?: rangoActivo().second) { fecha ->
                fechaFinSeleccionada = fecha
                actualizarCamposRango()
            }
        }

        binding.btnAplicarRango.setOnClickListener {
            val inicio = fechaInicioSeleccionada
            val fin = fechaFinSeleccionada
            if (inicio == null || fin == null) {
                Toast.makeText(requireContext(), "Selecciona fecha inicio y fecha fin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (inicio.isAfter(fin)) {
                Toast.makeText(requireContext(), "La fecha inicio no puede ser mayor que la fecha fin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            rangoPersonalizadoActivo = true
            actualizarDatos(viewModel.uiState.value)
        }

        binding.btnLimpiarRango.setOnClickListener {
            fechaReferencia = LocalDate.now()
            limpiarRangoPersonalizado(actualizar = true)
        }

        actualizarCamposRango()
    }

    private fun actualizarDatos(state: MovimientosUiState) {
        val gastosOrdenados = state.gastos.ordenadosRecientes()
        val (fechaInicio, fechaFin) = rangoActivo()
        val puntosGrafica = FinanceCalculator.generarPuntosProyectadosPorRango(
            gastosOrdenados,
            fechaInicio,
            fechaFin
        )
        val totalPeriodo = puntosGrafica.sumOf { it.value }
        binding.tvSaldoActual.text = "Saldo actual: ${MoneyUtils.formatearMonto(state.saldoActualUSD, state.moneda)}"
        binding.tvTotalGastosPeriodo.text = "Total gastos del periodo: ${MoneyUtils.formatearMonto(totalPeriodo, state.moneda)}"
        binding.tvRangoVisible.text = textoRangoVisible(fechaInicio, fechaFin)
        binding.tvSinGastos.visibility = if (gastosOrdenados.isEmpty()) View.VISIBLE else View.GONE
        adapter.actualizarDatos(gastosOrdenados, state.moneda)
        binding.lineChartGastos.setData(puntosGrafica, state.moneda)
        binding.pieChartGastos.setData(
            FinanceCalculator.distribucionMovimientosPorCategoriaEnRango(
                gastosOrdenados,
                fechaInicio,
                fechaFin
            ),
            state.moneda
        )
        actualizarCamposRango()
    }

    private fun rangoActivo(): Pair<LocalDate, LocalDate> {
        val inicio = fechaInicioSeleccionada
        val fin = fechaFinSeleccionada
        return if (rangoPersonalizadoActivo && inicio != null && fin != null) {
            inicio to fin
        } else {
            FinanceCalculator.obtenerRangoPeriodo(periodoVista, fechaReferencia)
        }
    }

    private fun moverFechaReferencia(haciaAdelante: Boolean): LocalDate {
        val factor = if (haciaAdelante) 1L else -1L
        return when (periodoVista) {
            FinanceConstants.PERIODO_SEMANA -> fechaReferencia.plusDays(7L * factor)
            FinanceConstants.PERIODO_QUINCENA -> fechaReferencia.plusDays(15L * factor)
            FinanceConstants.PERIODO_MES -> fechaReferencia.plusMonths(factor)
            else -> fechaReferencia.plusMonths(factor)
        }
    }

    private fun mostrarSelectorFecha(fechaInicial: LocalDate, onSeleccionada: (LocalDate) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                onSeleccionada(LocalDate.of(year, month + 1, dayOfMonth))
            },
            fechaInicial.year,
            fechaInicial.monthValue - 1,
            fechaInicial.dayOfMonth
        ).show()
    }

    private fun limpiarRangoPersonalizado(actualizar: Boolean) {
        fechaInicioSeleccionada = null
        fechaFinSeleccionada = null
        rangoPersonalizadoActivo = false
        actualizarCamposRango()
        if (actualizar && ::viewModel.isInitialized) {
            actualizarDatos(viewModel.uiState.value)
        }
    }

    private fun actualizarCamposRango() {
        binding.tvFechaInicio.text = fechaInicioSeleccionada?.format(formatoCampoFecha)
            ?: getString(R.string.start_date)
        binding.tvFechaFin.text = fechaFinSeleccionada?.format(formatoCampoFecha)
            ?: getString(R.string.end_date)
        val colorInicio = if (fechaInicioSeleccionada == null) R.color.text_secondary_gray else R.color.brown_primary
        val colorFin = if (fechaFinSeleccionada == null) R.color.text_secondary_gray else R.color.brown_primary
        binding.tvFechaInicio.setTextColor(requireContext().getColor(colorInicio))
        binding.tvFechaFin.setTextColor(requireContext().getColor(colorFin))
    }

    private fun textoRangoVisible(inicio: LocalDate, fin: LocalDate): String {
        if (!rangoPersonalizadoActivo && periodoVista == FinanceConstants.PERIODO_MES) {
            return inicio.format(formatoMes).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(localeEs) else char.toString()
            }
        }
        return "${inicio.format(formatoRangoCorto)} - ${fin.format(formatoRangoCompleto)}"
    }

    private fun List<MovimientoFinanciero>.ordenadosRecientes(): List<MovimientoFinanciero> {
        return sortedWith(
            compareByDescending<MovimientoFinanciero> { it.fecha }
                .thenByDescending { it.creadoEn }
        )
    }

    private fun confirmarEliminacion(movimiento: MovimientoFinanciero) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar gasto")
            .setMessage("Seguro que deseas eliminar este gasto?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                NotificationScheduler.cancelExpenseReminder(requireContext(), movimiento.id)
                viewModel.eliminarMovimiento(movimiento.id)
            }
            .show()
    }
}
