package com.example.genetics

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.api.Tratamiento
import java.text.SimpleDateFormat
import java.util.*

class TreatmentsAdapter(
    private var tratamientosList: List<Tratamiento>,
    private val onItemClick: (Tratamiento) -> Unit
) : RecyclerView.Adapter<TreatmentsAdapter.TreatmentViewHolder>() {

    private var filteredList = tratamientosList.toList()

    inner class TreatmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardTreatment)
        val textMedicamento: TextView = itemView.findViewById(R.id.textMedicamento)
        val textDosis: TextView = itemView.findViewById(R.id.textDosis)
        val textAnimal: TextView = itemView.findViewById(R.id.textAnimal)
        val textFecha: TextView = itemView.findViewById(R.id.textFecha)
        val textDuracion: TextView = itemView.findViewById(R.id.textDuracion)
        val textObservaciones: TextView = itemView.findViewById(R.id.textObservaciones)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < filteredList.size) {
                    onItemClick(filteredList[position])
                }
            }
        }

        fun bind(tratamiento: Tratamiento) {
            // Medicamento con emoji
            textMedicamento.text = "💊 ${tratamiento.medicamento}"

            // Dosis
            textDosis.text = "Dosis: ${tratamiento.dosis}"

            // Animal (por ahora solo ID, luego puedes mejorarlo)
            textAnimal.text = "Animal ID: ${tratamiento.animal}"

            // Fecha formateada
            textFecha.text = "📅 ${formatearFecha(tratamiento.fecha)}"

            // Duración
            textDuracion.text = "Duración: ${tratamiento.duracion}"

            // Observaciones (mostrar solo si existen)
            if (!tratamiento.observaciones.isNullOrEmpty()) {
                textObservaciones.text = "📝 ${tratamiento.observaciones}"
                textObservaciones.visibility = View.VISIBLE
            } else {
                textObservaciones.visibility = View.GONE
            }

            // Color de fondo suave para tratamientos
            cardView.setCardBackgroundColor(Color.parseColor("#D5F4E6"))
        }

        private fun formatearFecha(fecha: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                fecha // Si falla el formateo, devolver la fecha original
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreatmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_treatment, parent, false)
        return TreatmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreatmentViewHolder, position: Int) {
        if (position < filteredList.size) {
            holder.bind(filteredList[position])
        }
    }

    override fun getItemCount(): Int = filteredList.size

    // Método para actualizar la lista (útil para filtros)
    fun updateList(newList: List<Tratamiento>) {
        filteredList = newList
        notifyDataSetChanged()
    }
}