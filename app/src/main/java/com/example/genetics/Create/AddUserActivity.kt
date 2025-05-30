package com.example.genetics.Create

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.CreateUserRequest
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAddUserBinding
import kotlinx.coroutines.launch

class AddUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddUserBinding
    private val apiService = RetrofitClient.getApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "👤 Nuevo Usuario"

        // Configurar botones
        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                crearUsuario()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
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

        // Validación de contraseñas en tiempo real
        binding.editTextConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validarPasswordsCoinciden()
            }
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

    private fun validarPasswordsCoinciden(): Boolean {
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()

        return if (password != confirmPassword) {
            binding.editTextConfirmPassword.error = "Las contraseñas no coinciden"
            false
        } else {
            binding.editTextConfirmPassword.error = null
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

        // Validar contraseña
        val password = binding.editTextPassword.text.toString()
        if (password.isEmpty()) {
            binding.editTextPassword.error = "La contraseña es obligatoria"
            if (isValid) binding.editTextPassword.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            binding.editTextPassword.error = "La contraseña debe tener al menos 6 caracteres"
            if (isValid) binding.editTextPassword.requestFocus()
            isValid = false
        }

        // Validar confirmación de contraseña
        if (!validarPasswordsCoinciden()) {
            if (isValid) binding.editTextConfirmPassword.requestFocus()
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

    private fun crearUsuario() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Creando..."

        // Mapear rol del spinner al valor del backend
        val rolSeleccionado = when (binding.spinnerRol.selectedItem.toString()) {
            "👑 Administrador" -> "admin"
            "🏠 Dueño" -> "dueño"
            "👤 Usuario" -> "usuario"
            else -> "usuario"
        }

        val nuevoUsuario = CreateUserRequest(
            nombre = binding.editTextNombre.text.toString().trim(),
            apellidos = binding.editTextApellidos.text.toString().trim(),
            email = binding.editTextEmail.text.toString().trim(),
            password = binding.editTextPassword.text.toString(),
            rol = rolSeleccionado
        )

        lifecycleScope.launch {
            try {
                android.util.Log.d("ADD_USER", "🔄 Creando usuario: ${nuevoUsuario.email}")

                val response = apiService.createUser(nuevoUsuario)

                if (response.isSuccessful && response.body() != null) {
                    val usuarioCreado = response.body()!!
                    android.util.Log.d("ADD_USER", "✅ Usuario creado: ${usuarioCreado.id}")

                    Toast.makeText(this@AddUserActivity, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("ADD_USER", "❌ Error: $errorBody")

                    // Manejar errores específicos
                    val errorMessage = when (response.code()) {
                        400 -> "Datos incorrectos. Revisa la información introducida."
                        409 -> "Ya existe un usuario con este email."
                        422 -> "Email no válido o datos faltantes."
                        else -> "Error al crear usuario: ${response.code()}"
                    }

                    Toast.makeText(this@AddUserActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ADD_USER", "❌ Exception: ${e.message}", e)
                Toast.makeText(this@AddUserActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "👤 Crear Usuario"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}