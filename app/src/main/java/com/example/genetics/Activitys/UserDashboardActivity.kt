package com.example.genetics.Activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.AnimalsActivity
import com.example.genetics.Activitys.CalendarActivity
import com.example.genetics.Activitys.IncidentsActivity
import com.example.genetics.Activitys.TreatmentsActivity
import com.example.genetics.Create.AddAnimalActivity
import com.example.genetics.Create.AddEventActivity
import com.example.genetics.Create.AddIncidentActivity
import com.example.genetics.Create.AddTreatmentActivity
import com.example.genetics.LoginActivity
import com.example.genetics.R
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityUserDashboardBinding
import com.example.genetics.utils.safeApiCall
import com.example.genetics.utils.onSuccess
import com.example.genetics.utils.onError
import kotlinx.coroutines.launch

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private val apiService = RetrofitClient.getApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("USER_DASHBOARD", "🚀 Dashboard de Usuario iniciado")
        setupUI()
        loadStats()
    }

    private fun setupUI() {
        Log.d("USER_DASHBOARD", "🔧 Configurando UI para usuario normal...")

        // 🔧 TARJETAS CLICKEABLES - Solo las que puede usar un usuario normal
        binding.cardAnimals.setOnClickListener {
            Log.d("USER_DASHBOARD", "🐄 Click en Animals")
            startActivity(Intent(this, AnimalsActivity::class.java))
        }

        binding.cardIncidents.setOnClickListener {
            Log.d("USER_DASHBOARD", "🚨 Click en Incidents")
            startActivity(Intent(this, IncidentsActivity::class.java))
        }

        binding.cardTreatments.setOnClickListener {
            Log.d("USER_DASHBOARD", "💊 Click en Treatments")
            startActivity(Intent(this, TreatmentsActivity::class.java))
        }

        binding.cardEvents.setOnClickListener {
            Log.d("USER_DASHBOARD", "📅 Click en Events")
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // ✅ CONFIGURAR NAVEGACIÓN BOTTOM PARA USUARIOS
        setupBottomNavigation()

        // 🔧 BOTONES DE ACCIONES RÁPIDAS - Solo las permitidas
        binding.buttonNewAnimal.setOnClickListener {
            Log.d("USER_DASHBOARD", "🆕 Click en buttonNewAnimal")
            startActivity(Intent(this, AddAnimalActivity::class.java))
        }

        binding.buttonNewIncident.setOnClickListener {
            Log.d("USER_DASHBOARD", "🆕 Click en buttonNewIncident")
            startActivity(Intent(this, AddIncidentActivity::class.java))
        }

        binding.buttonNewTreatment.setOnClickListener {
            Log.d("USER_DASHBOARD", "🆕 Click en buttonNewTreatment")
            startActivity(Intent(this, AddTreatmentActivity::class.java))
        }

        binding.buttonNewEvent.setOnClickListener {
            Log.d("USER_DASHBOARD", "🆕 Click en buttonNewEvent")
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        // Cargar información del usuario actual
        cargarInfoUsuario()
    }

    private fun cargarInfoUsuario() {
        lifecycleScope.launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    // Personalizar subtítulo con nombre del usuario
                    val nombreCompleto = "${user.nombre} ${user.apellidos}".trim()
                    if (nombreCompleto.isNotEmpty()) {
                        binding.textSubtitle.text = "Panel de Control - $nombreCompleto"
                    } else {
                        binding.textSubtitle.text = "Panel de Control - Usuario"
                    }

                    // Mostrar rol si es relevante
                    val rolFormateado = when (user.rol) {
                        "admin" -> "👑 Administrador"
                        "usuario" -> "👤 Usuario"
                        "dueño" -> "🏠 Dueño"
                        else -> "Usuario"
                    }

                    Log.d("USER_DASHBOARD", "✅ Usuario cargado: ${user.nombre} ($rolFormateado)")
                }
            } catch (e: Exception) {
                Log.e("USER_DASHBOARD", "❌ Error cargando usuario: ${e.message}")
                // Mantener texto por defecto
                binding.textSubtitle.text = "Panel de Control - Usuario"
            }
        }
    }

    private fun setupBottomNavigation() {
        Log.d("USER_DASHBOARD", "🔧 Configurando Bottom Navigation para usuario...")

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            Log.d("USER_DASHBOARD", "📱 Bottom Nav item selected: ${item.itemId}")
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
                R.id.nav_profile -> {  // ✅ Perfil en lugar de configuración
                    showUserMenu()
                    true
                }
                else -> false
            }
        }
    }

    private fun showUserMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("👤 Mi Perfil")
            .setItems(arrayOf(
                "🔄 Actualizar datos",
                "👤 Ver mi perfil",
                "🔔 Notificaciones",
                "📱 Información de la app",
                "🚪 Cerrar sesión"
            )) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(this, "Actualizando datos...", Toast.LENGTH_SHORT).show()
                        loadStats()
                    }
                    1 -> {
                        Toast.makeText(this, "Ver perfil - Próximamente", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        Toast.makeText(this, "Configuración de notificaciones - Próximamente", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        mostrarInfoApp()
                    }
                    4 -> {
                        logout()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarInfoApp() {
        val mensaje = buildString {
            append("📱 Genetics - Gestión Ganadera\n\n")
            append("🏢 Sistema integral de gestión de ganado\n\n")
            append("✨ Tus funcionalidades:\n")
            append("• 🐄 Ver y gestionar animales\n")
            append("• 🚨 Reportar incidencias\n")
            append("• 💊 Registrar tratamientos\n")
            append("• 📅 Ver calendario de eventos\n\n")
            append("📞 Soporte: genetics@example.com\n")
            append("🌐 Web: www.genetics-app.com")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📱 Información de la App")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
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
                    Log.d("USER_DASHBOARD", "📊 Animales cargados: ${animals.size}")
                }
            }
            .onError { message ->
                Log.w("USER_DASHBOARD", "Error cargando animales: $message")
                binding.textAnimalsCount.text = "0"
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
                    Log.d("USER_DASHBOARD", "📊 Incidencias pendientes: $pending de ${incidents.size}")
                }
            }
            .onError { message ->
                Log.w("USER_DASHBOARD", "Error cargando incidencias: $message")
                binding.textIncidentsCount.text = "0"
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
                    Log.d("USER_DASHBOARD", "📊 Tratamientos recientes: ${recentTreatments.size} de ${treatments.size}")
                }
            }
            .onError { message ->
                Log.w("USER_DASHBOARD", "Error cargando tratamientos: $message")
                binding.textTreatmentsCount.text = "0"
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
                    Log.d("USER_DASHBOARD", "📊 Eventos próximos: ${upcomingEvents.size} de ${events.size}")
                }
            }
            .onError { message ->
                Log.w("USER_DASHBOARD", "Error cargando eventos: $message")
                binding.textEventsCount.text = "0"
            }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                Log.d("USER_DASHBOARD", "🚪 Cerrando sesión...")
                RetrofitClient.clearToken()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Limpiar selección del bottom navigation
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        // Recargar estadísticas al volver
        loadStats()

        Log.d("USER_DASHBOARD", "🔄 onResume - Dashboard actualizado")
    }
}