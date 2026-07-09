package com.example.fintrack.ui.movimientos

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.databinding.ActivityMovimientoFormBinding
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.utils.NotificationScheduler
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.PresupuestoCategoriaAlerta
import com.example.fintrack.viewmodel.MovimientosViewModel
import com.example.fintrack.viewmodel.PresupuestosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class MovimientoFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMovimientoFormBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: MovimientosViewModel by viewModels()

    private var tipoMovimiento: String = FinanceConstants.TIPO_INGRESO
    private var movimientoId: Long = 0L
    private var movimientoEditando: MovimientoFinanciero? = null
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovimientoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        tipoMovimiento = intent.getStringExtra(EXTRA_TIPO) ?: FinanceConstants.TIPO_INGRESO
        movimientoId = intent.getLongExtra(EXTRA_MOVIMIENTO_ID, 0L)

        configurarSpinners()
        configurarFecha()
        configurarBotones()

        if (movimientoId > 0L) {
            cargarMovimiento()
        } else {
            binding.etFecha.setText(formatoFecha.format(Date()))
            actualizarTitulo()
        }
    }

    private fun configurarSpinners() {
        val categorias = categoriasPorTipo(tipoMovimiento)
        binding.spinnerCategoria.adapter = ArrayAdapter(
            this,
            com.example.fintrack.R.layout.spinner_item_selected,
            categorias
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerFrecuencia.adapter = ArrayAdapter(
            this,
            com.example.fintrack.R.layout.spinner_item_selected,
            FinanceConstants.FRECUENCIAS
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun configurarFecha() {
        binding.etFecha.setOnClickListener {
            mostrarSelectorFecha()
        }
    }

    private fun configurarBotones() {
        binding.btnGuardar.setOnClickListener {
            guardarMovimiento()
        }

        binding.btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun cargarMovimiento() {
        lifecycleScope.launch {
            val movimiento = viewModel.obtenerMovimientoPorId(movimientoId)
            if (movimiento == null) {
                Toast.makeText(this@MovimientoFormActivity, "Movimiento no encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            movimientoEditando = movimiento
            tipoMovimiento = movimiento.tipo
            configurarSpinners()
            actualizarTitulo()

            binding.etEtiqueta.setText(movimiento.etiqueta)
            binding.etMonto.setText(String.format(Locale.US, "%.2f", MoneyUtils.convertirDesdeUSD(movimiento.montoUSD, sessionManager.obtenerMoneda())))
            binding.etFecha.setText(movimiento.fecha)
            binding.etDescripcion.setText(movimiento.descripcion)
            seleccionarValor(binding.spinnerCategoria, movimiento.categoria)
            seleccionarValor(binding.spinnerFrecuencia, movimiento.frecuencia)
        }
    }

    private fun guardarMovimiento() {
        limpiarErrores()

        val etiqueta = binding.etEtiqueta.text.toString().trim()
        val montoTexto = binding.etMonto.text.toString().trim().replace(",", ".")
        val fecha = binding.etFecha.text.toString().trim()
        val categoria = binding.spinnerCategoria.selectedItem?.toString().orEmpty()
        val frecuencia = binding.spinnerFrecuencia.selectedItem?.toString().orEmpty()
        val descripcion = binding.etDescripcion.text.toString().trim()

        if (etiqueta.isEmpty()) {
            binding.etEtiqueta.error = "La etiqueta es obligatoria"
            return
        }

        val monto = montoTexto.toDoubleOrNull()
        if (monto == null) {
            binding.etMonto.error = "El monto es obligatorio"
            return
        }
        if (monto <= 0.0) {
            binding.etMonto.error = "El monto debe ser mayor que 0"
            return
        }
        if (fecha.isEmpty()) {
            binding.etFecha.error = "La fecha es obligatoria"
            return
        }
        if (categoria.isEmpty()) {
            Toast.makeText(this, "Selecciona una categoria", Toast.LENGTH_SHORT).show()
            return
        }
        if (frecuencia.isEmpty()) {
            Toast.makeText(this, "Selecciona una frecuencia", Toast.LENGTH_SHORT).show()
            return
        }

        val montoUSD = MoneyUtils.convertirAUSD(monto, sessionManager.obtenerMoneda())
        val existente = movimientoEditando
        val movimiento = MovimientoFinanciero(
            id = existente?.id ?: 0L,
            usuarioId = sessionManager.obtenerUsuarioId(),
            tipo = tipoMovimiento,
            etiqueta = etiqueta,
            categoria = categoria,
            descripcion = descripcion,
            montoUSD = montoUSD,
            fecha = fecha,
            frecuencia = frecuencia,
            activo = existente?.activo ?: true,
            creadoEn = existente?.creadoEn ?: System.currentTimeMillis()
        )

        lifecycleScope.launch {
            if (existente?.tipo == FinanceConstants.TIPO_GASTO) {
                NotificationScheduler.cancelExpenseReminder(this@MovimientoFormActivity, existente.id)
            }

            val result = if (existente == null) {
                viewModel.guardarMovimiento(movimiento)
            } else {
                viewModel.actualizarMovimiento(movimiento)
            }

            val movimientoGuardadoId = result.movimientoId ?: existente?.id ?: 0L
            val recordatorioProgramado = if (result.exitoso && movimiento.tipo == FinanceConstants.TIPO_GASTO && movimientoGuardadoId > 0L) {
                NotificationScheduler.scheduleExpenseReminder(
                    context = this@MovimientoFormActivity,
                    movimientoId = movimientoGuardadoId,
                    etiqueta = movimiento.etiqueta,
                    montoUSD = movimiento.montoUSD,
                    moneda = sessionManager.obtenerMoneda(),
                    fechaGasto = movimiento.fecha
                )
            } else {
                false
            }

            val mensaje = if (recordatorioProgramado) {
                "${result.mensaje}. Recordatorio programado para un día antes"
            } else {
                result.mensaje
            }
            Toast.makeText(this@MovimientoFormActivity, mensaje, Toast.LENGTH_LONG).show()

            val alertaPresupuesto = if (result.exitoso && movimiento.tipo == FinanceConstants.TIPO_GASTO) {
                viewModel.obtenerAlertaPresupuestoCategoria(movimiento.categoria, movimiento.fecha)
            } else {
                null
            }

            if (alertaPresupuesto != null) {
                mostrarAlertaPresupuesto(alertaPresupuesto)
            } else {
                finish()
            }
        }
    }

    private fun mostrarAlertaPresupuesto(alerta: PresupuestoCategoriaAlerta) {
        val porcentaje = alerta.porcentajeUsado.roundToInt()
        val (titulo, mensaje) = if (alerta.estado == PresupuestosViewModel.ESTADO_SUPERADO) {
            "Has superado tu presupuesto" to "Has usado el $porcentaje% de tu presupuesto en ${alerta.categoria}."
        } else {
            "Atención" to "Has usado el $porcentaje% de tu presupuesto en ${alerta.categoria}."
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Entendido") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun limpiarErrores() {
        binding.etEtiqueta.error = null
        binding.etMonto.error = null
        binding.etFecha.error = null
    }

    private fun mostrarSelectorFecha() {
        val calendario = Calendar.getInstance()
        runCatching {
            val fechaActual = formatoFecha.parse(binding.etFecha.text.toString())
            if (fechaActual != null) {
                calendario.time = fechaActual
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendario.set(year, month, dayOfMonth)
                binding.etFecha.setText(formatoFecha.format(calendario.time))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun actualizarTitulo() {
        val esIngreso = tipoMovimiento == FinanceConstants.TIPO_INGRESO
        val accion = if (movimientoId > 0L) "Editar" else "Nuevo"
        val nombreTipo = if (esIngreso) "ingreso" else "gasto"
        binding.tvTitulo.text = "$accion $nombreTipo"
    }

    private fun categoriasPorTipo(tipo: String): List<String> {
        return if (tipo == FinanceConstants.TIPO_GASTO) {
            FinanceConstants.CATEGORIAS_GASTO
        } else {
            FinanceConstants.CATEGORIAS_INGRESO
        }
    }

    private fun seleccionarValor(spinner: android.widget.Spinner, valor: String) {
        val adapter = spinner.adapter
        var indiceOtro = -1
        for (index in 0 until adapter.count) {
            val item = adapter.getItem(index).toString()
            if (item == valor) {
                spinner.setSelection(index)
                return
            }
            if (item == "Otro") {
                indiceOtro = index
            }
        }
        if (indiceOtro >= 0) {
            spinner.setSelection(indiceOtro)
        }
    }

    companion object {
        const val EXTRA_TIPO = "EXTRA_TIPO"
        const val EXTRA_MOVIMIENTO_ID = "EXTRA_MOVIMIENTO_ID"
    }
}
