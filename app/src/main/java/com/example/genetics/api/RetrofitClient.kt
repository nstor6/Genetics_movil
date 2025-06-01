package com.example.genetics.api

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.genetics.LoginActivity
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://8e4c-83-97-144-149.ngrok-free.app/api/"

    private var retrofit: Retrofit? = null
    private var sharedPreferences: SharedPreferences? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        sharedPreferences = appContext!!.getSharedPreferences("genetics_prefs", Context.MODE_PRIVATE)
        Log.d("RETROFIT_CLIENT", "🔗 Conectando a: $BASE_URL")
        Log.d("RETROFIT_CLIENT", "✅ RetrofitClient inicializado correctamente")
    }

    private fun isInitialized(): Boolean {
        return sharedPreferences != null && appContext != null
    }

    private fun ensureInitialized() {
        if (!isInitialized()) {
            throw IllegalStateException(
                "RetrofitClient no ha sido inicializado. " +
                        "Llama a RetrofitClient.initialize(context) antes de usar el cliente."
            )
        }
    }

    private fun getOkHttpClient(): OkHttpClient {
        ensureInitialized()

        val logging = HttpLoggingInterceptor { message ->
            Log.d("HTTP_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 🔧 INTERCEPTOR DE AUTENTICACIÓN CORREGIDO
        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            Log.d("AUTH_INTERCEPTOR", "🌐 Petición a: $url")

            // 🔧 LISTA ACTUALIZADA de endpoints que NO requieren token
            val isPublicEndpoint = url.contains("/auth/login") ||
                    url.contains("/auth/register") ||
                    url.contains("/auth/registro") ||
                    url.contains("/auth/refresh")

            val requestBuilder = request.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            // 🔧 SIEMPRE añadir token si existe, excepto para endpoints públicos
            if (!isPublicEndpoint) {
                val token = getToken()
                Log.d("AUTH_INTERCEPTOR", "🔑 Token para ${url}: ${if (token != null) "✅ Presente (${token.take(20)}...)" else "❌ Ausente"}")

                if (token != null) {
                    // 🔧 ASEGURAR formato correcto del token
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                    Log.d("AUTH_INTERCEPTOR", "🔑 Header Authorization añadido: Bearer ${token.take(20)}...")
                } else {
                    Log.w("AUTH_INTERCEPTOR", "🚨 No hay token para endpoint protegido: $url")
                    // No redirigir inmediatamente, dejar que la respuesta 401 maneje esto
                }
            } else {
                Log.d("AUTH_INTERCEPTOR", "🔓 Endpoint público, no se añade token")
            }

            val finalRequest = requestBuilder.build()
            Log.d("HTTP_REQUEST", "🌐 ${finalRequest.method} ${finalRequest.url}")

            // 🔧 LOG DE HEADERS PARA DEBUGGING
            finalRequest.headers.forEach { (name, value) ->
                if (name.equals("Authorization", ignoreCase = true)) {
                    Log.d("HTTP_REQUEST", "📋 Header: $name: Bearer ${value.removePrefix("Bearer ").take(20)}...")
                } else {
                    Log.d("HTTP_REQUEST", "📋 Header: $name: $value")
                }
            }

            val response = chain.proceed(finalRequest)
            Log.d("HTTP_RESPONSE", "📡 Código: ${response.code}")
            Log.d("HTTP_RESPONSE", "📡 Mensaje: ${response.message}")

            // 🔧 MANEJO MEJORADO DE 401
            if (response.code == 401 && !isPublicEndpoint) {
                Log.w("AUTH_INTERCEPTOR", "🚨 401 Unauthorized para: $url")
                val responseBody = response.peekBody(1024).string()
                Log.w("AUTH_INTERCEPTOR", "🚨 Respuesta 401: $responseBody")

                // Solo limpiar token y redirigir si realmente es un problema de autenticación
                if (responseBody.contains("Authentication credentials") ||
                    responseBody.contains("Invalid token") ||
                    responseBody.contains("Token expired")) {
                    Log.w("AUTH_INTERCEPTOR", "🚨 Token inválido, limpiando y redirigiendo...")
                    clearToken()
                    redirectToLogin()
                }
            }

            response
        }

        return OkHttpClient.Builder()
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

    private fun redirectToLogin() {
        try {
            appContext?.let { context ->
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                Log.i("AUTH_INTERCEPTOR", "🔄 Redirigiendo al login...")
            } ?: run {
                Log.e("AUTH_INTERCEPTOR", "❌ AppContext es null, no se puede redirigir")
            }
        } catch (e: Exception) {
            Log.e("AUTH_INTERCEPTOR", "❌ Error redirigiendo al login: ${e.message}")
        }
    }

    fun getApiService(): ApiService {
        ensureInitialized()

        if (retrofit == null) {
            Log.d("RETROFIT_CLIENT", "🚀 Creando nueva instancia de Retrofit")
            Log.d("RETROFIT_CLIENT", "🌐 URL Base: $BASE_URL")

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            Log.d("RETROFIT_CLIENT", "✅ Retrofit creado exitosamente")
        }
        return retrofit!!.create(ApiService::class.java)
    }

    fun saveToken(token: String) {
        ensureInitialized()
        Log.d("RETROFIT_CLIENT", "💾 Guardando token: ${token.take(20)}...")
        sharedPreferences!!.edit()
            .putString("jwt_token", token)
            .apply()
    }

    fun getToken(): String? {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient no inicializado, retornando null para token")
            return null
        }
        val token = sharedPreferences!!.getString("jwt_token", null)
        if (token != null) {
            Log.d("RETROFIT_CLIENT", "🔍 Token recuperado: ${token.take(20)}...")
        } else {
            Log.w("RETROFIT_CLIENT", "🔍 No se encontró token guardado")
        }
        return token
    }

    fun clearToken() {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient no inicializado, no se puede limpiar token")
            return
        }

        Log.d("RETROFIT_CLIENT", "🗑️ Limpiando token...")
        sharedPreferences!!.edit()
            .remove("jwt_token")
            .apply()

        retrofit = null
    }

    fun isLoggedIn(): Boolean {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient no inicializado, retornando false para isLoggedIn")
            return false
        }

        val hasToken = getToken() != null
        Log.d("RETROFIT_CLIENT", "🔐 ¿Usuario logueado? $hasToken")
        return hasToken
    }

    // 🔧 NUEVA FUNCIÓN: Debug del token actual
    fun debugToken() {
        val token = getToken()
        if (token != null) {
            Log.d("RETROFIT_CLIENT", "🔍 DEBUG Token completo: $token")
            Log.d("RETROFIT_CLIENT", "🔍 DEBUG Token length: ${token.length}")
            // Verificar que el token tenga el formato correcto (JWT típicamente tiene 3 partes separadas por puntos)
            val parts = token.split(".")
            Log.d("RETROFIT_CLIENT", "🔍 DEBUG Token parts: ${parts.size} (debería ser 3 para JWT)")
        } else {
            Log.w("RETROFIT_CLIENT", "🔍 DEBUG: No hay token")
        }
    }
}