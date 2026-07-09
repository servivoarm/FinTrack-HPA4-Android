package com.example.fintrack.ui.historial

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fintrack.R
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.databinding.ActivityHistorialBinding
import com.example.fintrack.ui.movimientos.MovimientoAdapter
import com.example.fintrack.ui.movimientos.MovimientoFormActivity
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.NotificationScheduler
import com.example.fintrack.viewmodel.HistorialUiState
import com.example.fintrack.viewmodel.HistorialViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistorialBinding
    private lateinit var adapter: MovimientoAdapter
    private val viewModel: HistorialViewModel by viewModels()
    private val tipos = listOf(
        HistorialViewModel.FILTRO_TODOS,
        HistorialViewModel.FILTRO_INGRESOS,
        HistorialViewModel.FILTRO_GASTOS
    )
    private var categoriasActuales = listOf(HistorialViewModel.FILTRO_TODAS)
    private var fechaInicio: LocalDate? = null
    private var fechaFin: LocalDate? = null
    private val formatoCampo = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarRecycler()
        configurarFiltros()
        observarEstado()
        aplicarFiltros()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            aplicarFiltros()
        }
    }

    private fun configurarRecycler() {
        adapter = MovimientoAdapter(
            onEditar = { movimiento -> abrirEdicion(movimiento) },
            onEliminar = { movimiento -> confirmarEliminacion(movimiento) }
        )
        binding.rvHistorial.layoutManager = LinearLayoutManager(this)
        binding.rvHistorial.adapter = adapter
        binding.rvHistorial.setHasFixedSize(false)
    }

    private fun configurarFiltros() {
        binding.btnBackHistorial.setOnClickListener { finish() }
        binding.spinnerTipo.adapter = crearAdapter(tipos)
        binding.spinnerCategoria.adapter = crearAdapter(categoriasActuales)

        binding.tvFechaInicio.setOnClickListener {
            mostrarSelectorFecha(fechaInicio ?: LocalDate.now()) { fecha ->
                fechaInicio = fecha
                actualizarCamposFecha()
            }
        }

        binding.tvFechaFin.setOnClickListener {
            mostrarSelectorFecha(fechaFin ?: LocalDate.now()) { fecha ->
                fechaFin = fecha
                actualizarCamposFecha()
            }
        }

        binding.btnAplicarFiltros.setOnClickListener {
            aplicarFiltros()
        }

        binding.btnLimpiarFiltros.setOnClickListener {
            limpiarFiltros()
        }

        actualizarCamposFecha()
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

    private fun actualizarUi(state: HistorialUiState) {
        actualizarCategorias(state.categorias)
        adapter.actualizarDatos(state.movimientos, state.moneda)
        binding.tvConteoHistorial.text = "${state.movimientos.size} movimientos"
        binding.tvSinMovimientosHistorial.visibility = if (state.movimientos.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun aplicarFiltros() {
        val inicio = fechaInicio
        val fin = fechaFin
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            Toast.makeText(this, "La fecha inicio no puede ser mayor que la fecha fin", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.cargarDatos(
            tipoFiltro = binding.spinnerTipo.selectedItem?.toString() ?: HistorialViewModel.FILTRO_TODOS,
            categoriaFiltro = binding.spinnerCategoria.selectedItem?.toString() ?: HistorialViewModel.FILTRO_TODAS,
            fechaInicio = inicio,
            fechaFin = fin
        )
    }

    private fun limpiarFiltros() {
        fechaInicio = null
        fechaFin = null
        binding.spinnerTipo.setSelection(0)
        binding.spinnerCategoria.setSelection(0)
        actualizarCamposFecha()
        viewModel.cargarDatos()
    }

    private fun actualizarCategorias(categorias: List<String>) {
        if (categorias == categoriasActuales) return

        val seleccionActual = binding.spinnerCategoria.selectedItem?.toString() ?: HistorialViewModel.FILTRO_TODAS
        categoriasActuales = categorias
        binding.spinnerCategoria.adapter = crearAdapter(categoriasActuales)
        val nuevaPosicion = categoriasActuales.indexOf(seleccionActual).takeIf { it >= 0 } ?: 0
        binding.spinnerCategoria.setSelection(nuevaPosicion)
    }

    private fun mostrarSelectorFecha(
        fechaInicial: LocalDate,
        onSeleccionada: (LocalDate) -> Unit
    ) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                onSeleccionada(LocalDate.of(year, month + 1, dayOfMonth))
            },
            fechaInicial.year,
            fechaInicial.monthValue - 1,
            fechaInicial.dayOfMonth
        ).show()
    }

    private fun actualizarCamposFecha() {
        binding.tvFechaInicio.text = fechaInicio?.format(formatoCampo) ?: "Desde"
        binding.tvFechaFin.text = fechaFin?.format(formatoCampo) ?: "Hasta"
        binding.tvFechaInicio.setTextColor(getColor(if (fechaInicio == null) R.color.text_secondary_gray else R.color.brown_primary))
        binding.tvFechaFin.setTextColor(getColor(if (fechaFin == null) R.color.text_secondary_gray else R.color.brown_primary))
    }

    private fun abrirEdicion(movimiento: MovimientoFinanciero) {
        val intent = Intent(this, MovimientoFormActivity::class.java).apply {
            putExtra(MovimientoFormActivity.EXTRA_TIPO, movimiento.tipo)
            putExtra(MovimientoFormActivity.EXTRA_MOVIMIENTO_ID, movimiento.id)
        }
        startActivity(intent)
    }

    private fun confirmarEliminacion(movimiento: MovimientoFinanciero) {
        val tipoTexto = if (movimiento.tipo == FinanceConstants.TIPO_GASTO) "gasto" else "ingreso"
        AlertDialog.Builder(this)
            .setTitle("Eliminar movimiento")
            .setMessage("Seguro que deseas eliminar este $tipoTexto?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (movimiento.tipo == FinanceConstants.TIPO_GASTO) {
                    NotificationScheduler.cancelExpenseReminder(this, movimiento.id)
                }
                lifecycleScope.launch {
                    viewModel.eliminarMovimiento(movimiento.id)
                    aplicarFiltros()
                }
            }
            .show()
    }

    private fun crearAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(
            this,
            R.layout.spinner_item_selected,
            items
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}
