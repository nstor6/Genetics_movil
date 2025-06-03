package com.example.genetics.Activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Create.*
import com.example.genetics.LoginActivity
import com.example.genetics.R
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityDashboardBinding
import com.example.genetics.utils.safeApiCall
import com.example.genetics.utils.onSuccess
import com.example.genetics.utils.onError
import kotlinx.coroutines.launch
import okhttp3.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var webSocket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("DASHBOARD", "Dashboard iniciado")
        setupUI()
        loadStats()
        connectWebSocket()
    }

    private fun setupUI() {
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

        binding.cardUsers.setOnClickListener {
            startActivity(Intent(this, UsersActivity::class.java))
        }

        setupBottomNavigation()

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

        binding.buttonNewUser.setOnClickListener {
            startActivity(Intent(this, AddUserActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
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
                    showSettingsMenu()
                    true
                }
                else -> false
            }
        }
    }

    private fun showSettingsMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración")
            .setItems(arrayOf(
                "👥 Gestionar usuarios",
                "🔄 Actualizar datos",
                "👤 Mi perfil",
                "📱 Información de la app",
                "🚪 Cerrar sesión"
            )) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, UsersActivity::class.java))
                    1 -> lifecycleScope.launch { loadStats() }
                    2 -> Toast.makeText(this, "👤 Mi perfil - Próximamente", Toast.LENGTH_SHORT).show()
                    3 -> mostrarInfoApp()
                    4 -> logout()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarInfoApp() {
        val mensaje = """
            📱 Genetics - Gestión Ganadera
            
            🏢 Desarrollado para la gestión integral de ganado
            
            ✨ Funcionalidades:
            • 🐄 Gestión de animales
            • 🚨 Control de incidencias
            • 💊 Registro de tratamientos
            • 📅 Calendario de eventos
            • 👥 Administración de usuarios
            
            📞 Soporte: genetics@example.com
            🌐 Web: www.genetics-app.com
        """.trimIndent()

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
            loadUsersStats()
        }
    }

    private suspend fun loadAnimalsStats() {
        safeApiCall("LOAD_ANIMALS_STATS") {
            apiService.getAnimales()
        }.onSuccess { response ->
            response.body()?.let {
                binding.textAnimalsCount.text = it.results.size.toString()
            }
        }.onError { Log.w("DASHBOARD", "Error cargando animales: $it") }
    }

    private suspend fun loadIncidentsStats() {
        safeApiCall("LOAD_INCIDENTS_STATS") {
            apiService.getIncidencias()
        }.onSuccess { response ->
            response.body()?.let {
                val pending = it.results.count { inc -> inc.estado == "pendiente" }
                binding.textIncidentsCount.text = pending.toString()
            }
        }.onError { Log.w("DASHBOARD", "Error cargando incidencias: $it") }
    }

    private suspend fun loadTreatmentsStats() {
        safeApiCall("LOAD_TREATMENTS_STATS") {
            apiService.getTratamientos()
        }.onSuccess { response ->
            response.body()?.let {
                val recent = it.results.filter { tr ->
                    try {
                        val date = java.text.SimpleDateFormat("yyyy-MM-dd").parse(tr.fecha)
                        val weekAgo = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.DAY_OF_MONTH, -7)
                        }.time
                        date?.after(weekAgo) == true
                    } catch (e: Exception) {
                        false
                    }
                }
                binding.textTreatmentsCount.text = recent.size.toString()
            }
        }.onError { Log.w("DASHBOARD", "Error cargando tratamientos: $it") }
    }

    private suspend fun loadEventsStats() {
        safeApiCall("LOAD_EVENTS_STATS") {
            apiService.getEventos()
        }.onSuccess { response ->
            response.body()?.results?.let { eventos ->
                val upcoming = eventos.count {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
                        val date = sdf.parse(it.fecha_inicio.substring(0, 10))
                        val today = java.util.Calendar.getInstance().time
                        val nextWeek = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.DAY_OF_MONTH, 7)
                        }.time
                        date != null && date.after(today) && date.before(nextWeek)
                    } catch (e: Exception) {
                        false
                    }
                }
                binding.textEventsCount.text = upcoming.toString()
            }
        }.onError { Log.w("DASHBOARD", "Error cargando eventos: $it") }
    }

    private suspend fun loadUsersStats() {
        safeApiCall("LOAD_USERS_STATS") {
            apiService.getUsers()
        }.onSuccess { response ->
            val users = response.body() ?: emptyList()
            binding.textUsersCount.text = users.count { it.activo == true }.toString()
        }.onError {
            Log.w("DASHBOARD", "Error cargando usuarios: $it")
            binding.textUsersCount.text = "0"
        }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
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
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        lifecycleScope.launch {
            loadStats()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "App cerrada")
        Log.d("DASHBOARD", "🗑️ onDestroy")
    }

    // === WebSocket Setup ===
    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://tu-backend.com/ws/dashboard/") // Reemplaza por tu URL real
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WEBSOCKET", "✅ Conectado al WebSocket")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WEBSOCKET", "📩 Mensaje recibido: $text")
                if (text.contains("refresh", ignoreCase = true)) {
                    lifecycleScope.launch { loadStats() }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WEBSOCKET", "🛑 Cerrando WebSocket: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WEBSOCKET", "❌ Error WebSocket: ${t.message}")
            }
        })
    }
}
