package com.example.genetics.api.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.R
import com.example.genetics.api.models.Animals
import java.util.Locale

/**
 * Adapter para usuarios normales (solo lectura)
 */
class UserAnimalsAdapter(
    private val animalsList: List<Animals>, // ✅ Usar Animals (no Animal)
    private val onItemClick: ((Animals) -> Unit)? = null
) : RecyclerView.Adapter<UserAnimalsAdapter.AnimalViewHolder>() {

    inner class AnimalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageAnimal: ImageView = itemView.findViewById(R.id.imageAnimal)
        val textChapeta: TextView = itemView.findViewById(R.id.textChapeta)
        val textNombre: TextView = itemView.findViewById(R.id.textNombre)
        val textRaza: TextView = itemView.findViewById(R.id.textRaza)
        val textSexo: TextView = itemView.findViewById(R.id.textSexo)
        val textEstadoProductivo: TextView = itemView.findViewById(R.id.textEstadoProductivo)
        val textEstadoReproductivo: TextView = itemView.findViewById(R.id.textEstadoReproductivo)

        init {
            // ✅ Solo click normal - ir a detalles de SOLO LECTURA
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < animalsList.size) {
                    val animal = animalsList[position]
                    onItemClick?.invoke(animal)
                }
            }

            // ❌ NO HAY LONG CLICK - Sin opciones de edición/eliminar
        }

        fun bind(animal: Animals) {
            // ✅ USAR SAFE CALLS Y OPERADOR ELVIS - IGUAL QUE ARRIBA
            textChapeta.text = "📋 ${animal.chapeta ?: "N/A"}"

            textNombre.text = if (animal.nombre.isNullOrEmpty()) {
                "Sin nombre"
            } else {
                animal.nombre
            }

            textRaza.text = "🐄 ${animal.raza ?: "N/A"}"

            textSexo.text = when (animal.sexo?.lowercase(Locale.getDefault())) {
                "macho" -> "♂️ Macho"
                "hembra" -> "♀️ Hembra"
                else -> "❓ No especificado"
            }

            val estadoProductivo = animal.estado_productivo?.let { estado ->
                if (estado.isNotEmpty()) {
                    estado.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                } else {
                    "No especificado"
                }
            } ?: "No especificado"

            textEstadoProductivo.text = "📊 $estadoProductivo"

            val estadoReproductivo = animal.estado_reproductivo?.let { estado ->
                if (estado.isNotEmpty()) {
                    estado.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                } else {
                    "No especificado"
                }
            } ?: "No especificado"

            textEstadoReproductivo.text = "💕 $estadoReproductivo"

            // Configurar imagen
            if (!animal.foto_perfil_url.isNullOrEmpty()) {
                imageAnimal.setImageResource(R.drawable.cow_image)
            } else {
                imageAnimal.setImageResource(R.drawable.cow_image)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_animal, parent, false)
        return AnimalViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        if (position < animalsList.size) {
            holder.bind(animalsList[position])
        }
    }

    override fun getItemCount(): Int = animalsList.size
}