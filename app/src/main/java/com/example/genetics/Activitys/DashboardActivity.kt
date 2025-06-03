package com.example.genetics.Activitys

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
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
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val apiService = RetrofitClient.getApiService()

    // ✅ WebSocket usando RetrofitClient
    private var isWebSocketConnected = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("DASHBOARD", "🚀 Dashboard Admin iniciado")
        setupUI()
        loadStats()

        // ✅ USAR WEBSOCKET DE RETROFITCLIENT
        connectWebSocketRealTime()

        // Debug inicial
        debugWebSocketStatus()
    }

    private fun setupUI() {
        // Configurar clicks de tarjetas
        binding.cardAnimals.setOnClickListener {
            Log.d("DASHBOARD", "🐄 Navegando a AnimalsActivity")
            startActivity(Intent(this, AnimalsActivity::class.java))
        }

        binding.cardIncidents.setOnClickListener {
            Log.d("DASHBOARD", "🚨 Navegando a IncidentsActivity")
            startActivity(Intent(this, IncidentsActivity::class.java))
        }

        binding.cardTreatments.setOnClickListener {
            Log.d("DASHBOARD", "💊 Navegando a TreatmentsActivity")
            startActivity(Intent(this, TreatmentsActivity::class.java))
        }

        binding.cardEvents.setOnClickListener {
            Log.d("DASHBOARD", "📅 Navegando a CalendarActivity")
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        binding.cardUsers.setOnClickListener {
            Log.d("DASHBOARD", "👥 Navegando a UsersActivity")
            startActivity(Intent(this, UsersActivity::class.java))
        }

        // Configurar navegación inferior
        setupBottomNavigation()

        // Configurar botones de acción rápida - con safe calls
        binding.buttonViewAnimals?.setOnClickListener {
            Log.d("DASHBOARD", "➕ Crear nuevo animal")
            startActivity(Intent(this, AddAnimalActivity::class.java))
        }

        binding.buttonNewIncident?.setOnClickListener {
            Log.d("DASHBOARD", "➕ Crear nueva incidencia")
            startActivity(Intent(this, AddIncidentActivity::class.java))
        }

        binding.buttonNewTreatment?.setOnClickListener {
            Log.d("DASHBOARD", "➕ Crear nuevo tratamiento")
            startActivity(Intent(this, AddTreatmentActivity::class.java))
        }

        binding.buttonNewEvent?.setOnClickListener {
            Log.d("DASHBOARD", "➕ Crear nuevo evento")
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        binding.buttonNewUser?.setOnClickListener {
            Log.d("DASHBOARD", "➕ Crear nuevo usuario")
            startActivity(Intent(this, AddUserActivity::class.java))
        }

        // ✅ BOTÓN REFRESH MANUAL - con safe call
        try {
            val fabRefresh = binding.javaClass.getDeclaredField("fabRefresh")
            fabRefresh.isAccessible = true
            val fab = fabRefresh.get(binding) as? com.google.android.material.floatingactionbutton.FloatingActionButton
            fab?.setOnClickListener {
                refreshAllData()
            }
        } catch (e: Exception) {
            Log.d("DASHBOARD", "FAB refresh no disponible en el layout")
        }

        // ✅ MOSTRAR ESTADO DE CONEXIÓN
        updateConnectionStatus()
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

    // ✅ WEBSOCKET USANDO RETROFITCLIENT
    private fun connectWebSocketRealTime() {
        Log.d("DASHBOARD", "🔌 Conectando WebSocket para tiempo real...")

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("DASHBOARD_WS", "✅ WebSocket conectado exitosamente")
                isWebSocketConnected = true

                runOnUiThread {
                    updateConnectionStatus()
                    Toast.makeText(this@DashboardActivity, "📡 Conectado en tiempo real", Toast.LENGTH_SHORT).show()
                }

                // Solicitar estadísticas iniciales
                webSocket.send("""{"type": "get_stats", "from": "dashboard"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("DASHBOARD_WS", "📩 Mensaje recibido: $text")

                try {
                    val json = JSONObject(text)
                    val type = json.optString("type", "")

                    when (type) {
                        // ✅ MANEJO DE MENSAJES SEGÚN TU BACKEND
                        "notification" -> {
                            val data = json.getJSONObject("data")
                            val mensaje = data.optString("mensaje", "")
                            val tipoNotif = data.optString("tipo", "")

                            runOnUiThread {
                                showRealTimeNotification(mensaje, tipoNotif)
                            }
                        }

                        "new_log" -> {
                            val data = json.getJSONObject("data")
                            val entidad = data.optString("entidad_afectada", "")
                            val accion = data.optString("tipo_accion", "")

                            Log.d("DASHBOARD_WS", "📋 Nuevo log: $accion en $entidad")

                            // Actualizar estadísticas según la entidad afectada
                            runOnUiThread {
                                when (entidad) {
                                    "animal" -> lifecycleScope.launch {
                                        loadAnimalsStats()
                                        showQuickUpdate("🐄 Animales actualizados")
                                    }
                                    "incidencia" -> lifecycleScope.launch {
                                        loadIncidentsStats()
                                        showQuickUpdate("🚨 Incidencias actualizadas")
                                    }
                                    "tratamiento" -> lifecycleScope.launch {
                                        loadTreatmentsStats()
                                        showQuickUpdate("💊 Tratamientos actualizados")
                                    }
                                    "evento" -> lifecycleScope.launch {
                                        loadEventsStats()
                                        showQuickUpdate("📅 Eventos actualizados")
                                    }
                                    "usuario" -> lifecycleScope.launch {
                                        loadUsersStats()
                                        showQuickUpdate("👥 Usuarios actualizados")
                                    }
                                }
                            }
                        }

                        "animal_created", "animal_updated", "animal_deleted" -> {
                            Log.d("DASHBOARD_WS", "🐄 Cambio en animales: $type")
                            runOnUiThread {
                                lifecycleScope.launch {
                                    loadAnimalsStats()
                                    animateCardUpdate(binding.cardAnimals)
                                }
                            }
                        }

                        "broadcast" -> {
                            val data = json.getJSONObject("data")
                            val mensaje = data.optString("mensaje", "Actualización del sistema")

                            runOnUiThread {
                                showSystemBroadcast(mensaje)
                            }
                        }

                        "connection_established" -> {
                            Log.d("DASHBOARD_WS", "🔗 Conexión WebSocket establecida")
                        }

                        "heartbeat", "ping" -> {
                            // Responder al ping del servidor
                            webSocket.send("""{"type": "pong", "timestamp": ${System.currentTimeMillis()}}""")
                        }

                        else -> {
                            Log.d("DASHBOARD_WS", "📝 Mensaje no manejado: $type")
                        }
                    }

                } catch (e: Exception) {
                    Log.e("DASHBOARD_WS", "❌ Error procesando mensaje WebSocket: ${e.message}")
                    e.printStackTrace()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("DASHBOARD_WS", "🛑 WebSocket cerrándose: $code $reason")
                isWebSocketConnected = false
                runOnUiThread {
                    updateConnectionStatus()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("DASHBOARD_WS", "❌ Error WebSocket: ${t.message}")
                isWebSocketConnected = false

                runOnUiThread {
                    updateConnectionStatus()
                    Toast.makeText(this@DashboardActivity, "⚠️ Conexión en tiempo real perdida", Toast.LENGTH_SHORT).show()
                }

                // ✅ RECONEXIÓN AUTOMÁTICA
                handler.postDelayed({
                    if (!isFinishing && !isWebSocketConnected) {
                        Log.d("DASHBOARD_WS", "🔄 Intentando reconectar...")
                        connectWebSocketRealTime()
                    }
                }, 5000)
            }
        }

        // ✅ USAR EL WEBSOCKET DE RETROFITCLIENT
        RetrofitClient.connectNotificationsWebSocket(webSocketListener)
    }

    // ✅ MOSTRAR NOTIFICACIONES EN TIEMPO REAL
    private fun showRealTimeNotification(mensaje: String, tipo: String) {
        val icon = when (tipo) {
            "alerta_sanitaria" -> "🚨"
            "recordatorio" -> "⏰"
            "informativa" -> "ℹ️"
            else -> "📢"
        }

        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "$icon $mensaje",
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )

        // Añadir acción según el tipo
        when (tipo) {
            "alerta_sanitaria" -> {
                snackbar.setAction("Ver Incidencias") {
                    startActivity(Intent(this, IncidentsActivity::class.java))
                }
                // Vibración para alertas críticas
                vibrate(500)
            }
            "recordatorio" -> {
                snackbar.setAction("Ver Calendario") {
                    startActivity(Intent(this, CalendarActivity::class.java))
                }
            }
        }

        snackbar.show()
    }

    // ✅ ACTUALIZACIÓN RÁPIDA SIN NOTIFICACIÓN MOLESTA
    private fun showQuickUpdate(message: String) {
        Log.d("DASHBOARD", "⚡ $message")
        // Solo mostrar un pequeño toast si es necesario
        // Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ✅ BROADCAST DEL SISTEMA
    private fun showSystemBroadcast(mensaje: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📢 Mensaje del Sistema")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    // ✅ ANIMACIÓN PARA TARJETAS ACTUALIZADAS
    private fun animateCardUpdate(card: android.view.View) {
        card.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .withEndAction {
                card.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
            }
    }

    // ✅ VIBRACIÓN PARA ALERTAS
    private fun vibrate(durationMs: Long) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w("DASHBOARD", "No se pudo vibrar: ${e.message}")
        }
    }

    // ✅ ACTUALIZAR ESTADO DE CONEXIÓN
    private fun updateConnectionStatus() {
        // Usar reflexión para acceder al campo de forma segura
        try {
            val statusField = binding.javaClass.getDeclaredField("textConnectionStatus")
            statusField.isAccessible = true
            val statusText = statusField.get(binding) as? android.widget.TextView

            statusText?.let { textView ->
                if (isWebSocketConnected) {
                    textView.text = "🟢 Conectado en tiempo real"
                    textView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    textView.text = "🔴 Modo sin conexión"
                    textView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                }
            }
        } catch (e: Exception) {
            Log.d("DASHBOARD", "Campo textConnectionStatus no encontrado en el layout")
        }
    }

    // ✅ REFRESH MANUAL
    private fun refreshAllData() {
        Log.d("DASHBOARD", "🔄 Refresh manual iniciado")

        // Deshabilitar FAB de forma segura
        try {
            val fabField = binding.javaClass.getDeclaredField("fabRefresh")
            fabField.isAccessible = true
            val fab = fabField.get(binding) as? com.google.android.material.floatingactionbutton.FloatingActionButton
            fab?.isEnabled = false
        } catch (e: Exception) {
            // FAB no existe, continuar
        }

        lifecycleScope.launch {
            try {
                loadStats()
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "✅ Datos actualizados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "❌ Error actualizando", Toast.LENGTH_SHORT).show()
                }
            } finally {
                runOnUiThread {
                    // Rehabilitar FAB de forma segura
                    try {
                        val fabField = binding.javaClass.getDeclaredField("fabRefresh")
                        fabField.isAccessible = true
                        val fab = fabField.get(binding) as? com.google.android.material.floatingactionbutton.FloatingActionButton
                        fab?.isEnabled = true
                    } catch (e: Exception) {
                        // FAB no existe, continuar
                    }
                }
            }
        }
    }

    // ✅ DEBUG WEBSOCKET
    private fun debugWebSocketStatus() {
        Log.d("DASHBOARD_DEBUG", "=== DEBUG WEBSOCKET ===")
        Log.d("DASHBOARD_DEBUG", "WebSocket conectado: $isWebSocketConnected")
        Log.d("DASHBOARD_DEBUG", "RetrofitClient inicializado: ${RetrofitClient.isLoggedIn()}")

        val debugInfo = RetrofitClient.debugStatus()
        Log.d("DASHBOARD_DEBUG", debugInfo)
    }

    // ✅ CARGAR ESTADÍSTICAS USANDO TUS UTILS
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
        }.onError { errorMessage ->
            Log.w("DASHBOARD", "Error cargando animales: $errorMessage")
            binding.textAnimalsCount.text = "0"
        }
    }

    private suspend fun loadIncidentsStats() {
        safeApiCall("LOAD_INCIDENTS_STATS") {
            apiService.getIncidencias()
        }.onSuccess { response ->
            response.body()?.let {
                val pending = it.results.count { inc -> inc.estado == "pendiente" }
                binding.textIncidentsCount.text = pending.toString()
            }
        }.onError { errorMessage ->
            Log.w("DASHBOARD", "Error cargando incidencias: $errorMessage")
            binding.textIncidentsCount.text = "0"
        }
    }

    private suspend fun loadTreatmentsStats() {
        safeApiCall("LOAD_TREATMENTS_STATS") {
            apiService.getTratamientos()
        }.onSuccess { response ->
            response.body()?.let {
                val recent = it.results.filter { tr ->
                    try {
                        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(tr.fecha)
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
        }.onError { errorMessage ->
            Log.w("DASHBOARD", "Error cargando tratamientos: $errorMessage")
            binding.textTreatmentsCount.text = "0"
        }
    }

    private suspend fun loadEventsStats() {
        safeApiCall("LOAD_EVENTS_STATS") {
            apiService.getEventos()
        }.onSuccess { response ->
            response.body()?.results?.let { eventos ->
                val upcoming = eventos.count {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
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
        }.onError { errorMessage ->
            Log.w("DASHBOARD", "Error cargando eventos: $errorMessage")
            binding.textEventsCount.text = "0"
        }
    }

    private suspend fun loadUsersStats() {
        safeApiCall("LOAD_USERS_STATS") {
            apiService.getUsers()
        }.onSuccess { response ->
            val users = response.body() ?: emptyList()
            binding.textUsersCount.text = users.count { it.activo == true }.toString()
        }.onError { errorMessage ->
            Log.w("DASHBOARD", "Error cargando usuarios: $errorMessage")
            binding.textUsersCount.text = "0"
        }
    }

    private fun showSettingsMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración Admin")
            .setItems(arrayOf(
                "👥 Gestionar usuarios",
                "🔄 Actualizar datos",
                "📊 Ver logs del sistema",
                "🔌 Estado WebSocket",
                "👤 Mi perfil",
                "📱 Información de la app",
                "🚪 Cerrar sesión"
            )) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, UsersActivity::class.java))
                    1 -> refreshAllData()
                    2 -> Toast.makeText(this, "📊 Logs - Próximamente", Toast.LENGTH_SHORT).show()
                    3 -> showWebSocketStatus()
                    4 -> Toast.makeText(this, "👤 Mi perfil - Próximamente", Toast.LENGTH_SHORT).show()
                    5 -> mostrarInfoApp()
                    6 -> logout()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showWebSocketStatus() {
        val status = if (isWebSocketConnected) "🟢 CONECTADO" else "🔴 DESCONECTADO"
        val debugInfo = RetrofitClient.debugStatus()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔌 Estado WebSocket")
            .setMessage("Estado: $status\n\n$debugInfo")
            .setPositiveButton("Reconectar") { _, _ ->
                connectWebSocketRealTime()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarInfoApp() {
        val mensaje = """
            📱 Genetics - Gestión Ganadera
            
            👑 PANEL ADMINISTRADOR
            
            🏢 Sistema integral de gestión de ganado
            
            ✨ Funcionalidades Admin:
            • 🐄 Gestión completa de animales
            • 🚨 Control de incidencias
            • 💊 Registro de tratamientos
            • 📅 Calendario de eventos
            • 👥 Administración de usuarios
            • 📊 Logs del sistema
            • 📡 Notificaciones en tiempo real
            
            📞 Soporte: genetics@example.com
            🌐 Web: www.genetics-app.com
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📱 Información de la App")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                Log.d("DASHBOARD", "🚪 Cerrando sesión admin...")

                // Desconectar WebSocket
                RetrofitClient.disconnectNotificationsWebSocket()

                // Limpiar token y datos
                RetrofitClient.clearToken()

                // Ir al login
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

        // ✅ VERIFICAR Y RECONECTAR WEBSOCKET SI ES NECESARIO
        if (!isWebSocketConnected) {
            Log.d("DASHBOARD", "🔄 Reconectando WebSocket en onResume")
            connectWebSocketRealTime()
        }

        // Recargar estadísticas
        lifecycleScope.launch {
            loadStats()
        }

        Log.d("DASHBOARD", "🔄 Dashboard resumed - WebSocket: $isWebSocketConnected")
    }

    override fun onPause() {
        super.onPause()
        // NO desconectar WebSocket en onPause para mantener notificaciones en background
        Log.d("DASHBOARD", "⏸️ Dashboard paused - manteniendo WebSocket activo")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("DASHBOARD", "🗑️ Destruyendo Dashboard - Desconectando WebSocket")

        // ✅ DESCONECTAR WEBSOCKET SOLO AL DESTRUIR
        RetrofitClient.disconnectNotificationsWebSocket()
        isWebSocketConnected = false

        // Limpiar handlers
        handler.removeCallbacksAndMessages(null)
    }
}