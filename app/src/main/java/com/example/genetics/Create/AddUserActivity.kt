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
        } else if (nombre.length < 2) {
            binding.editTextNombre.error = "El nombre debe tener al menos 2 caracteres"
            binding.editTextNombre.requestFocus()
            isValid = false
        }

        // Validar apellidos
        val apellidos = binding.editTextApellidos.text.toString().trim()
        if (apellidos.isEmpty()) {
            binding.editTextApellidos.error = "Los apellidos son obligatorios"
            if (isValid) binding.editTextApellidos.requestFocus()
            isValid = false
        } else if (apellidos.length < 2) {
            binding.editTextApellidos.error = "Los apellidos deben tener al menos 2 caracteres"
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
        // Deshabilitar botón para evitar doble click
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

        android.util.Log.d("ADD_USER", "🔄 Iniciando creación de usuario...")
        android.util.Log.d("ADD_USER", "📤 Datos a enviar: nombre=${nuevoUsuario.nombre}, email=${nuevoUsuario.email}, rol=${nuevoUsuario.rol}")

        lifecycleScope.launch {
            try {
                android.util.Log.d("ADD_USER", "📡 Enviando petición al servidor...")

                val response = apiService.createUser(nuevoUsuario)

                android.util.Log.d("ADD_USER", "📡 Response recibida - Código: ${response.code()}")
                android.util.Log.d("ADD_USER", "📡 Response exitosa: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val usuarioCreado = response.body()!!
                    android.util.Log.d("ADD_USER", "✅ Usuario creado exitosamente - ID: ${usuarioCreado.id}")

                    Toast.makeText(this@AddUserActivity, "✅ Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()

                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("ADD_USER", "❌ Error del servidor: $errorBody")
                    android.util.Log.e("ADD_USER", "❌ Código de error: ${response.code()}")

                    // Manejar errores específicos del servidor
                    val errorMessage = when (response.code()) {
                        400 -> {
                            android.util.Log.e("ADD_USER", "❌ Error 400: Datos incorrectos")
                            "Datos incorrectos. Revisa la información introducida."
                        }
                        409 -> {
                            android.util.Log.e("ADD_USER", "❌ Error 409: Email ya existe")
                            "Ya existe un usuario con este email."
                        }
                        422 -> {
                            android.util.Log.e("ADD_USER", "❌ Error 422: Datos no válidos")
                            "Email no válido o datos faltantes."
                        }
                        500 -> {
                            android.util.Log.e("ADD_USER", "❌ Error 500: Error del servidor")
                            "Error interno del servidor. Inténtalo más tarde."
                        }
                        else -> {
                            android.util.Log.e("ADD_USER", "❌ Error desconocido: ${response.code()}")
                            "Error al crear usuario (${response.code()}). Contacta con soporte."
                        }
                    }

                    Toast.makeText(this@AddUserActivity, errorMessage, Toast.LENGTH_LONG).show()
                }

            } catch (e: java.net.ConnectException) {
                android.util.Log.e("ADD_USER", "❌ Error de conexión: ${e.message}")
                Toast.makeText(this@AddUserActivity, "❌ No se puede conectar al servidor. Verifica tu conexión a internet.", Toast.LENGTH_LONG).show()

            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("ADD_USER", "❌ Timeout: ${e.message}")
                Toast.makeText(this@AddUserActivity, "❌ La conexión tardó demasiado. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()

            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("ADD_USER", "❌ Host desconocido: ${e.message}")
                Toast.makeText(this@AddUserActivity, "❌ No se puede encontrar el servidor. Verifica la configuración.", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                android.util.Log.e("ADD_USER", "❌ Exception inesperada: ${e.message}", e)
                Toast.makeText(this@AddUserActivity, "❌ Error inesperado: ${e.localizedMessage ?: e.message ?: "Error desconocido"}", Toast.LENGTH_LONG).show()

            } finally {
                // Restaurar botón siempre
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "👤 Crear Usuario"
                android.util.Log.d("ADD_USER", "🔄 Botón restaurado")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}