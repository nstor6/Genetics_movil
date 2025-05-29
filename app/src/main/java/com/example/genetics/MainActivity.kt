package com.example.genetics

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.genetics.api.RetrofitClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar RetrofitClient
        RetrofitClient.initialize(this)

        // Verificar si el usuario ya está logueado
        if (RetrofitClient.isLoggedIn()) {
            // Ir al dashboard principal
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            // Ir al login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}