package com.example.genetics.api.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.R
import com.example.genetics.api.Incidencia
import java.text.SimpleDateFormat
import java.util.*

class IncidentsAdapter(
    private var incidenciasList: MutableList<Incidencia>, // ← Cambiar a MutableList
    private val onItemClick: (Incidencia) -> Unit
) : RecyclerView.Adapter<IncidentsAdapter.IncidentViewHolder>() {

    inner class IncidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardIncident)
        val textTipo: TextView = itemView.findViewById(R.id.textTipo)
        val textDescripcion: TextView = itemView.findViewById(R.id.textDescripcion)
        val textAnimal: TextView = itemView.findViewById(R.id.textAnimal)
        val textFecha: TextView = itemView.findViewById(R.id.textFecha)
        val textEstado: TextView = itemView.findViewById(R.id.textEstado)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < incidenciasList.size) {
                    onItemClick(incidenciasList[position])
                }
            }
        }

        fun bind(incidencia: Incidencia) {
            textTipo.text = "🚨 ${incidencia.tipo}"
            textDescripcion.text = incidencia.descripcion
            textAnimal.text = "Animal: ${getAnimalText(incidencia.animal)}"
            textFecha.text = "📅 ${formatearFecha(incidencia.fecha_deteccion)}"

            // Configurar estado con color
            textEstado.text = formatearEstado(incidencia.estado)
            textEstado.setBackgroundResource(getEstadoBackground(incidencia.estado))
            textEstado.setTextColor(Color.WHITE)

            // Color del borde según estado
            when (incidencia.estado) {
                "pendiente" -> cardView.setCardBackgroundColor(Color.parseColor("#FFF9E7"))
                "en tratamiento" -> cardView.setCardBackgroundColor(Color.parseColor("#E8F6FD"))
                "resuelto" -> cardView.setCardBackgroundColor(Color.parseColor("#D5F4E6"))
                else -> cardView.setCardBackgroundColor(Color.WHITE)
            }
        }

        private fun getAnimalText(animalId: Int): String {
            // TODO: Aquí deberías obtener la información del animal desde una lista o API
            return "ID: $animalId"
        }

        private fun formatearEstado(estado: String): String {
            return when (estado) {
                "pendiente" -> "Pendiente"
                "en tratamiento" -> "En Tratamiento"
                "resuelto" -> "Resuelto"
                else -> estado.replaceFirstChar { it.uppercase() }
            }
        }

        private fun getEstadoBackground(estado: String): Int {
            return when (estado) {
                "pendiente" -> R.drawable.badge_orange
                "en tratamiento" -> R.drawable.badge_blue
                "resuelto" -> R.drawable.badge_green
                else -> R.drawable.badge_gray
            }
        }

        private fun formatearFecha(fecha: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                fecha
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident, parent, false)
        return IncidentViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        if (position < incidenciasList.size) {
            holder.bind(incidenciasList[position])
        }
    }

    override fun getItemCount(): Int = incidenciasList.size

    // MÉTODO CORREGIDO - Actualiza la lista y notifica los cambios
    fun updateList(newList: List<Incidencia>) {
        incidenciasList.clear()
        incidenciasList.addAll(newList)
        notifyDataSetChanged()
    }
}