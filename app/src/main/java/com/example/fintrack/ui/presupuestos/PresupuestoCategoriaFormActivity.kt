package com.example.fintrack.ui.presupuestos

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import com.example.fintrack.databinding.ActivityPresupuestoCategoriaFormBinding
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.PresupuestosViewModel
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch

class PresupuestoCategoriaFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPresupuestoCategoriaFormBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: PresupuestosViewModel by viewModels()
    private var presupuestoId: Long = 0L
    private var presupuestoEditando: PresupuestoCategoria? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPresupuestoCategoriaFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        presupuestoId = intent.getLongExtra(EXTRA_PRESUPUESTO_ID, 0L)

        configurarCategoria()
        configurarBotones()

        if (presupuestoId > 0L) {
            cargarPresupuesto()
        } else {
            val hoy = LocalDate.now()
            binding.tvTituloPresupuestoCategoria.text = "Nuevo presupuesto"
            binding.etMes.setText(hoy.monthValue.toString())
            binding.etAnio.setText(hoy.year.toString())
        }
    }

    private fun configurarCategoria() {
        binding.spinnerCategoriaPresupuesto.adapter = ArrayAdapter(
            this,
            com.example.fintrack.R.layout.spinner_item_selected,
            FinanceConstants.CATEGORIAS_GASTO
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun configurarBotones() {
        binding.btnGuardarPresupuesto.setOnClickListener {
            guardarPresupuesto()
        }
        binding.btnCancelarPresupuesto.setOnClickListener {
            finish()
        }
    }

    private fun cargarPresupuesto() {
        lifecycleScope.launch {
            val presupuesto = viewModel.obtenerPresupuestoCategoriaPorId(presupuestoId)
            if (presupuesto == null) {
                Toast.makeText(this@PresupuestoCategoriaFormActivity, "Presupuesto no encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            presupuestoEditando = presupuesto
            binding.tvTituloPresupuestoCategoria.text = "Editar presupuesto"
            binding.etMontoLimite.setText(
                String.format(
                    Locale.US,
                    "%.2f",
                    MoneyUtils.convertirDesdeUSD(presupuesto.montoLimiteUSD, sessionManager.obtenerMoneda())
                )
            )
            binding.etMes.setText(presupuesto.mes.toString())
            binding.etAnio.setText(presupuesto.anio.toString())
            seleccionarCategoria(presupuesto.categoria)
        }
    }

    private fun guardarPresupuesto() {
        limpiarErrores()

        val categoria = binding.spinnerCategoriaPresupuesto.selectedItem?.toString().orEmpty()
        val monto = binding.etMontoLimite.text.toString().trim().replace(",", ".").toDoubleOrNull()
        val mes = binding.etMes.text.toString().trim().toIntOrNull()
        val anio = binding.etAnio.text.toString().trim().toIntOrNull()

        if (categoria.isEmpty()) {
            Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }
        if (monto == null) {
            binding.etMontoLimite.error = "El monto límite es obligatorio"
            return
        }
        if (monto <= 0.0) {
            binding.etMontoLimite.error = "El monto debe ser mayor que 0"
            return
        }
        if (mes == null || mes !in 1..12) {
            binding.etMes.error = "Mes inválido"
            return
        }
        if (anio == null || anio < 2000) {
            binding.etAnio.error = "Año inválido"
            return
        }

        val existente = presupuestoEditando
        val moneda = sessionManager.obtenerMoneda()
        val presupuesto = PresupuestoCategoria(
            id = existente?.id ?: 0L,
            usuarioId = sessionManager.obtenerUsuarioId(),
            categoria = categoria,
            montoLimiteUSD = MoneyUtils.convertirAUSD(monto, moneda),
            mes = mes,
            anio = anio,
            activo = existente?.activo ?: true,
            creadoEn = existente?.creadoEn ?: System.currentTimeMillis()
        )

        lifecycleScope.launch {
            val result = viewModel.guardarPresupuestoCategoria(presupuesto)
            Toast.makeText(this@PresupuestoCategoriaFormActivity, result.mensaje, Toast.LENGTH_SHORT).show()
            if (result.exitoso) {
                finish()
            }
        }
    }

    private fun seleccionarCategoria(categoria: String) {
        val adapter = binding.spinnerCategoriaPresupuesto.adapter
        var indiceOtro = -1
        for (index in 0 until adapter.count) {
            val item = adapter.getItem(index).toString()
            if (item == categoria) {
                binding.spinnerCategoriaPresupuesto.setSelection(index)
                return
            }
            if (item == "Otro") {
                indiceOtro = index
            }
        }
        if (indiceOtro >= 0) {
            binding.spinnerCategoriaPresupuesto.setSelection(indiceOtro)
        }
    }

    private fun limpiarErrores() {
        binding.etMontoLimite.error = null
        binding.etMes.error = null
        binding.etAnio.error = null
    }

    companion object {
        const val EXTRA_PRESUPUESTO_ID = "EXTRA_PRESUPUESTO_ID"
    }
}
