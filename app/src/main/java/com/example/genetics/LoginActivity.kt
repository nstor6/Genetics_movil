package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.DashboardActivity
import com.example.genetics.Activitys.UserDashboardActivity
import com.example.genetics.api.LoginRequest
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var apiService: com.example.genetics.api.ApiService
    private var isLoggingIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("LOGIN_ACTIVITY", "🚀 ===== LOGIN ACTIVITY INICIADO =====")

        // Verificar si viene un mensaje desde MainActivity
        val message = intent.getStringExtra("MESSAGE")
        if (message != null) {
            Log.d("LOGIN_ACTIVITY", "📝 Mensaje recibido: $message")
        }

        initializeLogin()
    }

    /**
     * Inicializar LoginActivity
     */
    private fun initializeLogin() {
        try {
            Log.d("LOGIN_ACTIVITY", "🔧 Inicializando LoginActivity...")

            // Verificar que RetrofitClient esté inicializado
            if (!RetrofitClient.isLoggedIn()) {
                Log.d("LOGIN_ACTIVITY", "✅ RetrofitClient listo para login")
            }

            // Inicializar API Service
            apiService = RetrofitClient.getApiService()
            Log.d("LOGIN_ACTIVITY", "✅ ApiService inicializado")

            setupUI()

            // Mostrar mensaje si viene de MainActivity
            val message = intent.getStringExtra("MESSAGE")
            if (message != null) {
                showMessage(message)
            }

            Log.d("LOGIN_ACTIVITY", "✅ LoginActivity listo")

        } catch (e: Exception) {
            Log.e("LOGIN_ACTIVITY", "❌ Error crítico inicializando LoginActivity: ${e.message}")
            e.printStackTrace()

            showCriticalError(
                "Error de Configuración",
                "No se pudo inicializar el login.\n\nError: ${e.message}",
                onRetry = { initializeLogin() },
                onExit = { finish() }
            )
        }
    }

    /**
     * Configurar interfaz de usuario
     */
    private fun setupUI() {
        Log.d("LOGIN_ACTIVITY", "🎨 Configurando UI...")

        // Configurar título
        supportActionBar?.title = "Genetics - Iniciar Sesión"

        // Botón de login
        binding.buttonLogin.setOnClickListener {
            if (!isLoggingIn) {
                performLogin()
            } else {
                Log.w("LOGIN_ACTIVITY", "⚠️ Login ya en progreso")
            }
        }

        // Botón demo (para desarrollo)
        binding.buttonDemo?.setOnClickListener {
            fillDemoCredentials()
        }

        // Enter en password para hacer login
        binding.editTextPassword.setOnEditorActionListener { _, _, _ ->
            if (!isLoggingIn) {
                performLogin()
            }
            true
        }

        Log.d("LOGIN_ACTIVITY", "✅ UI configurada")
    }

    /**
     * Llenar credenciales demo
     */
    private fun fillDemoCredentials() {
        Log.d("LOGIN_ACTIVITY", "🎭 Llenando credenciales demo")

        binding.editTextEmail.setText("admin@genetics.com")
        binding.editTextPassword.setText("admin123")

        Toast.makeText(this, "📝 Credenciales demo cargadas", Toast.LENGTH_SHORT).show()

        // Opcional: hacer login automático después de un momento
        lifecycleScope.launch {
            delay(1000)
            if (!isLoggingIn) {
                performLogin()
            }
        }
    }

    /**
     * Realizar proceso de login
     */
    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        Log.d("LOGIN_ACTIVITY", "🔐 === INICIANDO PROCESO DE LOGIN ===")
        Log.d("LOGIN_ACTIVITY", "📧 Email: $email")
        Log.d("LOGIN_ACTIVITY", "🔑 Password: ${if (password.isNotEmpty()) "[PRESENTE]" else "[VACÍO]"}")

        // Validaciones básicas
        if (!validateInput(email, password)) {
            return
        }

        // Evitar doble login
        if (isLoggingIn) {
            Log.w("LOGIN_ACTIVITY", "⚠️ Login ya en progreso, ignorando")
            return
        }

        // Iniciar proceso de login
        startLoginProcess(email, password)
    }

    /**
     * Validar entrada del usuario
     */
    private fun validateInput(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.editTextEmail.error = "El email es obligatorio"
                binding.editTextEmail.requestFocus()
                Toast.makeText(this, "⚠️ Por favor introduce tu email", Toast.LENGTH_SHORT).show()
                return false
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.editTextEmail.error = "Email no válido"
                binding.editTextEmail.requestFocus()
                Toast.makeText(this, "⚠️ Formato de email incorrecto", Toast.LENGTH_SHORT).show()
                return false
            }

            password.isEmpty() -> {
                binding.editTextPassword.error = "La contraseña es obligatoria"
                binding.editTextPassword.requestFocus()
                Toast.makeText(this, "⚠️ Por favor introduce tu contraseña", Toast.LENGTH_SHORT).show()
                return false
            }

            password.length < 3 -> {
                binding.editTextPassword.error = "Contraseña muy corta"
                binding.editTextPassword.requestFocus()
                Toast.makeText(this, "⚠️ La contraseña debe tener al menos 3 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        // Limpiar errores
        binding.editTextEmail.error = null
        binding.editTextPassword.error = null

        return true
    }

    /**
     * Iniciar proceso de login con estado visual
     */
    private fun startLoginProcess(email: String, password: String) {
        isLoggingIn = true
        setLoginUIState(true)

        lifecycleScope.launch {
            try {
                Log.d("LOGIN_ACTIVITY", "📡 Enviando credenciales al servidor...")

                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                Log.d("LOGIN_ACTIVITY", "📊 Response recibida:")
                Log.d("LOGIN_ACTIVITY", "   - Code: ${response.code()}")
                Log.d("LOGIN_ACTIVITY", "   - Successful: ${response.isSuccessful}")
                Log.d("LOGIN_ACTIVITY", "   - Message: ${response.message()}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    Log.d("LOGIN_ACTIVITY", "✅ === LOGIN EXITOSO ===")
                    Log.d("LOGIN_ACTIVITY", "🎟️ Token recibido: ${loginResponse.access.take(30)}...")

                    // Guardar token
                    RetrofitClient.saveToken(loginResponse.access)

                    // Mostrar éxito temporalmente
                    showLoginSuccess()

                    // Verificar usuario y redirigir
                    delay(1000) // Pausa para mostrar éxito
                    verifyUserAndRedirect()

                } else {
                    handleLoginError(response.code(), response.errorBody()?.string())
                }

            } catch (e: java.net.UnknownHostException) {
                Log.e("LOGIN_ACTIVITY", "🌐 Error: Host no encontrado")
                handleNetworkError("Servidor no encontrado",
                    "Verifica que la URL del servidor sea correcta y que esté ejecutándose.")

            } catch (e: java.net.ConnectException) {
                Log.e("LOGIN_ACTIVITY", "🔗 Error: Conexión rechazada")
                handleNetworkError("No se puede conectar",
                    "El servidor no responde. Verifica tu conexión a Internet.")

            } catch (e: java.net.SocketTimeoutException) {
                Log.e("LOGIN_ACTIVITY", "⏱️ Error: Timeout")
                handleNetworkError("Conexión lenta",
                    "El servidor tardó demasiado en responder.")

            } catch (e: Exception) {
                Log.e("LOGIN_ACTIVITY", "💥 Error inesperado: ${e.message}")
                e.printStackTrace()
                handleUnexpectedError(e)

            } finally {
                isLoggingIn = false
                setLoginUIState(false)
            }
        }
    }

    /**
     * Manejar errores de login del servidor
     */
    private fun handleLoginError(code: Int, errorBody: String?) {
        Log.e("LOGIN_ACTIVITY", "❌ Error de login:")
        Log.e("LOGIN_ACTIVITY", "   - Código: $code")
        Log.e("LOGIN_ACTIVITY", "   - Body: $errorBody")

        val (title, message) = when (code) {
            400 -> "Datos incorrectos" to "Verifica que tu email y contraseña sean correctos."
            401 -> "Credenciales inválidas" to "El email o la contraseña son incorrectos."
            403 -> "Acceso denegado" to "Tu cuenta puede estar desactivada. Contacta al administrador."
            404 -> "Servicio no encontrado" to "El servidor no está configurado correctamente."
            500 -> "Error del servidor" to "Hay un problema en el servidor. Inténtalo más tarde."
            503 -> "Servicio no disponible" to "El servidor está temporalmente fuera de servicio."
            else -> "Error desconocido" to "Ocurrió un error inesperado (código: $code)."
        }

        runOnUiThread {
            showErrorMessage(title, message)
        }
    }

    /**
     * Manejar errores de red
     */
    private fun handleNetworkError(title: String, message: String) {
        runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🌐 $title")
                .setMessage("$message\n\n¿Qué quieres hacer?")
                .setPositiveButton("🔄 Reintentar") { _, _ ->
                    // Reintentar con las mismas credenciales
                    val email = binding.editTextEmail.text.toString().trim()
                    val password = binding.editTextPassword.text.toString().trim()
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        startLoginProcess(email, password)
                    }
                }
                .setNeutralButton("🔧 Debug") { _, _ ->
                    showDebugInfo()
                }
                .setNegativeButton("❌ Cancelar", null)
                .show()
        }
    }

    /**
     * Manejar errores inesperados
     */
    private fun handleUnexpectedError(e: Exception) {
        runOnUiThread {
            showErrorMessage(
                "Error Inesperado",
                "Ocurrió un error no controlado:\n\n${e.message}\n\nSi el problema persiste, contacta al soporte técnico."
            )
        }
    }

    /**
     * Mostrar éxito de login
     */
    private fun showLoginSuccess() {
        runOnUiThread {
            binding.buttonLogin.text = "✅ Login Exitoso"
            binding.buttonLogin.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            Toast.makeText(this, "✅ ¡Login exitoso!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Verificar usuario y redirigir al dashboard apropiado
     */
    private suspend fun verifyUserAndRedirect() {
        try {
            Log.d("LOGIN_ACTIVITY", "🔍 === VERIFICANDO USUARIO ===")

            val userResponse = apiService.getCurrentUser()

            if (userResponse.isSuccessful && userResponse.body() != null) {
                val usuario = userResponse.body()!!

                Log.d("LOGIN_ACTIVITY", "✅ Usuario verificado:")
                Log.d("LOGIN_ACTIVITY", "   - ID: ${usuario.id}")
                Log.d("LOGIN_ACTIVITY", "   - Nombre: ${usuario.nombre} ${usuario.apellidos}")
                Log.d("LOGIN_ACTIVITY", "   - Email: ${usuario.email}")
                Log.d("LOGIN_ACTIVITY", "   - Rol: '${usuario.rol}'")
                Log.d("LOGIN_ACTIVITY", "   - isStaff: ${usuario.isStaff}")

                // Determinar dashboard según rol
                redirectToDashboard(usuario)

            } else {
                Log.e("LOGIN_ACTIVITY", "❌ Error verificando usuario después del login")
                Log.e("LOGIN_ACTIVITY", "   - Code: ${userResponse.code()}")
                Log.e("LOGIN_ACTIVITY", "   - Error: ${userResponse.errorBody()?.string()}")

                // Si no puede verificar el usuario, ir al dashboard por defecto
                redirectToDefaultDashboard("No se pudo verificar el usuario")
            }

        } catch (e: Exception) {
            Log.e("LOGIN_ACTIVITY", "❌ Error verificando usuario: ${e.message}")
            e.printStackTrace()
            redirectToDefaultDashboard("Error de verificación")
        }
    }

    /**
     * Redirigir al dashboard según el rol del usuario
     */
    private fun redirectToDashboard(usuario: com.example.genetics.api.Usuario) {
        try {
            val isAdmin = usuario.rol?.trim()?.lowercase() == "admin" || usuario.isStaff == true

            if (isAdmin) {
                Log.d("LOGIN_ACTIVITY", "👑 Redirigiendo a Dashboard Admin")

                runOnUiThread {
                    Toast.makeText(this, "¡Bienvenido, Admin ${usuario.nombre}! 👑", Toast.LENGTH_LONG).show()
                }

                val intent = Intent(this, DashboardActivity::class.java).apply {
                    putExtra("USER_NAME", "${usuario.nombre} ${usuario.apellidos}")
                    putExtra("USER_ROLE", usuario.rol)
                    putExtra("FROM_LOGIN", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)

            } else {
                Log.d("LOGIN_ACTIVITY", "👤 Redirigiendo a Dashboard Usuario")

                runOnUiThread {
                    Toast.makeText(this, "¡Bienvenido, ${usuario.nombre}! 👤", Toast.LENGTH_LONG).show()
                }

                val intent = Intent(this, UserDashboardActivity::class.java).apply {
                    putExtra("USER_NAME", "${usuario.nombre} ${usuario.apellidos}")
                    putExtra("USER_ROLE", usuario.rol)
                    putExtra("FROM_LOGIN", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }

            finish()

        } catch (e: Exception) {
            Log.e("LOGIN_ACTIVITY", "❌ Error redirigiendo: ${e.message}")
            e.printStackTrace()
            redirectToDefaultDashboard("Error de redirección")
        }
    }

    /**
     * Redirigir al dashboard por defecto
     */
    private fun redirectToDefaultDashboard(reason: String) {
        Log.w("LOGIN_ACTIVITY", "⚠️ Usando dashboard por defecto: $reason")

        runOnUiThread {
            Toast.makeText(this, "¡Bienvenido a Genetics!", Toast.LENGTH_SHORT).show()
        }

        val intent = Intent(this, UserDashboardActivity::class.java).apply {
            putExtra("FROM_LOGIN", true)
            putExtra("DEFAULT_REASON", reason)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Cambiar estado visual de la UI durante login
     */
    private fun setLoginUIState(isLogging: Boolean) {
        runOnUiThread {
            if (isLogging) {
                binding.buttonLogin.isEnabled = false
                binding.buttonLogin.text = "🔄 Iniciando sesión..."
                binding.editTextEmail.isEnabled = false
                binding.editTextPassword.isEnabled = false
                binding.buttonDemo?.isEnabled = false
            } else {
                binding.buttonLogin.isEnabled = true
                binding.buttonLogin.text = "🔐 Iniciar Sesión"
                binding.buttonLogin.setBackgroundColor(getColor(R.color.colorPrimary))
                binding.editTextEmail.isEnabled = true
                binding.editTextPassword.isEnabled = true
                binding.buttonDemo?.isEnabled = true
            }
        }
    }

    /**
     * Mostrar mensaje informativo
     */
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Mostrar mensaje de error
     */
    private fun showErrorMessage(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("❌ $title")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Enfocar en el campo de email para intentar de nuevo
                binding.editTextEmail.requestFocus()
            }
            .show()
    }

    /**
     * Mostrar información de debug
     */
    private fun showDebugInfo() {
        val debugInfo = buildString {
            append("🔧 Información de Debug\n\n")
            append("📱 Dispositivo: ${android.os.Build.MODEL}\n")
            append("🤖 Android: ${android.os.Build.VERSION.RELEASE}\n")
            append("🌐 URL Base: ${RetrofitClient.debugStatus()}\n")
            append("🔑 Tiene token: ${RetrofitClient.isLoggedIn()}\n")
            append("📡 RetrofitClient inicializado: ${try { apiService; true } catch (e: Exception) { false }}")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔧 Debug Info")
            .setMessage(debugInfo)
            .setPositiveButton("Copiar") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Debug Info", debugInfo)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "📋 Info copiada al portapapeles", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /**
     * Mostrar error crítico
     */
    private fun showCriticalError(
        title: String,
        message: String,
        onRetry: () -> Unit,
        onExit: () -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("💥 $title")
            .setMessage(message)
            .setPositiveButton("🔄 Reintentar") { _, _ -> onRetry() }
            .setNegativeButton("🚪 Salir") { _, _ -> onExit() }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        if (isLoggingIn) {
            Log.d("LOGIN_ACTIVITY", "⚠️ Bloqueando back durante login")
            Toast.makeText(this, "⏳ Espera a que termine el login", Toast.LENGTH_SHORT).show()
            return
        }

        super.onBackPressed()
        finishAffinity() // Cerrar toda la aplicación
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LOGIN_ACTIVITY", "🗑️ LoginActivity destruida")
        isLoggingIn = false
    }
}