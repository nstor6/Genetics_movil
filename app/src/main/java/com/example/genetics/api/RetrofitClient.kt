package com.example.genetics.api

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.genetics.LoginActivity
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Configuración del servidor
    private const val BASE_URL = "https://51b6-81-42-254-142.ngrok-free.app/api/"
    private const val WS_BASE_URL = "wss://51b6-81-42-254-142.ngrok-free.app/ws/"

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
     * Inicializar RetrofitClient
     * DEBE llamarse desde Application.onCreate() o MainActivity.onCreate()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        sharedPreferences = appContext!!.getSharedPreferences("genetics_prefs", Context.MODE_PRIVATE)
        Log.d("RETROFIT_CLIENT", "Conectando a: $BASE_URL")
        Log.d("RETROFIT_CLIENT", "WebSocket URL: $WS_BASE_URL")
        Log.d("RETROFIT_CLIENT", "RetrofitClient inicializado correctamente")
    }

    /**
     * Verificar si está inicializado
     */
    private fun isInitialized(): Boolean {
        return sharedPreferences != null && appContext != null
    }

    /**
     * Asegurar que esté inicializado
     */
    private fun ensureInitialized() {
        if (!isInitialized()) {
            throw IllegalStateException(
                "RetrofitClient no ha sido inicializado. " +
                        "Llama a RetrofitClient.initialize(context) antes de usar el cliente."
            )
        }
    }

    /**
     * Crear cliente HTTP con interceptores
     */
    private fun getOkHttpClient(): OkHttpClient {
        ensureInitialized()

        if (okHttpClient == null) {
            // Logging interceptor
            val logging = HttpLoggingInterceptor { message ->
                Log.d("HTTP_LOG", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Interceptor de autenticación
            val authInterceptor = Interceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()

                Log.d("AUTH_INTERCEPTOR", "Petición a: $url")

                // Endpoints públicos (no requieren token)
                val isPublicEndpoint = url.contains("/auth/login") ||
                        url.contains("/auth/register") ||
                        url.contains("/auth/registro") ||
                        url.contains("/auth/refresh") ||
                        url.contains("/health") ||
                        url.contains("/cors-test")

                val requestBuilder = request.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "GeneticsApp/1.0 Android")

                // Añadir token si es necesario
                if (!isPublicEndpoint) {
                    val token = getToken()
                    Log.d("AUTH_INTERCEPTOR", "Token para $url: ${if (token != null) "Presente" else "Ausente"}")

                    if (token != null) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                        Log.d("AUTH_INTERCEPTOR", "Header Authorization añadido")
                    } else {
                        Log.w("AUTH_INTERCEPTOR", "No hay token para endpoint protegido: $url")
                    }
                } else {
                    Log.d("AUTH_INTERCEPTOR", "Endpoint público, no se añade token")
                }

                val finalRequest = requestBuilder.build()
                Log.d("HTTP_REQUEST", "${finalRequest.method} ${finalRequest.url}")

                // Log de headers para debugging
                finalRequest.headers.forEach { (name, value) ->
                    if (name.equals("Authorization", ignoreCase = true)) {
                        Log.d("HTTP_REQUEST", "Header: $name: Bearer ${value.removePrefix("Bearer ").take(20)}...")
                    } else {
                        Log.d("HTTP_REQUEST", "Header: $name: $value")
                    }
                }

                val response = chain.proceed(finalRequest)
                Log.d("HTTP_RESPONSE", "Código: ${response.code}")
                Log.d("HTTP_RESPONSE", "Mensaje: ${response.message}")

                // Manejo de 401 Unauthorized
                if (response.code == 401 && !isPublicEndpoint) {
                    Log.w("AUTH_INTERCEPTOR", "401 Unauthorized para: $url")
                    val responseBody = response.peekBody(1024).string()
                    Log.w("AUTH_INTERCEPTOR", "Respuesta 401: $responseBody")

                    // Solo limpiar token si realmente es un problema de autenticación
                    if (responseBody.contains("Authentication credentials") ||
                        responseBody.contains("Invalid token") ||
                        responseBody.contains("Token expired")) {
                        Log.w("AUTH_INTERCEPTOR", "Token inválido, limpiando y redirigiendo...")
                        clearToken()
                        disconnectAllWebSockets()
                        redirectToLogin()
                    }
                }

                response
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
                }
                context.startActivity(intent)
                Log.i("AUTH_INTERCEPTOR", "Redirigiendo al login...")
            } ?: run {
                Log.e("AUTH_INTERCEPTOR", "AppContext es null, no se puede redirigir")
            }
        } catch (e: Exception) {
            Log.e("AUTH_INTERCEPTOR", "Error redirigiendo al login: ${e.message}")
        }
    }

    /**
     * Obtener instancia del API Service
     */
    fun getApiService(): ApiService {
        ensureInitialized()

        if (retrofit == null) {
            Log.d("RETROFIT_CLIENT", "Creando nueva instancia de Retrofit")
            Log.d("RETROFIT_CLIENT", "URL Base: $BASE_URL")

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            Log.d("RETROFIT_CLIENT", "Retrofit creado exitosamente")
        }
        return retrofit!!.create(ApiService::class.java)
    }

    /**
     * Conectar WebSocket de notificaciones
     */
    fun connectNotificationsWebSocket(listener: WebSocketListener) {
        ensureInitialized()
        val token = getToken()

        if (token == null) {
            Log.w("WEBSOCKET", "No hay token disponible para WebSocket")
            return
        }

        val url = "${WS_BASE_URL}notificaciones/?token=$token"
        val request = Request.Builder().url(url).build()

        notificationsWebSocket = getOkHttpClient().newWebSocket(request, listener)
        Log.d("WEBSOCKET", "Conectando WebSocket de notificaciones: $url")
    }

    /**
     * Conectar WebSocket de animales
     */
    fun connectAnimalsWebSocket(listener: WebSocketListener) {
        ensureInitialized()
        val token = getToken()

        if (token == null) {
            Log.w("WEBSOCKET", "No hay token disponible para WebSocket de animales")
            return
        }

        val url = "${WS_BASE_URL}animales/?token=$token"
        val request = Request.Builder().url(url).build()

        animalsWebSocket = getOkHttpClient().newWebSocket(request, listener)
        Log.d("WEBSOCKET", "Conectando WebSocket de animales: $url")
    }

    /**
     * Conectar WebSocket de logs (solo para admins)
     */
    fun connectLogsWebSocket(listener: WebSocketListener) {
        ensureInitialized()
        val token = getToken()

        if (token == null) {
            Log.w("WEBSOCKET", "No hay token disponible para WebSocket de logs")
            return
        }

        val url = "${WS_BASE_URL}logs/?token=$token"
        val request = Request.Builder().url(url).build()

        logsWebSocket = getOkHttpClient().newWebSocket(request, listener)
        Log.d("WEBSOCKET", "Conectando WebSocket de logs: $url")
    }

    /**
     * Desconectar WebSocket de notificaciones
     */
    fun disconnectNotificationsWebSocket() {
        notificationsWebSocket?.close(1000, "Desconexión normal")
        notificationsWebSocket = null
        Log.d("WEBSOCKET", "WebSocket de notificaciones desconectado")
    }

    /**
     * Desconectar WebSocket de animales
     */
    fun disconnectAnimalsWebSocket() {
        animalsWebSocket?.close(1000, "Desconexión normal")
        animalsWebSocket = null
        Log.d("WEBSOCKET", "WebSocket de animales desconectado")
    }

    /**
     * Desconectar WebSocket de logs
     */
    fun disconnectLogsWebSocket() {
        logsWebSocket?.close(1000, "Desconexión normal")
        logsWebSocket = null
        Log.d("WEBSOCKET", "WebSocket de logs desconectado")
    }

    /**
     * Desconectar todos los WebSockets
     */
    fun disconnectAllWebSockets() {
        disconnectNotificationsWebSocket()
        disconnectAnimalsWebSocket()
        disconnectLogsWebSocket()
        Log.d("WEBSOCKET", "Todos los WebSockets desconectados")
    }

    /**
     * Enviar mensaje por WebSocket de notificaciones
     */
    fun sendNotificationMessage(message: String): Boolean {
        return notificationsWebSocket?.send(message) ?: false
    }

    /**
     * Enviar mensaje por WebSocket de animales
     */
    fun sendAnimalsMessage(message: String): Boolean {
        return animalsWebSocket?.send(message) ?: false
    }

    /**
     * Enviar mensaje por WebSocket de logs
     */
    fun sendLogsMessage(message: String): Boolean {
        return logsWebSocket?.send(message) ?: false
    }

    /**
     * Verificar si WebSocket de notificaciones está conectado
     */
    fun isNotificationsWebSocketConnected(): Boolean {
        return notificationsWebSocket != null
    }

    /**
     * Verificar si WebSocket de animales está conectado
     */
    fun isAnimalsWebSocketConnected(): Boolean {
        return animalsWebSocket != null
    }

    /**
     * Verificar si WebSocket de logs está conectado
     */
    fun isLogsWebSocketConnected(): Boolean {
        return logsWebSocket != null
    }

    /**
     * Guardar token JWT
     */
    fun saveToken(token: String) {
        ensureInitialized()
        Log.d("RETROFIT_CLIENT", "Guardando token: ${token.take(20)}...")
        sharedPreferences!!.edit()
            .putString("jwt_token", token)
            .apply()
    }

    /**
     * Obtener token JWT
     */
    fun getToken(): String? {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "RetrofitClient no inicializado, retornando null para token")
            return null
        }
        val token = sharedPreferences!!.getString("jwt_token", null)
        if (token != null) {
            Log.d("RETROFIT_CLIENT", "Token recuperado: ${token.take(20)}...")
        } else {
            Log.w("RETROFIT_CLIENT", "No se encontró token guardado")
        }
        return token
    }

    /**
     * Limpiar token JWT y desconectar WebSockets
     */
    fun clearToken() {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "RetrofitClient no inicializado, no se puede limpiar token")
            return
        }

        Log.d("RETROFIT_CLIENT", "Limpiando token...")
        sharedPreferences!!.edit()
            .remove("jwt_token")
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
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "RetrofitClient no inicializado, retornando false para isLoggedIn")
            return false
        }

        val hasToken = getToken() != null
        Log.d("RETROFIT_CLIENT", "Usuario logueado: $hasToken")
        return hasToken
    }

    /**
     * Debug del token actual
     */
    fun debugToken() {
        val token = getToken()
        if (token != null) {
            Log.d("RETROFIT_CLIENT", "DEBUG Token completo: $token")
            Log.d("RETROFIT_CLIENT", "DEBUG Token length: ${token.length}")
            // Verificar que el token tenga el formato correcto (JWT típicamente tiene 3 partes separadas por puntos)
            val parts = token.split(".")
            Log.d("RETROFIT_CLIENT", "DEBUG Token parts: ${parts.size} (debería ser 3 para JWT)")
        } else {
            Log.w("RETROFIT_CLIENT", "DEBUG: No hay token")
        }
    }
}