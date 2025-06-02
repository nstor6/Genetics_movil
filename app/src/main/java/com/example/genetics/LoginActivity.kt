package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.DashboardActivity
import com.example.genetics.Activitys.UserDashboardActivity
import com.example.genetics.api.LoginRequest
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var apiService: com.example.genetics.api.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            RetrofitClient.initialize(this)
            apiService = RetrofitClient.getApiService()
            Log.d("LOGIN_ACTIVITY", "✅ RetrofitClient inicializado correctamente")
        } catch (e: Exception) {
            Log.e("LOGIN_ACTIVITY", "❌ Error inicializando RetrofitClient: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error de configuración. Reinicia la app.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        binding.buttonLogin.setOnClickListener {
            login()
        }

        binding.buttonDemo.setOnClickListener {
            fillDemoData()
        }
    }

    private fun fillDemoData() {
        binding.editTextEmail.setText("admin@genetics.com")
        binding.editTextPassword.setText("admin123")
        Toast.makeText(this, "Credenciales demo cargadas", Toast.LENGTH_SHORT).show()
    }

    private fun login() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        Log.d("LOGIN_DEBUG", "Intentando login con email: $email")

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonLogin.isEnabled = false
        binding.buttonLogin.text = "Entrando..."

        lifecycleScope.launch {
            try {
                Log.d("LOGIN_DEBUG", "Enviando request al servidor...")
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                Log.d("LOGIN_DEBUG", "Response code: ${response.code()}")
                Log.d("LOGIN_DEBUG", "Response successful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    Log.d("LOGIN_DEBUG", "Login exitoso! Token recibido")

                    RetrofitClient.saveToken(loginResponse.access)

                    Log.d("LOGIN_DEBUG", "🔍 Verificando rol del usuario después del login...")
                    verificarRolYRedirigir()

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LOGIN_DEBUG", "Error response: $errorBody")
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("LOGIN_DEBUG", "Exception durante login: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonLogin.isEnabled = true
                binding.buttonLogin.text = "Entrar"
            }
        }
    }

    private suspend fun verificarRolYRedirigir() {
        try {
            Log.d("LOGIN_DEBUG", "📡 Llamando a /api/auth/me/ para obtener datos del usuario...")

            val userResponse = apiService.getCurrentUser()

            if (userResponse.isSuccessful && userResponse.body() != null) {
                val usuario = userResponse.body()!!

                Log.d("LOGIN_DEBUG", "✅ Usuario obtenido exitosamente:")
                Log.d("LOGIN_DEBUG", "   - ID: ${usuario.id}")
                Log.d("LOGIN_DEBUG", "   - Nombre: ${usuario.nombre} ${usuario.apellidos}")
                Log.d("LOGIN_DEBUG", "   - Email: ${usuario.email}")
                Log.d("LOGIN_DEBUG", "   - Rol: '${usuario.rol}'")
                Log.d("LOGIN_DEBUG", "   - isStaff: ${usuario.isStaff}")
                Log.d("LOGIN_DEBUG", "   - Activo: ${usuario.activo}")

                // 👑 Validación segura del rol
                val isAdmin = usuario.rol?.trim()?.lowercase() == "admin" || usuario.isStaff == true

                if (isAdmin) {
                    Log.d("LOGIN_DEBUG", "👑 ADMINISTRADOR detectado - Abriendo DashboardActivity completo")
                    Toast.makeText(this@LoginActivity, "¡Bienvenido, Admin ${usuario.nombre}! 👑", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                } else {
                    Log.d("LOGIN_DEBUG", "👤 USUARIO NORMAL detectado - Abriendo UserDashboardActivity")
                    Toast.makeText(this@LoginActivity, "¡Bienvenido, ${usuario.nombre}! 👤", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                }

                finish()

            } else {
                val errorBody = userResponse.errorBody()?.string()
                Log.e("LOGIN_DEBUG", "❌ Error obteniendo datos del usuario después del login")
                Log.e("LOGIN_DEBUG", "   - Response code: ${userResponse.code()}")
                Log.e("LOGIN_DEBUG", "   - Error body: $errorBody")

                when (userResponse.code()) {
                    401 -> {
                        Log.e("LOGIN_DEBUG", "🚨 Token inválido o expirado")
                        Toast.makeText(this@LoginActivity, "Sesión inválida. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                        RetrofitClient.clearToken()
                    }
                    404 -> {
                        Log.e("LOGIN_DEBUG", "🚨 Endpoint /auth/me/ no encontrado en el servidor")
                        Toast.makeText(this@LoginActivity, "Configuración del servidor incompleta.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                        finish()
                    }
                    else -> {
                        Log.w("LOGIN_DEBUG", "⚠️ Error desconocido, usando dashboard de usuario por defecto")
                        Toast.makeText(this@LoginActivity, "¡Bienvenido a Genetics!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                        finish()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("LOGIN_DEBUG", "❌ Exception verificando rol del usuario: ${e.message}", e)
            Log.w("LOGIN_DEBUG", "⚠️ Error de conexión, usando dashboard de usuario por defecto")
            Toast.makeText(this@LoginActivity, "¡Bienvenido a Genetics!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
            finish()
        }
    }
}
