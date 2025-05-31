package com.example.genetics.Activitys

import UsersActivity
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
import com.example.genetics.Create.AddUserActivity
import com.example.genetics.LoginActivity
import com.example.genetics.R
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

        Log.d("DASHBOARD", "🚀 Dashboard iniciado")
        setupUI()
        loadStats()
    }

    private fun setupUI() {
        Log.d("DASHBOARD", "🔧 Configurando UI...")

        // 🔧 TARJETAS CLICKEABLES - Navegar a las respectivas activities
        binding.cardAnimals.setOnClickListener {
            Log.d("DASHBOARD", "🐄 Click en Animals")
            startActivity(Intent(this, AnimalsActivity::class.java))
        }

        binding.cardIncidents.setOnClickListener {
            Log.d("DASHBOARD", "🚨 Click en Incidents")
            startActivity(Intent(this, IncidentsActivity::class.java))
        }

        binding.cardTreatments.setOnClickListener {
            Log.d("DASHBOARD", "💊 Click en Treatments")
            startActivity(Intent(this, TreatmentsActivity::class.java))
        }

        binding.cardEvents.setOnClickListener {
            Log.d("DASHBOARD", "📅 Click en Events")
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // ✅ NUEVA TARJETA: Usuarios - CON DEBUG
        try {
            binding.cardUsers.setOnClickListener {
                Log.d("DASHBOARD", "👥 Click en Users - Intentando abrir UsersActivity")
                try {
                    val intent = Intent(this, UsersActivity::class.java)
                    Log.d("DASHBOARD", "👥 Intent creado correctamente")
                    startActivity(intent)
                    Log.d("DASHBOARD", "👥 StartActivity llamado")
                } catch (e: Exception) {
                    Log.e("DASHBOARD", "❌ Error al abrir UsersActivity: ${e.message}")
                    Toast.makeText(this, "Error al abrir Usuarios: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            Log.d("DASHBOARD", "✅ Listener de cardUsers configurado")
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error configurando cardUsers: ${e.message}")
        }

        // ✅ CONFIGURAR NAVEGACIÓN BOTTOM CON USUARIOS
        setupBottomNavigation()

        // 🔧 BOTONES DE ACCIONES RÁPIDAS
        binding.buttonViewAnimals.setOnClickListener {
            Log.d("DASHBOARD", "🆕 Click en buttonViewAnimals")
            startActivity(Intent(this, AddAnimalActivity::class.java))
        }

        binding.buttonNewIncident.setOnClickListener {
            Log.d("DASHBOARD", "🆕 Click en buttonNewIncident")
            startActivity(Intent(this, AddIncidentActivity::class.java))
        }

        binding.buttonNewTreatment.setOnClickListener {
            Log.d("DASHBOARD", "🆕 Click en buttonNewTreatment")
            startActivity(Intent(this, AddTreatmentActivity::class.java))
        }

        binding.buttonNewEvent.setOnClickListener {
            Log.d("DASHBOARD", "🆕 Click en buttonNewEvent")
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        // ✅ NUEVO BOTÓN: Nuevo Usuario - CON DEBUG
        try {
            binding.buttonNewUser.setOnClickListener {
                Log.d("DASHBOARD", "🆕👥 Click en buttonNewUser")
                try {
                    startActivity(Intent(this, AddUserActivity::class.java))
                } catch (e: Exception) {
                    Log.e("DASHBOARD", "❌ Error al abrir AddUserActivity: ${e.message}")
                    Toast.makeText(this, "Error al abrir Nuevo Usuario: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            Log.d("DASHBOARD", "✅ Listener de buttonNewUser configurado")
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error configurando buttonNewUser: ${e.message}")
        }
    }

    // ✅ FUNCIÓN PARA CONFIGURAR BOTTOM NAVIGATION CON USUARIOS
    private fun setupBottomNavigation() {
        Log.d("DASHBOARD", "🔧 Configurando Bottom Navigation...")

        try {
            // 🔧 NO establecer ningún item como seleccionado por defecto
            // binding.bottomNavigation.selectedItemId = ... (COMENTADO)

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                Log.d("DASHBOARD", "📱 Bottom Nav item selected: ${item.itemId}")
                when (item.itemId) {
                    R.id.nav_animals -> {
                        Log.d("DASHBOARD", "📱 Nav a Animals")
                        startActivity(Intent(this, AnimalsActivity::class.java))
                        true
                    }
                    R.id.nav_incidents -> {
                        Log.d("DASHBOARD", "📱 Nav a Incidents")
                        startActivity(Intent(this, IncidentsActivity::class.java))
                        true
                    }
                    R.id.nav_treatments -> {
                        Log.d("DASHBOARD", "📱 Nav a Treatments")
                        startActivity(Intent(this, TreatmentsActivity::class.java))
                        true
                    }
                    R.id.nav_calendar -> {
                        Log.d("DASHBOARD", "📱 Nav a Calendar")
                        startActivity(Intent(this, CalendarActivity::class.java))
                        true
                    }
                    R.id.nav_users -> {
                        Log.d("DASHBOARD", "📱 Nav a Users - Intentando abrir UsersActivity")
                        try {
                            startActivity(Intent(this, UsersActivity::class.java))
                            Log.d("DASHBOARD", "📱 UsersActivity abierto desde Bottom Nav")
                        } catch (e: Exception) {
                            Log.e("DASHBOARD", "❌ Error en Bottom Nav Users: ${e.message}")
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        true
                    }
                    else -> {
                        Log.w("DASHBOARD", "⚠️ Item no reconocido en Bottom Nav: ${item.itemId}")
                        false
                    }
                }
            }
            Log.d("DASHBOARD", "✅ Bottom Navigation configurado")
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error configurando Bottom Navigation: ${e.message}")
        }
    }

    // Resto del código permanece igual...
    private fun showSettingsMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración")
            .setItems(arrayOf("🔄 Actualizar datos", "👤 Mi perfil", "🚪 Cerrar sesión")) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(this, "Actualizando datos...", Toast.LENGTH_SHORT).show()
                        loadStats()
                    }
                    1 -> {
                        Toast.makeText(this, "Mi perfil - Próximamente", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
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
            loadUsersStats()
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

    private suspend fun loadUsersStats() {
        safeApiCall("LOAD_USERS_STATS") {
            apiService.getUsers()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    val activeUsers = users.count { it.activo == true }
                    binding.textUsersCount.text = activeUsers.toString()
                }
            }
            .onError { message ->
                android.util.Log.w("DASHBOARD", "Error cargando usuarios: $message")
                binding.textUsersCount.text = "0"
            }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                RetrofitClient.clearToken()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)
        loadStats()
    }
}