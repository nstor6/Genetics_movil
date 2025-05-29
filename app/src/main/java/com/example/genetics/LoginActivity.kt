package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.LoginRequest
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val apiService = RetrofitClient.getApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Pre-llenar con datos de demo
        binding.editTextEmail.setText("admin@genetics.com")
        binding.editTextPassword.setText("admin123")

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
    }

    private fun login() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loading
        binding.buttonLogin.isEnabled = false
        binding.buttonLogin.text = "Entrando..."

        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Guardar token
                    RetrofitClient.saveToken(loginResponse.access)

                    // Ir al dashboard
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()

                    Toast.makeText(this@LoginActivity, "¡Bienvenido a Genetics!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Restaurar botón
                binding.buttonLogin.isEnabled = true
                binding.buttonLogin.text = "Entrar"
            }
        }
    }
}