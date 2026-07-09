package com.example.fintrack.ui.movimientos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.databinding.ItemMovimientoBinding
import com.example.fintrack.utils.MoneyUtils

class MovimientoAdapter(
    private val onEditar: (MovimientoFinanciero) -> Unit,
    private val onEliminar: (MovimientoFinanciero) -> Unit
) : RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder>() {
    private val movimientos = mutableListOf<MovimientoFinanciero>()
    private var moneda: String = "USD"

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val binding = ItemMovimientoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovimientoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        holder.bind(movimientos[position])
    }

    override fun getItemCount(): Int = movimientos.size

    override fun getItemId(position: Int): Long = movimientos[position].id

    fun actualizarDatos(nuevosMovimientos: List<MovimientoFinanciero>, nuevaMoneda: String) {
        val listaCompletaOrdenada = nuevosMovimientos.sortedWith(
            compareByDescending<MovimientoFinanciero> { it.fecha }
                .thenByDescending { it.creadoEn }
        )
        movimientos.clear()
        movimientos.addAll(listaCompletaOrdenada)
        moneda = nuevaMoneda
        notifyDataSetChanged()
    }

    inner class MovimientoViewHolder(
        private val binding: ItemMovimientoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movimiento: MovimientoFinanciero) {
            binding.tvEtiqueta.text = movimiento.etiqueta
            binding.tvCategoriaFrecuencia.text = "${movimiento.categoria} - ${movimiento.frecuencia}"
            binding.tvFecha.text = movimiento.fecha
            binding.tvDescripcion.text = movimiento.descripcion.ifBlank { "Sin descripcion" }
            binding.tvMonto.text = MoneyUtils.formatearMonto(movimiento.montoUSD, moneda)
            binding.btnEditar.setOnClickListener { onEditar(movimiento) }
            binding.btnEliminar.setOnClickListener { onEliminar(movimiento) }
        }
    }
}
