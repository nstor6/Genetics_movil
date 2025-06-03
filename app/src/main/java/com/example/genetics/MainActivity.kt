package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.DashboardActivity
import com.example.genetics.Activitys.UserDashboardActivity
import com.example.genetics.api.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MAIN_ACTIVITY", "🚀 ===== INICIANDO GENETICS APP =====")
        Log.d("MAIN_ACTIVITY", "📱 Dispositivo: ${android.os.Build.MODEL}")
        Log.d("MAIN_ACTIVITY", "🤖 Android: ${android.os.Build.VERSION.RELEASE}")

        // Evitar múltiples inicializaciones
        if (isProcessing) {
            Log.w("MAIN_ACTIVITY", "⚠️ Ya se está procesando el inicio")
            return
        }
        isProcessing = true

        initializeApp()
    }

    /**
     * Inicializar toda la aplicación paso a paso
     */
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                Log.d("MAIN_ACTIVITY", "🔧 === PASO 1: Inicializando RetrofitClient ===")

                // Paso 1: Inicializar RetrofitClient
                val initSuccess = RetrofitClient.initialize(this@MainActivity)

                if (!initSuccess) {
                    Log.e("MAIN_ACTIVITY", "❌ Fallo crítico inicializando RetrofitClient")
                    showErrorDialog(
                        "Error de Configuración",
                        "No se pudo inicializar la conexión con el servidor.\n\n" +
                                "Posibles causas:\n" +
                                "• Sin conexión a Internet\n" +
                                "• Servidor no disponible\n" +
                                "• Configuración incorrecta\n\n" +
                                "¿Quieres intentar de nuevo?",
                        onRetry = { initializeApp() },
                        onCancel = { finish() }
                    )
                    return@launch
                }

                Log.d("MAIN_ACTIVITY", "✅ RetrofitClient inicializado correctamente")

                // Debug del estado
                RetrofitClient.debugStatus()

                // Pequeña pausa para que se complete la inicialización
                delay(500)

                Log.d("MAIN_ACTIVITY", "🔧 === PASO 2: Verificando estado del usuario ===")

                // Paso 2: Verificar estado del usuario
                checkUserAuthenticationStatus()

            } catch (e: Exception) {
                Log.e("MAIN_ACTIVITY", "💥 Error crítico durante inicialización: ${e.message}")
                e.printStackTrace()

                showErrorDialog(
                    "Error Crítico",
                    "Ocurrió un error inesperado:\n${e.message}\n\n¿Quieres reintentar?",
                    onRetry = { initializeApp() },
                    onCancel = { finish() }
                )
            }
        }
    }

    /**
     * Verificar estado de autenticación del usuario
     */
    private suspend fun checkUserAuthenticationStatus() {
        try {
            val hasToken = RetrofitClient.isLoggedIn()

            Log.d("MAIN_ACTIVITY", "🔐 Usuario tiene token guardado: $hasToken")

            if (hasToken) {
                Log.d("MAIN_ACTIVITY", "🔍 Verificando validez del token con el servidor...")
                verifyTokenWithServer()
            } else {
                Log.d("MAIN_ACTIVITY", "🔓 No hay token - Redirigiendo al login")
                goToLogin("Primera vez o sesión expirada")
            }

        } catch (e: Exception) {
            Log.e("MAIN_ACTIVITY", "❌ Error verificando autenticación: ${e.message}")
            e.printStackTrace()

            // En caso de error, ir al login por seguridad
            goToLogin("Error verificando sesión")
        }
    }

    /**
     * Verificar token con el servidor y obtener datos del usuario
     */
    private suspend fun verifyTokenWithServer() {
        try {
            Log.d("MAIN_ACTIVITY", "📡 Llamando a /api/auth/me/ para verificar token...")

            val apiService = RetrofitClient.getApiService()
            val response = apiService.getCurrentUser()

            Log.d("MAIN_ACTIVITY", "📊 Response code: ${response.code()}")
            Log.d("MAIN_ACTIVITY", "📊 Response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!

                Log.d("MAIN_ACTIVITY", "✅ === USUARIO AUTENTICADO CORRECTAMENTE ===")
                Log.d("MAIN_ACTIVITY", "👤 ID: ${user.id}")
                Log.d("MAIN_ACTIVITY", "👤 Nombre: ${user.nombre} ${user.apellidos}")
                Log.d("MAIN_ACTIVITY", "📧 Email: ${user.email}")
                Log.d("MAIN_ACTIVITY", "🏷️ Rol: '${user.rol}'")
                Log.d("MAIN_ACTIVITY", "👑 isStaff: ${user.isStaff}")
                Log.d("MAIN_ACTIVITY", "✅ Activo: ${user.activo}")

                // Determinar tipo de dashboard según rol
                redirectToAppropriiateDashboard(user)

            } else {
                val errorBody = response.errorBody()?.string()

                Log.w("MAIN_ACTIVITY", "❌ Token inválido o expirado")
                Log.w("MAIN_ACTIVITY", "📊 Response code: ${response.code()}")
                Log.w("MAIN_ACTIVITY", "📊 Error body: $errorBody")

                when (response.code()) {
                    401 -> {
                        Log.w("MAIN_ACTIVITY", "🚨 401 Unauthorized - Token expirado")
                        RetrofitClient.clearToken()
                        goToLogin("Tu sesión ha expirado")
                    }
                    403 -> {
                        Log.w("MAIN_ACTIVITY", "🚫 403 Forbidden - Sin permisos")
                        RetrofitClient.clearToken()
                        goToLogin("Sin permisos de acceso")
                    }
                    404 -> {
                        Log.e("MAIN_ACTIVITY", "🔍 404 - Endpoint /auth/me/ no encontrado")
                        // Probablemente problema del servidor, usar dashboard por defecto
                        Log.w("MAIN_ACTIVITY", "⚠️ Usando dashboard por defecto debido a error del servidor")
                        goToDefaultDashboard()
                    }
                    500 -> {
                        Log.e("MAIN_ACTIVITY", "💥 500 - Error interno del servidor")
                        goToDefaultDashboard()
                    }
                    else -> {
                        Log.w("MAIN_ACTIVITY", "⚠️ Error desconocido: ${response.code()}")
                        goToDefaultDashboard()
                    }
                }
            }

        } catch (e: java.net.UnknownHostException) {
            Log.e("MAIN_ACTIVITY", "🌐 Error: Servidor no encontrado")
            showConnectionError("Servidor no encontrado",
                "Verifica que el servidor esté ejecutándose y la URL sea correcta.")

        } catch (e: java.net.ConnectException) {
            Log.e("MAIN_ACTIVITY", "🔗 Error: No se puede conectar al servidor")
            showConnectionError("Sin conexión al servidor",
                "El servidor no responde. Verifica tu conexión a Internet.")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e("MAIN_ACTIVITY", "⏱️ Error: Timeout de conexión")
            showConnectionError("Conexión lenta",
                "El servidor tardó demasiado en responder.")

        } catch (e: Exception) {
            Log.e("MAIN_ACTIVITY", "💥 Error inesperado verificando token: ${e.message}")
            e.printStackTrace()

            // En caso de error de conexión, ir al dashboard por defecto
            Log.w("MAIN_ACTIVITY", "⚠️ Error de red - Usando dashboard por defecto")
            goToDefaultDashboard()
        }
    }

    /**
     * Redirigir al dashboard apropiado según el rol del usuario
     */
    private fun redirectToAppropriiateDashboard(user: com.example.genetics.api.Usuario) {
        try {
            // Lógica de roles mejorada
            val isAdmin = user.rol?.trim()?.lowercase() == "admin" || user.isStaff == true

            if (isAdmin) {
                Log.d("MAIN_ACTIVITY", "👑 === ACCESO ADMINISTRADOR ===")
                Log.d("MAIN_ACTIVITY", "🎯 Abriendo DashboardActivity (Admin)")

                Toast.makeText(this, "¡Bienvenido, Admin ${user.nombre}! 👑", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("USER_NAME", "${user.nombre} ${user.apellidos}")
                intent.putExtra("USER_ROLE", user.rol)
                startActivity(intent)

            } else {
                Log.d("MAIN_ACTIVITY", "👤 === ACCESO USUARIO NORMAL ===")
                Log.d("MAIN_ACTIVITY", "🎯 Abriendo UserDashboardActivity")

                Toast.makeText(this, "¡Bienvenido, ${user.nombre}! 👤", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, UserDashboardActivity::class.java)
                intent.putExtra("USER_NAME", "${user.nombre} ${user.apellidos}")
                intent.putExtra("USER_ROLE", user.rol)
                startActivity(intent)
            }

            finish()

        } catch (e: Exception) {
            Log.e("MAIN_ACTIVITY", "❌ Error redirigiendo al dashboard: ${e.message}")
            e.printStackTrace()
            goToDefaultDashboard()
        }
    }

    /**
     * Ir al dashboard por defecto (usuario normal)
     */
    private fun goToDefaultDashboard() {
        Log.d("MAIN_ACTIVITY", "🎯 Usando dashboard por defecto (UserDashboard)")
        Toast.makeText(this, "¡Bienvenido a Genetics!", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, UserDashboardActivity::class.java))
        finish()
    }

    /**
     * Ir al login con mensaje opcional
     */
    private fun goToLogin(reason: String? = null) {
        Log.d("MAIN_ACTIVITY", "🔄 Redirigiendo al login")

        if (reason != null) {
            Log.d("MAIN_ACTIVITY", "📝 Razón: $reason")
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
        }

        val intent = Intent(this, LoginActivity::class.java)
        if (reason != null) {
            intent.putExtra("MESSAGE", reason)
        }

        startActivity(intent)
        finish()
    }

    /**
     * Mostrar diálogo de error de conexión
     */
    private fun showConnectionError(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🌐 $title")
            .setMessage("$message\n\n¿Qué quieres hacer?")
            .setPositiveButton("🔄 Reintentar") { _, _ ->
                isProcessing = false
                initializeApp()
            }
            .setNeutralButton("🔧 Configuración") { _, _ ->
                // Aquí podrías abrir una pantalla de configuración
                Toast.makeText(this, "Configuración - Próximamente", Toast.LENGTH_SHORT).show()
                goToLogin()
            }
            .setNegativeButton("🚪 Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Mostrar diálogo de error genérico
     */
    private fun showErrorDialog(
        title: String,
        message: String,
        onRetry: () -> Unit,
        onCancel: () -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ $title")
            .setMessage(message)
            .setPositiveButton("🔄 Reintentar") { _, _ ->
                isProcessing = false
                onRetry()
            }
            .setNegativeButton("❌ Cancelar") { _, _ ->
                onCancel()
            }
            .setCancelable(false)
            .show()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MAIN_ACTIVITY", "🔄 onNewIntent llamado")

        // Si llegamos aquí desde un intent externo y ya estamos procesando, no hacer nada
        if (!isProcessing) {
            isProcessing = false
            initializeApp()
        }
    }

    override fun onBackPressed() {
        // En MainActivity, el botón atrás debe cerrar la app
        Log.d("MAIN_ACTIVITY", "🔙 Back presionado - Cerrando app")
        super.onBackPressed()
        finishAffinity() // Cerrar toda la aplicación
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MAIN_ACTIVITY", "🗑️ MainActivity destruida")
        isProcessing = false
    }
}