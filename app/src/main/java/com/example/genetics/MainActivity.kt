package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.genetics.api.RetrofitClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔧 CRÍTICO: Inicializar RetrofitClient ANTES de cualquier uso
        try {
            RetrofitClient.initialize(this)
            Log.d("MAIN_ACTIVITY", "✅ RetrofitClient inicializado correctamente")
        } catch (e: Exception) {
            Log.e("MAIN_ACTIVITY", "❌ Error inicializando RetrofitClient: ${e.message}")
            e.printStackTrace()
            // En caso de error, redirigir al login por seguridad
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Verificar si el usuario ya está logueado
        try {
            if (RetrofitClient.isLoggedIn()) {
                Log.d("MAIN_ACTIVITY", "🔑 Usuario ya logueado, redirigiendo al dashboard")
                // Ir al dashboard principal
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Log.d("MAIN_ACTIVITY", "🔓 Usuario no logueado, redirigiendo al login")
                // Ir al login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            Log.e("MAIN_ACTIVITY", "❌ Error verificando estado de login: ${e.message}")
            e.printStackTrace()
            // En caso de error, ir al login por seguridad
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}