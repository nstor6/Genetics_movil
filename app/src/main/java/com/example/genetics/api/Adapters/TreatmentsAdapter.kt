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
    private val tratamientosList: MutableList<Tratamiento>,
    private val onItemClick: (Tratamiento) -> Unit,
    private val onEditClick: ((Tratamiento) -> Unit)? = null,
    private val onDeleteClick: ((Tratamiento) -> Unit)? = null
) : RecyclerView.Adapter<TreatmentsAdapter.TreatmentViewHolder>() {

    inner class TreatmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardTreatment)
        val textMedicamento: TextView = itemView.findViewById(R.id.textMedicamento)
        val textDosis: TextView = itemView.findViewById(R.id.textDosis)
        val textAnimal: TextView = itemView.findViewById(R.id.textAnimal)
        val textFecha: TextView = itemView.findViewById(R.id.textFecha)
        val textDuracion: TextView = itemView.findViewById(R.id.textDuracion)
        val textObservaciones: TextView = itemView.findViewById(R.id.textObservaciones)

        init {
            // Click normal - ver detalles
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < tratamientosList.size) {
                    onItemClick(tratamientosList[position])
                }
            }

            // Long click - mostrar opciones de CRUD
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < tratamientosList.size) {
                    mostrarOpcionesTratamiento(tratamientosList[position])
                    true
                } else {
                    false
                }
            }
        }

        private fun mostrarOpcionesTratamiento(tratamiento: Tratamiento) {
            val context = itemView.context
            val opciones = arrayOf(
                "👁️ Ver detalles",
                "✏️ Editar tratamiento",
                "🗑️ Eliminar tratamiento"
            )

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Opciones para ${tratamiento.medicamento}")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> onItemClick(tratamiento) // Ver detalles
                        1 -> onEditClick?.invoke(tratamiento) // Editar
                        2 -> onDeleteClick?.invoke(tratamiento) // Eliminar
                    }
                }
                .show()
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
                fecha
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
        if (position < tratamientosList.size) {
            holder.bind(tratamientosList[position])
        }
    }

    override fun getItemCount(): Int = tratamientosList.size

    fun updateList(newList: List<Tratamiento>) {
        android.util.Log.d("TREATMENTS_ADAPTER", "🔄 Actualizando lista: ${newList.size} tratamientos")

        tratamientosList.clear()
        tratamientosList.addAll(newList)

        notifyDataSetChanged()

        android.util.Log.d("TREATMENTS_ADAPTER", "✅ Lista actualizada. Items en adapter: ${itemCount}")
    }

    // Método para eliminar un tratamiento específico de la lista
    fun removeTreatment(tratamiento: Tratamiento) {
        val index = tratamientosList.indexOfFirst { it.id == tratamiento.id }
        if (index != -1) {
            tratamientosList.removeAt(index)
            notifyItemRemoved(index)
            android.util.Log.d("TREATMENTS_ADAPTER", "🗑️ Tratamiento eliminado del adapter en posición $index")
        }
    }
}