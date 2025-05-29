package com.example.genetics.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 🔧 REEMPLAZA "192.168.1.XXX" CON TU IP LOCAL REAL
    // Ejemplo: si tu IP es 192.168.1.105, usa:
    // private const val BASE_URL = "http://192.168.1.105:8000/api/"

    private const val BASE_URL = "  https://2cb5-83-97-144-149.ngrok-free.app/api/"

    // 💡 ALTERNATIVAMENTE, puedes usar ngrok:
    // private const val BASE_URL = "https://tu-nueva-url.ngrok-free.app/api/"

    private var retrofit: Retrofit? = null
    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("genetics_prefs", Context.MODE_PRIVATE)
        Log.d("RETROFIT_CLIENT", "🔗 Conectando a: $BASE_URL")
    }

    private fun getOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("HTTP_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val token = getToken()
            Log.d("AUTH_INTERCEPTOR", "🔑 Token: ${if (token != null) "✅ Presente" else "❌ Ausente"}")

            val requestBuilder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            Log.d("HTTP_REQUEST", "🌐 ${request.method} ${request.url}")

            val response = chain.proceed(request)
            Log.d("HTTP_RESPONSE", "📡 Código: ${response.code}")

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
    }

    fun isLoggedIn(): Boolean {
        val hasToken = getToken() != null
        Log.d("RETROFIT_CLIENT", "🔐 ¿Usuario logueado? $hasToken")
        return hasToken
    }
}