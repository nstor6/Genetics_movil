package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Add.AddAnimalActivity
import com.example.genetics.Add.AddEventActivity
import com.example.genetics.Add.AddIncidentActivity
import com.example.genetics.Add.AddTreatmentActivity
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityDashboardBinding
import com.example.genetics.utils.safeApiCall
import com.example.genetics.utils.onSuccess
import com.example.genetics.utils.onError
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
        // 🔧 TARJETAS CLICKEABLES - Navegar a las respectivas activities
        binding.cardAnimals.setOnClickListener {
            startActivity(Intent(this, AnimalsActivity::class.java))
        }

        binding.cardIncidents.setOnClickListener {
            startActivity(Intent(this, IncidentsActivity::class.java))
        }

        binding.cardTreatments.setOnClickListener {
            startActivity(Intent(this, TreatmentsActivity::class.java))
        }

        binding.cardEvents.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // ✅ CONFIGURAR NAVEGACIÓN BOTTOM SIN SELECCIÓN POR DEFECTO
        setupBottomNavigation()

        // 🔧 BOTONES DE ACCIONES RÁPIDAS
        binding.buttonViewAnimals.setOnClickListener {
            startActivity(Intent(this, AddAnimalActivity::class.java))
        }

        binding.buttonNewIncident.setOnClickListener {
            startActivity(Intent(this, AddIncidentActivity::class.java))
        }

        binding.buttonNewTreatment.setOnClickListener {
            startActivity(Intent(this, AddTreatmentActivity::class.java))
        }

        binding.buttonNewEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }
    }

    // ✅ FUNCIÓN PARA CONFIGURAR BOTTOM NAVIGATION SIN SELECCIÓN
    private fun setupBottomNavigation() {
        // 🔧 NO establecer ningún item como seleccionado por defecto
        // binding.bottomNavigation.selectedItemId = ... (COMENTADO)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_animals -> {
                    startActivity(Intent(this, AnimalsActivity::class.java))
                    true
                }
                R.id.nav_incidents -> {
                    startActivity(Intent(this, IncidentsActivity::class.java))
                    true
                }
                R.id.nav_treatments -> {
                    startActivity(Intent(this, TreatmentsActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    // Ya estamos en el Dashboard/Settings, mostrar opciones
                    showSettingsMenu()
                    false // No seleccionar el item
                }
                else -> false
            }
        }
    }

    // ✅ MENÚ DE CONFIGURACIÓN CUANDO SE TOCA SETTINGS
    private fun showSettingsMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración")
            .setItems(arrayOf("🔄 Actualizar datos", "🚪 Cerrar sesión")) { _, which ->
                when (which) {
                    0 -> {
                        // Actualizar datos
                        Toast.makeText(this, "Actualizando datos...", Toast.LENGTH_SHORT).show()
                        loadStats() // Recargar estadísticas
                    }
                    1 -> {
                        // Cerrar sesión
                        logout()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            loadAnimalsStats()
            loadIncidentsStats()
            loadTreatmentsStats()
            loadEventsStats()
        }
    }

    private suspend fun loadAnimalsStats() {
        safeApiCall("LOAD_ANIMALS_STATS") {
            apiService.getAnimales()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val animals = response.body() ?: emptyList()
                    binding.textAnimalsCount.text = animals.size.toString()
                }
            }
            .onError { message ->
                android.util.Log.w("DASHBOARD", "Error cargando animales: $message")
            }
    }

    private suspend fun loadIncidentsStats() {
        safeApiCall("LOAD_INCIDENTS_STATS") {
            apiService.getIncidencias()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val incidents = response.body() ?: emptyList()
                    val pending = incidents.count { it.estado == "pendiente" }
                    binding.textIncidentsCount.text = pending.toString()
                }
            }
            .onError { message ->
                android.util.Log.w("DASHBOARD", "Error cargando incidencias: $message")
            }
    }

    private suspend fun loadTreatmentsStats() {
        safeApiCall("LOAD_TREATMENTS_STATS") {
            apiService.getTratamientos()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val treatments = response.body() ?: emptyList()
                    val recentTreatments = treatments.filter {
                        try {
                            val treatmentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it.fecha)
                            val weekAgo = java.util.Calendar.getInstance().apply {
                                add(java.util.Calendar.DAY_OF_MONTH, -7)
                            }.time
                            treatmentDate?.after(weekAgo) == true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    binding.textTreatmentsCount.text = recentTreatments.size.toString()
                }
            }
            .onError { message ->
                android.util.Log.w("DASHBOARD", "Error cargando tratamientos: $message")
            }
    }

    private suspend fun loadEventsStats() {
        safeApiCall("LOAD_EVENTS_STATS") {
            apiService.getEventos()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    val upcomingEvents = events.filter { evento ->
                        try {
                            val eventoFecha = evento.fecha_inicio.substring(0, 10)
                            val eventDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(eventoFecha)
                            val today = java.util.Calendar.getInstance().time
                            val nextWeek = java.util.Calendar.getInstance().apply {
                                add(java.util.Calendar.DAY_OF_MONTH, 7)
                            }.time
                            eventDate?.let { it.after(today) && it.before(nextWeek) } == true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    binding.textEventsCount.text = upcomingEvents.size.toString()
                }
            }
            .onError { message ->
                android.util.Log.w("DASHBOARD", "Error cargando eventos: $message")
            }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                RetrofitClient.clearToken()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity() // Cerrar todas las activities
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // 🔧 ASEGURAR que no hay nada seleccionado al volver
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        // Recargar estadísticas
        loadStats()
    }
}