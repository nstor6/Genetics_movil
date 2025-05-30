package com.example.genetics.api.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.R
import com.example.genetics.api.Tratamiento
import java.text.SimpleDateFormat
import java.util.*

class TreatmentsAdapter(
    private var tratamientosList: MutableList<Tratamiento>, // 🔧 CAMBIAR A MutableList
    private val onItemClick: (Tratamiento) -> Unit
) : RecyclerView.Adapter<TreatmentsAdapter.TreatmentViewHolder>() {

    private var filteredList = tratamientosList.toMutableList() // 🔧 CAMBIAR A MutableList

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

            // Animal - Mostrar ID por ahora (luego se puede mejorar con nombres)
            textAnimal.text = "Animal: ID ${tratamiento.animal}"

            // Fecha formateada
            textFecha.text = "📅 ${formatearFecha(tratamiento.fecha)}"

            // Duración
            textDuracion.text = "⏱️ ${tratamiento.duracion}"

            // Observaciones (mostrar solo si existen)
            if (!tratamiento.observaciones.isNullOrEmpty()) {
                textObservaciones.text = "📝 ${tratamiento.observaciones}"
                textObservaciones.visibility = View.VISIBLE
            } else {
                textObservaciones.visibility = View.GONE
            }

            // Color de fondo según antigüedad del tratamiento
            val backgroundColor = when {
                esTratamientoReciente(tratamiento.fecha) -> Color.parseColor("#D5F4E6") // Verde claro - Reciente
                esTratamientoAntiquo(tratamiento.fecha) -> Color.parseColor("#F5F5F5") // Gris - Antiguo
                else -> Color.parseColor("#E8F6FD") // Azul claro - Intermedio
            }
            cardView.setCardBackgroundColor(backgroundColor)
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

        private fun esTratamientoReciente(fecha: String): Boolean {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val treatmentDate = inputFormat.parse(fecha)
                val weekAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -7)
                }.time
                treatmentDate?.after(weekAgo) == true
            } catch (e: Exception) {
                false
            }
        }

        private fun esTratamientoAntiquo(fecha: String): Boolean {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val treatmentDate = inputFormat.parse(fecha)
                val monthAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                }.time
                treatmentDate?.before(monthAgo) == true
            } catch (e: Exception) {
                false
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

    // 🔧 MÉTODO CRÍTICO: updateList() - Este es el que faltaba
    fun updateList(newList: List<Tratamiento>) {
        android.util.Log.d("TREATMENTS_ADAPTER", "🔄 Actualizando lista: ${newList.size} tratamientos")

        tratamientosList.clear()
        tratamientosList.addAll(newList)

        filteredList.clear()
        filteredList.addAll(newList)

        notifyDataSetChanged()

        android.util.Log.d("TREATMENTS_ADAPTER", "✅ Lista actualizada. Items en adapter: ${itemCount}")
    }

    // Método para filtrar por animal
    fun filterByAnimal(animalId: Int?) {
        filteredList.clear()
        if (animalId == null) {
            filteredList.addAll(tratamientosList)
        } else {
            filteredList.addAll(tratamientosList.filter { it.animal == animalId })
        }
        notifyDataSetChanged()
        android.util.Log.d("TREATMENTS_ADAPTER", "🔍 Filtrado por animal $animalId: ${filteredList.size} resultados")
    }

    // Método para filtrar por medicamento
    fun filterByMedicine(medicine: String) {
        filteredList.clear()
        if (medicine.isEmpty()) {
            filteredList.addAll(tratamientosList)
        } else {
            filteredList.addAll(tratamientosList.filter {
                it.medicamento.contains(medicine, ignoreCase = true)
            })
        }
        notifyDataSetChanged()
        android.util.Log.d("TREATMENTS_ADAPTER", "🔍 Filtrado por medicamento '$medicine': ${filteredList.size} resultados")
    }

    // Método para obtener tratamientos recientes
    fun getTratamientosRecientes(): List<Tratamiento> {
        return tratamientosList.filter { tratamiento ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val treatmentDate = inputFormat.parse(tratamiento.fecha)
                val weekAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -7)
                }.time
                treatmentDate?.after(weekAgo) == true
            } catch (e: Exception) {
                false
            }
        }
    }

    // Método para limpiar filtros
    fun clearFilters() {
        filteredList.clear()
        filteredList.addAll(tratamientosList)
        notifyDataSetChanged()
        android.util.Log.d("TREATMENTS_ADAPTER", "🧹 Filtros limpiados: ${filteredList.size} tratamientos")
    }
}