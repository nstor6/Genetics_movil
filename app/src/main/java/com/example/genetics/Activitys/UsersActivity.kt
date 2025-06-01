package com.example.genetics.Activitys  // ← LÍNEA AÑADIDA

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.Create.AddUserActivity
import com.example.genetics.Edit.EditUserActivity
import com.example.genetics.R
import com.example.genetics.api.Adapters.UsersAdapter
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.UpdateUserRequest
import com.example.genetics.api.Usuario
import kotlinx.coroutines.launch

class UsersActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var recyclerViewUsers: androidx.recyclerview.widget.RecyclerView
    private lateinit var textEmptyState: android.widget.LinearLayout
    private lateinit var fabAddUser: com.google.android.material.floatingactionbutton.FloatingActionButton

    private val apiService = RetrofitClient.getApiService()
    private lateinit var usersAdapter: UsersAdapter
    private val usuariosList = mutableListOf<Usuario>()

    companion object {
        private const val REQUEST_ADD_USER = 4001
        private const val REQUEST_EDIT_USER = 4002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        initializeViews()
        setupUI()
        loadUsers()
    }

    // ... resto del código permanece igual
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        textEmptyState = findViewById(R.id.textEmptyState)
        fabAddUser = findViewById(R.id.fabAddUser)
    }

    private fun setupUI() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "👥 Usuarios"

        // Configurar RecyclerView con callbacks de CRUD
        usersAdapter = UsersAdapter(
            usuariosList,
            onItemClick = { usuario -> mostrarDetallesUsuario(usuario) },
            onEditClick = { usuario -> editarUsuario(usuario) },
            onDeleteClick = { usuario -> confirmarEliminarUsuario(usuario) },
            onToggleActiveClick = { usuario -> toggleUsuarioActivo(usuario) }
        )

        recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(this@UsersActivity)
            adapter = usersAdapter
        }

        // Configurar FAB
        fabAddUser.setOnClickListener {
            val intent = Intent(this, AddUserActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_USER)
        }

        // Configurar refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadUsers()
        }
    }

    // ... resto de métodos permanecen iguales

    private fun mostrarDetallesUsuario(usuario: Usuario) {
        val mensaje = buildString {
            append("👤 Nombre: ${usuario.nombre} ${usuario.apellidos}\n")
            append("📧 Email: ${usuario.email}\n")
            append("🏷️ Rol: ${formatearRol(usuario.rol)}\n")
            append("📅 Miembro desde: ${formatearFecha(usuario.fechaCreacion)}\n")
            append("🕐 Último acceso: ${formatearFechaUltimoAcceso(usuario.ultimoAcceso)}\n")
            append("📊 Estado: ${if (usuario.activo == true) "✅ Activo" else "❌ Inactivo"}")
            if (usuario.isStaff == true) {
                append("\n👑 Usuario administrador")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles del Usuario")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .setNeutralButton("✏️ Editar") { _, _ ->
                editarUsuario(usuario)
            }
            .setNegativeButton("🔄 ${if (usuario.activo == true) "Desactivar" else "Activar"}") { _, _ ->
                toggleUsuarioActivo(usuario)
            }
            .show()
    }

    private fun editarUsuario(usuario: Usuario) {
        val intent = Intent(this, EditUserActivity::class.java)
        intent.putExtra("USER_ID", usuario.id)
        startActivityForResult(intent, REQUEST_EDIT_USER)
    }

    private fun confirmarEliminarUsuario(usuario: Usuario) {
        // Verificaciones de seguridad antes de mostrar el diálogo
        val mensaje = when {
            usuario.rol == "admin" -> {
                "⚠️ No se puede eliminar un usuario administrador por seguridad."
            }
            usuario.isStaff == true -> {
                "⚠️ No se puede eliminar un usuario con permisos de staff."
            }
            else -> {
                "¿Estás seguro de que quieres eliminar al usuario '${usuario.nombre} ${usuario.apellidos}'?\n\n" +
                        "⚠️ Esta acción:\n" +
                        "• Eliminará permanentemente la cuenta del usuario\n" +
                        "• Puede afectar datos asociados (animales, incidencias, etc.)\n" +
                        "• NO se puede deshacer\n\n" +
                        "¿Continuar con la eliminación?"
            }
        }

        if (usuario.rol == "admin" || usuario.isStaff == true) {
            // Solo mostrar mensaje de advertencia
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("❌ Eliminación no permitida")
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show()
        } else {
            // Mostrar diálogo de confirmación
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar Usuario")
                .setMessage(mensaje)
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    eliminarUsuario(usuario)
                }
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }

    private fun eliminarUsuario(usuario: Usuario) {
        // Mostrar indicador de carga
        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminando usuario...")
            .setMessage("Por favor espera...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                android.util.Log.d("USERS_ACTIVITY", "🗑️ Iniciando eliminación del usuario ID: ${usuario.id}")

                val response = apiService.deleteUser(usuario.id!!)

                loadingDialog.dismiss()

                if (response.isSuccessful) {
                    android.util.Log.d("USERS_ACTIVITY", "✅ Usuario eliminado exitosamente")

                    // Mostrar mensaje de éxito
                    Toast.makeText(
                        this@UsersActivity,
                        "✅ Usuario '${usuario.nombre} ${usuario.apellidos}' eliminado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Eliminar del adapter inmediatamente para mejor UX
                    usersAdapter.removeUser(usuario)

                    // Actualizar estado vacío si es necesario
                    updateEmptyState(usuariosList.isEmpty())

                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("USERS_ACTIVITY", "❌ Error del servidor: $errorBody")

                    // Manejar errores específicos
                    val errorMessage = when (response.code()) {
                        403 -> "❌ No tienes permisos para eliminar este usuario"
                        404 -> "❌ Usuario no encontrado (puede haber sido eliminado por otro administrador)"
                        409 -> "❌ No se puede eliminar: el usuario tiene datos asociados que deben eliminarse primero"
                        500 -> "❌ Error interno del servidor. Inténtalo más tarde"
                        else -> "❌ Error al eliminar usuario (código: ${response.code()})"
                    }

                    mostrarErrorEliminacion(errorMessage, errorBody)
                }

            } catch (e: java.net.ConnectException) {
                loadingDialog.dismiss()
                android.util.Log.e("USERS_ACTIVITY", "❌ Error de conexión: ${e.message}")
                mostrarErrorEliminacion("❌ No se puede conectar al servidor. Verifica tu conexión a internet.", null)

            } catch (e: java.net.SocketTimeoutException) {
                loadingDialog.dismiss()
                android.util.Log.e("USERS_ACTIVITY", "❌ Timeout: ${e.message}")
                mostrarErrorEliminacion("❌ La operación tardó demasiado. Inténtalo de nuevo.", null)

            } catch (e: Exception) {
                loadingDialog.dismiss()
                android.util.Log.e("USERS_ACTIVITY", "❌ Exception inesperada: ${e.message}", e)
                mostrarErrorEliminacion("❌ Error inesperado: ${e.localizedMessage ?: "Error desconocido"}", null)
            }
        }
    }

    private fun mostrarErrorEliminacion(mensaje: String, detallesTecnicos: String?) {
        val mensajeCompleto = if (detallesTecnicos != null) {
            "$mensaje\n\nDetalles técnicos: $detallesTecnicos"
        } else {
            mensaje
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error al eliminar usuario")
            .setMessage(mensajeCompleto)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun toggleUsuarioActivo(usuario: Usuario) {
        // Como no tienes el endpoint toggle-active, usaremos una actualización parcial
        val nuevoEstado = !(usuario.activo ?: true)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("${if (nuevoEstado) "Activar" else "Desactivar"} Usuario")
            .setMessage("¿Estás seguro de que quieres ${if (nuevoEstado) "activar" else "desactivar"} al usuario '${usuario.nombre} ${usuario.apellidos}'?")
            .setPositiveButton("Sí") { _, _ ->
                actualizarEstadoUsuario(usuario, nuevoEstado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarEstadoUsuario(usuario: Usuario, nuevoEstado: Boolean) {
        lifecycleScope.launch {
            try {
                // Usar updateUser en lugar del método que no existe
                val updateRequest = UpdateUserRequest(
                    nombre = usuario.nombre,
                    apellidos = usuario.apellidos,
                    email = usuario.email,
                    rol = usuario.rol,
                    activo = nuevoEstado
                )

                val response = apiService.updateUser(usuario.id!!, updateRequest)

                if (response.isSuccessful && response.body() != null) {
                    val mensaje = if (nuevoEstado) {
                        "Usuario activado correctamente"
                    } else {
                        "Usuario desactivado correctamente"
                    }

                    Toast.makeText(this@UsersActivity, mensaje, Toast.LENGTH_SHORT).show()

                    // Recargar la lista para mostrar el cambio
                    loadUsers()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("USERS_ACTIVITY", "❌ Error actualizando estado: $errorBody")
                    Toast.makeText(this@UsersActivity, "Error al cambiar estado del usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("USERS_ACTIVITY", "❌ Exception actualizando estado: ${e.message}", e)
                Toast.makeText(this@UsersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatearRol(rol: String?): String {
        return when (rol) {
            "admin" -> "👑 Administrador"
            "usuario" -> "👤 Usuario"
            "dueño" -> "🏠 Dueño"
            else -> "❓ Sin definir"
        }
    }

    private fun formatearFecha(fecha: String?): String {
        return try {
            if (fecha != null) {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: java.util.Date())
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
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: java.util.Date())
            } else {
                "Nunca"
            }
        } catch (e: Exception) {
            fecha ?: "Nunca"
        }
    }

    private fun loadUsers() {
        android.util.Log.d("USERS_ACTIVITY", "🔄 Iniciando carga de usuarios...")
        swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getUsers()
                android.util.Log.d("USERS_ACTIVITY", "📡 Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!
                    android.util.Log.d("USERS_ACTIVITY", "✅ Usuarios recibidos: ${users.size}")

                    // Limpiar y actualizar la lista
                    usuariosList.clear()
                    usuariosList.addAll(users.sortedBy { it.nombre })

                    // Notificar al adapter
                    usersAdapter.notifyDataSetChanged()

                    android.util.Log.d("USERS_ACTIVITY", "📋 Lista actualizada. Items en adapter: ${usersAdapter.itemCount}")

                    // Actualizar UI vacía
                    updateEmptyState(users.isEmpty())

                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("USERS_ACTIVITY", "❌ Error: $errorBody")
                    Toast.makeText(this@UsersActivity, "Error cargando usuarios: ${response.code()}", Toast.LENGTH_SHORT).show()
                    updateEmptyState(true)
                }

            } catch (e: Exception) {
                android.util.Log.e("USERS_ACTIVITY", "❌ Exception: ${e.message}", e)
                Toast.makeText(this@UsersActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                updateEmptyState(true)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            textEmptyState.visibility = View.VISIBLE
            recyclerViewUsers.visibility = View.GONE
            android.util.Log.d("USERS_ACTIVITY", "👻 Mostrando estado vacío")
        } else {
            textEmptyState.visibility = View.GONE
            recyclerViewUsers.visibility = View.VISIBLE
            android.util.Log.d("USERS_ACTIVITY", "📋 Mostrando lista de usuarios")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_USER, REQUEST_EDIT_USER -> {
                if (resultCode == RESULT_OK) {
                    android.util.Log.d("USERS_ACTIVITY", "🔄 Recargando después de crear/editar usuario")
                    loadUsers()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}