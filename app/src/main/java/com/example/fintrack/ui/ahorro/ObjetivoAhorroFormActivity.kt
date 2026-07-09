package com.example.fintrack.ui.ahorro

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.databinding.ActivityObjetivoAhorroFormBinding
import com.example.fintrack.utils.FinanceConstants
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.PresupuestosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ObjetivoAhorroFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityObjetivoAhorroFormBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: PresupuestosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObjetivoAhorroFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        binding.spinnerFrecuenciaAporte.adapter = ArrayAdapter(
            this,
            com.example.fintrack.R.layout.spinner_item_selected,
            FinanceConstants.FRECUENCIAS_AHORRO
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.btnGuardarObjetivo.setOnClickListener {
            guardarObjetivo()
        }

        binding.btnCancelarObjetivo.setOnClickListener {
            finish()
        }
    }

    private fun guardarObjetivo() {
        limpiarErrores()

        val nombre = binding.etNombreObjetivo.text.toString().trim()
        val descripcion = binding.etDescripcionObjetivo.text.toString().trim()
        val montoMeta = binding.etMontoMeta.text.toString().trim().replace(",", ".").toDoubleOrNull()
        val aporteSugerido = binding.etAporteSugerido.text.toString().trim().replace(",", ".").toDoubleOrNull()
        val frecuencia = binding.spinnerFrecuenciaAporte.selectedItem?.toString().orEmpty()

        if (nombre.isEmpty()) {
            binding.etNombreObjetivo.error = "El nombre es obligatorio"
            return
        }
        if (montoMeta == null) {
            binding.etMontoMeta.error = "El monto meta es obligatorio"
            return
        }
        if (montoMeta <= 0.0) {
            binding.etMontoMeta.error = "El monto meta debe ser mayor que 0"
            return
        }
        if (aporteSugerido == null) {
            binding.etAporteSugerido.error = "El aporte sugerido es obligatorio"
            return
        }
        if (aporteSugerido < 0.0) {
            binding.etAporteSugerido.error = "El aporte sugerido debe ser mayor o igual que 0"
            return
        }
        if (frecuencia.isEmpty()) {
            Toast.makeText(this, "Selecciona una frecuencia", Toast.LENGTH_SHORT).show()
            return
        }

        val moneda = sessionManager.obtenerMoneda()
        val objetivo = ObjetivoAhorro(
            usuarioId = sessionManager.obtenerUsuarioId(),
            nombre = nombre,
            descripcion = descripcion,
            montoMetaUSD = MoneyUtils.convertirAUSD(montoMeta, moneda),
            aporteSugeridoUSD = MoneyUtils.convertirAUSD(aporteSugerido, moneda),
            frecuenciaAporte = frecuencia,
            fechaCreacion = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        lifecycleScope.launch {
            val result = viewModel.crearObjetivo(objetivo)
            Toast.makeText(this@ObjetivoAhorroFormActivity, result.mensaje, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun limpiarErrores() {
        binding.etNombreObjetivo.error = null
        binding.etMontoMeta.error = null
        binding.etAporteSugerido.error = null
    }
}
