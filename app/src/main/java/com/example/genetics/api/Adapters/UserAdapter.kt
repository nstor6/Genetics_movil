package com.example.genetics.api.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView
import com.example.genetics.R
import com.example.genetics.api.Usuario
import java.text.SimpleDateFormat
import java.util.*

class UsersAdapter(
    private val usuariosList: MutableList<Usuario>,
    private val onItemClick: (Usuario) -> Unit,
    private val onEditClick: ((Usuario) -> Unit)? = null,
    private val onDeleteClick: ((Usuario) -> Unit)? = null,
    private val onToggleActiveClick: ((Usuario) -> Unit)? = null
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardUser)
        val imageUser: ImageView = itemView.findViewById(R.id.imageUser)
        val textNombre: TextView = itemView.findViewById(R.id.textNombre)
        val textEmail: TextView = itemView.findViewById(R.id.textEmail)
        val textRol: TextView = itemView.findViewById(R.id.textRol)
        val textEstado: TextView = itemView.findViewById(R.id.textEstado)
        val textFechaCreacion: TextView = itemView.findViewById(R.id.textFechaCreacion)
        val textUltimoAcceso: TextView = itemView.findViewById(R.id.textUltimoAcceso)

        init {
            // Click normal - ver detalles
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < usuariosList.size) {
                    onItemClick(usuariosList[position])
                }
            }

            // Long click - mostrar opciones de CRUD
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < usuariosList.size) {
                    mostrarOpcionesUsuario(usuariosList[position])
                    true
                } else {
                    false
                }
            }
        }

        private fun mostrarOpcionesUsuario(usuario: Usuario) {
            val context = itemView.context
            val opciones = mutableListOf<String>()

            opciones.add("👁️ Ver detalles")
            opciones.add("✏️ Editar usuario")

            // Solo permitir activar/desactivar, no eliminar por seguridad
            val estadoText = if (usuario.activo == true) "❌ Desactivar" else "✅ Activar"
            opciones.add(estadoText)

            // Solo permitir eliminar si no es el usuario actual y no es admin
            if (usuario.rol != "admin") {
                opciones.add("🗑️ Eliminar usuario")
            }

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Opciones para ${usuario.nombre}")
                .setItems(opciones.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> onItemClick(usuario) // Ver detalles
                        1 -> onEditClick?.invoke(usuario) // Editar
                        2 -> onToggleActiveClick?.invoke(usuario) // Activar/Desactivar
                        3 -> if (usuario.rol != "admin") onDeleteClick?.invoke(usuario) // Eliminar (solo si no es admin)
                    }
                }
                .show()
        }

        fun bind(usuario: Usuario) {
            // Nombre completo
            textNombre.text = "${usuario.nombre} ${usuario.apellidos}"

            // Email
            textEmail.text = "📧 ${usuario.email}"

            // Rol con emoji y color
            val (rolTexto, rolColor) = when (usuario.rol) {
                "admin" -> Pair("👑 Administrador", Color.parseColor("#E74C3C"))
                "usuario" -> Pair("👤 Usuario", Color.parseColor("#3498DB"))
                "dueño" -> Pair("🏠 Dueño", Color.parseColor("#27AE60"))
                else -> Pair("❓ Sin definir", Color.parseColor("#7F8C8D"))
            }
            textRol.text = rolTexto
            textRol.setTextColor(rolColor)

            // Estado (activo/inactivo)
            if (usuario.activo == true) {
                textEstado.text = "✅ Activo"
                textEstado.setTextColor(Color.parseColor("#27AE60"))
                cardView.alpha = 1.0f
            } else {
                textEstado.text = "❌ Inactivo"
                textEstado.setTextColor(Color.parseColor("#E74C3C"))
                cardView.alpha = 0.6f
            }

            // Fecha de creación
            textFechaCreacion.text = "📅 Miembro desde: ${formatearFecha(usuario.fechaCreacion)}"

            // Último acceso
            textUltimoAcceso.text = "🕐 Último acceso: ${formatearFechaUltimoAcceso(usuario.ultimoAcceso)}"

            // Imagen de perfil (por ahora placeholder)
            val avatarResource = when (usuario.rol) {
                "admin" -> R.drawable.user_image
                "dueño" -> R.drawable.user_image
                else -> R.drawable.user_image
            }
            imageUser.setImageResource(avatarResource)

            // Color de fondo de la card según el rol
            val backgroundColor = when (usuario.rol) {
                "admin" -> Color.parseColor("#FADBD8") // Rojo muy claro
                "dueño" -> Color.parseColor("#D5F4E6") // Verde muy claro
                "usuario" -> Color.parseColor("#E8F6FD") // Azul muy claro
                else -> Color.parseColor("#F8F9FA") // Gris muy claro
            }
            cardView.setCardBackgroundColor(backgroundColor)

            // Borde especial para administradores (ahora funciona con MaterialCardView)
            if (usuario.rol == "admin") {
                cardView.strokeColor = Color.parseColor("#E74C3C")
                cardView.strokeWidth = 6
            } else {
                cardView.strokeWidth = 0
            }
        }

        private fun formatearFecha(fecha: String?): String {
            return try {
                if (fecha != null) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(fecha)
                    outputFormat.format(date ?: Date())
                } else {
                    "No disponible"
                }
            } catch (e: Exception) {
                fecha ?: "No disponible"
            }
        }

        private fun formatearFechaUltimoAcceso(fecha: String?): String {
            return try {
                if (fecha != null) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(fecha)
                    val now = Date()
                    val diffInMs = now.time - (date?.time ?: 0)
                    val diffInDays = diffInMs / (1000 * 60 * 60 * 24)

                    when {
                        diffInDays == 0L -> "Hoy"
                        diffInDays == 1L -> "Ayer"
                        diffInDays < 7 -> "Hace $diffInDays días"
                        else -> outputFormat.format(date ?: Date())
                    }
                } else {
                    "Nunca"
                }
            } catch (e: Exception) {
                "Desconocido"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        if (position < usuariosList.size) {
            holder.bind(usuariosList[position])
        }
    }

    override fun getItemCount(): Int = usuariosList.size

    fun updateList(newList: List<Usuario>) {
        android.util.Log.d("USERS_ADAPTER", "🔄 Actualizando lista: ${newList.size} usuarios")

        usuariosList.clear()
        usuariosList.addAll(newList)

        notifyDataSetChanged()

        android.util.Log.d("USERS_ADAPTER", "✅ Lista actualizada. Items en adapter: ${itemCount}")
    }

    // Método para eliminar un usuario específico de la lista
    fun removeUser(usuario: Usuario) {
        val index = usuariosList.indexOfFirst { it.id == usuario.id }
        if (index != -1) {
            usuariosList.removeAt(index)
            notifyItemRemoved(index)
            android.util.Log.d("USERS_ADAPTER", "🗑️ Usuario eliminado del adapter en posición $index")
        }
    }

    // Método para filtrar por rol
    fun filterByRole(role: String) {
        // Esta funcionalidad se puede implementar después si es necesaria
        android.util.Log.d("USERS_ADAPTER", "🔍 Filtrado por rol: $role")
    }

    // Método para filtrar por estado (activo/inactivo)
    fun filterByStatus(activeOnly: Boolean) {
        // Esta funcionalidad se puede implementar después si es necesaria
        android.util.Log.d("USERS_ADAPTER", "🔍 Filtrado por estado activo: $activeOnly")
    }
}