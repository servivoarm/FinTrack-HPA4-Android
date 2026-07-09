package com.example.fintrack.ui.presupuestos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import com.example.fintrack.databinding.ItemPresupuestoCategoriaBinding
import com.example.fintrack.utils.MoneyUtils
import com.example.fintrack.viewmodel.PresupuestoCategoriaResumen
import com.example.fintrack.viewmodel.PresupuestosViewModel
import kotlin.math.roundToInt

class PresupuestoCategoriaAdapter(
    private val onEditar: (PresupuestoCategoria) -> Unit,
    private val onEliminar: (PresupuestoCategoria) -> Unit
) : RecyclerView.Adapter<PresupuestoCategoriaAdapter.PresupuestoViewHolder>() {
    private val presupuestos = mutableListOf<PresupuestoCategoriaResumen>()
    private var moneda: String = "USD"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresupuestoViewHolder {
        val binding = ItemPresupuestoCategoriaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PresupuestoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresupuestoViewHolder, position: Int) {
        holder.bind(presupuestos[position])
    }

    override fun getItemCount(): Int = presupuestos.size

    fun actualizarDatos(nuevosPresupuestos: List<PresupuestoCategoriaResumen>, nuevaMoneda: String) {
        presupuestos.clear()
        presupuestos.addAll(nuevosPresupuestos)
        moneda = nuevaMoneda
        notifyDataSetChanged()
    }

    inner class PresupuestoViewHolder(
        private val binding: ItemPresupuestoCategoriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(resumen: PresupuestoCategoriaResumen) {
            val presupuesto = resumen.presupuesto
            val porcentaje = resumen.porcentajeUsado.roundToInt()
            val porcentajeProgreso = porcentaje.coerceIn(0, 100)
            val estadoColor = colorPorEstado(resumen.estado)

            binding.tvCategoriaPresupuesto.text = presupuesto.categoria
            binding.tvPorcentajePresupuesto.text = "$porcentaje% usado"
            binding.tvMontoPresupuesto.text =
                "${MoneyUtils.formatearMonto(resumen.gastadoUSD, moneda)} / ${MoneyUtils.formatearMonto(presupuesto.montoLimiteUSD, moneda)}"
            binding.tvRestantePresupuesto.text =
                "Restante: ${MoneyUtils.formatearMonto(resumen.restanteUSD.coerceAtLeast(0.0), moneda)}"
            binding.tvEstadoPresupuesto.text = textoEstado(resumen.estado)
            binding.tvEstadoPresupuesto.setTextColor(estadoColor)
            binding.tvPorcentajePresupuesto.setTextColor(estadoColor)
            binding.progressPresupuesto.progress = porcentajeProgreso
            binding.btnEditarPresupuesto.setOnClickListener { onEditar(presupuesto) }
            binding.btnEliminarPresupuesto.setOnClickListener { onEliminar(presupuesto) }
        }

        private fun textoEstado(estado: String): String {
            return when (estado) {
                PresupuestosViewModel.ESTADO_ALERTA -> "Alerta: estás cerca del límite"
                PresupuestosViewModel.ESTADO_SUPERADO -> "Has superado tu presupuesto"
                else -> "Dentro del presupuesto"
            }
        }

        private fun colorPorEstado(estado: String): Int {
            return when (estado) {
                PresupuestosViewModel.ESTADO_ALERTA -> Color.parseColor("#B7791F")
                PresupuestosViewModel.ESTADO_SUPERADO -> Color.parseColor("#B44B4B")
                else -> Color.parseColor("#2E7D55")
            }
        }
    }
}
