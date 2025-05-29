package com.example.genetics.api.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.AddIncidentActivity
import com.example.genetics.AddTreatmentActivity
import com.example.genetics.AnimalDetailActivity
import com.example.genetics.R
import com.example.genetics.api.Animals
import java.util.Locale

// Actualizar la clase AnimalsAdapter para incluir callback de edición:
class AnimalsAdapter(
    private val animalsList: List<Animals>,
    private val onItemClick: ((Animals) -> Unit)? = null,
    private val onEditClick: ((Animals) -> Unit)? = null
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
            // Click normal - ir a detalles
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < animalsList.size) {
                    val animal = animalsList[position]
                    onItemClick?.invoke(animal)
                }
            }

            // Long click - mostrar opciones
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < animalsList.size) {
                    val animal = animalsList[position]
                    mostrarOpcionesAnimal(animal)
                    true
                } else {
                    false
                }
            }
        }

        private fun mostrarOpcionesAnimal(animal: Animals) {
            val context = itemView.context
            val opciones = arrayOf("👁️ Ver detalles", "✏️ Editar animal", "🚨 Nueva incidencia", "💊 Nuevo tratamiento")

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Opciones para ${animal.chapeta}")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> onItemClick?.invoke(animal) // Ver detalles
                        1 -> onEditClick?.invoke(animal) // Editar
                        2 -> {
                            // Nueva incidencia
                            val intent = Intent(context, AddIncidentActivity::class.java)
                            intent.putExtra("PRESELECTED_ANIMAL_ID", animal.id)
                            intent.putExtra("PRESELECTED_ANIMAL_NAME", "${animal.chapeta} - ${animal.nombre ?: "Sin nombre"}")
                            context.startActivity(intent)
                        }
                        3 -> {
                            // Nuevo tratamiento
                            val intent = Intent(context, AddTreatmentActivity::class.java)
                            intent.putExtra("PRESELECTED_ANIMAL_ID", animal.id)
                            intent.putExtra("PRESELECTED_ANIMAL_NAME", "${animal.chapeta} - ${animal.nombre ?: "Sin nombre"}")
                            context.startActivity(intent)
                        }
                    }
                }
                .show()
        }

        // El resto del método bind() permanece igual...
        fun bind(animal: Animals) {
            // Usar operador safe call (?.) y elvis operator (?:)
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

            // Capitalizar primera letra de forma segura
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

            // Configurar imagen de forma segura
            if (!animal.foto_perfil_url.isNullOrEmpty()) {
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