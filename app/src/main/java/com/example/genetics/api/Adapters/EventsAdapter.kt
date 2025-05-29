package com.example.genetics.api.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.R
import com.example.genetics.api.Evento
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapter(
    private var eventosList: List<Evento>,
    private val onItemClick: (Evento) -> Unit,
    private val onOptionsClick: (Evento) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private var filteredList = eventosList.toList()

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardEvent)
        val textEventIcon: TextView = itemView.findViewById(R.id.textEventIcon)
        val textEventTitle: TextView = itemView.findViewById(R.id.textEventTitle)
        val textEventTime: TextView = itemView.findViewById(R.id.textEventTime)
        val textEventDescription: TextView = itemView.findViewById(R.id.textEventDescription)
        val textEventAnimal: TextView = itemView.findViewById(R.id.textEventAnimal)
        val textEventType: TextView = itemView.findViewById(R.id.textEventType)
        val textEventRecurrence: TextView = itemView.findViewById(R.id.textEventRecurrence)
        val viewEventIndicator: View = itemView.findViewById(R.id.viewEventIndicator)
        val buttonEventOptions: ImageButton = itemView.findViewById(R.id.buttonEventOptions)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < filteredList.size) {
                    onItemClick(filteredList[position])
                }
            }

            buttonEventOptions.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < filteredList.size) {
                    onOptionsClick(filteredList[position])
                }
            }
        }

        fun bind(evento: Evento) {
            // Configurar icono y colores según tipo
            val (icon, color, backgroundColor) = getEventStyle(evento.tipo)

            textEventIcon.text = icon
            viewEventIndicator.setBackgroundColor(color)
            cardView.setCardBackgroundColor(backgroundColor)

            // Título del evento
            textEventTitle.text = evento.titulo

            // Hora del evento
            textEventTime.text = formatearHora(evento.fecha_inicio)

            // Descripción
            if (!evento.descripcion.isNullOrEmpty()) {
                textEventDescription.text = evento.descripcion
                textEventDescription.visibility = View.VISIBLE
            } else {
                textEventDescription.visibility = View.GONE
            }

            // Animal relacionado
            if (evento.animal != null) {
                textEventAnimal.text = "🐄 Animal ID: ${evento.animal}"
                textEventAnimal.visibility = View.VISIBLE
            } else {
                textEventAnimal.text = "📋 Evento general"
                textEventAnimal.visibility = View.VISIBLE
            }

            // Tipo de evento (limpio, sin emoji)
            textEventType.text = limpiarTipoEvento(evento.tipo)
            textEventType.setBackgroundResource(getTypeBackground(evento.tipo))

            // Recurrencia
            if (evento.recurrente) {
                textEventRecurrence.visibility = View.VISIBLE
                textEventRecurrence.text = "🔄 Recurrente"
            } else {
                textEventRecurrence.visibility = View.GONE
            }
        }

        private fun getEventStyle(tipo: String): Triple<String, Int, Int> {
            return when {
                tipo.contains("visita", ignoreCase = true) ||
                        tipo.contains("veterinaria", ignoreCase = true) ->
                    Triple("🩺", Color.parseColor("#27ae60"), Color.parseColor("#d5f4e6"))

                tipo.contains("tratamiento", ignoreCase = true) ||
                        tipo.contains("medicamento", ignoreCase = true) ->
                    Triple("💊", Color.parseColor("#f39c12"), Color.parseColor("#fef9e7"))

                tipo.contains("parto", ignoreCase = true) ->
                    Triple("🍼", Color.parseColor("#e74c3c"), Color.parseColor("#fadbd8"))

                tipo.contains("vacunación", ignoreCase = true) ->
                    Triple("🏥", Color.parseColor("#9b59b6"), Color.parseColor("#f4ecf7"))

                tipo.contains("producción", ignoreCase = true) ->
                    Triple("🥛", Color.parseColor("#3498db"), Color.parseColor("#e8f6fd"))

                tipo.contains("pesaje", ignoreCase = true) ->
                    Triple("⚖️", Color.parseColor("#34495e"), Color.parseColor("#f8f9fa"))

                tipo.contains("traslado", ignoreCase = true) ->
                    Triple("🚚", Color.parseColor("#e67e22"), Color.parseColor("#fef5e7"))

                tipo.contains("inspección", ignoreCase = true) ->
                    Triple("🔍", Color.parseColor("#16a085"), Color.parseColor("#d1f2eb"))

                else -> Triple("📋", Color.parseColor("#7f8c8d"), Color.parseColor("#f8f9fa"))
            }
        }

        private fun getTypeBackground(tipo: String): Int {
            return when {
                tipo.contains("visita", ignoreCase = true) -> R.drawable.badge_green
                tipo.contains("tratamiento", ignoreCase = true) -> R.drawable.badge_orange
                tipo.contains("parto", ignoreCase = true) -> R.drawable.badge_red
                tipo.contains("producción", ignoreCase = true) -> R.drawable.badge_blue
                else -> R.drawable.badge_gray
            }
        }

        private fun limpiarTipoEvento(tipo: String): String {
            // Remover emojis del tipo para mostrar solo el texto
            return tipo.replace(Regex("[🩺💊🍼🥛🏥⚖️🚚🔍📊🌾✂️📋]\\s*"), "")
        }

        private fun formatearHora(fechaHora: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(fechaHora)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                try {
                    // Intentar solo con fecha
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    inputFormat.parse(fechaHora)
                    "Todo el día"
                } catch (e2: Exception) {
                    "Sin hora"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        if (position < filteredList.size) {
            holder.bind(filteredList[position])
        }
    }

    override fun getItemCount(): Int = filteredList.size

    // Método para actualizar la lista completa
    fun updateList(newList: List<Evento>) {
        eventosList = newList
        filteredList = newList
        notifyDataSetChanged()
    }

    // Método para filtrar por tipo de evento
    fun filterByType(tipo: String) {
        filteredList = if (tipo.isEmpty() || tipo == "todos") {
            eventosList
        } else {
            eventosList.filter {
                it.tipo.contains(tipo, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    // Método para filtrar por fecha específica
    fun filterByDate(fecha: String) {
        filteredList = eventosList.filter { evento ->
            try {
                val eventoFecha = evento.fecha_inicio.substring(0, 10) // yyyy-MM-dd
                eventoFecha == fecha
            } catch (e: Exception) {
                false
            }
        }
        notifyDataSetChanged()
    }

    // Método para filtrar por animal
    fun filterByAnimal(animalId: Int?) {
        filteredList = if (animalId == null) {
            eventosList
        } else {
            eventosList.filter { it.animal == animalId }
        }
        notifyDataSetChanged()
    }

    // Método para obtener eventos de un rango de fechas
    fun getEventsInRange(fechaInicio: String, fechaFin: String): List<Evento> {
        return eventosList.filter { evento ->
            try {
                val eventoFecha = evento.fecha_inicio.substring(0, 10)
                eventoFecha >= fechaInicio && eventoFecha <= fechaFin
            } catch (e: Exception) {
                false
            }
        }
    }
}