package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.DashboardActivity
import com.example.genetics.Activitys.UserDashboardActivity
import com.example.genetics.api.RetrofitClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MAIN_ACTIVITY", "🚀 MainActivity iniciado")

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
                Log.d("MAIN_ACTIVITY", "🔑 Usuario ya logueado, verificando rol...")
                // ✅ NUEVA LÓGICA: Verificar rol y redirigir al dashboard correspondiente
                verificarRolYRedirigir()
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

    // ✅ FUNCIÓN SIMPLIFICADA: DashboardActivity para admins, UserDashboardActivity para usuarios
    private fun verificarRolYRedirigir() {
        lifecycleScope.launch {
            try {
                Log.d("MAIN_ACTIVITY", "🔍 Verificando rol del usuario...")

                val apiService = RetrofitClient.getApiService()
                val response = apiService.getCurrentUser()

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    Log.d("MAIN_ACTIVITY", "✅ Usuario obtenido:")
                    Log.d("MAIN_ACTIVITY", "   - Nombre: ${user.nombre}")
                    Log.d("MAIN_ACTIVITY", "   - Email: ${user.email}")
                    Log.d("MAIN_ACTIVITY", "   - Rol: '${user.rol}'")
                    Log.d("MAIN_ACTIVITY", "   - isStaff: ${user.isStaff}")

                    // ✅ LÓGICA SIMPLIFICADA DE REDIRECCIÓN
                    val isAdmin = user.rol == "admin" || user.isStaff == true

                    if (isAdmin) {
                        Log.d("MAIN_ACTIVITY", "👑 Usuario es ADMINISTRADOR - Abriendo DashboardActivity (original)")
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                    } else {
                        Log.d("MAIN_ACTIVITY", "👤 Usuario es NORMAL - Abriendo UserDashboardActivity")
                        startActivity(Intent(this@MainActivity, UserDashboardActivity::class.java))
                    }

                    finish()

                } else {
                    Log.e("MAIN_ACTIVITY", "❌ Error obteniendo usuario: ${response.code()}")
                    Log.e("MAIN_ACTIVITY", "   - Error body: ${response.errorBody()?.string()}")

                    // Si no puede verificar el usuario, limpiar token y ir al login
                    Log.w("MAIN_ACTIVITY", "🔄 Limpiando token y redirigiendo al login")
                    RetrofitClient.clearToken()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }

            } catch (e: Exception) {
                Log.e("MAIN_ACTIVITY", "❌ Exception verificando rol: ${e.message}", e)

                // En caso de error de conexión, ir al dashboard de usuario por defecto
                Log.w("MAIN_ACTIVITY", "⚠️ Error de conexión, abriendo dashboard de usuario por defecto")
                startActivity(Intent(this@MainActivity, UserDashboardActivity::class.java))
                finish()
            }
        }
    }
}