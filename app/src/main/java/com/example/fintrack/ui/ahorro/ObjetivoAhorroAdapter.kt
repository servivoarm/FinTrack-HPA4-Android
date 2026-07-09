package com.example.fintrack.ui.ahorro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.databinding.ItemObjetivoAhorroBinding
import com.example.fintrack.utils.MoneyUtils
import kotlin.math.roundToInt

class ObjetivoAhorroAdapter(
    private val onAgregarAporte: (ObjetivoAhorro) -> Unit,
    private val onEliminar: (ObjetivoAhorro) -> Unit
) : RecyclerView.Adapter<ObjetivoAhorroAdapter.ObjetivoViewHolder>() {
    private val objetivos = mutableListOf<ObjetivoAhorro>()
    private var moneda: String = "USD"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjetivoViewHolder {
        val binding = ItemObjetivoAhorroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ObjetivoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ObjetivoViewHolder, position: Int) {
        holder.bind(objetivos[position])
    }

    override fun getItemCount(): Int = objetivos.size

    fun actualizarDatos(nuevosObjetivos: List<ObjetivoAhorro>, nuevaMoneda: String) {
        objetivos.clear()
        objetivos.addAll(nuevosObjetivos)
        moneda = nuevaMoneda
        notifyDataSetChanged()
    }

    inner class ObjetivoViewHolder(
        private val binding: ItemObjetivoAhorroBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(objetivo: ObjetivoAhorro) {
            val progreso = if (objetivo.montoMetaUSD > 0.0) {
                ((objetivo.montoActualUSD / objetivo.montoMetaUSD) * 100).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
            val porcentaje = progreso.roundToInt()

            binding.tvNombreObjetivo.text = objetivo.nombre
            binding.tvDescripcionObjetivo.text = objetivo.descripcion.ifBlank { "Sin descripcion" }
            binding.tvMontoObjetivo.text = "${MoneyUtils.formatearMonto(objetivo.montoActualUSD, moneda)} / ${MoneyUtils.formatearMonto(objetivo.montoMetaUSD, moneda)}"
            binding.tvPorcentajeObjetivo.text = "$porcentaje% completado"
            binding.progressObjetivo.progress = porcentaje
            binding.tvFrecuenciaAporte.text = "Frecuencia: ${objetivo.frecuenciaAporte}"
            binding.tvAporteSugerido.text = "Aporte sugerido: ${MoneyUtils.formatearMonto(objetivo.aporteSugeridoUSD, moneda)}"
            binding.btnAgregarAporte.setOnClickListener { onAgregarAporte(objetivo) }
            binding.btnEliminarObjetivo.setOnClickListener { onEliminar(objetivo) }
        }
    }
}
