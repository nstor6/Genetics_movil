package com.example.genetics

import android.app.Application
import android.util.Log
import com.example.genetics.api.RetrofitClient

class GeneticsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 🔧 INICIALIZAR RetrofitClient al inicio de la aplicación
        try {
            RetrofitClient.initialize(this)
            Log.d("GENETICS_APP", "✅ RetrofitClient inicializado en Application.onCreate()")
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "❌ Error crítico inicializando RetrofitClient: ${e.message}")
            e.printStackTrace()
            // La aplicación continuará pero las funciones de red fallarán
        }
    }
}