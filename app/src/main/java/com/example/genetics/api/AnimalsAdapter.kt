package com.example.genetics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.api.Animals
import java.util.Locale

class AnimalsAdapter(
    private val animalsList: List<Animals>,
    private val onItemClick: (Animals) -> Unit
) : RecyclerView.Adapter<AnimalsAdapter.AnimalViewHolder>() {

    inner class AnimalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageAnimal: ImageView = itemView.findViewById(R.id.imageAnimal)
        val textChapeta: TextView = itemView.findViewById(R.id.textChapeta)
        val textNombre: TextView = itemView.findViewById(R.id.textNombre)
        val textRaza: TextView = itemView.findViewById(R.id.textRaza)
        val textSexo: TextView = itemView.findViewById(R.id.textSexo)
        val textEstadoProductivo: TextView = itemView.findViewById(R.id.textEstadoProductivo)
        val textEstadoReproductivo: TextView = itemView.findViewById(R.id.textEstadoReproductivo)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < animalsList.size) {
                    onItemClick(animalsList[position])
                }
            }
        }

        fun bind(animals: Animals) {
            // Usar operador safe call (?.) y elvis operator (?:)
            textChapeta.text = "📋 ${animals.chapeta ?: "N/A"}"

            textNombre.text = if (animals.nombre.isNullOrEmpty()) {
                "Sin nombre"
            } else {
                animals.nombre
            }

            textRaza.text = "🐄 ${animals.raza ?: "N/A"}"

            textSexo.text = when (animals.sexo?.lowercase(Locale.getDefault())) {
                "macho" -> "♂️ Macho"
                "hembra" -> "♀️ Hembra"
                else -> "❓ No especificado"
            }

            // Capitalizar primera letra de forma segura
            val estadoProductivo = animals.estado_productivo?.let { estado ->
                if (estado.isNotEmpty()) {
                    estado.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                } else {
                    "No especificado"
                }
            } ?: "No especificado"

            textEstadoProductivo.text = "📊 $estadoProductivo"

            val estadoReproductivo = animals.estado_reproductivo?.let { estado ->
                if (estado.isNotEmpty()) {
                    estado.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                } else {
                    "No especificado"
                }
            } ?: "No especificado"

            textEstadoReproductivo.text = "💕 $estadoReproductivo"

            // Configurar imagen de forma segura
            if (!animals.foto_perfil_url.isNullOrEmpty()) {
                // TODO: Usar Glide o Picasso para cargar la imagen
                // Glide.with(itemView.context).load(animal.foto_perfil_url).into(imageAnimal)
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