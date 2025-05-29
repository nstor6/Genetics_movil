package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityDashboardBinding
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val apiService = RetrofitClient.getApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadStats()
    }

    private fun setupUI() {
        // Configurar navegación bottom nav
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_animals -> {
                    // Ir a lista de animales
                    startActivity(Intent(this, AnimalsActivity::class.java))
                    true
                }
                R.id.nav_incidents -> {
                    // Ir a incidencias
                    startActivity(Intent(this, IncidentsActivity::class.java))
                    true
                }
                R.id.nav_treatments -> {
                    // Ir a tratamientos
                    Toast.makeText(this, "Tratamientos - Próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_calendar -> {
                    // Ir a calendario
                    Toast.makeText(this, "Calendario - Próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    // Configuración y logout
                    logout()
                    true
                }
                else -> false
            }
        }

        // Configurar botones de acción del dashboard
        binding.buttonViewAnimals.setOnClickListener {
            startActivity(Intent(this, AnimalsActivity::class.java))
        }

        binding.buttonNewIncident.setOnClickListener {
            startActivity(Intent(this, AddIncidentActivity::class.java))
        }

        binding.buttonNewTreatment.setOnClickListener {
            Toast.makeText(this, "Nuevo tratamiento - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Configurar FAB para nuevo animal
        binding.fabNewAnimal.setOnClickListener {
            startActivity(Intent(this, AnimalsActivity::class.java))
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                // Cargar estadísticas
                val animalsResponse = apiService.getAnimales()
                val incidentsResponse = apiService.getIncidencias()
                val treatmentsResponse = apiService.getTratamientos()
                val eventsResponse = apiService.getEventos()

                if (animalsResponse.isSuccessful) {
                    val animals = animalsResponse.body() ?: emptyList()
                    binding.textAnimalsCount.text = animals.size.toString()
                }

                if (incidentsResponse.isSuccessful) {
                    val incidents = incidentsResponse.body() ?: emptyList()
                    val pending = incidents.count { it.estado == "pendiente" }
                    binding.textIncidentsCount.text = pending.toString()
                }

                if (treatmentsResponse.isSuccessful) {
                    val treatments = treatmentsResponse.body() ?: emptyList()
                    binding.textTreatmentsCount.text = treatments.size.toString()
                }

                if (eventsResponse.isSuccessful) {
                    val events = eventsResponse.body() ?: emptyList()
                    binding.textEventsCount.text = events.size.toString()
                }

            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Error cargando datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        RetrofitClient.clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}