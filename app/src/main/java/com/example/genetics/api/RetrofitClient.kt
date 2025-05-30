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
    private const val BASE_URL = "https://6d9d-83-97-144-149.ngrok-free.app/api/"

    private var retrofit: Retrofit? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
        sharedPreferences = appContext.getSharedPreferences("genetics_prefs", Context.MODE_PRIVATE)
        Log.d("RETROFIT_CLIENT", "🔗 Conectando a: $BASE_URL")
    }

    private fun getOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("HTTP_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor de autenticación con redirección automática
        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            // CORREGIDO: No verificar token para peticiones de login
            val isLoginRequest = url.contains("/auth/login") || url.contains("/auth/register")

            if (!isLoginRequest) {
                val token = getToken()
                Log.d("AUTH_INTERCEPTOR", "🔑 Token: ${if (token != null) "✅ Presente" else "❌ Ausente"}")

                // Si no hay token Y no es una petición de login, redirigir
                if (token == null) {
                    Log.w("AUTH_INTERCEPTOR", "🚨 No hay token, redirigiendo al login...")
                    redirectToLogin()
                    // Continúar con la petición sin token (fallará pero evita crash)
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Método para redirigir al login
    private fun redirectToLogin() {
        try {
            val intent = Intent(appContext, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            appContext.startActivity(intent)
            Log.i("AUTH_INTERCEPTOR", "🔄 Redirigiendo al login...")
        } catch (e: Exception) {
            Log.e("AUTH_INTERCEPTOR", "❌ Error redirigiendo al login: ${e.message}")
        }
    }

    fun getApiService(): ApiService {
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
        Log.d("RETROFIT_CLIENT", "💾 Guardando token: ${token.take(10)}...")
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }

    fun clearToken() {
        Log.d("RETROFIT_CLIENT", "🗑️ Limpiando token...")
        sharedPreferences.edit()
            .remove("jwt_token")
            .apply()

        // Limpiar también la instancia de Retrofit para forzar recreación
        retrofit = null
    }

    fun isLoggedIn(): Boolean {
        val hasToken = getToken() != null
        Log.d("RETROFIT_CLIENT", "🔐 ¿Usuario logueado? $hasToken")
        return hasToken
    }
}