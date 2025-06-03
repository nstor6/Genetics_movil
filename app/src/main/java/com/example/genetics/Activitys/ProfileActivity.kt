package com.example.genetics.Activitys

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.LoginActivity
import com.example.genetics.R
import com.example.genetics.api.ChangePasswordRequest
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.UpdateUserRequest
import com.example.genetics.api.Usuario
import com.example.genetics.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val apiService = RetrofitClient.getApiService()
    private var currentUser: Usuario? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadUserProfile()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_profile -> {
                enableEditMode()
                true
            }
            R.id.action_change_password -> {
                showChangePasswordDialog()
                true
            }
            R.id.action_logout -> {
                confirmLogout()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "👤 Mi Perfil"

        // Inicialmente en modo solo lectura
        setEditMode(false)

        // Configurar listeners
        binding.buttonSaveChanges.setOnClickListener {
            if (validateFields()) {
                saveUserChanges()
            }
        }

        binding.buttonCancelEdit.setOnClickListener {
            cancelEdit()
        }

        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.buttonLogout.setOnClickListener {
            confirmLogout()
        }

        // Configurar imagen de perfil
        binding.imageProfile.setOnClickListener {
            Toast.makeText(this, "Cambio de foto de perfil - Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentProfile.visibility = View.GONE

        lifecycleScope.launch {
            try {
                android.util.Log.d("PROFILE_ACTIVITY", "🔄 Cargando perfil del usuario...")

                val response = apiService.getCurrentUser()

                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()!!
                    displayUserInfo(currentUser!!)

                    android.util.Log.d("PROFILE_ACTIVITY", "✅ Perfil cargado: ${currentUser!!.nombre}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("PROFILE_ACTIVITY", "❌ Error cargando perfil: $errorBody")

                    Toast.makeText(this@ProfileActivity, "Error cargando perfil", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                android.util.Log.e("PROFILE_ACTIVITY", "❌ Exception cargando perfil: ${e.message}", e)
                Toast.makeText(this@ProfileActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.contentProfile.visibility = View.VISIBLE
            }
        }
    }

    private fun displayUserInfo(user: Usuario) {
        // Información básica
        binding.editTextNombre.setText(user.nombre)
        binding.editTextApellidos.setText(user.apellidos)
        binding.editTextEmail.setText(user.email)

        // Rol con formato bonito
        val rolFormateado = when (user.rol) {
            "admin" -> "👑 Administrador"
            "usuario" -> "👤 Usuario"
            "dueño" -> "🏠 Dueño"
            else -> "Usuario"
        }
        binding.textRol.text = rolFormateado

        // Estado
        val estadoText = if (user.activo == true) "✅ Activo" else "❌ Inactivo"
        val estadoColor = if (user.activo == true)
            android.graphics.Color.parseColor("#27AE60")
        else
            android.graphics.Color.parseColor("#E74C3C")

        binding.textEstado.text = estadoText
        binding.textEstado.setTextColor(estadoColor)

        // Fechas
        binding.textFechaCreacion.text = "📅 Miembro desde: ${formatearFecha(user.fechaCreacion)}"
        binding.textUltimoAcceso.text = "🕐 Último acceso: ${formatearFechaCompleta(user.ultimoAcceso)}"

        // Información adicional
        if (user.isStaff == true) {
            binding.textInfoAdicional.visibility = View.VISIBLE
            binding.textInfoAdicional.text = "👑 Usuario con permisos de administrador"
        } else {
            binding.textInfoAdicional.visibility = View.GONE
        }

        // Configurar imagen de perfil (placeholder por ahora)
        binding.imageProfile.setImageResource(
            when (user.rol) {
                "admin" -> R.drawable.user_image
                "dueño" -> R.drawable.user_image
                else -> R.drawable.user_image
            }
        )
    }

    private fun enableEditMode() {
        setEditMode(true)
        binding.editTextNombre.requestFocus()
        Toast.makeText(this, "Modo edición activado", Toast.LENGTH_SHORT).show()
    }

    private fun setEditMode(editMode: Boolean) {
        // Habilitar/deshabilitar campos editables
        binding.editTextNombre.isEnabled = editMode
        binding.editTextApellidos.isEnabled = editMode
        binding.editTextEmail.isEnabled = editMode

        // Mostrar/ocultar botones de edición
        if (editMode) {
            binding.layoutEditButtons.visibility = View.VISIBLE
            binding.layoutActions.visibility = View.GONE
        } else {
            binding.layoutEditButtons.visibility = View.GONE
            binding.layoutActions.visibility = View.VISIBLE
        }

        // Cambiar apariencia de los campos
        val backgroundRes = if (editMode) {
            android.R.drawable.edit_text
        } else {
            android.R.color.transparent
        }

        binding.editTextNombre.setBackgroundResource(backgroundRes)
        binding.editTextApellidos.setBackgroundResource(backgroundRes)
        binding.editTextEmail.setBackgroundResource(backgroundRes)
    }

    private fun validateFields(): Boolean {
        // Validar nombre
        val nombre = binding.editTextNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            binding.editTextNombre.error = "El nombre es obligatorio"
            binding.editTextNombre.requestFocus()
            return false
        } else if (nombre.length < 2) {
            binding.editTextNombre.error = "El nombre debe tener al menos 2 caracteres"
            binding.editTextNombre.requestFocus()
            return false
        }

        // Validar apellidos
        val apellidos = binding.editTextApellidos.text.toString().trim()
        if (apellidos.isEmpty()) {
            binding.editTextApellidos.error = "Los apellidos son obligatorios"
            binding.editTextApellidos.requestFocus()
            return false
        } else if (apellidos.length < 2) {
            binding.editTextApellidos.error = "Los apellidos deben tener al menos 2 caracteres"
            binding.editTextApellidos.requestFocus()
            return false
        }

        // Validar email
        val email = binding.editTextEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.editTextEmail.error = "El email es obligatorio"
            binding.editTextEmail.requestFocus()
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Email no válido"
            binding.editTextEmail.requestFocus()
            return false
        }

        return true
    }

    private fun saveUserChanges() {
        binding.buttonSaveChanges.isEnabled = false
        binding.buttonSaveChanges.text = "Guardando..."

        val updateRequest = UpdateUserRequest(
            nombre = binding.editTextNombre.text.toString().trim(),
            apellidos = binding.editTextApellidos.text.toString().trim(),
            email = binding.editTextEmail.text.toString().trim(),
            rol = currentUser?.rol, // Mantener el rol actual
            activo = currentUser?.activo // Mantener el estado actual
        )

        lifecycleScope.launch {
            try {
                android.util.Log.d("PROFILE_ACTIVITY", "💾 Guardando cambios del perfil...")

                val response = apiService.updateUser(currentUser!!.id!!, updateRequest)

                if (response.isSuccessful && response.body() != null) {
                    val updatedUser = response.body()!!
                    currentUser = updatedUser

                    android.util.Log.d("PROFILE_ACTIVITY", "✅ Perfil actualizado exitosamente")

                    Toast.makeText(this@ProfileActivity, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()

                    // Volver al modo lectura
                    setEditMode(false)

                    // Actualizar la información mostrada
                    displayUserInfo(updatedUser)

                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("PROFILE_ACTIVITY", "❌ Error actualizando perfil: $errorBody")

                    val errorMessage = when (response.code()) {
                        400 -> "Datos incorrectos. Revisa la información."
                        409 -> "Ya existe un usuario con este email."
                        422 -> "Email no válido o datos faltantes."
                        else -> "Error al actualizar perfil: ${response.code()}"
                    }

                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                android.util.Log.e("PROFILE_ACTIVITY", "❌ Exception actualizando perfil: ${e.message}", e)
                Toast.makeText(this@ProfileActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSaveChanges.isEnabled = true
                binding.buttonSaveChanges.text = "💾 Guardar Cambios"
            }
        }
    }

    private fun cancelEdit() {
        // Restaurar valores originales
        currentUser?.let { displayUserInfo(it) }

        // Volver al modo lectura
        setEditMode(false)

        Toast.makeText(this, "Edición cancelada", Toast.LENGTH_SHORT).show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val editCurrentPassword = dialogView.findViewById<android.widget.EditText>(R.id.editCurrentPassword)
        val editNewPassword = dialogView.findViewById<android.widget.EditText>(R.id.editNewPassword)
        val editConfirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.editConfirmPassword)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔒 Cambiar Contraseña")
            .setView(dialogView)
            .setPositiveButton("Cambiar") { _, _ ->
                val currentPassword = editCurrentPassword.text.toString()
                val newPassword = editNewPassword.text.toString()
                val confirmPassword = editConfirmPassword.text.toString()

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validatePasswordChange(current: String, new: String, confirm: String): Boolean {
        when {
            current.isEmpty() -> {
                Toast.makeText(this, "Introduce tu contraseña actual", Toast.LENGTH_SHORT).show()
                return false
            }
            new.isEmpty() -> {
                Toast.makeText(this, "Introduce la nueva contraseña", Toast.LENGTH_SHORT).show()
                return false
            }
            new.length < 6 -> {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }
            new != confirm -> {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return false
            }
            new == current -> {
                Toast.makeText(this, "La nueva contraseña debe ser diferente a la actual", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        val changeRequest = ChangePasswordRequest(oldPassword, newPassword)

        lifecycleScope.launch {
            try {
                android.util.Log.d("PROFILE_ACTIVITY", "🔒 Cambiando contraseña...")

                val response = apiService.changePassword(changeRequest)

                if (response.isSuccessful) {
                    android.util.Log.d("PROFILE_ACTIVITY", "✅ Contraseña cambiada exitosamente")

                    Toast.makeText(this@ProfileActivity, "Contraseña cambiada correctamente", Toast.LENGTH_SHORT).show()

                    // Mostrar diálogo de confirmación
                    androidx.appcompat.app.AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("✅ Contraseña Actualizada")
                        .setMessage("Tu contraseña ha sido cambiada exitosamente.\n\nPor seguridad, se cerrará tu sesión y deberás iniciar sesión nuevamente.")
                        .setPositiveButton("Entendido") { _, _ ->
                            logout()
                        }
                        .setCancelable(false)
                        .show()

                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("PROFILE_ACTIVITY", "❌ Error cambiando contraseña: $errorBody")

                    val errorMessage = when (response.code()) {
                        400 -> "La contraseña actual es incorrecta"
                        422 -> "La nueva contraseña no cumple los requisitos"
                        else -> "Error al cambiar contraseña"
                    }

                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                android.util.Log.e("PROFILE_ACTIVITY", "❌ Exception cambiando contraseña: ${e.message}", e)
                Toast.makeText(this@ProfileActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmLogout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        android.util.Log.d("PROFILE_ACTIVITY", "🚪 Cerrando sesión desde perfil...")

        lifecycleScope.launch {
            try {
                // Intentar logout en el servidor
                apiService.logout()
            } catch (e: Exception) {
                // Ignorar errores del logout en servidor
                android.util.Log.w("PROFILE_ACTIVITY", "Error en logout del servidor: ${e.message}")
            }

            // Limpiar token local
            RetrofitClient.clearToken()

            // Redirigir al login
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("MESSAGE", "Sesión cerrada desde Mi Perfil")
            }
            startActivity(intent)
            finishAffinity()

            Toast.makeText(this@ProfileActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
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

    private fun formatearFechaCompleta(fecha: String?): String {
        return try {
            if (fecha != null) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: Date())
            } else {
                "Nunca"
            }
        } catch (e: Exception) {
            fecha ?: "Nunca"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}