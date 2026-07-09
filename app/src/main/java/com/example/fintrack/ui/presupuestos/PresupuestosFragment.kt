package com.example.fintrack.ui.presupuestos

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import com.example.fintrack.databinding.FragmentPresupuestosBinding
import com.example.fintrack.ui.ahorro.ObjetivoAhorroAdapter
import com.example.fintrack.ui.ahorro.ObjetivoAhorroFormActivity
import com.example.fintrack.ui.profile.ProfileHeaderController
import com.example.fintrack.utils.FinanceCalculator
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.viewmodel.PresupuestosUiState
import com.example.fintrack.viewmodel.PresupuestosViewModel
import kotlinx.coroutines.launch

class PresupuestosFragment : Fragment() {
    private var _binding: FragmentPresupuestosBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PresupuestosViewModel
    private lateinit var adapter: ObjetivoAhorroAdapter
    private lateinit var presupuestoAdapter: PresupuestoCategoriaAdapter
    private lateinit var profileHeaderController: ProfileHeaderController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresupuestosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[PresupuestosViewModel::class.java]
        profileHeaderController = ProfileHeaderController(this, binding.ivPerfilPresupuestos)
        configurarRecycler()
        configurarPresupuestosCategoria()
        profileHeaderController.configurar()
        binding.lineChartAhorros.setEmptyMessage("Sin aportes registrados para graficar")
        binding.pieChartAhorros.setEmptyMessage("Sin datos para graficar")

        binding.btnNuevoObjetivo.setOnClickListener {
            startActivity(Intent(requireContext(), ObjetivoAhorroFormActivity::class.java))
        }

        binding.btnNuevoPresupuestoCategoria.setOnClickListener {
            startActivity(Intent(requireContext(), PresupuestoCategoriaFormActivity::class.java))
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
        adapter = ObjetivoAhorroAdapter(
            onAgregarAporte = { objetivo -> mostrarDialogoAporte(objetivo) },
            onEliminar = { objetivo -> confirmarEliminacion(objetivo) }
        )
        binding.rvObjetivos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObjetivos.adapter = adapter
        binding.rvObjetivos.isNestedScrollingEnabled = true
        binding.rvObjetivos.setHasFixedSize(false)
    }

    private fun configurarPresupuestosCategoria() {
        presupuestoAdapter = PresupuestoCategoriaAdapter(
            onEditar = { presupuesto ->
                val intent = Intent(requireContext(), PresupuestoCategoriaFormActivity::class.java).apply {
                    putExtra(PresupuestoCategoriaFormActivity.EXTRA_PRESUPUESTO_ID, presupuesto.id)
                }
                startActivity(intent)
            },
            onEliminar = { presupuesto -> confirmarEliminacionPresupuesto(presupuesto) }
        )
        binding.rvPresupuestosCategoria.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPresupuestosCategoria.adapter = presupuestoAdapter
        binding.rvPresupuestosCategoria.isNestedScrollingEnabled = true
        binding.rvPresupuestosCategoria.setHasFixedSize(false)
    }

    private fun actualizarDatos(state: PresupuestosUiState) {
        binding.tvIngresosMensualesEstimados.text = "Ingresos mensuales estimados: ${MoneyUtils.formatearMonto(state.ingresosMensualesEstimadosUSD, state.moneda)}"
        binding.tvGastosMensualesEstimados.text = "Gastos mensuales estimados: ${MoneyUtils.formatearMonto(state.gastosMensualesEstimadosUSD, state.moneda)}"
        binding.tvAhorrosSeparados.text = "Ahorros separados: ${MoneyUtils.formatearMonto(state.totalAhorrosUSD, state.moneda)}"
        binding.tvSaldoDisponibleEstimado.text = "Saldo disponible estimado: ${MoneyUtils.formatearMonto(state.saldoDisponibleEstimadoUSD, state.moneda)}"
        binding.tvTotalPresupuestado.text = "Total presupuestado: ${MoneyUtils.formatearMonto(state.totalPresupuestadoUSD, state.moneda)}"
        binding.tvTotalGastadoPresupuestos.text = "Gastado en presupuestos: ${MoneyUtils.formatearMonto(state.totalGastadoPresupuestosUSD, state.moneda)}"
        binding.tvTotalRestantePresupuestos.text = "Restante en presupuestos: ${MoneyUtils.formatearMonto(state.totalRestantePresupuestosUSD.coerceAtLeast(0.0), state.moneda)}"
        binding.tvSinPresupuestosCategoria.visibility = if (state.presupuestosCategoria.isEmpty()) View.VISIBLE else View.GONE
        presupuestoAdapter.actualizarDatos(state.presupuestosCategoria, state.moneda)
        binding.tvSinObjetivos.visibility = if (state.objetivos.isEmpty()) View.VISIBLE else View.GONE
        adapter.actualizarDatos(state.objetivos, state.moneda)
        binding.lineChartAhorros.setData(
            FinanceCalculator.generarPuntosAhorroAcumulado(state.aportes),
            state.moneda
        )
        binding.pieChartAhorros.setData(
            FinanceCalculator.distribucionAhorroPorObjetivo(state.objetivos),
            state.moneda
        )
    }

    private fun mostrarDialogoAporte(objetivo: ObjetivoAhorro) {
        val contenedor = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val etMonto = EditText(requireContext()).apply {
            hint = "Monto"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etNota = EditText(requireContext()).apply {
            hint = "Nota opcional"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        contenedor.addView(etMonto)
        contenedor.addView(etNota)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Agregar aporte")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Agregar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val monto = etMonto.text.toString().trim().replace(",", ".").toDoubleOrNull()
                val state = viewModel.uiState.value
                if (monto == null) {
                    etMonto.error = "El monto es obligatorio"
                    return@setOnClickListener
                }
                if (monto <= 0.0) {
                    etMonto.error = "El monto debe ser mayor que 0"
                    return@setOnClickListener
                }

                val montoUSD = MoneyUtils.convertirAUSD(monto, state.moneda)
                if (montoUSD > state.saldoActualUSD) {
                    etMonto.error = "No puedes aportar mas que el saldo disponible"
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val result = viewModel.agregarAporte(
                        objetivoId = objetivo.id,
                        montoUSD = montoUSD,
                        nota = etNota.text.toString().trim()
                    )
                    Toast.makeText(requireContext(), result.mensaje, Toast.LENGTH_SHORT).show()
                    if (result.exitoso) {
                        viewModel.recargarDatos()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmarEliminacion(objetivo: ObjetivoAhorro) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar objetivo")
            .setMessage("Seguro que deseas eliminar este objetivo?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarObjetivo(objetivo.id)
            }
            .show()
    }

    private fun confirmarEliminacionPresupuesto(presupuesto: PresupuestoCategoria) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar presupuesto")
            .setMessage("Seguro que deseas eliminar el presupuesto de ${presupuesto.categoria}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarPresupuestoCategoria(presupuesto.id)
            }
            .show()
    }
}
