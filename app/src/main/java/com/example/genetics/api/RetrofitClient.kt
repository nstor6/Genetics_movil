package com.example.genetics.api

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.genetics.LoginActivity
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 🔧 ACTUALIZA ESTA URL SEGÚN TU CONFIGURACIÓN
    // Para emulador Android Studio:
    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    // Para dispositivo físico (cambia XXX por tu IP):
    // private const val BASE_URL = "http://192.168.1.XXX:8000/api/"

    // Para ngrok (actualiza con tu URL actual):
    // private const val BASE_URL = "https://51b6-81-42-254-142.ngrok-free.app/api/"

    // Para servidor en la nube:
    // private const val BASE_URL = "https://tu-dominio.com/api/"

    private const val WS_BASE_URL = "ws://10.0.2.2:8000/ws/"

    // Variables globales
    private var retrofit: Retrofit? = null
    private var sharedPreferences: SharedPreferences? = null
    private var appContext: Context? = null
    private var okHttpClient: OkHttpClient? = null

    // WebSocket connections
    private var notificationsWebSocket: WebSocket? = null
    private var animalsWebSocket: WebSocket? = null
    private var logsWebSocket: WebSocket? = null

    /**
     * Inicializar RetrofitClient con validación completa
     */
    fun initialize(context: Context): Boolean {
        return try {
            appContext = context.applicationContext
            sharedPreferences = appContext!!.getSharedPreferences("genetics_prefs", Context.MODE_PRIVATE)

            Log.d("RETROFIT_CLIENT", "🚀 Inicializando RetrofitClient...")
            Log.d("RETROFIT_CLIENT", "📡 URL Base: $BASE_URL")
            Log.d("RETROFIT_CLIENT", "📱 Dispositivo: ${Build.MODEL}")
            Log.d("RETROFIT_CLIENT", "🌐 Android: ${Build.VERSION.RELEASE}")

            // Verificar conectividad
            if (!isNetworkAvailable()) {
                Log.e("RETROFIT_CLIENT", "❌ Sin conexión a Internet")
                return false
            }

            Log.d("RETROFIT_CLIENT", "✅ Conectividad verificada")
            Log.d("RETROFIT_CLIENT", "✅ RetrofitClient inicializado correctamente")
            true

        } catch (e: Exception) {
            Log.e("RETROFIT_CLIENT", "❌ Error crítico inicializando: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Verificar si está inicializado
     */
    private fun isInitialized(): Boolean {
        val initialized = sharedPreferences != null && appContext != null
        if (!initialized) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient NO está inicializado")
        }
        return initialized
    }

    /**
     * Verificar conectividad de red
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = appContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                Log.d("RETROFIT_CLIENT", "🌐 Conectividad (API ≥23): $hasInternet")
                hasInternet
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                val connected = networkInfo?.isConnected == true

                Log.d("RETROFIT_CLIENT", "🌐 Conectividad (API <23): $connected")
                connected
            }
        } catch (e: Exception) {
            Log.e("RETROFIT_CLIENT", "❌ Error verificando conectividad: ${e.message}")
            false
        }
    }

    /**
     * Asegurar que esté inicializado
     */
    private fun ensureInitialized() {
        if (!isInitialized()) {
            throw IllegalStateException(
                "RetrofitClient no ha sido inicializado. " +
                        "Llama a RetrofitClient.initialize(context) en Application.onCreate() o MainActivity.onCreate()"
            )
        }
    }

    /**
     * Crear cliente HTTP con interceptores mejorados
     */
    private fun getOkHttpClient(): OkHttpClient {
        ensureInitialized()

        if (okHttpClient == null) {
            Log.d("RETROFIT_CLIENT", "🔧 Creando OkHttpClient...")

            // Logging interceptor
            val logging = HttpLoggingInterceptor { message ->
                // Evitar logs demasiado largos
                if (message.length > 1000) {
                    Log.d("HTTP_LOG", "${message.take(500)}... [TRUNCADO] ...${message.takeLast(100)}")
                } else {
                    Log.d("HTTP_LOG", message)
                }
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Interceptor de autenticación mejorado
            val authInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val url = originalRequest.url.toString()

                Log.d("AUTH_INTERCEPTOR", "📡 Petición a: ${originalRequest.method} $url")

                // Endpoints públicos (no requieren token)
                val isPublicEndpoint = url.contains("/auth/login") ||
                        url.contains("/auth/register") ||
                        url.contains("/auth/refresh") ||
                        url.contains("/health") ||
                        url.contains("/cors-test") ||
                        url.contains("/status")

                val requestBuilder = originalRequest.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "GeneticsApp/1.0 Android/${Build.VERSION.RELEASE}")

                // Añadir token si es necesario
                if (!isPublicEndpoint) {
                    val token = getToken()

                    if (token != null) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                        Log.d("AUTH_INTERCEPTOR", "✅ Token añadido (${token.take(20)}...)")
                    } else {
                        Log.w("AUTH_INTERCEPTOR", "⚠️ No hay token para endpoint protegido")
                    }
                } else {
                    Log.d("AUTH_INTERCEPTOR", "🔓 Endpoint público - sin token")
                }

                val request = requestBuilder.build()
                val startTime = System.currentTimeMillis()

                try {
                    val response = chain.proceed(request)
                    val endTime = System.currentTimeMillis()

                    Log.d("HTTP_RESPONSE", "📊 ${response.code} ${response.message} (${endTime - startTime}ms)")

                    // Manejo específico de errores
                    when (response.code) {
                        401 -> {
                            if (!isPublicEndpoint) {
                                Log.w("AUTH_INTERCEPTOR", "🚨 401 Unauthorized - Token inválido")
                                val responseBody = response.peekBody(1024).string()
                                Log.w("AUTH_INTERCEPTOR", "Respuesta: $responseBody")

                                if (responseBody.contains("token", ignoreCase = true) ||
                                    responseBody.contains("authentication", ignoreCase = true) ||
                                    responseBody.contains("credentials", ignoreCase = true)) {

                                    Log.w("AUTH_INTERCEPTOR", "🗑️ Limpiando token inválido")
                                    clearToken()
                                    redirectToLogin()
                                }
                            }
                        }
                        403 -> {
                            Log.w("AUTH_INTERCEPTOR", "🚫 403 Forbidden - Sin permisos")
                        }
                        404 -> {
                            Log.w("AUTH_INTERCEPTOR", "🔍 404 Not Found - Endpoint no existe: $url")
                        }
                        500 -> {
                            Log.e("AUTH_INTERCEPTOR", "💥 500 Server Error")
                        }
                    }

                    response

                } catch (e: Exception) {
                    Log.e("AUTH_INTERCEPTOR", "💥 Error en petición: ${e.message}")
                    throw e
                }
            }

            okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            Log.d("RETROFIT_CLIENT", "✅ OkHttpClient creado")
        }

        return okHttpClient!!
    }

    /**
     * Redirigir al login
     */
    private fun redirectToLogin() {
        try {
            appContext?.let { context ->
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("SESSION_EXPIRED", true)
                }
                context.startActivity(intent)
                Log.i("AUTH_INTERCEPTOR", "🔄 Redirigiendo al login por sesión expirada")
            } ?: run {
                Log.e("AUTH_INTERCEPTOR", "❌ AppContext es null, no se puede redirigir")
            }
        } catch (e: Exception) {
            Log.e("AUTH_INTERCEPTOR", "❌ Error redirigiendo al login: ${e.message}")
        }
    }

    /**
     * Obtener instancia del API Service
     */
    fun getApiService(): ApiService {
        ensureInitialized()

        if (retrofit == null) {
            Log.d("RETROFIT_CLIENT", "🔧 Creando instancia de Retrofit...")

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            Log.d("RETROFIT_CLIENT", "✅ Retrofit creado exitosamente")
        }

        return retrofit!!.create(ApiService::class.java)
    }

    // ========== WEBSOCKETS ==========

    /**
     * Conectar WebSocket de notificaciones
     */
    fun connectNotificationsWebSocket(listener: WebSocketListener) {
        ensureInitialized()
        val token = getToken()

        if (token == null) {
            Log.w("WEBSOCKET", "⚠️ No hay token para WebSocket de notificaciones")
            return
        }

        try {
            val url = "${WS_BASE_URL}notificaciones/?token=$token"
            val request = Request.Builder().url(url).build()

            notificationsWebSocket = getOkHttpClient().newWebSocket(request, listener)
            Log.d("WEBSOCKET", "🔌 Conectando WebSocket notificaciones: $url")
        } catch (e: Exception) {
            Log.e("WEBSOCKET", "❌ Error conectando WebSocket notificaciones: ${e.message}")
        }
    }

    fun disconnectNotificationsWebSocket() {
        notificationsWebSocket?.close(1000, "Desconexión normal")
        notificationsWebSocket = null
        Log.d("WEBSOCKET", "🔌 WebSocket notificaciones desconectado")
    }

    fun sendNotificationMessage(message: String): Boolean {
        return try {
            notificationsWebSocket?.send(message) ?: false
        } catch (e: Exception) {
            Log.e("WEBSOCKET", "❌ Error enviando mensaje: ${e.message}")
            false
        }
    }

    // ========== TOKEN MANAGEMENT ==========

    /**
     * Guardar token JWT
     */
    fun saveToken(token: String) {
        ensureInitialized()

        Log.d("RETROFIT_CLIENT", "💾 Guardando token: ${token.take(20)}...")
        sharedPreferences!!.edit()
            .putString("jwt_token", token)
            .putLong("token_timestamp", System.currentTimeMillis())
            .apply()
    }

    /**
     * Obtener token JWT
     */
    fun getToken(): String? {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient no inicializado para obtener token")
            return null
        }

        val token = sharedPreferences!!.getString("jwt_token", null)
        val timestamp = sharedPreferences!!.getLong("token_timestamp", 0)

        if (token != null) {
            val age = (System.currentTimeMillis() - timestamp) / 1000 / 60 // minutos
            Log.d("RETROFIT_CLIENT", "🔑 Token encontrado (edad: ${age}min)")
            return token
        } else {
            Log.d("RETROFIT_CLIENT", "🔑 No hay token guardado")
            return null
        }
    }

    /**
     * Limpiar token JWT
     */
    fun clearToken() {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient no inicializado para limpiar token")
            return
        }

        Log.d("RETROFIT_CLIENT", "🗑️ Limpiando token y datos de sesión...")

        sharedPreferences!!.edit()
            .remove("jwt_token")
            .remove("token_timestamp")
            .apply()

        // Limpiar instancias
        retrofit = null
        okHttpClient = null

        // Desconectar WebSockets
        disconnectAllWebSockets()
    }

    /**
     * Verificar si el usuario está logueado
     */
    fun isLoggedIn(): Boolean {
        val hasToken = getToken() != null
        Log.d("RETROFIT_CLIENT", "🔐 Usuario logueado: $hasToken")
        return hasToken
    }

    /**
     * Desconectar todos los WebSockets
     */
    fun disconnectAllWebSockets() {
        disconnectNotificationsWebSocket()
        // Añadir otros WebSockets cuando los implementes
        Log.d("WEBSOCKET", "🔌 Todos los WebSockets desconectados")
    }

    /**
     * Debug completo del estado
     */
    fun debugStatus() {
        Log.d("RETROFIT_CLIENT", "=== DEBUG RETROFIT CLIENT ===")
        Log.d("RETROFIT_CLIENT", "Inicializado: ${isInitialized()}")
        Log.d("RETROFIT_CLIENT", "URL Base: $BASE_URL")
        Log.d("RETROFIT_CLIENT", "Tiene token: ${isLoggedIn()}")
        Log.d("RETROFIT_CLIENT", "Conectividad: ${isNetworkAvailable()}")
        Log.d("RETROFIT_CLIENT", "Retrofit creado: ${retrofit != null}")
        Log.d("RETROFIT_CLIENT", "OkHttp creado: ${okHttpClient != null}")
        Log.d("RETROFIT_CLIENT", "=============================")
    }

    // ========== WEBSOCKETS ADICIONALES (AGREGAR AL FINAL DEL ARCHIVO) ==========

    /**
     * Conectar WebSocket de animales
     */
    fun connectAnimalsWebSocket(listener: WebSocketListener) {
        ensureInitialized()
        val token = getToken()

        if (token == null) {
            Log.w("WEBSOCKET", "⚠️ No hay token para WebSocket de animales")
            return
        }

        try {
            val url = "${WS_BASE_URL}animales/?token=$token"
            val request = Request.Builder().url(url).build()

            animalsWebSocket = getOkHttpClient().newWebSocket(request, listener)
            Log.d("WEBSOCKET", "🔌 Conectando WebSocket animales: $url")
        } catch (e: Exception) {
            Log.e("WEBSOCKET", "❌ Error conectando WebSocket animales: ${e.message}")
        }
    }

    /**
     * Conectar WebSocket de logs (solo para admins)
     */
    fun connectLogsWebSocket(listener: WebSocketListener) {
        ensureInitialized()
        val token = getToken()

        if (token == null) {
            Log.w("WEBSOCKET", "⚠️ No hay token para WebSocket de logs")
            return
        }

        try {
            val url = "${WS_BASE_URL}logs/?token=$token"
            val request = Request.Builder().url(url).build()

            logsWebSocket = getOkHttpClient().newWebSocket(request, listener)
            Log.d("WEBSOCKET", "🔌 Conectando WebSocket logs: $url")
        } catch (e: Exception) {
            Log.e("WEBSOCKET", "❌ Error conectando WebSocket logs: ${e.message}")
        }
    }

    /**
     * Desconectar WebSocket de animales
     */
    fun disconnectAnimalsWebSocket() {
        animalsWebSocket?.close(1000, "Desconexión normal")
        animalsWebSocket = null
        Log.d("WEBSOCKET", "🔌 WebSocket animales desconectado")
    }

    /**
     * Desconectar WebSocket de logs
     */
    fun disconnectLogsWebSocket() {
        logsWebSocket?.close(1000, "Desconexión normal")
        logsWebSocket = null
        Log.d("WEBSOCKET", "🔌 WebSocket logs desconectado")
    }

    /**
     * Enviar mensaje por WebSocket de animales
     */
    fun sendAnimalsMessage(message: String): Boolean {
        return try {
            animalsWebSocket?.send(message) ?: false
        } catch (e: Exception) {
            Log.e("WEBSOCKET", "❌ Error enviando mensaje animales: ${e.message}")
            false
        }
    }

    /**
     * Enviar mensaje por WebSocket de logs
     */
    fun sendLogsMessage(message: String): Boolean {
        return try {
            logsWebSocket?.send(message) ?: false
        } catch (e: Exception) {
            Log.e("WEBSOCKET", "❌ Error enviando mensaje logs: ${e.message}")
            false
        }
    }

    /**
     * Verificar estado de conexiones WebSocket
     */
    fun isNotificationsWebSocketConnected(): Boolean {
        return notificationsWebSocket != null
    }

    fun isAnimalsWebSocketConnected(): Boolean {
        return animalsWebSocket != null
    }

    fun isLogsWebSocketConnected(): Boolean {
        return logsWebSocket != null
    }

    /**
     * Actualizar método disconnectAllWebSockets() para incluir todos
     */
    private fun disconnectAllWebSocketsComplete() {
        disconnectNotificationsWebSocket()
        disconnectAnimalsWebSocket()
        disconnectLogsWebSocket()
        Log.d("WEBSOCKET", "🔌 Todos los WebSockets desconectados completamente")
    }

}