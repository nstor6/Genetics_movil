package com.example.genetics.Edit

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.UpdateUserRequest
import com.example.genetics.api.Usuario
import com.example.genetics.databinding.ActivityEditUserBinding
import kotlinx.coroutines.launch

class EditUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditUserBinding
    private val apiService = RetrofitClient.getApiService()
    private var userId: Int = -1
    private var currentUser: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener ID del usuario a editar
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarDatosUsuario()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✏️ Editar Usuario"

        // Configurar botones
        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarCambios()
            }
        }

        binding.buttonCancel.setOnClickListener {
            confirmarSalida()
        }

        // Configurar validación en tiempo real
        setupValidation()
    }

    private fun setupValidation() {
        // Validación de email en tiempo real
        binding.editTextEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validarEmail()
            }
        }
    }

    private fun cargarDatosUsuario() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = apiService.getUser(userId)

                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()!!
                    llenarFormulario(currentUser!!)
                } else {
                    Toast.makeText(this@EditUserActivity, "Error cargando usuario", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditUserActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun llenarFormulario(usuario: Usuario) {
        // Información básica
        binding.editTextNombre.setText(usuario.nombre)
        binding.editTextApellidos.setText(usuario.apellidos)
        binding.editTextEmail.setText(usuario.email)

        // Configurar rol
        val rolIndex = when (usuario.rol) {
            "admin" -> 3 // 👑 Administrador
            "dueño" -> 2  // 🏠 Dueño
            "usuario" -> 1 // 👤 Usuario
            else -> 0
        }
        binding.spinnerRol.setSelection(rolIndex)

        // Estado activo/inactivo
        binding.switchActivo.isChecked = usuario.activo ?: true

        // Mostrar información adicional
        binding.textFechaCreacion.text = "📅 Miembro desde: ${formatearFecha(usuario.fechaCreacion)}"
        binding.textUltimoAcceso.text = "🕐 Último acceso: ${formatearFechaUltimoAcceso(usuario.ultimoAcceso)}"

        // Mostrar estado staff si aplica
        if (usuario.isStaff == true) {
            binding.textEstadoStaff.visibility = View.VISIBLE
            binding.textEstadoStaff.text = "👑 Usuario con permisos de administración"
        }
    }

    private fun validarEmail(): Boolean {
        val email = binding.editTextEmail.text.toString().trim()
        return if (email.isEmpty()) {
            binding.editTextEmail.error = "El email es obligatorio"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Email no válido"
            false
        } else {
            binding.editTextEmail.error = null
            true
        }
    }

    private fun validarCampos(): Boolean {
        var isValid = true

        // Validar nombre
        val nombre = binding.editTextNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            binding.editTextNombre.error = "El nombre es obligatorio"
            binding.editTextNombre.requestFocus()
            isValid = false
        }

        // Validar apellidos
        val apellidos = binding.editTextApellidos.text.toString().trim()
        if (apellidos.isEmpty()) {
            binding.editTextApellidos.error = "Los apellidos son obligatorios"
            if (isValid) binding.editTextApellidos.requestFocus()
            isValid = false
        }

        // Validar email
        if (!validarEmail()) {
            if (isValid) binding.editTextEmail.requestFocus()
            isValid = false
        }

        // Validar rol
        val rol = binding.spinnerRol.selectedItem.toString()
        if (rol == "Seleccionar rol") {
            Toast.makeText(this, "Selecciona un rol", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun guardarCambios() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        // Mapear rol del spinner al valor del backend
        val rolSeleccionado = when (binding.spinnerRol.selectedItem.toString()) {
            "👑 Administrador" -> "admin"
            "🏠 Dueño" -> "dueño"
            "👤 Usuario" -> "usuario"
            else -> currentUser?.rol ?: "usuario"
        }

        val usuarioActualizado = UpdateUserRequest(
            nombre = binding.editTextNombre.text.toString().trim(),
            apellidos = binding.editTextApellidos.text.toString().trim(),
            email = binding.editTextEmail.text.toString().trim(),
            rol = rolSeleccionado,
            activo = binding.switchActivo.isChecked
        )

        lifecycleScope.launch {
            try {
                android.util.Log.d("EDIT_USER", "🔄 Actualizando usuario ID: $userId")

                val response = apiService.updateUser(userId, usuarioActualizado)

                if (response.isSuccessful && response.body() != null) {
                    val usuarioRespuesta = response.body()!!
                    android.util.Log.d("EDIT_USER", "✅ Usuario actualizado: ${usuarioRespuesta.id}")

                    Toast.makeText(this@EditUserActivity, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("EDIT_USER", "❌ Error: $errorBody")

                    // Manejar errores específicos
                    val errorMessage = when (response.code()) {
                        400 -> "Datos incorrectos. Revisa la información introducida."
                        404 -> "Usuario no encontrado."
                        409 -> "Ya existe un usuario con este email."
                        422 -> "Email no válido o datos faltantes."
                        else -> "Error al actualizar usuario: ${response.code()}"
                    }

                    Toast.makeText(this@EditUserActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("EDIT_USER", "❌ Exception: ${e.message}", e)
                Toast.makeText(this@EditUserActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "💾 Guardar Cambios"
            }
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

    private fun confirmarSalida() {
        if (hayCambiosSinGuardar()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Cambios sin guardar")
                .setMessage("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?")
                .setPositiveButton("Salir sin guardar") { _, _ ->
                    finish()
                }
                .setNegativeButton("Continuar editando", null)
                .setNeutralButton("Guardar y salir") { _, _ ->
                    if (validarCampos()) {
                        guardarCambios()
                    }
                }
                .show()
        } else {
            finish()
        }
    }

    private fun hayCambiosSinGuardar(): Boolean {
        if (currentUser == null) return false

        val rolActual = when (binding.spinnerRol.selectedItem.toString()) {
            "👑 Administrador" -> "admin"
            "🏠 Dueño" -> "dueño"
            "👤 Usuario" -> "usuario"
            else -> currentUser?.rol ?: "usuario"
        }

        return binding.editTextNombre.text.toString().trim() != (currentUser?.nombre ?: "") ||
                binding.editTextApellidos.text.toString().trim() != (currentUser?.apellidos ?: "") ||
                binding.editTextEmail.text.toString().trim() != (currentUser?.email ?: "") ||
                rolActual != (currentUser?.rol ?: "") ||
                binding.switchActivo.isChecked != (currentUser?.activo ?: true)
    }

    override fun onSupportNavigateUp(): Boolean {
        confirmarSalida()
        return true
    }

    override fun onBackPressed() {
        confirmarSalida()
        super.onBackPressed()
    }
}