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

    // 🔧 REEMPLAZA CON TU URL ACTUAL
    private const val BASE_URL = " https://9e5a-83-97-144-149.ngrok-free.app/api/"

    private var retrofit: Retrofit? = null
    private var sharedPreferences: SharedPreferences? = null // CAMBIADO: Ahora es nullable
    private var appContext: Context? = null // CAMBIADO: Ahora es nullable

    fun initialize(context: Context) {
        appContext = context.applicationContext
        sharedPreferences = appContext!!.getSharedPreferences("genetics_prefs", Context.MODE_PRIVATE)
        Log.d("RETROFIT_CLIENT", "🔗 Conectando a: $BASE_URL")
        Log.d("RETROFIT_CLIENT", "✅ RetrofitClient inicializado correctamente")
    }

    // NUEVO: Método para verificar si está inicializado
    private fun isInitialized(): Boolean {
        return sharedPreferences != null && appContext != null
    }

    // NUEVO: Método para asegurar inicialización
    private fun ensureInitialized() {
        if (!isInitialized()) {
            throw IllegalStateException(
                "RetrofitClient no ha sido inicializado. " +
                        "Llama a RetrofitClient.initialize(context) antes de usar el cliente."
            )
        }
    }

    private fun getOkHttpClient(): OkHttpClient {
        ensureInitialized() // Verificar inicialización

        val logging = HttpLoggingInterceptor { message ->
            Log.d("HTTP_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor de autenticación con redirección automática
        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            // No verificar token para peticiones de login/registro
            val isLoginRequest = url.contains("/auth/login") || url.contains("/auth/register")

            if (!isLoginRequest) {
                val token = getToken()
                Log.d("AUTH_INTERCEPTOR", "🔑 Token: ${if (token != null) "✅ Presente" else "❌ Ausente"}")

                // Si no hay token Y no es una petición de login, redirigir
                if (token == null) {
                    Log.w("AUTH_INTERCEPTOR", "🚨 No hay token, redirigiendo al login...")
                    redirectToLogin()
                    // Continuar con la petición sin token (fallará pero evita crash)
                }
            } else {
                Log.d("AUTH_INTERCEPTOR", "🔓 Petición de login/registro, omitiendo verificación de token")
            }

            val requestBuilder = request.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            // Solo agregar Authorization si no es login/registro Y hay token
            if (!isLoginRequest) {
                val token = getToken()
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }

            val finalRequest = requestBuilder.build()
            Log.d("HTTP_REQUEST", "🌐 ${finalRequest.method} ${finalRequest.url}")

            val response = chain.proceed(finalRequest)
            Log.d("HTTP_RESPONSE", "📡 Código: ${response.code}")

            // Solo verificar 401 para peticiones que NO son de login
            if (!isLoginRequest && response.code == 401) {
                Log.w("AUTH_INTERCEPTOR", "🚨 Token inválido (401), redirigiendo al login...")
                clearToken()
                redirectToLogin()
            }

            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            // 🔧 TIMEOUTS OPTIMIZADOS para reducir cancelaciones
            .connectTimeout(15, TimeUnit.SECONDS)    // Tiempo para establecer conexión
            .readTimeout(20, TimeUnit.SECONDS)       // Tiempo para leer respuesta
            .writeTimeout(15, TimeUnit.SECONDS)      // Tiempo para enviar datos
            .callTimeout(25, TimeUnit.SECONDS)       // Tiempo total máximo por llamada
            // 🔧 CONFIGURACIONES ADICIONALES
            .retryOnConnectionFailure(true)          // Reintentar automáticamente
            .followRedirects(true)                   // Seguir redirecciones HTTP
            .followSslRedirects(true)               // Seguir redirecciones HTTPS
            .build()
    }

    // Método para redirigir al login
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
        ensureInitialized() // AÑADIDO: Verificar inicialización

        if (retrofit == null) {
            Log.d("RETROFIT_CLIENT", "🚀 Creando nueva instancia de Retrofit")
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }

    fun saveToken(token: String) {
        ensureInitialized() // AÑADIDO: Verificar inicialización
        Log.d("RETROFIT_CLIENT", "💾 Guardando token: ${token.take(10)}...")
        sharedPreferences!!.edit()
            .putString("jwt_token", token)
            .apply()
    }

    fun getToken(): String? {
        if (!isInitialized()) {
            Log.w("RETROFIT_CLIENT", "⚠️ RetrofitClient no inicializado, retornando null para token")
            return null
        }
        return sharedPreferences!!.getString("jwt_token", null)
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

        // Limpiar también la instancia de Retrofit para forzar recreación
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
}