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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar Usuario")
            .setMessage("¿Estás seguro de que quieres eliminar al usuario '${usuario.nombre} ${usuario.apellidos}'?\n\n⚠️ Esta acción eliminará también todos los datos asociados (animales, incidencias, tratamientos creados por este usuario).\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarUsuario(usuario)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarUsuario(usuario: Usuario) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("USERS_ACTIVITY", "🗑️ Eliminando usuario ID: ${usuario.id}")

                val response = apiService.deleteUser(usuario.id!!)

                if (response.isSuccessful) {
                    Toast.makeText(this@UsersActivity, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show()

                    // Eliminar del adapter inmediatamente
                    usersAdapter.removeUser(usuario)

                    // Actualizar estado vacío si es necesario
                    updateEmptyState(usuariosList.isEmpty())

                    android.util.Log.d("USERS_ACTIVITY", "✅ Usuario eliminado exitosamente")
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("USERS_ACTIVITY", "❌ Error eliminando: $errorBody")
                    Toast.makeText(this@UsersActivity, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("USERS_ACTIVITY", "❌ Exception eliminando: ${e.message}", e)
                Toast.makeText(this@UsersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleUsuarioActivo(usuario: Usuario) {
        lifecycleScope.launch {
            try {
                val response = apiService.toggleUserActive(usuario.id!!)

                if (response.isSuccessful && response.body() != null) {
                    val usuarioActualizado = response.body()!!
                    val mensaje = if (usuarioActualizado.activo == true) {
                        "Usuario activado correctamente"
                    } else {
                        "Usuario desactivado correctamente"
                    }

                    Toast.makeText(this@UsersActivity, mensaje, Toast.LENGTH_SHORT).show()

                    // Recargar la lista para mostrar el cambio
                    loadUsers()
                } else {
                    Toast.makeText(this@UsersActivity, "Error al cambiar estado del usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
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